package controlador;

import controlador.apoios.GuardaPadraoBrM;
import controlador.apoios.InfoDiagrama;
import desenho.FormaElementar;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import util.XMLGenerate;
import controlador.Diagrama.TipoDeDiagrama;
/** Reads and writes diagram streams, envelopes, files and XML without owning model state. */
final class PersistenciaDiagrama {
    private PersistenciaDiagrama() {}

    static synchronized ByteArrayOutputStream SaveToStream(Diagrama othis) {
        try {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            try (ObjectOutput out = new ObjectOutputStream(ba)) {
                // Deep stack: the recursive walk outgrows the EDT on large, interlinked diagrams.
                util.PilhaProfunda.escrever(out, othis);
            }
            return ba;
        } catch (IOException iOException) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_SAVELOAD_STREAM", iOException.getMessage());
            return null;
        }
    }

    static TipoDeDiagrama GetTipoOnXml(Document doc) {
        try {
            NodeList nodeLst = doc.getElementsByTagName(Diagrama.nodePrincipal);
            Element prin = (Element) nodeLst.item(0);
            String tp = prin.getAttribute("TIPO");
            return TipoDeDiagrama.valueOf(tp);
        } catch (Exception e) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_TIPO_XML", e.getMessage());
            return null;
        }
    }

    static synchronized Diagrama LoadFromStream(ByteArrayOutputStream ba) {
        try {
            byte[] bytes = ba.toByteArray();
            try (ObjectInputStream in = new util.LeitorSeguro(new ByteArrayInputStream(bytes))) {
                return (Diagrama) util.PilhaProfunda.ler(in);
            }
        } catch (ClassCastException | ClassNotFoundException | IOException e) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD", e.getMessage());
            return null;
        }
    }

    static synchronized Diagrama LoadFromFile(File arq, Editor master) {
        if (arq == null || master.IsOpen(arq)) {
            return null;
        }
        Diagrama res = null;

        String onome = nomeSemExtensao(arq);

        if (util.Arquivo.IsModelo(arq)) {
            try {
                return prepararModelo(lerModelo(arq), arq, master);
            } catch (ClassCastException | NullPointerException | IOException iOException) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_FILE_BRM", iOException.getMessage());
                return null;
            }
        } else {
            Document doc = util.XMLGenerate.LoadDocument(arq);
            if (doc == null) {
                return null;
            }
            TipoDeDiagrama tp = GetTipoOnXml(doc);
            res = master.Novo(tp);
            if (!res.LoadFromXML(doc, false)) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_FILE_XML", "[IS BRM XML?]");
            }
            //Recria o UID para se ter a certeza de que ele é único, não repetido por uma eventual cópaia do arquivo.
            res.ReGeraUniversalUnicID();
            res.setArquivo(arq.getAbsolutePath());
            res.SetNome(onome);
            master.addLastOpened(arq.getAbsolutePath());
            return res;
        }
    }

    /**
     * Reads and decodes a .brM3 or .brMj file. Decoding runs in the import worker, which may
     * take up to its deadline on a hostile file, so this takes no lock and touches no editor
     * state: AberturaEmSegundoPlano calls it off the EDT.
     */
    static Diagrama lerModelo(File arq) throws IOException {
        boolean brMj = util.Arquivo.IsbrMj(arq);
        byte[] bytes = util.LimitesEntrada.ler(arq, brMj ? util.LimitesEntrada.JSON_BYTES : util.LimitesEntrada.BINARIO_BYTES);
        // Decoded by a separate worker process: the file may come from anyone.
        return util.TrabalhadorImportacao.abrir(bytes, brMj);
    }

    /** On the EDT: attaches a diagram read by {@link #lerModelo} to the editor as that file. */
    static Diagrama prepararModelo(Diagrama res, File arq, Editor master) {
        res.setMaster(master);
        //Recria o UID para se ter a certeza de que ele é único, não repetido por uma eventual cópaia do arquivo.
        res.ReGeraUniversalUnicID();
        res.setArquivo(arq.getAbsolutePath());
        master.addLastOpened(arq.getAbsolutePath());
        res.SetNome(nomeSemExtensao(arq));
        return res;
    }

    private static String nomeSemExtensao(File arq) {
        String onome = arq.getName();
        int dot = onome.lastIndexOf('.');
        return dot > 0 ? onome.substring(0, dot) : onome;
    }

    static synchronized Diagrama LoadFromBrm(GuardaPadraoBrM seguranca, Editor master) {
        if (seguranca == null) {
            return null;
        }
        Diagrama res = seguranca.getDiagrama();
        if (res == null) {
            return null;
        }
        res.setMaster(master);
        //Recria o UID para se ter a certeza de que ele é único, não repetido por uma eventual cópaia do arquivo.
        res.ReGeraUniversalUnicID();
        res.setArquivo("");
        //O nome está aramzendo no TAG para facilitar a identificação.
        res.SetNome(seguranca.Tag);
        return res;
    }

    static boolean LoadFromXML(Diagrama alvo, Document doc, boolean colando) {

        HashMap<Element, FormaElementar> link = new HashMap<>();

        try {

            //<editor-fold defaultstate="collapsed" desc="Remover espaços - http://stackoverflow.com/questions/978810/how-to-strip-whitespace-only-text-nodes-from-a-dom-before-serialization">
            XPathFactory xpathFactory = XPathFactory.newInstance();
            // XPath to find empty text nodes.
            XPathExpression xpathExp = xpathFactory.newXPath().compile(
                    "//text()[normalize-space(.) = '']");
            NodeList emptyTextNodes = (NodeList) xpathExp.evaluate(doc, XPathConstants.NODESET);

            // Remove each empty text node from document.
            for (int i = 0; i < emptyTextNodes.getLength(); i++) {
                Node emptyTextNode = emptyTextNodes.item(i);
                emptyTextNode.getParentNode().removeChild(emptyTextNode);
            }
            //</editor-fold>

            NodeList nodeLst = doc.getElementsByTagName(Diagrama.nodePrincipal);
            Node mer = nodeLst.item(0);
            nodeLst = mer.getChildNodes();

            if (colando) {
                alvo.ClearSelect(true);
                ((InfoDiagrama) alvo.infoDiagrama).setDiagramaOldUniversalUnicID(String.valueOf(((Element) mer).getAttribute("UniversalUnicID")));
            }

            alvo.isLoadCreate = true;
            alvo.isCarregando = true;
            int tl = 0;
            int maxID = 0;
            for (int s = 0; s < nodeLst.getLength(); s++) {
                Node fstNode = nodeLst.item(s);
                if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element fstElmnt = (Element) fstNode;
                    FormaElementar res = alvo.runCriadorFromXml(fstElmnt, colando);
                    if (res == null) {
                        util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD", "Lixo ou objeto alienígena encontrado: " + fstElmnt.toString() + " - não é possível colar este objeto", "[]");
                        continue;
                    }
                    tl++;
                    if (!colando) {
                        maxID = Math.max(maxID, res.getID());
                    }
                    link.put(fstElmnt, res);
                    maxID = alvo.OnLoadingXMLitem(res, fstElmnt, colando, maxID, link);
                }
            }
            if (!colando) {
                alvo.TotalID = maxID;
            }
            alvo.isLoadCreate = false;

            link.keySet().stream().forEach((el) -> {
                FormaElementar proc = link.get(el);
                proc.CommitXML(el, link);
            });
            alvo.isCarregando = false;

            if (colando) {
                if (((Element) mer).hasAttribute("FIRST_SEL")) {
                    alvo.ReestrutureSelecao(((Element) mer).getAttribute("FIRST_SEL"), link);
                }
            }
            if (tl > 0 && colando) {
                alvo.DoMuda(null);
            }
            alvo.PerformInspector();

        } catch (Exception e) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD", e.getMessage());
            alvo.isLoadCreate = false;
            alvo.isCarregando = false;
            return false;
        }
        alvo.repaint();
        return true;
    }

    static FormaElementar ReflectionObj(Diagrama alvo, Class classeDoObj) {
        Class[] argsConstr = new Class[]{Diagrama.class};
        Object[] omodelo = new Object[]{alvo};
        Constructor construtor;
        FormaElementar res;
        try {
            construtor = classeDoObj.getConstructor(argsConstr);
            try {
                res = (FormaElementar) construtor.newInstance(omodelo);
                return res;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_REFLECTION_CREATE_OBJ", e.getMessage());
            }
        } catch (NoSuchMethodException e) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_REFLECTION_CONSTRUCTOR", e.getMessage());
        }
        return null;
    }

    static FormaElementar runCriadorFromXml(Diagrama alvo, Element xml, boolean colando) {
        int classe = xml.getNodeName().hashCode();

        if (classe == InfoDiagrama.class.getSimpleName().hashCode()) {
            if (!colando) {
                alvo.LoadFromXML(alvo.infoDiagrama, xml, false);
            }
            return alvo.infoDiagrama;
        }

        final Class[] classes = alvo.getCassesDoDiagrama();

        for (Class cl : classes) {
            if (cl.getSimpleName().hashCode() == classe) {
                FormaElementar res = alvo.ReflectionObj(cl);
                if (res != null) {
                    alvo.LoadFromXML(res, xml, colando);
                }
                return res;
            }
        }
        return null;
    }

    static void LoadFromXML(Diagrama alvo, FormaElementar obj, Element xml, boolean colando) {
        try {
            obj.LoadFromXML(xml, colando);
        } catch (Exception e) {
            util.BrLogger.Logger("ERROR_OBJECT_LOAD", Integer.toString(obj.getID()) + " - " + obj.getClass().getSimpleName(), e.getMessage());
        }
    }

    static String SaveToXml(Diagrama othis, boolean justSel) {
        return XMLGenerate.GeraXMLFrom(othis, justSel);
    }

    static boolean Salvar(Diagrama alvo, File fileName, boolean pergunta) {
        if (fileName.exists() && pergunta) {
            if (util.Dialogos.ShowMessageConfirm(alvo.master.getRootPane(), Editor.fromConfiguracao.getValor("Controler.MSG_QUESTION_REWRITE")) != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        String txt = alvo.getNome();
        String onome = fileName.getName();
        alvo.versaoA = Diagrama.VERSAO_A;
        alvo.versaoB = Diagrama.VERSAO_B;
        alvo.versaoC = Diagrama.VERSAO_C;
        int dot = onome.lastIndexOf('.');
        if (dot > 0) onome = onome.substring(0, dot);
        alvo.setNome(onome);

        if (util.Arquivo.IsModelo(fileName)) {
            try {
                if (util.Arquivo.IsbrMj(fileName)) {
                    // Persist the saved state, so a second Save produces the same JSON bytes.
                    boolean dirty = alvo.getMudou();
                    alvo.setMudou(false);
                    try {
                        util.FormatoBrMj.salvar(fileName.toPath(), alvo);
                    } catch (IOException e) {
                        alvo.setMudou(dirty);
                        throw e;
                    }
                } else {
                    util.GravacaoAtomica.gravar(fileName.toPath(), saida -> {
                        try (ObjectOutput out = new ObjectOutputStream(saida)) {
                            alvo.setArquivo("");
                            GuardaPadraoBrM seg = new GuardaPadraoBrM(alvo);
                            seg.versaoDiagrama = alvo.versaoA + "." + alvo.versaoB + "." + alvo.versaoC;
                            out.writeObject(seg);
                        }
                    });
                }
                alvo.setArquivo(fileName.getAbsolutePath());
                alvo.master.addLastOpened(fileName.getAbsolutePath());

                alvo.setMudou(false);
                alvo.master.DoAutoSaveCompleto();
                alvo.PerformInspector();
                return true;
            } catch (IOException iOException) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_SAVE_BRM", iOException.getMessage());
                alvo.setNome(txt);
                return false;
            }
        } else {
            try {
                StringWriter ou = XMLGenerate.GeraXMLtoSaveFrom(alvo, false);
                util.GravacaoAtomica.gravar(fileName.toPath(), saida -> {
                    try (Writer out = new OutputStreamWriter(saida)) {
                        out.write(ou.getBuffer().toString());
                    }
                });
                alvo.setArquivo(fileName.getAbsolutePath());
                alvo.master.addLastOpened(fileName.getAbsolutePath());

                alvo.setMudou(false);
                alvo.master.DoAutoSaveCompleto();
                alvo.PerformInspector();
                return true;
            } catch (IOException iOException) {
                util.BrLogger.Logger("ERROR_DIAGRAMA_SAVE_XML", iOException.getMessage());
                alvo.setNome(txt);
                return false;
            }
        }
    }

    static boolean AutoSalvar(Diagrama alvo, ArrayList<byte[]> as) {
        alvo.versaoA = Diagrama.VERSAO_A;
        alvo.versaoB = Diagrama.VERSAO_B;
        alvo.versaoC = Diagrama.VERSAO_C;
        try {
            ByteArrayOutputStream fo = new ByteArrayOutputStream();
            try (ObjectOutput out = new ObjectOutputStream(fo)) {
                GuardaPadraoBrM seg = new GuardaPadraoBrM(alvo);
                seg.versaoDiagrama = alvo.versaoA + "." + alvo.versaoB + "." + alvo.versaoC;
                seg.Tag = alvo.getNome();
                out.writeObject(seg);
            }
            as.add(fo.toByteArray());
            return true;
        } catch (IOException iOException) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_AUTOSAVE_MEM", iOException.getMessage());
            return false;
        }
    }
}
