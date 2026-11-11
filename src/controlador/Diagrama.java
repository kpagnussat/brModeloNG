/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador;

import controlador.apoios.GuardaPadraoBrM;
import controlador.apoios.InfoDiagrama;
import controlador.apoios.TreeItem;
import controlador.inspector.InspectorItemBase;
import controlador.inspector.InspectorItemExtender;
import controlador.inspector.InspectorProperty;
import desenho.Ancorador;
import desenho.Elementar;
import desenho.FormaElementar;
import desenho.formas.Desenhador;
import desenho.formas.Forma;
import desenho.formas.FormaTextoBase.AlinhamentoTexto;
import desenho.formas.Legenda;
import desenho.linhas.SuperLinha;
import desenho.preAnyDiagrama.PreTexto.TipoTexto;
import diagramas.conceitual.Texto;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import util.BoxingJava;
import util.Utilidades;

/**
 *
 * @author Rick
 */
public class Diagrama implements Serializable, ClipboardOwner {

    private static final long serialVersionUID = 21212121212121L;

    /**
     * Classes existentes.
     */
    private final Class[] classesDoDiagrama = new Class[]{};

    public Class[] getCassesDoDiagrama() {
        return classesDoDiagrama;
    }
    // <editor-fold defaultstate="collapsed" desc="Campos">
    protected transient Editor master;
    private int baseRecuo = 3;
    private boolean draging = false;
    protected Color pontoCor = Color.BLACK;
    protected Color pontoCorMultSel = Color.GREEN;
    protected FormaElementar infoDiagrama = null;
    public static final String VERSAO_A = "3";
    public static final String VERSAO_B = "2";
    public static final String VERSAO_C = "0";
    protected String versaoA = Diagrama.VERSAO_A;
    protected String versaoB = Diagrama.VERSAO_B;
    protected String versaoC = Diagrama.VERSAO_C;
    private TipoDeDiagrama tipo = TipoDeDiagrama.tpConceitual;
    transient private String nome;
    transient private String Arquivo = "";
    //mostra uma grade no modelo.
    private Font font;
    private Color foreColor = Elementar.defaultColor;
    private int heigth = 4096;
    private int width = 4096;
    private double zoom = 1.0;
    private Ancorador superAncorador = null;

    public Cursor getCursor() {
        return this.master.getBox().getCursor();
    }

    public void setCursor(Cursor cursor) {
        if (getComando() != null) {
            this.master.getBox().setCursor(getEditor().getControler().MakeCursor(getComando()));
            return;
        }
        this.master.getBox().setCursor(cursor);
    }

    /**
     * @return the pontoWidth
     */
    public int getPontoWidth() {
        return master.getBox().getPontoWidth();
    }

    /**
     * @return the pontoHeigth
     */
    public int getPontoHeigth() {
        return master.getBox().getPontoHeigth();
    }

    public Color getBackground() {
        return master.getBox().getBackground();
    }

    public Color getForeColor() {
        return foreColor;
    }

