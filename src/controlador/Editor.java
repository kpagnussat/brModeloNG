/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador;

import controlador.apoios.Historico;
import controlador.editores.DrawerEditor;
import controlador.editores.LegendaEditor;
import controlador.inspector.Inspector;
import controlador.inspector.InspectorDicas;
import controlador.inspector.InspectorExtenderEditor;
import controlador.inspector.InspectorItemBase;
import controlador.inspector.InspectorProperty;
import desenho.Elementar;
import desenho.FormaElementar;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import principal.Aplicacao;
import util.CopFormatacao;

/**
 *
 * @author Rick
 */
public class Editor extends BaseControlador implements KeyListener {

    // Keep the historical Swing editor descriptor; scheduling state is transient.
    private static final long serialVersionUID = -2784985447407333183L;

    public Diagrama diagramaAtual;
    private Selecionador multSel = null;
    public static final Configuer fromConfiguracao = new Configuer();

    public static Controler fromControler() {
        return _fromControler;
    }
    private static Controler _fromControler = null;

    private JScrollPane parente = null;
    private Mostrador showDiagramas = null;

    public util.CopFormatacao CopiadorFormatacao = new CopFormatacao();

    public Editor() {
        super();
        setDoubleBuffered(true);
        initBox();
        historicos.add(diagramaAtual);
        setFocusable(true);
        addKeyListener(this);
        _fromControler = controler;
        setFocusTraversalKeysEnabled(false);
        // <editor-fold defaultstate="collapsed" desc="Teclas">
        KeyStroke key_crtl = KeyStroke.getKeyStroke(util.Atalhos.tecla(), util.Atalhos.mascara());
        KeyStroke key_crtl_up = KeyStroke.getKeyStroke(util.Atalhos.tecla(), 0, true);

        KeyStroke key_crtlEshift = KeyStroke.getKeyStroke(util.Atalhos.tecla(), util.Atalhos.mascara() | InputEvent.SHIFT_DOWN_MASK, false);
        KeyStroke key_shiftEcrtl = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, util.Atalhos.mascara() | InputEvent.SHIFT_DOWN_MASK, false);
        KeyStroke key_crtlEshift_up = KeyStroke.getKeyStroke(util.Atalhos.tecla(), InputEvent.SHIFT_DOWN_MASK, true);
        KeyStroke key_shiftEcrtl_up = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, util.Atalhos.mascara(), true);

        KeyStroke key_shift = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, InputEvent.SHIFT_DOWN_MASK);
        KeyStroke key_shift_up = KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true);
        KeyStroke key_alt = KeyStroke.getKeyStroke(KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK);
        KeyStroke key_alt_up = KeyStroke.getKeyStroke(KeyEvent.VK_ALT, 0, true);

        final String ac_crtl = "AC_CRTL";
        final String ac_crtl_up = "AC_CRTL_UP";

        final String ac_crtlEshift = "AC_CRTLeSHIFT";
        final String ac_shiftEcrtl = "AC_SHIFTeCRTL";
        final String ac_crtlEshift_up = "AC_CRTLeSHIFT_UP";
        final String ac_shiftEcrtl_up = "AC_SHIFTeCRTL_UP";

        final String ac_shift = "AC_SHIFT";
        final String ac_shift_up = "AC_SHIFT_UP";
        final String ac_alt = "AC_ALT";
        final String ac_alt_up = "AC_ALT_UP";

        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(key_crtl, ac_crtl);
        inputMap.put(key_crtl_up, ac_crtl_up);

        inputMap.put(key_crtlEshift, ac_crtlEshift);
        inputMap.put(key_shiftEcrtl, ac_shiftEcrtl);
        inputMap.put(key_crtlEshift_up, ac_crtlEshift_up);
        inputMap.put(key_shiftEcrtl_up, ac_shiftEcrtl_up);

        inputMap.put(key_shift, ac_shift);
        inputMap.put(key_shift_up, ac_shift_up);
        inputMap.put(key_alt, ac_alt);
        inputMap.put(key_alt_up, ac_alt_up);

        Action al_crtl = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controlDown = true;
            }
        };
        Action al_crtl_up = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controlDown = false;
            }
        };

        Action al_crtlEshift = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controlDown = true;
                shiftDown = true;
            }
        };
        Action al_shiftEcrtl = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controlDown = true;
                shiftDown = true;
            }
        };
        Action al_crtlEshift_up = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                controlDown = false;
            }
        };
        Action al_shiftEcrtl_up = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shiftDown = false;
            }
        };

        Action al_shift = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shiftDown = true;
            }
        };
        Action al_shift_up = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                shiftDown = false;
            }
        };
        Action al_alt = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                altDown = true;
            }
        };
        Action al_alt_up = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!altDown) {
                    /////bug do java ao ler o key AltGr do teclado em Windows - swing
                    controlDown = false;
                }
                altDown = false;
            }
        };

        ActionMap actionMap = getActionMap();
        actionMap.put(ac_crtl, al_crtl);

        actionMap.put(ac_crtlEshift, al_crtlEshift);
        actionMap.put(ac_shiftEcrtl, al_shiftEcrtl);
        actionMap.put(ac_crtlEshift_up, al_crtlEshift_up);
        actionMap.put(ac_shiftEcrtl_up, al_shiftEcrtl_up);

        actionMap.put(ac_crtl_up, al_crtl_up);
        actionMap.put(ac_shift, al_shift);
        actionMap.put(ac_shift_up, al_shift_up);
        actionMap.put(ac_alt, al_alt);
        actionMap.put(ac_alt_up, al_alt_up);
        setActionMap(actionMap);
