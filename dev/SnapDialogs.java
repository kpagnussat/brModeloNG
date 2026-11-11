import java.awt.Graphics2D;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Dev-only screenshot harness for the editor dialogs (companion of Snap): opens a conceptual
 * and a logical diagram, builds each editor exactly as the app does (parent frame, Inicie),
 * renders it off-screen at its own packed/designed size and prints that size.
 * Dialogs are realized (addNotify) but never shown, so modal ones do not block.
 *
 * Usage: ./gradlew snapDialogs -Pargs="<metal|gtk|flatclaro|flatescuro> <uiScale> <outDir> <label>
 *            <conceptual.brM3> <logical.brM3> [other.brM3 ...]"
 */
public class SnapDialogs {

    static void render(Window w, File out) throws Exception {
        w.addNotify();
        w.validate();
        // Paint only the Swing root; native window decorations are outside it.
        // Using the window size leaves an unpainted strip at the bottom.
        javax.swing.JRootPane root = ((RootPaneContainer) w).getRootPane();
        BufferedImage img = new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        root.paint(g);
        g.dispose();
        ImageIO.write(img, "png", out);
        System.out.println(out.getName() + " size=" + img.getWidth() + "x" + img.getHeight()
                + " pref=" + w.getPreferredSize().width + "x" + w.getPreferredSize().height);
    }

    /** Crop actual rendered fixtures/dialogs, never redraw or synthesize teaching examples. */
    static void exportHelpImages(File directory, String label) throws Exception {
        File help = new File(directory, "help"); help.mkdirs();
        String[][] copies = {
            {"main_conceitual", "janela"}, {"status_erro", "status"},
            {"campos", "campos"}, {"irfk", "ir-fk"}, {"irunique", "ir-unique"},
            {"codigo", "sql"}, {"impressao_conceitual", "impressao"}, {"partes", "partes"}
        };
        for (String[] item : copies) java.nio.file.Files.copy(
                new File(directory, label + "_" + item[0] + ".png").toPath(),
                new File(help, item[1] + ".png").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        crop(directory, label, "main_conceitual", "entidades-relacionamento", 445, 315, 845, 100);
        crop(directory, label, "main_conceitual", "atributos", 445, 195, 370, 205);
        crop(directory, label, "main_conceitual", "multivalorado", 445, 360, 350, 90);
        crop(directory, label, "main_conceitual", "especializacao", 650, 385, 180, 330);
        crop(directory, label, "main_conceitual", "associativa", 1100, 385, 215, 440);
        crop(directory, label, "main_conceitual", "paleta-conceitual", 1350, 105, 50, 630);
        crop(directory, label, "atributo_inspector", "atributo-inspector", 0, 0, 345, 530);
        crop(directory, label, "ligacao_inspector", "ligacao-inspector", 0, 0, 345, 480);
        crop(directory, label, "main_logico", "logico", 440, 165, 880, 665);
        for (String kind : new String[]{"fluxo", "atividade", "eap", "livre"})
            crop(directory, label, "main_" + kind, kind, 450, 190, 830, 590);
    }
    static void crop(File directory, String label, String source, String name, int x, int y, int w, int h) throws Exception {
        BufferedImage rendered = ImageIO.read(new File(directory, label + "_" + source + ".png"));
        ImageIO.write(rendered.getSubimage(x, y, w, h), "png", new File(new File(directory, "help"), name + ".png"));
    }

    static Object tabsForHelp(java.awt.Frame frame) throws Exception {
        var field = principal.FramePrincipal.class.getDeclaredField("TabInspector");
        field.setAccessible(true); return field.get(frame);
    }

    static void renderComponent(javax.swing.JComponent component, File out) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { component.paint(graphics); } finally { graphics.dispose(); }
        ImageIO.write(image, "png", out);
    }