    public void setForeColor(Color foreColor) {
        this.foreColor = foreColor;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public int getHeight() {
        return heigth;
    }

    public int getWidth() {
        return width;
    }
    /**
     * Ao verificar-se o total de itens em um Diagrama, para saber se algum foi incluído, é preciso exluir do total aqueles que fazem parte do próprio objto. Até então apenas o InfoDiagrama.
     */
    public static final int totalInicialDeItens = 1;

    public String getArquivo() {
        return Arquivo;
    }

    public void setArquivo(String Arquivo) {
        this.Arquivo = Arquivo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
        if (master != null) {
            master.RePopuleBarraDiagramas(false);
        }
    }

    public void SetNome(String nome) {
        this.nome = nome;
    }

    public TipoDeDiagrama getTipo() {
        return tipo;
    }

    protected void setTipo(TipoDeDiagrama tipo) {
        this.tipo = tipo;
    }

    public String getTipoDeDiagramaFormatado() {
        return Editor.fromConfiguracao.getValor("Inspector.lst.tipodiagrama." + getTipo().name().substring(2).toLowerCase());
    }

    public String getVersao() {
        return versaoA + "." + versaoB + "." + versaoC;
    }

    public boolean isMostrarInfoDiagrama() {
        return infoDiagrama.isVisible();
    }

    public void setMostrarInfoDiagrama(boolean mostrarInfoDiagrama) {
        this.infoDiagrama.setVisible(mostrarInfoDiagrama);
    }

    public Editor getEditor() {
        return master;
    }

    public Color getPontoCorMultSel() {
        return pontoCorMultSel;
    }

    public void setPontoCorMultSel(Color pc) {
        this.pontoCorMultSel = pc;
    }

    public Color getPontoCor() {
        return pontoCor;
    }

    public void setPontoCor(Color pontoCor) {
        this.pontoCor = pontoCor;
    }

    /**
     * @return the editorBackColor
     */
    public final Color getEditorBackColor() {
        return master.getBox().getBackground();
    }

    /**
     * @return the baseRecuo
     */
    public int getBaseRecuo() {
        return baseRecuo;
    }

    /**
     * @param baseRecuo the baseRecuo to set
     */
    public void setBaseRecuo(int baseRecuo) {
        this.baseRecuo = baseRecuo;
    }
    protected ArrayList<Elementar> subItens = new ArrayList<>();

    public ArrayList<Elementar> getSubItens() {
        return subItens;
    }
    protected ArrayList<FormaElementar> ListaDeItens = new ArrayList<>();

    public ArrayList<FormaElementar> getListaDeItens() {
        return ListaDeItens;
    }
    ArrayList<FormaElementar> itensSelecionados = new ArrayList<>();

    public ArrayList<FormaElementar> getItensSelecionados() {
        return itensSelecionados;
    }
    // </editor-fold>

    public Diagrama(Editor omaster) {
        super();
        master = omaster;
        font = Elementar.CloneFont(this.master.getBox().getFont());
        isLoadCreate = true;
        infoDiagrama = new InfoDiagrama(this);
        infoDiagrama.SetBounds(5, 5, 300, 150);
        isLoadCreate = false;
        heigth = master.getBox().EditorMaxHeigth;
        width = master.getBox().EditorMaxWidth;

        meusComandos.add(Controler.Comandos.cmdDesenhador.name());
        meusComandos.add(Controler.Comandos.cmdLegenda.name());
        meusComandos.add(Controler.Comandos.cmdTexto.name());
        meusComandos.add(Controler.Comandos.cmdApagar.name());
        nome = "";

        superAncorador = new Ancorador(this);
        superAncorador.SetBounds(50, 50, 18, 36);
    }

    public final ArrayList<String> meusComandos = new ArrayList<>();

    public void setMaster(Editor master) {
        this.master = master;
    }
    int _tick = 0;

    private void tick() {
        _tick++;
    }

    /**
     * @return the zoom
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * @param zoom the zoom to set
     */
    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    private void PinteGrade(Graphics2D g) {
        PinturaDiagrama.PinteGrade(this, g);
    }

    /**
     * Mostra a área de impressão no diagrama
     *
     * @param g
     * @param wdt largura
     * @param ht altura
     */
    public void PaintAI(Graphics2D g, int wdt, int ht) {
        PinturaDiagrama.PaintAI(this, g, wdt, ht);
    }

    /**
     * Não deve ser salvo. Trata-se de uma lista de elementos visíveis para criar uma navegação
     */
    private transient TreeItem TreeNavegacao = null;

    /**
     * Atualiza uma lista de elementos visíveis para criar uma navegação.
     *
     * @param atualisar a lista: (sim | não)
     * @return
     */
    public TreeItem AtualizeTreeNavegacao(boolean atualisar) {
        if (atualisar || TreeNavegacao == null) {
            TreeNavegacao = new TreeItem(getNomeFormatado());
            // The selection rectangle lives among the items while it is dragged; it is not an
            // object of the diagram and has no icon (issue #31 of the original brModelo).
            getListaDeItens().stream().filter((it) -> (it instanceof Forma && !(it instanceof Selecionador))).map(it -> (Forma) it).forEach((it) -> {
                it.MostreSeParaExibicao(TreeNavegacao);
            });
        }
        return TreeNavegacao;
    }

    /**
     * Trata-se de uma lista de elementos visíveis para criar uma navegação
     *
     * @return [lista de elementos]
     */
    public TreeItem getTreeNavegacao() {
        return AtualizeTreeNavegacao(false);
    }

    public void SelecioneByID(int i, boolean mostrar) {
        FormaElementar e = FindByID(i);
        if (e == null) {
            master.AtualizeTreeNavegacao();
            return;
        }
        DiagramaDoSelecao(e, false, false);
        if (mostrar) {
            master.Mostre(e.getLocation());
        }
    }

    /**
     * Quando InfoDigrama gera as propriedades ele as informa ao modelo, possibilitando a inclusão de algum comando.
     *
     * @param res propriedades ainda não preenchidas
     */
    public void BeginProperty(ArrayList<InspectorProperty> res) {

    }

    /**
     * Quando InfoDigrama gera as propriedades ele as informa ao modelo, possibilitando a inclusão de algum comando.
     *
     * @param res propriedades já preenchidas
     */
    public void EndProperty(ArrayList<InspectorProperty> res) {

    }

    /**
     * Quando InfoDigrama roda o DoAnyThing() ela chama este método no Diagrama,
     *
     * @param Tag - vem do DoAnyThing do InfoDiagrama
     */
    public void DoAnyThing(int Tag) {

    }

    /**
     * Insere comandos no submenu "menuCMD" (Comandos) do menu diagrama. O menu "item" é um submenu a ser populado.
     *
     * @param item Implementado, como exemplo em DiagramaEap
     */
    public void populeComandos(JMenuItem item) {
        item.setEnabled(false);
    }

    /**
     * Roda um comando criado pelo AcaoDiagrama.
     *
     * @param comm qualquer comando em formato string. Implementado, como exemplo em DiagramaEap
     */
    public void rodaComando(String comm) {

    }

    public int getID() {
        return infoDiagrama.getID();
    }

    public String getUniversalUnicID() {
        return ((InfoDiagrama) infoDiagrama).getDiagramaUniversalUnicID();
    }

    public final void ReGeraUniversalUnicID() {
        ((InfoDiagrama) infoDiagrama).ReGeraGuardaUnicDiagramaID();
    }

    public String getNomeFormatado() {
        return getNome().isEmpty() ? "<<" + getTipoDeDiagramaFormatado() + ">>" : getNome();
    }

    /**
     * Método para setar um valor String diretamente no diagrama, disparado pelo InfoDiagrama
     *
     * @param str
     * @param tag
     */
    public void setFromString(String str, int tag) {
        //# talvez implantar o setFromInt e etc.
    }

    /**
     * Rodado após o carregamento do diagrama a aprtir de um arquivo. será ultil no futuro para setar propriedades default em novas versões dos diagramas a partir das versões do brMOdelo. roda dentro do método: ProcessePosOpen(Diagrama res) na classe Editor
     */
    public void OnAfterLoad(boolean isXml) {

    }

    public enum TipoDeDiagrama {
        tpConceitual, tpLogico, tpFluxo, tpAtividade, tpEap, tpLivre
    }

    public boolean isAlterado() {
        return (this.getListaDeItens().size() > Diagrama.totalInicialDeItens
                || this.getMudou() || !("".equals(this.getArquivo())));
    }

    // <editor-fold defaultstate="collapsed" desc="Eventos">
    public void ProcessPaint(Graphics2D Canvas) {

        double z = master.getBox().getZoom();
        Canvas.scale(z, z);
        if (master.isShowGrid()) {
            PinteGrade(Canvas);
        }
        if (master.getBox().isMostrarAreaImpressao()) {
            PaintAI(Canvas, master.getBox().areaImpressaoWidth, master.getBox().areaImpressaoHeigth);
        }

        for (int i = subItens.size() - 1; i > -1; i--) {
            Elementar e = subItens.get(i);
            if (e.CanPaint()) {
                e.DoPaint(Canvas);
            }
        }

        superAncorador.DoPaint(Canvas);
    }

    private transient boolean pinturaExterna = false;

    /**
     * True while the diagram is painted onto another surface (print preview, image export).
     * Shapes must not resize themselves to that surface's font metrics then: the preview is
     * scaled down, its rounded metrics differ, and the document would come back altered.
     */
    public boolean isPinturaExterna() {
        return pinturaExterna;
    }

    public void ExternalPaint(Graphics g) {
        pinturaExterna = true;
        try {
            PinturaDiagrama.ExternalPaint(this, g);
        } finally {
            pinturaExterna = false;
        }
    }

    public void ExternalPaintSelecao(Graphics g) {
        pinturaExterna = true;
        try {
            PinturaDiagrama.ExternalPaintSelecao(this, g);
        } finally {
            pinturaExterna = false;
        }
    }

    /**
     * Máxima região pintada X e Y
     *
     * @return Point
     */
    public Point getPontoExtremo() {
        final int borda = 4;
        int rX = 0;
        int rY = 0;
        for (FormaElementar el : ListaDeItens) {
            rX = Math.max(rX, el.getLeftWidth());
            rY = Math.max(rY, el.getTopHeight());
        }
        if (rX > 0) {
            rX += borda;
        }
        if (rY > 0) {
            rY += borda;
        }
        return new Point(rX, rY);
    }

    /**
     * Máxima região pintada dentre os selecionados. X e Y
     *
     * @return Point
     */
    public Point getPontoExtremoSelecionado() {
        final int borda = 4;
        int rX = 0;
        int rY = 0;
        for (FormaElementar el : getItensSelecionados()) {
            rX = Math.max(rX, el.getLeftWidth());
            rY = Math.max(rY, el.getTopHeight());
        }
        if (rX > 0) {
            rX += borda;
        }
        if (rY > 0) {
            rY += borda;
        }
        return new Point(rX, rY);
    }

    public Point getPontoMenorSelecionado() {
        int rX = getWidth();
        int rY = getHeight();
        for (FormaElementar el : getItensSelecionados()) {
            rX = Math.min(rX, el.getLeft());
            rY = Math.min(rY, el.getTop());
        }
        return new Point(rX, rY);
    }

    public void mouseClick(MouseEvent e) {
    }

    public void mouseDblClick(MouseEvent e) {
        e = tradutorZoom(e);

        setElementarSobMouse(desenho.PosicionamentoAncorador.capture(this, superAncorador, e.getPoint()));
        if (elementarSobMouse != null) {
            elementarSobMouse.mouseDblClicked(e);
        }
    }

    public void mousePressed(MouseEvent e) {
        // Before the click changes the selection; focus loss would arrive only afterwards.
        master.ConfirmeEdicaoInspector();
        master.requestFocus();
        e = tradutorZoom(e);

        if (comando != null) {
            isLoadCreate = true;
            Controler.Comandos criando = comando;
            FormaElementar criado = RealiseComando(e.getPoint());
            EncaixeGrade.criar(master, criando, criado);
            isLoadCreate = false;
            if (cliq1 == null) {
                DoMuda(null);
            }
            return;
        }

        if (!((elementarSobMouse != null) && (elementarSobMouse == elementarSobMouse.IsMeOrMine(e.getPoint())))) {
            setElementarSobMouse(desenho.PosicionamentoAncorador.capture(this, superAncorador, e.getPoint()));
        }
        if (elementarSobMouse instanceof desenho.linhas.PontoDeLinha ponto && !ponto.getDono().isSelecionado()) {
            DiagramaDoSelecao(ponto.getDono(), true, false);
        }

        if (elementarSobMouse != null) {
            elementarSobMouse.mousePressed(e);
            draging = true;
            if (elementarSobMouse != superAncorador) {
                superAncorador.SetVisible(false); //não precisa repintar!
            }
        } else {
            DiagramaDownPos.setLocation(e.getPoint());
            isMouseDown = true;
            master.InitMultiSel(DiagramaDownPos);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        processaMouseEntredExited(e, elementarSobMouse, false);
    }

    private void processaMouseEntredExited(MouseEvent ev, Elementar el, boolean enter) {
        ev = tradutorZoom(ev);

        if (el != null) {
            if (enter) {
                el.mouseEntered(ev);
            } else {
                el.mouseExited(ev);
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        e = tradutorZoom(e);

        if (draging && (elementarSobMouse != null)) {
            elementarSobMouse.mouseDragged(e);
            return;
        }
        if (isMouseDown) {
            int x = e.getX();
            int y = e.getY();
            final int ALeft = (x < DiagramaDownPos.x) ? x : DiagramaDownPos.x;
            final int ATop = (y < DiagramaDownPos.y) ? y : DiagramaDownPos.y;
            final int AWidth = Math.abs(x - DiagramaDownPos.x);
            final int AHeight = Math.abs(y - DiagramaDownPos.y);

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    master.getMultSel().setBounds(ALeft, ATop, AWidth, AHeight);
                    master.repaint();
                }
            });
        }
    }

    /**
     * Método que aplica o fator de zoom nas coordenadas do mouse.
     *
     * @param e evento do Mouse (geralmente interessa apenas as coordenadas);
     * @return novo evento com novas coordenadas
     */
    protected MouseEvent tradutorZoom(MouseEvent e) {
        Point p = EncaixeGrade.coordenadas(e.getPoint(), getZoom());

        MouseEvent ex = new MouseEvent(master.getBox(), e.getID(), e.getWhen(), e.getModifiersEx(),
                p.x, p.y, e.getClickCount(), false, e.getButton());

        return ex;

    }

    /**
     * Método que aplica o fator de zoom nas coordenadas e.
     *
     * @param e = ponto (geralmente coordenadas);
     * @return novo ponto
     */
    protected Point tradutorZoom(Point e) {
        return EncaixeGrade.coordenadas(e, getZoom());
    }

    /**
     * Dado um retângulo ele é converido para as coordenadas na tela no caso do diagrma estar ampliado ou reduzido (Zoom)
     *
     * @param r retângulo a converter.
     * @return retângulo convertido.
     */
    public Rectangle ZoomRectangle(Rectangle r) {
        double z = getZoom();
        return new Rectangle((int) (r.x * z), (int) (r.y * z), (int) (r.width * z), (int) (r.height * z));
    }

    public void mouseMoved(MouseEvent e) {
        if (isMouseDown || draging) {
            mouseReleased(e);
            return;
        }

        e = tradutorZoom(e);

        Elementar olde = elementarSobMouse;

        setElementarSobMouse(desenho.PosicionamentoAncorador.capture(this, superAncorador, e.getPoint()));

        if (elementarSobMouse != null) {
            elementarSobMouse.mouseMoved(e);
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
        if (comando != null) {
            Elementar composicao = elementarSobMouse;
            if (composicao != null && elementarSobMouse.isComposto()) {
                composicao = composicao.ProcessaComposicao(e.getPoint());
            }
            ProcessaOverDraw(false, composicao);
        } else if (olde != elementarSobMouse) {
            processaMouseEntredExited(e, olde, false);
            processaMouseEntredExited(e, elementarSobMouse, true);
        }
    }
    private FormaElementar overDraw = null;

    public Rectangle AlmentarRetangulo(Rectangle r, int x, int y) {
        return Utilidades.Grow(r, x, y, 0);
    }

    /**
     * Colore uma borda na Forma que estiver sobre o Mouse no momento em que estiver selecionado um novo objeto a ser inserido no Diagrama.
     *
     * @param limpa apagar.
     * @param el elementar um cujo o mouse está ou estava sobre .
     *
     */
    public void ProcessaOverDraw(boolean limpa, Elementar el) {
        int x = 4;
        int y = 4;
        if (limpa) {
            if (overDraw != null) {
                overDraw.setOverMe(false);
                Rectangle rec = AlmentarRetangulo(overDraw.getBounds(), x, y);
                overDraw.InvalidateArea(rec);
                overDraw = null;
            }
            return;
        }
        if (el == null && overDraw != null) {
            overDraw.setOverMe(false);
            Rectangle rec = AlmentarRetangulo(overDraw.getBounds(), x, y);
            overDraw.InvalidateArea(rec);
            overDraw = null;
        } else if (el != null && el instanceof FormaElementar && el != overDraw) {
            if (overDraw != null) {
                overDraw.setOverMe(false);
                Rectangle rec = AlmentarRetangulo(overDraw.getBounds(), x, y);
                overDraw.InvalidateArea(rec);
            }
            overDraw = (FormaElementar) el;
            overDraw.setOverMe(true);
            Rectangle rec = AlmentarRetangulo(overDraw.getBounds(), x, y);
            overDraw.InvalidateArea(rec);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Trata Elementar">
    /**
     * Verifica que componente está na posição "ponto" obedecendo a ordem z [melhoria: 16/05/2014] Dá preferência ao obj selecionado, independentemente da ordem z para fins de melhor seleção e movimentação com o mouse.
     *
     * @param ponto = local. Poder ser a posição x e y do mouse
     * @return o Componente.
     */
    public Elementar CaptureFromPoint(Point ponto) {
        Elementar res = getSelecionado();
        if (res != null) {
            res = res.IsMeOrMine(ponto);
            if (res != null) {
                return res;
            }
        }
        for (Elementar el : subItens) {
            res = el.IsMeOrMine(ponto);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    public Elementar CaptureFromPoint(Elementar nor, Point ponto) {
        Elementar res = null;
        for (Elementar el : subItens) {
            res = el.IsMeOrMine(ponto, nor);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    public Forma CaptureBaseFromPoint(Elementar nor, Point ponto) {
        Elementar res = null;
        for (Elementar el : subItens) {
            res = el.IsMeOrMineBase(ponto, nor);
            if (res != null) {
                return (Forma) res;
            }
        }
        return null;
    }
    private transient Elementar elementarSobMouse = null;

    /**
     * @return the elementarSobMouse
     */
    public Elementar getElementarSobMouse() {
        return elementarSobMouse;
    }

    /**
     * @param elementarSobMouse the elementarSobMouse to set
     */
    public void setElementarSobMouse(Elementar elementarSobMouse) {
        this.elementarSobMouse = elementarSobMouse;
    }
    // </editor-fold>
    Point DiagramaDownPos = new Point(0, 0);
    boolean isMouseDown = false;

    public void mouseReleased(MouseEvent e) {
        e = tradutorZoom(e);

        if (isMouseDown) {
            isMouseDown = false;
            Rectangle recsel = master.getMultSel().getBounds();
            boolean combine = (isShiftDown() || isControlDown());
            if (!combine) {
                ClearSelect();
            }
            final Point p = e.getPoint();
            master.FinishMultiSel();
            subItens.stream().filter((c) -> (c instanceof FormaElementar)).map((c) -> (FormaElementar) c).filter((item) -> (item.IntersectPath(recsel)))
                    .sorted((p1, p2) -> Double.compare(Utilidades.distance(p1.getLocation(), p), Utilidades.distance(p2.getLocation(), p)))
                    .forEach((item) -> {
                        DiagramaDoSelecao(item, true, true);
                    });

            repaint(Utilidades.Grow(recsel, 2, 2, 0));
        }
        if (draging && (elementarSobMouse != null)) {
            elementarSobMouse.mouseReleased(e);
            superAncorador.Posicione(getSelecionado());
        }
        draging = false;
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (util.Atalhos.pressionado(e)) {
            if (e.getWheelRotation() < 0) {
                getEditor().ZoomMais();
            } else {
                getEditor().ZoomMenos();
            }
            e.consume();
        }
    }

    // </editor-fold>
    public transient boolean IsStopEvents = false;
    public transient boolean isCarregando = false;
    protected transient boolean isLoadCreate = false;
    private boolean mudou = false;
    /**
     * Armazena o ID
     */
    protected int TotalID = 0;
    public Point ScrPosicao = new Point(0, 0);

    /**
     * Gera um ID único para cada elemetar criado pelo modelo.
     *
     * @return novo ID
     */
    public int getElementarID() {
        return ++TotalID;
    }

    public boolean getMudou() {
        return mudou;
    }

    public void setMudou(boolean value) {
        if (mudou != value) {
            mudou = value;
            if (!value) {
                PerformInspector();
            }
            getEditor().getShowDiagramas().repaint();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Seleção">
    /**
     * Esconde ou mostra os pontos de uma Componente
     *
     * @param esconde true: esconde, false: mostra.
     */
    public void HidePontosOnSelecao(boolean esconde) {
        SelecaoDiagrama.HidePontosOnSelecao(this, esconde);
    }

    /**
     * Seleciona um componente
     *
     * @param item
     * @return selecionou?
     */
    public boolean DiagramaDoSelecao(FormaElementar item) {
        return DiagramaDoSelecao(item, false, false);
    }

    /**
     * Seleciona um componente
     *
     * @param item
     * @param ehmouse "É mouse": true: processa combinações de teclado para seleção múltipla (multiseleção).
     * @param ForcarMultSel
     * @return
     */
    public boolean DiagramaDoSelecao(FormaElementar item, boolean ehmouse, boolean ForcarMultSel) {
        return SelecaoDiagrama.DiagramaDoSelecao(this, superAncorador, item, ehmouse, ForcarMultSel);
    }

    void PontosCor(FormaElementar item) {
        PontosCor(item, false);
    }

    void PontosCor(FormaElementar item, boolean verde) {
        item.DoPontoCor(verde);
    }

    void AddSelect(FormaElementar item) {
        SelecaoDiagrama.AddSelect(this, item);
    }

    public void PromoveToFirstSelect(FormaElementar item) {
        SelecaoDiagrama.PromoveToFirstSelect(this, item);
    }

    void RemoveSelect(FormaElementar item) {
        SelecaoDiagrama.RemoveSelect(this, item);
    }

    /**
     * Limpa/des-seleciona todos os selecionados.
     */
    public void ClearSelect() {
        ClearSelect(true);
    }

    /**
     * Limpa/des-seleciona todos os selecionados. Redesenha o Inspector
     *
     * @param performInsp
     */
    public void ClearSelect(boolean performInsp) {
        SelecaoDiagrama.ClearSelect(this, superAncorador, performInsp);
    }

    public boolean TemSelecionado() {
        return itensSelecionados.size() > 0;
    }

    public FormaElementar getSelecionado() {
        if (TemSelecionado()) {
            return itensSelecionados.get(0);
        }
        return null;
    }

    public void setSelecionado(FormaElementar sel) {
        DiagramaDoSelecao(sel, false, false);
    }
    // </editor-fold>

    public void ReciveProcessMove(FormaElementar nor, int x, int y) {
        itensSelecionados.stream().filter(item -> item != nor).forEach(item -> item.DoMove(x, y));
    }

    //<editor-fold defaultstate="collapsed" desc="Teclas">
    /**
     * @return the shiftDown
     */
    public boolean isShiftDown() {
        return master.isShiftDown();
    }

    /**
     * @return the altDown
     */
    public boolean isAltDown() {
        return master.isAltDown();
    }

    /**
     * @return the controlDown
     */
    public boolean isControlDown() {
        return master.isControlDown();
    }
    // </editor-fold>

    public void DoFormaResize(Rectangle ret) {
        if (ret.x == 0 && ret.y == 0 && ret.width == 0 && ret.height == 0) {
            return;
        }
        itensSelecionados.stream().filter((de) -> (de instanceof Forma)).map((de) -> (Forma) de).forEach((item) -> {
            item.ReciveFormaResize(ret);
        });
        repaint();
    }

    public void DoBaseReenquadreReposicione() {
        itensSelecionados.stream().filter((item) -> (!item.Reenquadre())).forEach((item) -> {
            item.Reposicione();
        });
    }

    /**
     * Recebe as teclas do Editor.
     *
     * @param e
     */
    public void ProcesseTeclas(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            setComando(null);
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            InspectorItemBase ppr = getEditor().getInspectorEditor().FindByProperty("setTexto");
            if (ppr != null) {
                if (getEditor().getInspectorEditor().getSelecionado() == ppr) {
                    getEditor().getInspectorEditor().PerformSelect(null);
                }
                getEditor().getInspectorEditor().PerformSelect(ppr);
                if (ppr instanceof InspectorItemExtender) {
                    ((InspectorItemExtender) ppr).ExternalRun();
                }
            }
            return;
        }

        if (itensSelecionados.isEmpty()) {
            return;
        }

        FormaElementar item = itensSelecionados.get(0);
        int x = 0, y = 0;
        int inc = 3;
        if (util.Atalhos.pressionado(e)) {
            inc = 1;
        }

        if (EncaixeGrade.ativo()) inc = master.getGridWidth();

        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                x = -inc;
                y = 0;
                break;
            case KeyEvent.VK_RIGHT:
                x = inc;
                y = 0;
                break;
            case KeyEvent.VK_UP:
                x = 0;
                y = -inc;
                break;
            case KeyEvent.VK_DOWN:
                x = 0;
                y = inc;
                break;
        }

        if (item.isAncorado()) {
            e.consume();
        } else if (x != 0 || y != 0) {
            if (e.isShiftDown() && item instanceof Forma) {
                Rectangle rec = new Rectangle(0, 0, -x, -y);
                ((Forma) item).DoFormaResize(rec);
            } else {
                item.DoRaiseMove(x, y);
            }
            superAncorador.Posicione(item);
            DoBaseReenquadreReposicione();
            HidePontosOnSelecao(false);
            PerformInspector();
            e.consume();
        }
    }

    /**
     * Recebe as comandos na forma de teclas Editor.
     *
     * @param k
     */
    public void ProcesseTeclas(int k) {
        if (itensSelecionados.isEmpty()) {
            return;
        }

        FormaElementar item = itensSelecionados.get(0);
        if (item.isAncorado()) {
            return;
        }

        int x = 0, y = 0;
        int inc = EncaixeGrade.ativo() ? master.getGridWidth() : (isControlDown() ? 1 : 3);

        switch (k) {
            case KeyEvent.VK_LEFT:
                x = -inc;
                y = 0;
                break;
            case KeyEvent.VK_RIGHT:
                x = inc;
                y = 0;
                break;
            case KeyEvent.VK_UP:
                x = 0;
                y = -inc;
                break;
            case KeyEvent.VK_DOWN:
                x = 0;
                y = inc;
                break;
        }

        if (x != 0 || y != 0) {
            if (isShiftDown() && item instanceof Forma) {
                Rectangle rec = new Rectangle(0, 0, -x, -y);
                ((Forma) item).DoFormaResize(rec);
            } else {
                item.DoRaiseMove(x, y);
            }
            superAncorador.Posicione(item);
            DoBaseReenquadreReposicione();
            HidePontosOnSelecao(false);

        }
        master.requestFocus();
        PerformInspector();
    }

    public boolean SelecioneProximo() {
        if (itensSelecionados.isEmpty()) {
            return false;
        }

        FormaElementar item = itensSelecionados.get(0);
        if (itensSelecionados.size() == 1 && ListaDeItens.size() > 1) {
            int idxAtual = ListaDeItens.indexOf(item);
            int idx = idxAtual;
            while (true) {
                idx++;
                if (idx >= ListaDeItens.size()) {
                    idx = 0;
                }
                if (idx == idxAtual) {
                    break;
                }
                item = ListaDeItens.get(idx);
                if (item.isSelecionavel()) {
                    DiagramaDoSelecao(item, false, false);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean SelecioneAnterior() {
        if (itensSelecionados.isEmpty()) {
            return false;
        }

        FormaElementar item = itensSelecionados.get(0);
        if (itensSelecionados.size() == 1 && ListaDeItens.size() > 1) {
            int idxAtual = ListaDeItens.indexOf(item);
            int idx = idxAtual;
            while (true) {
                idx--;
                if (idx < 0) {
                    idx = ListaDeItens.size() - 1;
                }
                if (idx == idxAtual) {
                    break;
                }
                item = ListaDeItens.get(idx);
                if (item.isSelecionavel()) {
                    DiagramaDoSelecao(item, false, false);
                    return true;
                }
            }
        }
        return false;
    }

    public void SelecioneTodos() {
        ClearSelect(false);
        getListaDeItens().forEach((it) -> {
            DiagramaDoSelecao(it, false, true);
        });
        PerformInspector();
    }

    public void SelecioneTodosDoTipo() {
        FormaElementar sel = getSelecionado();
        if (sel == null) return;
        ClearSelect(false);
        DiagramaDoSelecao(sel, false, true);
        getListaDeItens().stream().filter(s -> s.getClass() == sel.getClass() && s != sel).forEach((it) -> {
            DiagramaDoSelecao(it, false, true);
        });
        PerformInspector();
    }

    /**
     * Adiciona um componente ao Diagrama.
     *
     * @param aThis
     */
    public final void Add(FormaElementar aThis) {
        subItens.add(aThis);
        ListaDeItens.add(aThis);
    }

    public boolean IsMultSelecionado() {
        return itensSelecionados.size() > 1;
    }

    private boolean Remove(FormaElementar item) {
        if (!item.isCanBeDeleted()) {
            return item.AskToDelete();
        }
        ListaDeItens.remove(item);
        subItens.remove(item);
        item.Destroy();
        return true;
    }

    public boolean Remove(FormaElementar item, boolean removeSel) {
        if (Remove(item)) {
            if (removeSel) {
                itensSelecionados.remove(item);
            }
            return true;
        }
        return false;
    }

    public boolean deleteSelecao() {
        if (TemSelecionado()) {
            itensSelecionados.stream().forEach((el) -> {
                Remove(el);
            });
            itensSelecionados.clear();
            DoMuda(null);
            repaint();
            master.getControler().makeEnableComands();
            return true;
        }
        return false;
    }

    // <editor-fold defaultstate="collapsed" desc="Comando">
    private transient Controler.Comandos comando = null;

    protected void setComando(Controler.Comandos cmd) {
        comando = cmd;
        if (cmd == null) {
            master.NoAction();
            setCursor(new Cursor((Cursor.DEFAULT_CURSOR)));
            ProcessaOverDraw(true, null);
        }
    }

    /**
     * After a palette tool created an object: the tool stays while Ctrl is held or when it was
     * chosen with a double click (Editor.isFerramentaFixa); otherwise the arrow comes back.
     */
    protected void ManterOuSoltarComando(Controler.Comandos com) {
        if (master.isControlDown() || master.isFerramentaFixa()) {
            setComando(com);
        } else {
            setComando(null);
        }
    }

    public Controler.Comandos getComando() {
        return comando;
    }

    public void DoAction(ActionEvent ev) {
        if (ev.getActionCommand() == null || ev.getActionCommand().isEmpty()) {
            setComando(null);
            return;
        }
        try {
            Controler.Comandos cmd = Controler.Comandos.valueOf(ev.getActionCommand());
            if (comando != cmd) {
                cliq1 = null;
                cliq2 = null;
            }
            setComando(cmd);
        } catch (Exception e) {
            setComando(null);
        }
    }

    public class clickForma {

        private final FormaElementar forma;
        private final Point ponto;

        public clickForma(FormaElementar forma, Point ponto) {
            this.forma = forma;
            this.ponto = ponto;
        }

        public FormaElementar getForma() {
            return forma;
        }

        public Point getPonto() {
            return ponto;
        }
    }

    /**
     * Usada para criar objetos na forma padrão de forma externa
     *
     * @param cmd
     * @param posi
     * @return novo objeto/objeto criado
     */
    public FormaElementar ExternalRealiseComando(Controler.Comandos cmd, Point posi) {
        Controler.Comandos c = getComando();
        setComando(cmd);
        FormaElementar res = RealiseComando(posi);
        setComando(c);
        return res;
    }

    protected transient clickForma cliq1 = null, cliq2 = null;

    protected FormaElementar RealiseComando(Point posi) {
        FormaElementar resu = null;
        Controler.Comandos com = comando;
        Elementar res = null;

        switch (com) {
            case cmdDesenhador:
                Desenhador dz = new Desenhador(this);
                dz.SetBounds(posi.x, posi.y, 250, 150);
                dz.Reenquadre();
                resu = dz;
                break;
            case cmdLegenda:
                Legenda leg = new Legenda(this);
                leg.SetBounds(posi.x, posi.y, 150, 45);
                leg.Reenquadre();
                resu = leg;
                break;
            case cmdTexto:
                res = CaptureFromPoint(posi);
                Texto Tx = new Texto(this, "Texto");
                if (res instanceof SuperLinha) {
                    Tx.SetBounds(posi.x, posi.y, 100, 18);
                    Tx.setAlinhamento(AlinhamentoTexto.alEsquerda);
                    Tx.setTipo(TipoTexto.tpEmBranco);
                    Tx.setCentrarVertical(false);
                    ((SuperLinha) res).setTag(Tx);
                } else {
                    Tx.SetBounds(posi.x, posi.y, 150, 36);
                    Tx.Reenquadre();
                }
                resu = Tx;
                break;
            case cmdApagar:
                res = CaptureFromPoint(posi);
                if (res instanceof FormaElementar) {
                    resu = (FormaElementar) res;
                    ClearSelect();
                    setSelecionado(resu);
                    deleteSelecao();
                }
                resu = null;
        }
        cliq1 = null;
        cliq2 = null;

        ManterOuSoltarComando(com);
        if (resu != null) {
            resu.BringToFront();
        }
        return resu;
    }

    // </editor-fold>
    /**
     * Redesenha o inspector com o objeto selecionado. É chamado também no método setMuda(true).
     */
    public void PerformInspector() {
        if (!itensSelecionados.isEmpty()) {
            getEditor().PerformInspectorFor(itensSelecionados.get(0));
        } else {
            getEditor().PerformInspectorFor(infoDiagrama);
        }
    }

    /**
     * Redesenha o inspector com o objeto selecionado.
     *
     * @param force = limpa as propriedades previamente capturadas de forma que a chamada ao inspector force uma recarga.
     */
    public void PerformInspector(boolean force) {
        if (force) {
            getEditor().getInspectorEditor().ForceFullOnCarregue();
        }
        PerformInspector();
    }

    /**
     * Este médodo é usado quando precisa-se usar o inspector para editar propriedade de sub componete
     *
     * @param ed: componete principal em edição.
     * @param bj: o java não tem passagem por referencia - boxing da "propriedade" em edição
     * @return: o sub componente (se for o caso).
     */
    public Object processeEdicaoSubItem(FormaElementar ed, BoxingJava bj) {
        return ed;
    }

    public boolean AceitaEdicao(InspectorProperty prop, String valor) {
        FormaElementar param;
        if (itensSelecionados.isEmpty()) {
            param = infoDiagrama;
        } else {
            param = itensSelecionados.get(0);
        }
        Object ed = param;
        String P = prop.property;

        //caso de edição de sub componente. Exemplo: EntidadeAssociativa.Relacao
        if (P.indexOf('.') > 0) {
            util.BoxingJava bj = new util.BoxingJava(P);
            ed = processeEdicaoSubItem(param, bj);
            P = bj.Str;
        }
        final String propriedade = P;

        final Class[] par = new Class[1];
        final Object[] vl = new Object[1];
        try {
            switch (prop.tipo) {
                case tpBooleano:
                    par[0] = Boolean.TYPE;
                    vl[0] = Boolean.parseBoolean(valor);
                    break;
                case tpCor:
                    par[0] = Color.class;
                    vl[0] = util.Utilidades.StringToColor(valor);
                    break;
                case tpMenu:
                    par[0] = Integer.TYPE;
                    int p = Integer.parseInt(valor);
                    vl[0] = p;
                    break;
                case tpNumero:
                    par[0] = Integer.TYPE;
                    final int tmp = Integer.parseInt(valor);
                    vl[0] = tmp;

                    final String[] pprtMv = {"setLeft", "setTop", "setWidth", "setHeight"};
                    if (Arrays.asList(pprtMv).indexOf(propriedade) > -1) {
                        if (ed instanceof Forma) {
                            IsStopEvents = true;
                            itensSelecionados.stream().filter(e -> e.getClass().equals(param.getClass())).map(f -> (Forma) f).forEach(Ed -> {
                                Rectangle ret;
                                if ((propriedade.equals(pprtMv[0]))) {
                                    ret = new Rectangle(Ed.getLeft() - tmp, 0, 0, 0);
                                } else if (propriedade.equals(pprtMv[1])) {
                                    ret = new Rectangle(0, Ed.getTop() - tmp, 0, 0);
                                } else if (propriedade.equals(pprtMv[2])) {
                                    ret = new Rectangle(0, 0, Ed.getWidth() - tmp, 0);
                                } else {
                                    ret = new Rectangle(0, 0, 0, Ed.getHeight() - tmp);
                                }
                                Ed.ReciveFormaResize(ret);

                                Ed.DoRaizeReenquadreReposicione();
                            });
                            IsStopEvents = false;
                            DoMuda(param);
                            return true;
                        }
                    }
                    break;
                default:
                    par[0] = String.class;
                    vl[0] = valor;
            }


            if ((ed instanceof InfoDiagrama) || !(ed instanceof FormaElementar) || ((ed instanceof FormaElementar) && (((FormaElementar) ed).isParte()))) {
                Class cl = ed.getClass();
                Method mthd = cl.getMethod(propriedade, par);
                mthd.invoke(ed, vl);
                DoMuda(param);
            } else {
                final List<FormaElementar> lst;
                lst = itensSelecionados.stream().filter(e -> e.getClass().equals(param.getClass())).collect(Collectors.toList());

                IsStopEvents = true;
                for (FormaElementar Ed : lst) {
                    Class cl = Ed.getClass();
                    Method mthd = cl.getMethod(propriedade, par);
                    mthd.invoke(Ed, vl);
                }
                IsStopEvents = false;
                DoMuda(param);
            }

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            util.BrLogger.Logger("ERROR_SET_PROPERTY", e.getMessage());
            IsStopEvents = false;
            superAncorador.InvalidateArea();
            return false;
        }
        superAncorador.InvalidateArea();
        return true;
    }

    /**
     * Usado para os métodos copiarFormatacao.
     *
     * @param param O que está sendo colado
     * @param prop uma de suas propriedades
     * @param valor novo valor.
     * @return
     */
    public boolean ColeFormatacao(FormaElementar param, InspectorProperty prop, String valor) {
        Object ed = param;
        String propriedade = prop.property;

        //caso de edição de sub componente. Exemplo: EntidadeAssociativa.Relacao
        if (propriedade.indexOf('.') > 0) {
            util.BoxingJava bj = new util.BoxingJava(propriedade);
            ed = processeEdicaoSubItem(param, bj);
            propriedade = bj.Str;
        }

        Class[] par = new Class[1];
        Object[] vl = new Object[1];
        try {
            switch (prop.tipo) {
                case tpBooleano:
                    par[0] = Boolean.TYPE;
                    vl[0] = Boolean.parseBoolean(valor);
                    break;
                case tpCor:
                    par[0] = Color.class;
                    vl[0] = util.Utilidades.StringToColor(valor);
                    break;
                case tpMenu:
                    par[0] = Integer.TYPE;
                    int p = Integer.parseInt(valor);
                    vl[0] = p;
                    break;
                case tpNumero:
                    par[0] = Integer.TYPE;
                    int tmp = Integer.parseInt(valor);
                    vl[0] = tmp;

                    String[] pprtMv = {"setLeft", "setTop", "setWidth", "setHeight"};
                    if (Arrays.asList(pprtMv).indexOf(propriedade) > -1) {
                        Rectangle ret;
                        if (ed instanceof Forma) {
                            Forma Ed = (Forma) ed;
                            if ((propriedade.equals(pprtMv[0]))) {
                                ret = new Rectangle(Ed.getLeft() - tmp, 0, 0, 0);
                            } else if (propriedade.equals(pprtMv[1])) {
                                ret = new Rectangle(0, Ed.getTop() - tmp, 0, 0);
                            } else if (propriedade.equals(pprtMv[2])) {
                                ret = new Rectangle(0, 0, Ed.getWidth() - tmp, 0);
                            } else {
                                ret = new Rectangle(0, 0, 0, Ed.getHeight() - tmp);
                            }
                            Ed.DoFormaResize(ret);
                            Ed.DoRaizeReenquadreReposicione();
                            DoMuda(Ed);
                            return true;
                        }
                    }
                    break;
                default:
                    par[0] = String.class;
                    vl[0] = valor;
            }
            Class cl = ed.getClass();
            Method mthd = cl.getMethod(propriedade, par);
            mthd.invoke(ed, vl);
            DoMuda(param);

        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            util.BrLogger.Logger("ERROR_SET_PROPERTY", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Com base nos nomes dos objetos traduzidos nos arq. de confg. nomeia os objetos no ato de criação.
     *
     * @param padrao nome dado por mim
     * @return nome traduzido/configurado pelo usuário.
     */
    public final String Nomeie(String padrao) {
        String txt = Editor.fromConfiguracao.getValor("diagrama." + padrao + ".nome");
        int res = 1;
        ArrayList<String> txts = new ArrayList<>();
        ListaDeItens.stream().filter((el) -> (el instanceof Forma)).map(el -> (Forma) el).forEach(el -> el.EscrevaTexto(txts));

        while (txts.indexOf(txt + "_" + res) != -1) {
            res++;
        }
        return txt + "_" + res;
    }

    /**
     * Procura um artefato pelo ID
     *
     * @param id
     * @return encontrado
     */
    public FormaElementar FindByID(int id) {
        for (FormaElementar f : getListaDeItens()) {
            if (f.getID() == id) {
                return f;
            }
        }
        return null;
    }

//    Nunca usado! 20/09/2014
    public void DoMuda(FormaElementar who) {
        if (isLoadCreate || isCarregando) {
            return;
        }
        setMudou(true);
        try {
            master.DoDiagramaMuda();
        } catch (Exception e) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_MUDA", e.getMessage());
        }
        PerformInspector();
    }

    //<editor-fold defaultstate="collapsed" desc="Save Load">
    /**
     * Gera uma Stream a partir de um modelo
     *
     * @param othis
     * @return
     */
    public synchronized static ByteArrayOutputStream SaveToStream(Diagrama othis) {
        ByteArrayOutputStream bytes = PersistenciaDiagrama.SaveToStream(othis);
        if (bytes != null) othis.tick();
        return bytes;
    }
    public final static String nodePrincipal = "DIAGRAMA";

    private static TipoDeDiagrama GetTipoOnXml(Document doc) {
        return PersistenciaDiagrama.GetTipoOnXml(doc);
    }

    /**
     * Gera um diagrama novo a partir de uma Stream
     *
     * @param ba
     * @return
     */
    public synchronized static Diagrama LoadFromStream(ByteArrayOutputStream ba) {
        return PersistenciaDiagrama.LoadFromStream(ba);
    }

    /**
     * Gera um diagrama novo a partir de um Arquivo
     *
     * @param arq
     * @param master
     * @return
     */
    public synchronized static Diagrama LoadFromFile(File arq, Editor master) {
        return PersistenciaDiagrama.LoadFromFile(arq, master);
    }

    /**
     * Gera um diagrama novo a partir de um GuardaPadraoBrM
     *
     * @param seguranca
     * @param master
     * @return
     */
    public synchronized static Diagrama LoadFromBrm(GuardaPadraoBrM seguranca, Editor master) {
        return PersistenciaDiagrama.LoadFromBrm(seguranca, master);
    }

    public boolean LoadFromXML(Document doc, boolean colando) {
        return PersistenciaDiagrama.LoadFromXML(this, doc, colando);
    }

    protected int OnLoadingXMLitem(FormaElementar res, Element fstElmnt, boolean colando, int maxID, HashMap<Element, FormaElementar> link) {
        return maxID;
    }

    protected void ReestrutureSelecao(String oid, HashMap<Element, FormaElementar> link) {
        FormaElementar ja = null;
        for (Element el : link.keySet()) {
            FormaElementar proc = link.get(el);
            if (ja == null && el.hasAttribute("ID") && el.getAttribute("ID").equals(oid)) {
                ja = proc;
            }
            DiagramaDoSelecao(proc, false, true);
        }
        if (ja != null) {
            PromoveToFirstSelect(ja);
        }
    }

    protected FormaElementar ReflectionObj(Class classeDoObj) {
        return PersistenciaDiagrama.ReflectionObj(this, classeDoObj);
    }

    protected FormaElementar runCriadorFromXml(Element xml, boolean colando) {
        return PersistenciaDiagrama.runCriadorFromXml(this, xml, colando);
    }

    protected void LoadFromXML(FormaElementar obj, Element xml, boolean colando) {
        PersistenciaDiagrama.LoadFromXML(this, obj, xml, colando);
    }

    public static String SaveToXml(Diagrama othis, boolean justSel) {
        return PersistenciaDiagrama.SaveToXml(othis, justSel);
    }

    public boolean Salvar() {
        File arq = util.Dialogos.ShowDlgSaveDiagrama(master.getRootPane(), this);
        if (arq == null) {
            return false;
        }
        return Salvar(arq, false);
    }

    public boolean Salvar(String fileName) {
        if ("".equals(fileName) || fileName == null) {
            return Salvar();
        }
        File arq = new File(fileName);
        return Salvar(arq, false);
    }

    public boolean Salvar(File fileName, boolean pergunta) {
        return PersistenciaDiagrama.Salvar(this, fileName, pergunta);
    }

    public boolean AutoSalvar(ArrayList<byte[]> as) {
        return PersistenciaDiagrama.AutoSalvar(this, as);
    }
    //</editor-fold>

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    public void doCopy() {
        TransferenciaDiagrama.doCopy(this);
    }

    public void doCopy(BufferedImage img) {
        TransferenciaDiagrama.doCopy(this, img);
    }

    public void doPaste() {
        TransferenciaDiagrama.doPaste(this);
    }

    public boolean doPaste(String txt) {
        return TransferenciaDiagrama.doPaste(this, txt);
    }

    boolean processePaste(Document doc) {
        return TransferenciaDiagrama.processePaste(this, doc);
    }

    public void repaint() {
        if (isCarregando) {
            return;
        }
        master.getBox().repaint();
    }

    public void repaint(Rectangle r) {
        if (isCarregando) {
            return;
        }
        r = ZoomRectangle(r);
        master.getBox().repaint(r);
    }

    /**
     * Get the String residing on the clipboard.
     *
     * @return any text found on the Clipboard; if none found, return an empty String. Código copiado da internet.
     */
    public static String getClipboardContents() {
        return TransferenciaDiagrama.getClipboardContents();
    }

    public static BufferedImage getClipboardContentsImage() {
        return TransferenciaDiagrama.getClipboardContentsImage();
    }

    @Override
    public String toString() {
        return super.toString() + "{"
                + Diagrama.SaveToXml(this, false) + "}";
    }

    public static Diagrama Factory(TipoDeDiagrama otipo, Editor ed) {
        Diagrama res = null;
        switch (otipo) {
            case tpConceitual:
                res = new diagramas.conceitual.DiagramaConceitual(ed);
                break;
            case tpLogico:
                res = new diagramas.logico.DiagramaLogico(ed);
                break;
            case tpFluxo:
                res = new diagramas.fluxo.DiagramaFluxo(ed);
                break;
            case tpAtividade:
                res = new diagramas.atividade.DiagramaAtividade(ed);
                break;
            case tpEap:
                res = new diagramas.eap.DiagramaEap(ed);
                break;
            case tpLivre:
                res = new diagramas.livre.DiagramaLivre(ed);
                break;
            default:
                res = new Diagrama(ed);
                break;
        }
        ed.NomeieDiagrama(res);
        return res;
    }

    public class AcaoDiagrama extends Acao {

        public AcaoDiagrama(Editor editor, String texto, String ico, String descricao, String command) {
            super(editor, texto, ico, descricao, command);
            comm = command;
        }

        private Diagrama d = null;
        private String comm = "";

        public AcaoDiagrama(Diagrama diag, String texto, String ico, String descricao, String command) {
            this(diag.getEditor(), texto, ico, descricao, command);
            d = diag;
            comm = command;
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            if (d != null) {
                d.rodaComando(comm);
            }
        }
    }

    public boolean EstaColandoNoMesmoDigDaOrigem() {
        return ((InfoDiagrama) infoDiagrama).IsTheShame();
    }

    public int AlinhamentoH() {
        return ((InfoDiagrama) infoDiagrama).getAlinhamento_h();
    }

    public int AlinhamentoV() {
        return ((InfoDiagrama) infoDiagrama).getAlinhamento_v();
    }

    public void ExternalSuperAncorador() {
        superAncorador.Posicione(getSelecionado());
    }

    //# Introduzino em 21/04/2017
    public void InfoDiagrama_ToXmlValores(Document doc, Element me) {
    }

    //# Introduzino em 21/04/2017
    public boolean InfoDiagrama_LoadFromXML(Element me, boolean colando) {
        return true;
    }

    /**
     * Mostra apenas os artefatos selecionados e os que estiverem ligados a eles. É setado para false no OnAfterLoad
     */
    private boolean realce = false;

    public boolean isRealce() {
        return realce;
    }

    public void SetRealce(boolean realce) {
        this.realce = realce;
    }

    public void setRealce(boolean realce) {
        if (this.realce == realce) {
            return;
        }
        this.realce = realce;
        if (!realce) {
            getListaDeItens().stream().filter(fo -> fo instanceof FormaElementar).forEach(fo -> fo.setDisablePainted(false));
        } else {
            final ArrayList<FormaElementar> res = new ArrayList<>();
            getListaDeItens().stream().filter(f -> getItensSelecionados().indexOf(f) > -1).forEach(item -> {
                if (item instanceof Forma) {
                    AdicionePrinFromRealce(res, item);
                    Forma f = (Forma) item;
                    f.getListaDeFormasLigadas().forEach(lfl -> {
                        AdicioneSubsFromRealce(res, lfl);
                    });
                    f.getListaDeLigacoes().stream().filter(l -> l instanceof SuperLinha).forEach(lfl -> {
                        AdicioneSubsFromRealce(res, lfl);
                    });
                } else if (item instanceof SuperLinha) {
                    AdicionePrinFromRealce(res, item);
                    SuperLinha sl = (SuperLinha) item;
                    res.add(sl.getFormaPontaA());
                    res.add(sl.getFormaPontaB());
                }
            });
            getListaDeItens().stream().filter(f -> res.indexOf(f) == -1).forEach(fo -> fo.setDisablePainted(true));
        }
        repaint();
    }

    /**
     * Função acionada no momento de se coletar os artefatos que são parte de um artefato princiapal no momento de realçar o diagrama.<br>
     * É chamada pela função AdicionePrinFromRealce(...)<br>
     *
     * @param res: lista de eleentos coletado.<br>
     * @param item: item a ser analisado.
     */
    protected void AdicioneSubsFromRealce(ArrayList<FormaElementar> res, FormaElementar item) {
        if (item instanceof Forma) {
            Forma f = (Forma) item;
            if (f.isParte()) {
                res.add((Forma) f.getPrincipal());
                return;
            }
        }
        res.add(item);
    }

    /**
     * Função acionada no momento de se coletar os artefatos no momento de realçar o diagrama.<br>
     * <br>
     *
     * @param res: lista de eleentos coletado.<br>
     * @param item: item a ser analisado.
     */
    protected void AdicionePrinFromRealce(ArrayList<FormaElementar> res, FormaElementar item) {
        AdicioneSubsFromRealce(res, item);
    }

    private final String v300 = "3.0.0";
    private final String v310 = "3.1.0";

    public boolean LoadVersao(String fromXml) {
        if (fromXml == null || fromXml.isEmpty()) {
            return false;
        }
        ArrayList<String> ver = new ArrayList<>();
        ver.add(v300);
        ver.add(v310);
        ver.add(Diagrama.VERSAO_A + "." + Diagrama.VERSAO_B + "." + Diagrama.VERSAO_C); //Atual
        
        fromXml = fromXml.trim();
        if (ver.indexOf(ver) == -1) {
            return false;
        }
        
        String[] v = fromXml.split("\\.");
        versaoA = v[0];
        versaoB = v[1];
        versaoC = v[2];
        

        return true;
    }
}