// </editor-fold>

    }
    private QuadroDeEdicao box;

    public int getMargem() {
        return box.getEditorMargem();
    }

    private void initBox() {
        box = new QuadroDeEdicao(this);
        this.setSize((2 * box.getEditorMargem()) + box.getEditorAtualWidth(), (2 * box.getEditorMargem()) + box.getEditorAtualHeigth());
        this.setPreferredSize(this.getSize());
        this.setLayout(null);

        this.add(box);

        box.setSize(box.getEditorAtualWidth(), box.getEditorAtualHeigth());
        box.setPreferredSize(box.getSize());
        box.setLocation(box.getEditorMargem(), box.getEditorMargem());
        PreCarregueConfig();
        diagramaAtual = Novo(null);
        prepareDiagramaAtual();
    }

    public void CarregueConfig() {
        PerformInspectorCfg();
    }

    private void PreCarregueConfig() {
        ConfiguracaoEditor.PreCarregueConfig(this);
    }

    public QuadroDeEdicao getBox() {
        return box;
    }

    public void setBox(QuadroDeEdicao box) {
        this.box = box;
    }

    public void AjusteTamanho() {
        this.setSize((2 * box.getEditorMargem()) + box.getEditorAtualWidth(), (2 * box.getEditorMargem()) + box.getEditorAtualHeigth());
        this.setPreferredSize(this.getSize());

        box.setSize(box.getEditorAtualWidth(), box.getEditorAtualHeigth());
        box.setPreferredSize(box.getSize());
    }

    private ISuperControler FramePrincipal;

    public ISuperControler getFramePrincipal() {
        return FramePrincipal;
    }

    public void setFramePrincipal(ISuperControler FramePrincipal) {
        this.FramePrincipal = FramePrincipal;
    }

    //<editor-fold defaultstate="collapsed" desc="Teclas">
    @Override
    public void keyTyped(KeyEvent e) {
        String letras = " !@#$%&*()-_+='\"[{]},.;:<>|\\/?¬£¢§ªº}";
        if (diagramaAtual.getSelecionado() != null && (Character.isLetterOrDigit(e.getKeyChar()) || letras.indexOf(e.getKeyChar()) > -1)) {
            InspectorItemBase ex = getInspectorEditor().FindByProperty("setTexto");
            if (ex == null && diagramaAtual.getTipo() == Diagrama.TipoDeDiagrama.tpLogico) {
                ex = getInspectorEditor().FindByProperty("Campo.setTexto");
            }
            if (ex != null && ex.CanEdit()) {
                if (ex.getOndeEditar() instanceof JTextField) {
                    getInspectorEditor().PerformSelect(ex);
                    ex.getOndeEditar().requestFocus();
                    ((JTextField) (ex.getOndeEditar())).setText((isApagarTextoAoEditar() ? String.valueOf(e.getKeyChar()) : ex.getValor() + String.valueOf(e.getKeyChar())));
                    e.consume();
                } else {
                    if (ex.getOndeEditar() instanceof InspectorExtenderEditor) {
                        InspectorExtenderEditor tmx = (InspectorExtenderEditor) ex.getOndeEditar();
                        if (tmx.getAcaoTipo() == InspectorExtenderEditor.TipoDeAcao.tpAcaoDlgTexto) {
                            getInspectorEditor().PerformSelect(ex);
                            tmx.RunDlg((isApagarTextoAoEditar() ? String.valueOf(e.getKeyChar()) : ex.getValor() + String.valueOf(e.getKeyChar())));
                            shiftDown = false;
                            altDown = false;
                            controlDown = false;
                            e.consume();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (diagramaAtual != null) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_UP:
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_ENTER:
                    diagramaAtual.ProcesseTeclas(e);
                    break;
                case KeyEvent.VK_DELETE:
                    if (diagramaAtual != null) {
                        diagramaAtual.deleteSelecao();
                        e.consume();
                    }
                    break;
                case KeyEvent.VK_TAB:
                    if (e.isControlDown()) {
                        if (e.isShiftDown()) {
                            if (diagramaAtual.SelecioneAnterior()) {
                                e.consume();
                            }
                        } else if (diagramaAtual.SelecioneProximo()) {
                            e.consume();
                        }
                    } else {
                        transferFocus();
                    }
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
    private boolean shiftDown = false, altDown = false, controlDown = false;

    /**
     * @return the shiftDown
     */
    public boolean isShiftDown() {
        return shiftDown;
    }

    public void setShiftDown(boolean shiftDown) {
        this.shiftDown = shiftDown;
    }

    public boolean isAltDown() {
        return altDown;
    }

    public void setAltDown(boolean altDown) {
        this.altDown = altDown;
    }

    /**
     * @return the controlDown
     */
    public boolean isControlDown() {
        return controlDown;
    }

    public void setControlDown(boolean controlDown) {
        this.controlDown = controlDown;
    }
    // </editor-fold>

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); //paint background

        if (diagramaAtual == null) {
            return;
        }

        RenderingHints renderHints
                = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        renderHints.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        Graphics2D Canvas = (Graphics2D) g;
        Canvas.addRenderingHints(renderHints);

        if (!diagramaAtual.isPinturaExterna()) {
            util.MesaDiagrama.pintar(Canvas, box.getBounds());
        }
    }

    /**
     * Retorna o selecionador.
     *
     * @return the multSel
     */
    public Selecionador getMultSel() {
        return multSel;
    }

    public void FinishMultiSel() {
        if (multSel == null) {
            return;
        };
        diagramaAtual.Remove(multSel, false);
        getMultSel().Finish();
    }

    public void InitMultiSel(Point _local) {
        multSel = new Selecionador(diagramaAtual);
        getMultSel().Init(_local);
    }

    //<editor-fold defaultstate="collapsed" desc="Configurações">
    private boolean mostrarDimensoesAoMover = true;
    private boolean showGrid = false;
    private int gridWidth = 20;

    private Diagrama.TipoDeDiagrama tipoDefault = Diagrama.TipoDeDiagrama.tpEap;

    public Diagrama.TipoDeDiagrama getTipoDefault() {
        return tipoDefault;
    }

    public void setTipoDefault(Diagrama.TipoDeDiagrama tipoDefault) {
        this.tipoDefault = tipoDefault;
    }

    public void setTipoDefaultInt(int tipoDefault) {
        if (tipoDefault < Diagrama.TipoDeDiagrama.values().length && tipoDefault > -1) {
            this.tipoDefault = Diagrama.TipoDeDiagrama.values()[tipoDefault];
        }
    }

    public boolean isPropagueDeleteToLines() {
        return propagueDeleteToLines;
    }

    public void setPropagueDeleteToLines(boolean propagueDeleteToLines) {
        this.propagueDeleteToLines = propagueDeleteToLines;
    }

    private boolean propagueDeleteToLines = true;

    public int getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(int GridWidth) {
        if (GridWidth <= 0 || GridWidth > 600) {
            GridWidth = 20;
        }
        this.gridWidth = GridWidth;
        repaint();
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }

    public boolean isMostrarDimensoesAoMover() {
        return mostrarDimensoesAoMover;
    }

    public void setMostrarDimensoesAoMover(boolean mostrarDimensoesAoMover) {
        this.mostrarDimensoesAoMover = mostrarDimensoesAoMover;
    }

    private Inspector editorConfig = null;

    public Inspector getEditorConfig() {
        return editorConfig;
    }

    public void setEditorConfig(Inspector editorConfig) {
        this.editorConfig = editorConfig;
        if (editorConfig != null) {
            editorConfig.setEditor(this);
        }
    }

    private ArrayList<InspectorProperty> GenerateProperty() {
        return ConfiguracaoEditor.GenerateProperty(this);
    }

    private boolean salvarLocation = true;

    public void setSalvarLocation(boolean sn) {
        salvarLocation = sn;
    }

    public boolean isSalvarLocation() {
        return salvarLocation;
    }

    void PerformInspectorCfg() {
        setTextoDica(null, "");
        getEditorConfig().Carrege(GenerateProperty());
    }

    private boolean AceitaEdicao(InspectorProperty propriedade, String valor) {
        return ConfiguracaoEditor.AceitaEdicao(this, propriedade, valor);
    }

    //<editor-fold defaultstate="collapsed" desc="Zoom">
    /**
     * @return the zoom
     */
    public double getZoom() {
        return box.getZoom();
    }

    /**
     * Limite 10% - 800%
     *
     * @param zoom the zoom to set
     */
    public void setZoom(double zoom) {
        if (zoom < 0.1 || zoom > 8) {
            zoom = 1.0;
        }
        box.setZoom(zoom);
        AjusteTamanho();
        if (diagramaAtual != null) {
            diagramaAtual.setZoom(zoom);
            diagramaAtual.PerformInspector();
            if (lblZoom != null) {
                lblZoom.setText(Double.toString(zoom * 100) + "%");
            }
        } else if (lblZoom != null) {
            lblZoom.setText("");
        }
    }

    /**
     * @param zoom the zoom to set
     */
    private void SetZoom(double zoom) {
        if (zoom < 0.1 || zoom > 4) {
            zoom = 1.0;
        }
        box.setZoom(zoom);
        AjusteTamanho();
        if (lblZoom != null) {
            lblZoom.setText(Double.toString(zoom * 100) + "%");
        }
    }

    public final double[] zoonsd = new double[]{
        0.125, 0.25, 0.5, 0.75, 1.0, 1.25, 1.50, 1.75, 2.0, 3.0, 4.0, 5.0
    };

    public void ZoomMenos() {
        double z = getZoom();
        int p = -1;
        for (int i = 0; i < zoonsd.length; i++) {
            if (zoonsd[i] == z) {
                p = i - 1;
            }
        }
        if (p < 0) {
            return;
        }
        z = zoonsd[p];

        setZoom(z);
    }

    public void ZoomMais() {
        double z = getZoom();
        int p = zoonsd.length;
        for (int i = 0; i < zoonsd.length; i++) {
            if (zoonsd[i] == z) {
                p = i + 1;
            }
        }
        if (p > zoonsd.length - 1) {
            return;
        }
        z = zoonsd[p];
        setZoom(z);
    }

    private JLabel lblZoom = null;

    public void setLabelZoom(JLabel lblZoom) {
        this.lblZoom = lblZoom;
        lblZoom.setText("100.0%");
    }
    //</editor-fold>

    public void setMostrarAreaImpressao(boolean mostrarAreaImpressao, int wdt, int ht) {
        this.box.setMostrarAreaImpressao(mostrarAreaImpressao, wdt, ht);
    }

    /**
     * Último total de itens do modelo. Redesenha o Tree apenas no caso da
     * quantidade de itens mudar
     */
    private int lastTreeCount = -1;

    /**
     * Atualiza uma lista de elementos visíveis para criar uma navegação.
     */
    public void AtualizeTreeNavegacao() {
        int tl = diagramaAtual.AtualizeTreeNavegacao(true).getChildCount();
        //só atualiza o Treeview se houver alterado a quantidade de elementos 
        if (tl != lastTreeCount) {
            lastTreeCount = tl;
            FramePrincipal.DoComandoExterno(Controler.menuComandos.cmdTreeNavegador);
        }
    }

    /**
     * Rola o Scroll para mostra a coordenada x, y
     *
     * @param co - Coordenadas
     */
    public void Mostre(Point co) {
        scrolling = true;
        parente.getHorizontalScrollBar().setValue(co.x);
        parente.getVerticalScrollBar().setValue(co.y);
        scrolling = false;
        RegistePosicao();
    }

    /**
     * Recebe o evento de MouseWheel do quadro de edição se não consumido.
     *
     * @param e
     */
    /**
     * Mouse-wheel scrolling of the diagram, following the usual desktop convention:
     * wheel = vertical, Shift+wheel = horizontal (Ctrl+wheel = zoom, handled in Diagrama).
     * Upstream picked the axis by whichever viewport edge (right or bottom) the pointer
     * was closer to, so the same gesture scrolled in different directions depending on
     * where the mouse was. Shift also covers touchpads and tilt wheels: AWT on Linux
     * delivers horizontal wheel events as Shift+wheel.
     */
    public void ScrollMove(MouseWheelEvent e) {
        javax.swing.JScrollBar barra = e.isShiftDown()
                ? parente.getHorizontalScrollBar()
                : parente.getVerticalScrollBar();
        barra.setValue(barra.getValue() + 2 * e.getUnitsToScroll());
        e.consume();
    }

    public boolean IsOpen(File arq) {
        return ArquivosEditor.IsOpen(this, arq);
    }

    //<editor-fold defaultstate="collapsed" desc="Arquivos recentes">
    private ArrayList<String> recentes = new ArrayList<>();

    public ArrayList<String> getRecentes() {
        return recentes;
    }

    public void setRecentes(ArrayList<String> recentes) {
        this.recentes = recentes;
    }

    public void addLastOpened(String arq) {
        recentes.remove(arq);
        recentes.add(0, arq);
        reloadMenuRecentes();
    }

    private JMenu MenuRecente = null;

    public JMenu getMenuRecente() {
        return MenuRecente;
    }

    public void setMenuRecente(JMenu MenuRecente) {
        this.MenuRecente = MenuRecente;
    }

    public void reloadMenuRecentes() {
        ArquivosRecentes.reloadMenuRecentes(this, () -> new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                File arq = new File(((JMenuItem) ae.getSource()).getToolTipText());
                if (arq.exists()) {
                    AbrirDiagramaFromFile(arq);
                } else {
                    JOptionPane.showMessageDialog(getParent(), "Diagrama não pode ser aberto. Arquivo não encontrado.", "Informação", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
    }
    //</editor-fold>

    /**
     * Usado para verificar se o mesmo diagrama já não estar aberto ou se ao
     * Savar Como não se sobrescreveu um dos arquivos do diagrama aberto.
     *
     * @param Diagrama
     */
    void ChecarArquivosBiAbertos(Diagrama diag) {
        ArquivosEditor.ChecarArquivosBiAbertos(this, diag);
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Outros Editores">
    /**
     * Quando precisar usar o Inspector para editar propriedades de qualquer
     * outro objeto fora do escopo do configEditor ou InspectorEditor, basta que
     * a classe que efetivará as edições implemente esta interface. Para o
     * efeito de carregar os itens no Inspector, a própria classe o fará, a
     * exemplo do editor DrawerEditor
     */
    public interface iParaOutrosInspectors {

        boolean AceitaEdicao(InspectorProperty propriedade, String valor);

        boolean ProcesseCmdFromInspector(String property);
    }

    /**
     * Inspector "curinga" para uso diverso
     */
    private Inspector outroInspector = null;
    private iParaOutrosInspectors falsoEditor = null;

    public void InicieNovoInspector(Inspector quem, iParaOutrosInspectors falsoEdt) {
        falsoEditor = falsoEdt;
        outroInspector = quem;
        outroInspector.setEditor(this);
    }

    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Menus e botões">
    final Controler controler = new Controler(this);

    public Controler getControler() {
        return controler;
    }

    /** Applies a name or value still open in an Inspector field; see Inspector.ConfirmeTextoPendente. */
    public void ConfirmeEdicaoInspector() {
        if (InspectorEditor != null) {
            InspectorEditor.ConfirmeTextoPendente();
        }
        if (editorConfig != null) {
            editorConfig.ConfirmeTextoPendente();
        }
    }

    public void DoActionExtender(ActionEvent ev) {
        ConfirmeEdicaoInspector();
        if (diagramaAtual != null) {
            if (ev == null || ev.getActionCommand() == null || ev.getActionCommand().isEmpty()) {
                return;
            }
            try {
                Controler.menuComandos cmd = Controler.menuComandos.valueOf(ev.getActionCommand());
                if (ArquivosEditor.processeComando(this, cmd, ev)) return;
                switch (cmd) {
                    case cmdRendo:
                        if (!refazer()) {
                            util.BrLogger.Logger("ERROR_CMD_RENDO", null);
                        }
                        controler.makeEnableComands();
                        break;
                    case cmdUndo:
                        if (!desfazer()) {
                            util.BrLogger.Logger("ERROR_CMD_UNDO", null);
                        }
                        controler.makeEnableComands();
                        break;
                    case cmdDel:
                        diagramaAtual.deleteSelecao();
                        controler.makeEnableComands();
                        break;
                    case cmdSelectAllByType:
                        diagramaAtual.SelecioneTodosDoTipo();
                        controler.makeEnableComands();
                        break;
                    case cmdSelectAll:
                        diagramaAtual.SelecioneTodos();
                        controler.makeEnableComands();
                        break;
                    case cmdSelProx:
                        diagramaAtual.SelecioneProximo();
                        break;
                    case cmdBringToFront:
                        if (diagramaAtual.getSelecionado() != null) {
                            diagramaAtual.getSelecionado().BringToFront();
                            diagramaAtual.getSelecionado().Invalidate();
                        }
                        break;
                    case cmdSendToBack:
                        if (diagramaAtual.getSelecionado() != null) {
                            diagramaAtual.getSelecionado().SendToBack();
                            diagramaAtual.getSelecionado().Invalidate();
                        }
                        break;

                    case cmdSelAnt:
                        diagramaAtual.SelecioneAnterior();
                        break;

                    case cmdMicroAjuste0:
                        diagramaAtual.ProcesseTeclas(KeyEvent.VK_LEFT);
                        break;
                    case cmdMicroAjuste1:
                        diagramaAtual.ProcesseTeclas(KeyEvent.VK_UP);
                        break;
                    case cmdMicroAjuste2:
                        diagramaAtual.ProcesseTeclas(KeyEvent.VK_DOWN);
                        break;
                    case cmdMicroAjuste3:
                        diagramaAtual.ProcesseTeclas(KeyEvent.VK_RIGHT);
                        break;

                    case cmdCopy:
                        diagramaAtual.doCopy();
                        break;
                    case cmdCopyFormat:
                        if (diagramaAtual.getSelecionado() != null) {
                            CopiadorFormatacao.Copiar(diagramaAtual.getSelecionado());
                            controler.makeEnableComands();
                        }
                        break;

                    case cmdDimPastLeft:
                    case cmdDimPastTop:
                    case cmdDimPastRight:
                    case cmdDimPastBottom:
                    case cmdDimPastWidth:
                    case cmdDimPastHeight:
                    case cmdDimAlignH:
                    case cmdDimAlignV:
                        if (diagramaAtual.getSelecionado() != null) {
                            ArrayList<FormaElementar> lst = new ArrayList<>();
                            diagramaAtual.getItensSelecionados().stream().forEach(f -> lst.add(f));
                            diagramaAtual.setSelecionado(null);
                            CopiadorFormatacao.Ajustar(lst, cmd, diagramaAtual.AlinhamentoH(), diagramaAtual.AlinhamentoV());
                            diagramaAtual.DoMuda(diagramaAtual.getSelecionado());
                            //Seleciona novamente.
                            lst.stream().forEach(el -> diagramaAtual.DiagramaDoSelecao(el, false, true));
                            diagramaAtual.PromoveToFirstSelect(lst.get(0));
                        }
                        break;

                    case cmdPasteFormat:
                        if (diagramaAtual.getSelecionado() != null) {
                            CopiadorFormatacao.Colar(diagramaAtual.getItensSelecionados());
                        }
                        break;
                    case cmdCut:
                        diagramaAtual.doCopy();
                        diagramaAtual.deleteSelecao();
                        controler.makeEnableComands();
                        break;

                    case cmdRealcar:
                        diagramaAtual.setRealce(!diagramaAtual.isRealce());
                        controler.makeEnableComands();
                        break;

                    case cmdPaste:
                        diagramaAtual.doPaste();
                        controler.makeEnableComands();
                        break;

                    case cmdNew:
                        if (ev.getSource() instanceof JMenuItem) {
                            JMenuItem tmp = (JMenuItem) ev.getSource();
                            Add(tmp.getName());
                            RePopuleBarraDiagramas(true);
                        } else if (ev.getSource() instanceof JButton) {
                            JButton tmp = (JButton) ev.getSource();

                            Add(tmp.getName());
                            RePopuleBarraDiagramas(true);
                        }
                        break;

                    case cmdPrint:
                        FramePrincipal.DoComandoExterno(cmd);
                        break;
                    case cmdClose:
                        Diagrama afechar = diagramaAtual;
                        int idx = getDiagramas().indexOf(afechar) + 1;
                        int tam = getDiagramas().size();
                        if (idx == tam) {
                            idx -= 2;
                        }
                        if (idx > -1) {
                            Diagrama nv = getDiagramas().get(idx);
                            if (FechaDiagrama(afechar, nv)) {
                                diagramaAtual = nv;
                                prepareDiagramaAtual();
                            }
                        } else {
                            FechaDiagrama(afechar, null);
                        }
                        controler.makeEnableComands();
                        RePopuleBarraDiagramas(true);
                        break;
                    default:
                        getDicas().setTexto(cmd.toString());
                }
            } catch (Exception e) {
                util.BrLogger.Logger("ERROR_CMD_INVALID", ev.getActionCommand(), e.getMessage());
            }
        }
    }

    private java.util.concurrent.CompletableFuture<Diagrama> AbrirDiagramaFromFile(File arq) {
        return ArquivosEditor.AbrirDiagramaFromFile(this, arq);
    }

    void ProcessePosOpen(Diagrama res, boolean isXml) {
        ArquivosEditor.ProcessePosOpen(this, res, isXml);
    }

    /**
     * A palette tool chosen with a double click stays active after each object it creates,
     * until Escape, the arrow or another tool (issue #5 of the original brModelo). Holding
     * Ctrl while clicking on the sheet keeps the tool for that click, as before.
     */
    private transient boolean ferramentaFixa = false;

    public boolean isFerramentaFixa() {
        return ferramentaFixa;
    }

    /** Called by a double click on a palette tool, after its two single clicks chose it. */
    public void FixarFerramenta() {
        if (diagramaAtual != null && diagramaAtual.getComando() != null) {
            ferramentaFixa = true;
            setTextoDica(null, fromConfiguracao.getValor("Controler.interface.FerramentaFixa.dica"));
        }
    }

    public void DoAction(ActionEvent ev) {
        ferramentaFixa = false;
        if (diagramaAtual != null) {
            diagramaAtual.DoAction(ev);
            boolean eacao = false;
            Acao ac = null;
            if (((AbstractButton) ev.getSource()).getAction() instanceof Acao) {
                eacao = true;
                ac = (Acao) ((AbstractButton) ev.getSource()).getAction();
                try {
                    setTextoDica(null, ac.getValue(Action.SHORT_DESCRIPTION).toString());
                } finally {
                }
            }
            if (ev.getSource() instanceof JMenuItem) {
                if (eacao) {
                    controler.SelecioneForAction(ac);
                }
            }
        }
    }

    public void PerformInspectorFor(FormaElementar itemSel) {
        setTextoDica(null, "");
        getInspectorEditor().Carrege(itemSel.GenerateFullProperty());
        EditoresRefresh(itemSel);
        FramePrincipal.DoComandoExterno(Controler.menuComandos.cmdTreeSelect);
    }

    public boolean AceitaEdicao(Inspector origem, InspectorProperty propriedade, String valor) {
        if (origem == editorConfig) {
            return AceitaEdicao(propriedade, valor);
        }
        if (origem == outroInspector) {
            return falsoEditor.AceitaEdicao(propriedade, valor);
        }
        if (diagramaAtual != null) {
            return diagramaAtual.AceitaEdicao(propriedade, valor);
        }
        return false;
    }

    public void PopuleBarra(JComponent tollBar) {
        controler.PopuleBarra(tollBar);
        if (diagramaAtual != null) {
            controler.AjusteBarra(diagramaAtual.meusComandos);
        }
    }

    /**
     * Lista os modelos na Barra.
     */
    public void RePopuleBarraDiagramas(boolean reset) {
        if (showDiagramas != null) {
            if (reset) {
                showDiagramas.Reset(historicos.getDiagramas().indexOf(diagramaAtual));
            } else {
                showDiagramas.repaint();
            }
        }
    }

    public void PopuleBarra(JMenu menu) {
        controler.PopuleBarra(menu);
    }

    public void PopuleMenus(JMenu MenuEditar, JMenu MenuArquivo, JPopupMenu popup) {
        controler.PopuleMenus(MenuEditar, MenuArquivo, popup);
    }

    public void NoAction() {
        ferramentaFixa = false;
        controler.BtnNothing.setSelected(true);
    }
    // </editor-fold>

    private InspectorDicas Dicas = null;

    public InspectorDicas getDicas() {
        return Dicas;
    }

    public void setDicas(InspectorDicas Dicas) {
        this.Dicas = Dicas;
    }

    private Inspector InspectorEditor = null;

    public Inspector getInspectorEditor() {
        return InspectorEditor;
    }

    public void setInspectorEditor(Inspector InspectorEditor) {
        this.InspectorEditor = InspectorEditor;
        InspectorEditor.setEditor(this);
        diagramaAtual.PerformInspector();
    }

    public void setTextoDica(Inspector quem, String txt) {
        if (Dicas != null) {
            Dicas.setTexto(txt);
        }
    }

    public static String getClassTexto(Elementar obj) {
        return obj.getClass().getSimpleName();
    }

    public boolean SelectItemByID(int id) {
        if (diagramaAtual != null) {
            FormaElementar res = diagramaAtual.FindByID(id);
            if (res != null) {
                return diagramaAtual.DiagramaDoSelecao(res, false, false);
            }
        }
        return false;
    }

    public final void Add(String pa) {
        ArrayList<String> lst = new ArrayList<>();
        for (Diagrama.TipoDeDiagrama tp : Diagrama.TipoDeDiagrama.values()) {
            lst.add(tp.name());
        }
        int res = lst.indexOf(pa);
        if (res == -1) {
            return;
        }
        diagramaAtual = Novo(Diagrama.TipoDeDiagrama.values()[res]);
        prepareDiagramaAtual();
        historicos.add(diagramaAtual);
    }

    public final Diagrama Add() {
        diagramaAtual = Novo(null);
        prepareDiagramaAtual();
        historicos.add(diagramaAtual);
        return diagramaAtual;
    }

    public Diagrama AddAsAtual(String strTipo) {
        Add(strTipo);
        RePopuleBarraDiagramas(true);
        return diagramaAtual;
    }

    public Diagrama Novo(Diagrama.TipoDeDiagrama tipo) {
        //lembrando que sempre deve haver um diagrama aberto.
        if (tipo == null) {
            tipo = getTipoDefault();//Default!!!
        }
        return Diagrama.Factory(tipo, this);
    }

    private transient boolean scrolling = false;

    public void setParente(JScrollPane parente) {
        this.parente = parente;
        parente.getHorizontalScrollBar().setBlockIncrement(100);
        parente.getVerticalScrollBar().setBlockIncrement(100);
        parente.getHorizontalScrollBar().setUnitIncrement(10);
        parente.getVerticalScrollBar().setUnitIncrement(10);

        parente.getHorizontalScrollBar().addAdjustmentListener((AdjustmentEvent ae) -> {
            if (!scrolling) {
                RegistePosicao();
            }
        });
        parente.getVerticalScrollBar().addAdjustmentListener((AdjustmentEvent ae) -> {
            if (!scrolling) {
                RegistePosicao();
            }
        });
    }

    /**
     * Atualiza a camada de apresentação da aplicação a pedido do Diagrama.
     */
    //<editor-fold defaultstate="collapsed" desc="Histórico">
    public void DoDiagramaMuda() {
        RegistePosicao();
        historicos.append(diagramaAtual);
        controler.makeEnableComands();
        AtualizeTreeNavegacao();
        doneAutoSave = false;
    }

    public void RegistePosicao() {
        if (parente != null) {
            diagramaAtual.ScrPosicao = new Point(parente.getHorizontalScrollBar().getValue(), parente.getVerticalScrollBar().getValue());
        }
    }

    public void prepareDiagramaAtual() {
        try {
            SetZoom(diagramaAtual.getZoom());
            if (parente != null) {
                scrolling = true;
                parente.getHorizontalScrollBar().setValue(diagramaAtual.ScrPosicao.x);
                parente.getVerticalScrollBar().setValue(diagramaAtual.ScrPosicao.y);
                scrolling = false;
            }

            controler.makeEnableComands();

            //Ocorria uma exceção no momento do start da aplicação, já que muitas partes do Editor ainda não esta linkado.
            if (getInspectorEditor() != null) {
                diagramaAtual.PerformInspector();
                controler.AjusteBarra(diagramaAtual.meusComandos);

                if (showDiagramas != null) {
                    showDiagramas.repaint();
                }
                repaint();
                lastTreeCount = -1;
                AtualizeTreeNavegacao();

                diagramaAtual.populeComandos(controler.BarraMenu.getItem(0));
                diagramaAtual.ExternalSuperAncorador();
            }

        } catch (Exception e) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_PREPARE", e.getMessage());
        }
    }

    final Historico historicos = new Historico(this);

    public boolean desfazer() {
        Diagrama res = historicos.desfazer(diagramaAtual);
        if (res != null) {
            double z = diagramaAtual.getZoom();
            diagramaAtual = res;
            diagramaAtual.setZoom(z);
            prepareDiagramaAtual();
            return true;
        }
        return false;
    }

    public boolean refazer() {
        Diagrama res = historicos.refazer(diagramaAtual);
        if (res != null) {
            double z = diagramaAtual.getZoom();
            diagramaAtual = res;
            diagramaAtual.setZoom(z);
            prepareDiagramaAtual();
            return true;
        }
        return false;
    }

    public boolean podeDesfazer() {
        return historicos.podeDesfazer(diagramaAtual);
    }

    public boolean podeRefazer() {
        return historicos.podeRefazer(diagramaAtual);
    }
    //</editor-fold>

    private transient LegendaEditor editorLegenda = null;

    /**
     * Chamado pelo Inspector quando for lançado um comando
     *
     * @param who
     * @param property
     * @return
     */
    public boolean ProcesseCmdFromInspector(Inspector who, String property) {

        if (who == editorConfig) {
            return false;
        }
        if (who == outroInspector) {
            return falsoEditor.ProcesseCmdFromInspector(property);
        }

        FormaElementar.nomeComandos cmd;
        try {
            cmd = FormaElementar.nomeComandos.valueOf(property);
        } catch (Exception e) {
            return false;
        }

        switch (cmd) {
            case cmdLoadImg:
                if (diagramaAtual != null && diagramaAtual.getSelecionado() instanceof desenho.formas.Desenhador) {
                    String res = util.Dialogos.ShowDlgFileImg(this.getRootPane());
                    if (res == null) {
                        return true; //não é erro, foi cancelado pelo usuário.
                    }
                    getFramePrincipal().Super_Esperando();
                    if (((desenho.formas.Desenhador) diagramaAtual.getSelecionado()).LoadImageFromFile(res)) {
                        getFramePrincipal().Super_Pronto();
                        PerformInspectorFor(diagramaAtual.getSelecionado());
                        return true;
                    }
                    getFramePrincipal().Super_Pronto();
                    PerformInspectorFor(diagramaAtual.getSelecionado());
                }
                break;
            case cmdDlgLegenda:
                if (diagramaAtual != null && diagramaAtual.getSelecionado() instanceof desenho.formas.Legenda) {
                    if (((desenho.formas.Legenda) diagramaAtual.getSelecionado()).canShowEditor()) {
                        EditorLegendaCarregue(diagramaAtual.getSelecionado());
                        return true;
                    }
                }
                break;
            case cmdCallDrawerEditor:
                if (diagramaAtual != null && diagramaAtual.getSelecionado() instanceof desenho.preDiagrama.iBaseDrawer) {
                    EditorBaseDrawerCarregue(diagramaAtual.getSelecionado());
                    return true;
                }
                break;
            case cmdExcluirSubItem:
                if (diagramaAtual != null) {
                    diagramaAtual.getSelecionado().ExcluirSubItem(who.getSelecionado().getPropriedade().Tag);
                    PerformInspectorFor(diagramaAtual.getSelecionado());
                    return true;
                }
                break;
            case cmdFonte:
                if (diagramaAtual != null) {
                    FormaElementar fe;
                    if (diagramaAtual.getSelecionado() == null) {
                        fe = diagramaAtual.infoDiagrama;
                    } else {
                        fe = diagramaAtual.getSelecionado();
                    }
                    Font f = fe.getFont();
                    f = util.Dialogos.ShowDlgFont(this.getRootPane(), f);
                    if (f != null) {
                        if (fe == diagramaAtual.infoDiagrama) {
                            fe.setFont(f);
                        } else {
                            List<FormaElementar> lst = diagramaAtual.getItensSelecionados().stream().filter(e -> e.getClass().equals(fe.getClass())).collect(Collectors.toList());
                            for (FormaElementar Ed : lst) {
                                Ed.setFont(f);
                            }
                        }
                        diagramaAtual.DoMuda(fe);
                        PerformInspectorFor(fe);
                        return true;
                    }
                }
                break;
            case cmdAdicionarSubItem:
                if (diagramaAtual != null) {
                    diagramaAtual.getSelecionado().AdicionarSubItem(who.getSelecionado().getPropriedade().Tag);
                    PerformInspectorFor(diagramaAtual.getSelecionado());
                    return true;
                }
                break;
            case cmdDoAnyThing:
                if (diagramaAtual != null) {
                    if (diagramaAtual.getSelecionado() == null) {
                        diagramaAtual.DoAnyThing(who.getSelecionado().getPropriedade().Tag);
                    } else {
                        diagramaAtual.getSelecionado().DoAnyThing(who.getSelecionado().getPropriedade().Tag);
                        diagramaAtual.PerformInspector(true);
                    }
                    return true;
                }
                break;
        }
        //se tudo certo rodar o doInspector aqui. ///Acho que não: 21/03.
        return false;
    }

    public void EditoresRefresh(FormaElementar itemSel) {
        if (editorLegenda != null && editorLegenda.isVisible()) {
            if (itemSel instanceof desenho.formas.Legenda && ((desenho.formas.Legenda) itemSel).canShowEditor()) {
                EditorLegendaCarregue(itemSel);
            } else {
                EditorLegendaCarregue(null);
            }
        }
    }

    private void EditorLegendaCarregue(FormaElementar itemSel) {
        if (editorLegenda == null) {
            editorLegenda = new LegendaEditor();
            editorLegenda.setLocation(200, 200);
        }
        editorLegenda.Init((desenho.formas.Legenda) itemSel);
        editorLegenda.setVisible(true);
    }

    private void EditorBaseDrawerCarregue(FormaElementar selecionado) {
        DrawerEditor de = new DrawerEditor(Aplicacao.fmPrincipal, true);
        de.setLocationRelativeTo(Aplicacao.fmPrincipal);
        de.Inicie(selecionado);
        de.setVisible(true);
    }

    boolean FechaDiagrama(Diagrama afechar, Diagrama noLugar) {
        boolean needProcAutoSave = true;
        if (afechar.getMudou()) {
            afechar.ClearSelect(true);
            int res = util.Dialogos.ShowMessageSave(afechar);
            if (res == JOptionPane.OK_OPTION) {
                if (!afechar.Salvar(afechar.getArquivo())) {
                    return false;
                }
                needProcAutoSave = false;
            } else if (res == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }
        if (noLugar == null) {
            noLugar = Add();
        }
        afechar.IsStopEvents = true;
        historicos.removeDiagrama(afechar, noLugar);
        if (needProcAutoSave) {
            DoAutoSaveCompleto();
        }
        return true;
    }

    public void FechaDiagrama(int idx) {
        Diagrama afechar = getDiagramas().get(idx);
        if (afechar == diagramaAtual) {
            idx++;
        }

        int tam = getDiagramas().size();
        if (idx == tam) {
            idx -= 2;
        }
        if (idx > -1) {
            Diagrama nv = getDiagramas().get(idx);
            if (afechar != diagramaAtual) {
                nv = diagramaAtual;
            }
            if (FechaDiagrama(afechar, nv)) {
                diagramaAtual = nv;
                prepareDiagramaAtual();
            }
        } else {
            FechaDiagrama(afechar, null);
        }
        controler.makeEnableComands();
        RePopuleBarraDiagramas(true);
    }

    public ArrayList<Diagrama> getDiagramas() {
        return historicos.getDiagramas();
    }

    public Mostrador getShowDiagramas() {
        return showDiagramas;
    }

    public void setShowDiagramas(Mostrador shDiag) {
        this.showDiagramas = shDiag;
        if (shDiag != null) {
            shDiag.setMaster(this);
        }
    }

    public void AtiveDiagrama(int diagNro) {
        diagramaAtual = historicos.getDiagramas().get(diagNro);
        prepareDiagramaAtual();
    }

    public void setSelected(Diagrama diag) {
        int i = getDiagramas().indexOf(diag);
        showDiagramas.setSelectedIndex(i);
        AtiveDiagrama(i);
    }

    public void NomeieDiagrama(Diagrama diag) {
        if (diag == null) {
            return;
        }
        List<String> txts = getDiagramas().stream().map(d -> d.getNome()).collect(Collectors.toList());
        int res = 1;
        String txt = diag.getTipoDeDiagramaFormatado();
        while (txts.indexOf(txt + "_" + res) != -1) {
            res++;
        }
        diag.setNome(txt + "_" + res);
    }

    public void FecharTudo() {
        historicos.removeAll();
        diagramaAtual = Add();
        prepareDiagramaAtual();
        controler.makeEnableComands();
        RePopuleBarraDiagramas(true);
        DoAutoSaveCompleto();
    }

    //<editor-fold defaultstate="collapsed" desc="Auto Salvar">
    final ArrayList<byte[]> autoSave = new ArrayList<>();
    boolean autoSaveAtivo = false;
    boolean autoSaveIniciado = false;
    int autoSaveInterval = 5;
    transient Timer autoSaveTimer;
    transient Timer statusTimer;
    transient ExecutorService autoSaveWriter;
    transient boolean shuttingDown;

    public int getAutoSaveInterval() {
        return autoSaveInterval;
    }

    public void setAutoSaveInterval(int autoSaveInterval) {
        AutoSalvamentoEditor.setAutoSaveInterval(this, autoSaveInterval);
    }

    /**
     * Sempre que o AutoSaveInterval for alterado este método é chamado.
     * Inclusive no início da aplicação.
     *
     * @return
     */
    boolean InicieAutoSave() {
        return AutoSalvamentoEditor.InicieAutoSave(this);
    }

    void startAutoSave(int initialDelay) {
        AutoSalvamentoEditor.startAutoSave(this, initialDelay);
    }

    /**
     * Deve ser chamado quando o Editor carregar a configuração. Apenas uma vez!
     *
     * @param iter = intervalo lido.
     * @return
     */
    boolean PreInicieAutoSave(int iter) {
        return AutoSalvamentoEditor.PreInicieAutoSave(this, iter);
    }

    /**
     * Já executou o autoSave? Sempre que um diagrama mudar, doneAutoSave =
     * false;
     */
    boolean doneAutoSave = true;

    /**
     * Captures changed diagrams on the EDT, then queues file I/O on one daemon writer.
     *
     * @return
     */
    boolean DoAutoSave() {
        return AutoSalvamentoEditor.DoAutoSave(this);
    }

    boolean AutoSalveToFile(ArrayList<byte[]> snapshot, String file) {
        return AutoSalvamentoEditor.AutoSalveToFile(this, snapshot, file);
    }

    ExecutorService autoSaveWriter() {
        return AutoSalvamentoEditor.autoSaveWriter(this);
    }

    void queueAutoSave() {
        AutoSalvamentoEditor.queueAutoSave(this);
    }

    private static void onEdt(Runnable action) {
        AutoSalvamentoEditor.onEdt(action);
    }

    /**
     * Termina o processo de auto-salvamento.
     */
    public void EndAutoSave() {
        AutoSalvamentoEditor.EndAutoSave(this);
    }

    /** Finish queued writes before exit without blocking the EDT on disk I/O. */
    public void EndAutoSave(Runnable afterWrites) {
        AutoSalvamentoEditor.EndAutoSave(this, afterWrites);
    }

    /**
     * Após salvar ou fechar um diagrama, ele deve ser removido do arquivo de
     * auto-save.
     */
    public void DoAutoSaveCompleto() {
        AutoSalvamentoEditor.DoAutoSaveCompleto(this);
    }

    Temporizador tempoAs;

    /**
     * Legacy task type retained for the serialized Editor descriptor. Its callbacks now
     * run exclusively from Swing timers on the EDT; no java.util.Timer is created.
     */
    class Temporizador implements Runnable {

        private final Editor master;
        private final int tipo;
        private String MSG = "";
        private int tick = 0;

        final int AUTOSAVE = 0;
        final int STATUS = 1;

        public Temporizador(Editor omaster) {
            master = omaster;
            this.tipo = AUTOSAVE;
        }

        /**
         * Legacy status callback; message expiration now belongs to MensagensStatus.
         *
         * @param omaster
         * @param msg
         */
        public Temporizador(Editor omaster, String msg) {
            master = omaster;
            this.tipo = STATUS;
            MSG = msg;
        }

        @Override
        public void run() {
            if (tipo == AUTOSAVE) {
                master.DoAutoSave();
            } else {
                master.DoStatus(MSG);
            }
        }
    }

    /**
     * Lê o arquivo de auto-salvamento e verifica se houve o fechamento abrupto
     * do Editor. Se sim, carrega os Diagramas como estavam.
     *
     * @return true se carregou algo. O retorno faz disparar uma mensagem de
     * aviso ao usuário.
     */
    public boolean LoadAutoSave() {
        return AutoSalvamentoEditor.LoadAutoSave(this);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Status principal">
    private JLabel lblStatus = null;

    public JLabel getLblStatus() {
        return lblStatus;
    }

    public void setLblStatus(JLabel lblStatus) {
        this.lblStatus = lblStatus;
    }

    public void DoStatus(String msg) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> DoStatus(msg));
            return;
        }
        if (lblStatus == null || shuttingDown) return;
        statusTimer = util.MensagensStatus.mostrar(lblStatus, msg, false);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Configs">
    /**
     * Mostar menu do ancorador (ao lado dos artefatos)
     */
    private boolean Ancorador = false;

    public boolean isAncorador() {
        return Ancorador;
    }

    public void setAncorador(boolean Ancorador) {
        if (this.Ancorador == Ancorador) {
            return;
        }
        this.Ancorador = Ancorador;
        if (diagramaAtual != null) {
            diagramaAtual.ExternalSuperAncorador();
        }
    }

    /**
     * Mostrar os ID's dos artefatos no diagrama
     */
    private boolean mostrarIDs = false;

    public boolean isMostrarIDs() {
        return mostrarIDs;
    }

    public void setMostrarIDs(boolean mostrarIDs) {
        if (this.mostrarIDs == mostrarIDs) {
            return;
        }
        this.mostrarIDs = mostrarIDs;
    }

    /**
     * Mostrar hints (tooltips) no inspector
     */
    private boolean mostrarTooltips = false;

    public boolean isMostrarTooltips() {
        return mostrarTooltips;
    }

    public void setMostrarTooltips(boolean mostrarTooltips) {
        if (this.mostrarTooltips == mostrarTooltips) {
            return;
        }
        this.mostrarTooltips = mostrarTooltips;
        for (Inspector inspector : new Inspector[]{InspectorEditor, editorConfig, outroInspector}) {
            if (inspector != null) for (var row : inspector.getItens()) controlador.inspector.DicasInspector.atualizar(row);
        }
    }

    private boolean apagarTextoAoEditar = true;
    
    
    public boolean isApagarTextoAoEditar() {
        return apagarTextoAoEditar;
    }

    public void setApagarTextoAoEditar(boolean apagarTextoAoEditar) {
        this.apagarTextoAoEditar = apagarTextoAoEditar;
    }
    
    //</editor-fold>
}