    // Called on the EDT. Opening reads the file in a worker process and completes later on
    // the EDT, so events keep being dispatched here until it has.
    static controlador.Diagrama open(File f) throws Exception {
        controlador.Editor ed = principal.Aplicacao.fmPrincipal.getEditor();
        Method m = controlador.Editor.class.getDeclaredMethod("AbrirDiagramaFromFile", File.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        CompletableFuture<controlador.Diagrama> aberto = (CompletableFuture<controlador.Diagrama>) m.invoke(ed, f);
        SecondaryLoop espera = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        // exit goes through the queue, so it cannot run before enter has started pumping.
        aberto.whenComplete((d, e) -> SwingUtilities.invokeLater(espera::exit));
        if (!aberto.isDone()) espera.enter();
        controlador.Diagrama d = aberto.get(1, TimeUnit.MINUTES);
        if (d == null) throw new IllegalStateException("not opened: " + f);
        return ed.diagramaAtual;
    }

    // Any failure ends the process: the frame would otherwise keep the JVM, and a CI step, alive.
    public static void main(String[] a) {
        try {
            run(a);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    static void run(String[] a) throws Exception {
        String laf = a[0];
        float scale = Float.parseFloat(a[1]);
        System.setProperty("flatlaf.uiScale", a[1]);
        File dir = new File(a[2]);
        String label = a[3];
        File conc = new File(a[4]);
        File logi = new File(a[5]);
        dir.mkdirs();

        SwingUtilities.invokeAndWait(() -> {
            try {
                if (laf.equals("flatclaro")) {
                    com.formdev.flatlaf.FlatLightLaf.setup();
                } else if (laf.equals("flatescuro")) {
                    com.formdev.flatlaf.FlatDarkLaf.setup();
                } else if (laf.equals("flatintellij")) {
                    com.formdev.flatlaf.FlatIntelliJLaf.setup();
                } else if (laf.equals("flatdarcula")) {
                    com.formdev.flatlaf.FlatDarculaLaf.setup();
                } else {
                    UIManager.setLookAndFeel(laf.equals("metal") ? "javax.swing.plaf.metal.MetalLookAndFeel"
                            : "com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                }
                util.EstiloUI.iniciar();
                principal.FramePrincipal f = new principal.FramePrincipal();
                principal.Aplicacao.fmPrincipal = f;
                // Keep autosave's blinking status text out of deterministic screenshots.
                f.getEditor().setAutoSaveInterval(0);
                f.setSize(Math.round(1400 * scale), Math.round(1050 * scale));
                f.addNotify();
                f.validate();
                java.lang.reflect.Field tabsField = principal.FramePrincipal.class.getDeclaredField("TabInspector");
                tabsField.setAccessible(true);
                javax.swing.JTabbedPane tabs = (javax.swing.JTabbedPane)tabsField.get(f);
                System.out.println("Inspector font=" + tabs.getFont().getSize2D() + " width=" + tabs.getWidth());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(1500);

        SwingUtilities.invokeAndWait(() -> {
            try {
                java.awt.Frame fr = principal.Aplicacao.fmPrincipal;
                String p = dir.getPath() + "/" + label + "_";

                diagramas.conceitual.DiagramaConceitual dc = (diagramas.conceitual.DiagramaConceitual) open(conc);
                util.BrLogger.Clean();
                controlador.editores.EditorDeAtributos ea = new controlador.editores.EditorDeAtributos(fr, true);
                ea.Inicie(dc);
                render(ea, new File(p + "atributos.png"));
                var rowsField = controlador.editores.EditorDeAtributos.class.getDeclaredField("Principal");
                rowsField.setAccessible(true);
                javax.swing.JPanel rows = (javax.swing.JPanel)rowsField.get(ea);
                if (rows.getComponentCount() > 0) {
                    java.awt.Container row = (java.awt.Container)rows.getComponent(0);
                    for (java.awt.Component cell : row.getComponents()) if (cell instanceof javax.swing.JButton button) {
                        button.getModel().setRollover(true);
                        for (var listener : row.getMouseListeners()) listener.mouseEntered(new java.awt.event.MouseEvent(row, java.awt.event.MouseEvent.MOUSE_ENTERED, 0, 0, 1, 1, 0, false));
                        render(ea, new File(p + "atributos_hover.png"));
                        button.getModel().setRollover(false);
                    }
                }
                fr.validate();
                render(fr, new File(p + "main_conceitual.png"));
                // Help captures use only objects in the committed invented fixture.
                javax.swing.JTabbedPane helpInspector = (javax.swing.JTabbedPane) tabsForHelp(fr);
                var attrForHelp = dc.getListaDeItens().stream()
                        .filter(item -> item instanceof diagramas.conceitual.Atributo attribute && attribute.getTexto().equals("Nome"))
                        .findFirst().orElseThrow();
                dc.setSelecionado(attrForHelp);
                principal.Aplicacao.fmPrincipal.getEditor().PerformInspectorFor(attrForHelp);
                fr.validate();
                renderComponent(helpInspector, new File(p + "atributo_inspector.png"));
                var lineForHelp = dc.getListaDeItens().stream()
                        .filter(item -> item instanceof diagramas.conceitual.Ligacao line && line.LigaRelacaoEntidade())
                        .findFirst().orElseThrow();
                dc.setSelecionado(lineForHelp);
                principal.Aplicacao.fmPrincipal.getEditor().PerformInspectorFor(lineForHelp);
                fr.validate();
                renderComponent(helpInspector, new File(p + "ligacao_inspector.png"));
                dc.ClearSelect(false);


                // A deterministic example of the real unread-error indicator, without
                // depending on missing help files or other environment-specific errors.
                util.BrLogger.Clean();
                util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_FILE_BRM", "Exemplo: não foi possível abrir o arquivo");
                fr.validate();
                java.awt.Container status = principal.Aplicacao.fmPrincipal.getEditor().getLblStatus().getParent();
                BufferedImage statusImage = new BufferedImage(status.getWidth(), status.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D statusGraphics = statusImage.createGraphics();
                status.paint(statusGraphics);
                statusGraphics.dispose();
                ImageIO.write(statusImage, "png", new File(p + "status_erro.png"));
                util.BrLogger.Clean();

                diagramas.logico.DiagramaLogico dl = (diagramas.logico.DiagramaLogico) open(logi);
                // Select the first table so editors open on a populated one, as after a click.
                diagramas.logico.Tabela t = dl.getListaDeTabelas().get(0);
                dl.setSelecionado(t);

                controlador.editores.EditorDeCampos ec = new controlador.editores.EditorDeCampos(fr, true);
                ec.Inicie(dl);
                render(ec, new File(p + "campos.png"));
                controlador.editores.EditorDeIR ir = new controlador.editores.EditorDeIR(fr, true);
                ir.Inicie(dl);
                render(ir, new File(p + "ir.png"));
                controlador.editores.EditorDeIrFK fk = new controlador.editores.EditorDeIrFK(fr, true);
                // FK constraints live on Matrícula in this fixture.
                dl.setSelecionado(dl.getListaDeTabelas().get(2));
                fk.Inicie(dl);
                render(fk, new File(p + "irfk.png"));
                // Taller copy so the field rows below the header are visible too.
                fk.setSize(fk.getWidth(), fk.getHeight() + 500);
                render(fk, new File(p + "irfk_alto.png"));
                controlador.editores.EditorDeIrUnique un = new controlador.editores.EditorDeIrUnique(fr, true);
                dl.setSelecionado(t);
                un.Inicie(dl);
                render(un, new File(p + "irunique.png"));
                controlador.editores.EditorDeTipos tp = new controlador.editores.EditorDeTipos(fr, true);
                tp.Inicie(dl);
                render(tp, new File(p + "tipos_vazio.png"));
                var treeField = controlador.editores.EditorDeTipos.class.getDeclaredField("tree");
                treeField.setAccessible(true);
                ((javax.swing.JTree)treeField.get(tp)).setSelectionRow(1);
                render(tp, new File(p + "tipos.png"));

                // About uses its own content, independently of browser help.
                principal.FrameSobre sobre = new principal.FrameSobre(fr, true);
                Method ini = principal.FrameSobre.class.getDeclaredMethod("Inicie");
                ini.setAccessible(true);
                ini.invoke(sobre);
                render(sobre, new File(p + "sobre.png"));
                render(new principal.FormaLogs(fr, true), new File(p + "logs.png"));
                util.BrLogger.Logger("Controler.ERRO_SAME_FILE", "biblioteca.brM3");
                util.BrLogger.Logger("INFO_DIAGRAMA_ABERTO", "Diagrama de exemplo aberto");
                principal.FormaLogs populatedLogs = new principal.FormaLogs(fr, true);
                render(populatedLogs, new File(p + "logs_populado.png"));
                var tableField = principal.FormaLogs.class.getDeclaredField("tbPrincipal");
                tableField.setAccessible(true);
                ((javax.swing.JTable)tableField.get(populatedLogs)).setRowSelectionInterval(0, 0);
                render(populatedLogs, new File(p + "logs_selecionado.png"));
                util.BrLogger.Clean();
                controlador.conversor.conversorDialogo converter = new controlador.conversor.conversorDialogo(fr, true);
                controlador.conversor.conversorOpcoes options = new controlador.conversor.conversorOpcoes();
                options.Textos.add("Converter atributos multivalorados");
                options.Questoes.add("Criar uma tabela para cada atributo multivalorado");
                options.Questoes.add("Manter os atributos na tabela de origem");
                options.Observacoes.add("A conversão preserva o diagrama conceitual de origem.");
                converter.Inicializar(dc, dl, options, null);
                render(converter, new File(p + "conversor.png"));

                // Remaining windows, rendered as constructed (no extra setup), one failure never
                // stops the rest.
                java.util.Map<String, java.util.concurrent.Callable<Window>> resto = new java.util.LinkedHashMap<>();
                resto.put("cli", () -> new principal.cli.FormCli(fr, true));
                resto.put("drawer", () -> {
                    controlador.editores.DrawerEditor drawer = new controlador.editores.DrawerEditor(fr, true);
                    desenho.preDiagrama.baseDrawer data = new desenho.preDiagrama.baseDrawer(dc, "Biblioteca Aurora");
                    data.AddItem();
                    data.AddItem().setTipo(desenho.preDiagrama.baseDrawerItem.tipoDrawer.tpElipse);
                    drawer.Inicie(data);
                    var field = controlador.editores.DrawerEditor.class.getDeclaredField("Lista");
                    field.setAccessible(true);
                    ((javax.swing.JList<?>)field.get(drawer)).setSelectedIndex(0);
                    return drawer;
                });
                resto.put("texto", () -> {
                    controlador.editores.EditorTexto text = new controlador.editores.EditorTexto(fr, true);
                    text.setTexto("id_aluno INTEGER\nemail VARCHAR(120)\n\nCampos do exemplo Biblioteca Aurora");
                    return text;
                });
                resto.put("codigo", () -> {
                    controlador.editores.MostradorDeCodigo code = new controlador.editores.MostradorDeCodigo(fr, true);
                    code.setDbModel(dl.getDataModel());
                    code.setTexto("CREATE TABLE Aluno (\n  id_aluno INTEGER PRIMARY KEY,\n  email VARCHAR(120) UNIQUE\n);\n");
                    return code;
                });
                resto.put("eap", () -> new diagramas.eap.EapFormManual(fr, true));
                resto.put("salvar", () -> {
                    principal.Salvar save = new principal.Salvar(fr, true);
                    boolean changed = dc.getMudou();
                    dc.setMudou(true);
                    save.Carregue(principal.Aplicacao.fmPrincipal.getEditor());
                    dc.setMudou(changed);
                    return save;
                });
                resto.put("impressao", () -> {
                    principal.fmImpressao fi = new principal.fmImpressao(fr, true);
                    fi.setDiagrama(dl);
                    return fi;
                });
                resto.put("preview", () -> {
                    controlador.Impressor printer = new controlador.Impressor();
                    printer.setDiagrama(dl);
                    principal.fmImpressaoPreview preview = new principal.fmImpressaoPreview(fr, true);
                    preview.Inicie(printer);
                    return preview;
                });
                resto.put("atualizar", () -> {
                    principal.FormAtualizar update = new principal.FormAtualizar();
                    // Populate the version labels without an external update-server request.
                    for (String name : new String[]{"lblVersao", "lblNovaVersao"}) {
                        var field = principal.FormAtualizar.class.getDeclaredField(name);
                        field.setAccessible(true);
                        ((javax.swing.JLabel)field.get(update)).setText(principal.Aplicacao.VERSAO_A + "." + principal.Aplicacao.VERSAO_B + "." + principal.Aplicacao.VERSAO_C);
                    }
                    var field = principal.FormAtualizar.class.getDeclaredField("lblLink");
                    field.setAccessible(true);
                    ((javax.swing.JLabel)field.get(update)).setText("Versão já atualizada!");
                    return update;
                });
                resto.put("fonte", () -> {
                    util.JFontChooser chooser = new util.JFontChooser();
                    Method method = util.JFontChooser.class.getDeclaredMethod("createDialog", java.awt.Component.class);
                    method.setAccessible(true);
                    return (Window) method.invoke(chooser, fr);
                });
                resto.put("legenda", () -> {
                    controlador.editores.LegendaEditor legend = new controlador.editores.LegendaEditor();
                    desenho.formas.Legenda data = new desenho.formas.Legenda(dc);
                    data.addLegenda("Entidades", javax.swing.UIManager.getColor("Component.accentColor"));
                    data.addLegenda("Relacionamentos", javax.swing.UIManager.getColor("Label.foreground"));
                    legend.Init(data);
                    return legend;
                });
                resto.put("partes_vazio", () -> new partepronta.FormPartes());
                resto.put("partes", () -> {
                    partepronta.FormPartes parts = new partepronta.FormPartes();
                    parts.externalSalvar = new javax.swing.JMenuItem("Salvar partes");
                    var selection = dc.getSelecionado();
                    dc.setSelecionado(dc.getListaDeItens().stream().filter(item -> item instanceof diagramas.conceitual.Entidade).findFirst().orElseThrow());
                    parts.NovoBotao("Biblioteca Aurora", dc);
                    dc.ClearSelect(false);
                    if (selection != null) dc.setSelecionado(selection);
                    return parts;
                });
                resto.put("executor", () -> {
                    util.DlgExecutor executor = new util.DlgExecutor(fr, true);
                    executor.setTitle("brModelo NG: Executar comandos");
                    executor.Texto.setText("# Biblioteca Aurora\n# Digite os comandos a executar");
                    return executor;
                });
                int failures = 0;
                for (java.util.Map.Entry<String, java.util.concurrent.Callable<Window>> e : resto.entrySet()) {
                    try {
                        render(e.getValue().call(), new File(p + e.getKey() + ".png"));
                    } catch (Throwable falha) {
                        System.out.println(e.getKey() + " FAILED " + falha);
                        falha.printStackTrace();
                        failures++;
                    }
                }

                if (failures > 0) throw new IllegalStateException(failures + " dialog screenshots failed");
                dl.ClearSelect(false);
                fr.validate();
                render(fr, new File(p + "main_logico.png"));
                // Optional diagrams use the same file-open and main-window paint paths.
                for (int i = 6; i < a.length; i++) {
                    File extra = new File(a[i]);
                    open(extra);
                    fr.validate();
                    String name = extra.getName().replaceFirst("\\.brM3$", "");
                    render(fr, new File(p + "main_" + name + ".png"));
                }
                // Back to the conceptual tab after the print preview painted it scaled down.
                principal.fmImpressao fc = new principal.fmImpressao(fr, true);
                fc.setDiagrama(dc);
                render(fc, new File(p + "impressao_conceitual.png"));
                principal.Aplicacao.fmPrincipal.getEditor().setSelected(dc);
                fr.validate();
                render(fr, new File(p + "main_conceitual_pos.png"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        if (scale == 1f && a.length >= 10) exportHelpImages(dir, label);
        if (laf.startsWith("flat") && scale == 1f) {
            SwingUtilities.invokeAndWait(() -> util.TemaAplicacao.aplicar("claro"));
            SwingUtilities.invokeAndWait(() -> {
                try { render(principal.Aplicacao.fmPrincipal, new File(dir, label + "_live_claro.png")); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            SwingUtilities.invokeAndWait(() -> util.TemaAplicacao.aplicar("escuro"));
            SwingUtilities.invokeAndWait(() -> {
                try { render(principal.Aplicacao.fmPrincipal, new File(dir, label + "_live_escuro.png")); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
        }
        System.exit(0);
    }
}
