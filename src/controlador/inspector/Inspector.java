/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controlador.inspector;

import controlador.Editor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 *
 * @author ccandido
 */
public class Inspector extends JScrollPane {

    // Pinned to the value computed before ConfirmeTextoPendente was added: the Inspector is a
    // window panel, never written to a diagram file, and the serialized-form golden stays as is.
    private static final long serialVersionUID = -6311791789443975805L;

    //<editor-fold defaultstate="collapsed" desc="Base do componente">
    public Inspector() {
        super();
        Init();
    }

    private void Init() {
        box = new PainelLinhas();
        this.add(box);
        setViewportView(box);
        box.setBackground(util.EstiloUI.fundo(box, "Panel.background"));
        getViewport().setBackground(util.EstiloUI.fundo(getViewport(), "Panel.background"));
        util.FocoInspector.instalar(this);
        // Font-driven row sizes, with single-command sections joined visually.
        // The item list and each header's collapse state stay intact.
        box.setLayout(new DisposicaoPropriedades());
        box.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        getHorizontalScrollBar().setBlockIncrement(100);
        getVerticalScrollBar().setBlockIncrement(100);
        getHorizontalScrollBar().setUnitIncrement(10);
        getVerticalScrollBar().setUnitIncrement(10);

        addComponentListener(new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent e) {
                DoResize();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        setFocusable(true);

        ActionListener al_down = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SelectNext(false);
            }
        };
        ActionListener al_up = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SelectNext(true);
            }
        };

        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        this.registerKeyboardAction(al_down, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        this.registerKeyboardAction(al_down, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        this.registerKeyboardAction(al_up, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        this.registerKeyboardAction(al_up, stroke, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        InitEditores();
        GradePropriedades.instalar(this);

    }

    private void InitEditores() {
        TipoMenu.setVisible(false);
        TipoTexto.setVisible(false);
        TipoSN.setVisible(false);
        TipoMenu.setEditable(true);
        TipoDlg.setVisible(false);

        TipoMenu.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    EndEdit(true, false);
                }
            }
        });
        TipoSN.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                EndEdit(true, false);
            }
        });

        TipoTexto.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    EndEdit(false, false);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    EndEdit(true, false);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        // Leaving the field keeps what was typed, as Enter does (issue #7 of the original).
        TipoTexto.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (!e.isTemporary()) {
                    ConfirmeTextoPendente();
                }
            }
        });

        TipoTexto.setBorder(null);
    }

    private transient boolean confirmando = false;

    /**
     * Applies text typed in the inline field and not yet confirmed with Enter. Called before
     * anything that would reload the field or change what is selected (leaving the field,
     * another row, a click on the diagram or the object tree, a menu command), so the typed
     * value reaches the item it was typed for. No-op when the field is closed or unchanged.
     */
    public void ConfirmeTextoPendente() {
        if (confirmando || selecionado == null || selecionado.getOndeEditar() != TipoTexto
                || TipoTexto.getParent() != selecionado || !Itens.contains(selecionado)) {
            return;
        }
        confirmando = true;
        try {
            EndEdit(true, false);
        } finally {
            confirmando = false;
        }
    }

    public void DoResize() {
        // Widths follow the viewport (PainelLinhas tracks it); just relayout and repaint.
        box.revalidate();
        box.repaint();
    }
    private JPanel box;

    public JPanel getBox() {
        return box;
    }
    private ArrayList<InspectorItemBase> Itens = new ArrayList<>();

    public ArrayList<InspectorItemBase> getItens() {
        return Itens;
    }
    public final int espaco = 1;

    /**
     * Rows container. Tracking the viewport width makes rows stretch to the visible width
     * (no manual scrollbar-width math); height stays at the preferred size so it scrolls.
     */
    private static class PainelLinhas extends JPanel implements javax.swing.Scrollable {

        @Override
        public java.awt.Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
    protected final JComboBox TipoMenu = new JComboBox();
    protected final JCheckBox TipoSN = new JCheckBox();
    protected final InspectorExtenderEditor TipoDlg = new InspectorExtenderEditor(this);
    protected final JTextField TipoTexto = new JTextField();
    private Editor editor = null;

    public Editor getEditor() {
        return editor;
    }

    public void setEditor(Editor editor) {
        this.editor = editor;
    }

    //</editor-fold>
    public InspectorItemBase Add(InspectorProperty pprt) {
        if (pprt.tipo == InspectorProperty.TipoDeProperty.tpNothing) {
            return null;
        }
        InspectorItemBase item = InspectorItemBase.SuperFactory(this, pprt);
        this.box.add(item);
        Itens.add(item);
        GradePropriedades.instalar(item);
        DicasInspector.instalar(item);
        box.revalidate();
        return item;
    }

    public InspectorItemBase AddSeparador(String caption) {
        return Add(InspectorProperty.getPropertySeparador(caption));
    }

    //<editor-fold defaultstate="collapsed" desc="Seleção">
    private InspectorItemBase selecionado = null;

    public InspectorItemBase getSelecionado() {
        return selecionado;
    }

    private void setSelecionado(InspectorItemBase selecionado) {
        if (this.selecionado != selecionado) {
            if (this.selecionado != null) {
                this.selecionado.setSelecionado(false);
            }
            this.selecionado = selecionado;
            if (this.selecionado != null) {
                this.selecionado.setSelecionado(true);
            }
        }
        if (getDicas() == null) {
            if (this.selecionado != null) {
                editor.setTextoDica(this, selecionado.getPropriedade().dica);
            } else {
                editor.setTextoDica(this, "");
            }
        } else {
            if (this.selecionado != null) {
                SetTextoDica(selecionado.getPropriedade().dica);
            } else {
                SetTextoDica("");
            }
        }
    }

    private InspectorDicas dicas = null;

    public void SetTextoDica(String txt) {
        if (getDicas() != null) {
            getDicas().setTexto(txt);
        }
    }

    public void PerformSelect(InspectorItemBase aThis) {
        if (aThis != selecionado) {
            ConfirmeTextoPendente();
            if (!Itens.contains(aThis)) {
                return;
            }
        }
        CarregueValor(aThis);
        setSelecionado(aThis);
    }

    public void PerformDica() {
        if (getDicas() != null) {
            if (this.selecionado != null) {
                SetTextoDica(selecionado.getPropriedade().dica);
            } else {
                SetTextoDica("");
            }
        } else {
            if (editor != null) {
                if (this.selecionado != null) {
                    editor.setTextoDica(this, selecionado.getPropriedade().dica);
                } else {
                    editor.setTextoDica(this, "");
                }
            }
        }
    }

    /**
     * Evita loop infinito
     */
    private void SelectNext(boolean sobe) {
        if (Itens.isEmpty()) {
            return;
        }

        int p = Itens.indexOf(selecionado);
        if (sobe) {
            p--;
            if (p < 0) {
                p = Itens.size() - 1;
            }

            InspectorItemBase ib = Itens.get(p);
            while ((ib instanceof InspectorItemSeparador) || !ib.CanEdit() || !ib.isVisible() || (ib instanceof InspectorItemMenu)) {
                p--;
                if (p < 0) {
                    return;
                }
                ib = Itens.get(p);
            }
            PerformSelect(Itens.get(p));

        } else {
            p++;
            if (p > Itens.size() - 1) {
                p = 0;
            }

            InspectorItemBase ib = Itens.get(p);
            while ((ib instanceof InspectorItemSeparador) || !ib.CanEdit() || !ib.isVisible() || (ib instanceof InspectorItemMenu)) {
                p++;
                if (p > Itens.size() - 1) {
                    return;
                }
                ib = Itens.get(p);
            }
            PerformSelect(Itens.get(p));
        }

    }
    //</editor-fold>

    /**
     * Evita que ao carregar JCombobox o evento SELECTED entre (outros) ocorra!
     */
    private boolean stopEdicao = false;

    /**
     * Ocorre quando termina-se de editar um valor de propriedade (neste caso em
     * um InspectorItemBase)
     *
     * @param validar o valor deve ser aceito?
     * @param sair sair da edição?
     */
    public void EndEdit(final boolean validar, final boolean sair) {
        if (stopEdicao) {
            return;
        }
        if (!validar) {
            if (sair) {
                setSelecionado(null);
            } else {
                CarregueValor(selecionado);
            }
        } else {
            if (selecionado == null) {
                return;
            }
            String txt = "";
            if (selecionado.getOndeEditar() == TipoMenu) {
                txt = Integer.toString(TipoMenu.getSelectedIndex());
            } else if (selecionado.getOndeEditar() == TipoDlg) {

                if (selecionado.getPropriedade().tipo == InspectorProperty.TipoDeProperty.tpSelecObject) {
                    if (editor.SelectItemByID(Integer.valueOf(selecionado.getPropriedade().property))) {
                        return;
                    }
                    setSelecionado(null);
                    return;
                }

                if (selecionado.getPropriedade().tipo == InspectorProperty.TipoDeProperty.tpCommand) {
                    if (editor.ProcesseCmdFromInspector(this, selecionado.getPropriedade().property)) {
                        return;
                    }
                    setSelecionado(null);
                    return;
                }

                txt = TipoDlg.getTexto();
            } else if (selecionado.getOndeEditar() == TipoSN) {
                txt = Boolean.toString(TipoSN.isSelected());
            } else {
                txt = TipoTexto.getText();
            }
            if (!txt.equals(selecionado.getValor())) {
                if (!editor.AceitaEdicao(this, selecionado.getPropriedade(), txt)) {
                    selecionado.setFalhou(true);
                }
            }

            if (sair) {
                setSelecionado(null);
            } else {
                CarregueValor(selecionado);
            }
        }
    }

    /**
     * Carrega o valor da propriedade no editor
     *
     * @param aThis InspectorItemBase a carregar
     */
    private void CarregueValor(InspectorItemBase aThis) {
        stopEdicao = true;
        if (aThis instanceof InspectorItemMenu) {
            TipoMenu.removeAllItems();
            for (String v : aThis.getPropriedade().opcoesMenu) {
                TipoMenu.addItem(v);
            }
            TipoMenu.setSelectedIndex(Integer.parseInt(aThis.getValor()));
        } else if ((aThis instanceof InspectorItemExtender)) {
            TipoDlg.setTexto(aThis.getTransValor());
        } else if (aThis instanceof InspectorItemSN) {
            TipoSN.setSelected(Boolean.parseBoolean(aThis.getValor()));
            TipoSN.setText(aThis.getTransValor());
        } else if (aThis instanceof InspectorItemTexto) {
            TipoTexto.setText(aThis.getTransValor());
        }
        stopEdicao = false;
    }

    public void Clear() {
        for (InspectorItemBase item : Itens) {
            this.box.remove(item);
        }
        Itens.clear();
        box.validate();
        RePosicionar();
        repaint();
    }

    /**
     * ArrayList contendo as últimas propriedades carregadas.
     */
    private ArrayList<InspectorProperty> gerado = null;

    /**
     * Apaga o valor de "gerado" de forma que o próximo PerformInspector
     * carregue os itens. É usado no caso de se clicar no próprio objeto e o
     * clique mudar uma condição de status do objeto que deve ser mostrada no
     * inspector: exemplo: clique na legenda (no item da legenda).
     */
    public void ForceFullOnCarregue() {
        gerado.clear();
    }

    public void Carrege(ArrayList<InspectorProperty> conjPropriedades) {
        boolean eq = false;
        if (gerado != null && gerado.size() == conjPropriedades.size()) {
            eq = true;
            for (int i = 0; i < gerado.size(); i++) {
                if (!gerado.get(i).tipo.equals(conjPropriedades.get(i).tipo)) {
                    eq = false;
                    break;
                }
            }
        }
        boolean novo = false;
        if (eq) {
            for (int i = 0; i < gerado.size(); i++) {
                InspectorItemBase it = Itens.get(i);
                it.setPropriedade(conjPropriedades.get(i));
                it.repaint();
            }
            if (selecionado != null) {
                CarregueValor(selecionado);
            }
        } else {
            Clear();
            conjPropriedades.stream().forEach((ipp) -> {
                Add(ipp);
            });
            InspectorItemBase tmp = AddSeparador("");
            ((InspectorItemSeparador) tmp).endOFF = true;
            tmp.setBackground(util.EstiloUI.fundo(tmp, "Panel.background"));
            novo = true;
        }
        gerado = conjPropriedades;
        RefreshAllCanEdit();
        if (novo) {
            Itens.stream().filter(pp -> pp instanceof InspectorItemSeparador).map(pp -> (InspectorItemSeparador) pp).forEach(pp -> {
                if (pp.getPropriedade().opcional.equals("-")) {
                    pp.setEstado('+');
                    HideShow(pp, '+');
                }
            });
        }
        RePosicionar();
    }

    private ArrayList<InspectorItemBase> getListItensForProperty(ArrayList<String> pprs) {
        ArrayList<InspectorItemBase> res = new ArrayList<>();
        pprs.stream().forEach((v) -> {
            getItens().stream().filter((it) -> (it.getPropriedade().isMe(v))).forEach((it) -> {
                res.add(it);
            });
        });
        return res;
    }

    /**
     * Dado um Item (sel), verifica se ele é um agrupador, ou seja, se ele
     * agrupa itens que<br/>
     * serão des/habilitados conforme seu valor atual. Primeiro, pega-se a
     * relação do itens habilitáveis e os habilita.<br/>
     * Depois a dos não habilitáveis e os desabilita.<br/>
     *
     * @param sel
     * @param valor
     */
    public void MakeCanEditGrupo(InspectorItemBase sel) {
        InspectorProperty insp = sel.getPropriedade();
        if (insp.agrupada == null) {
            return;
        }
        String valor = sel.getValor();

        MakeCanEdit(getListItensForProperty(insp.QuaisCanEditIf(valor)), true);
        MakeCanEdit(getListItensForProperty(insp.QuaisCanEditNotIf(valor)), false);

        repaint();
    }

    /**
     * Seta o valor de CanEdit dos itens
     *
     * @param toMake
     * @param ena
     */
    private void MakeCanEdit(ArrayList<InspectorItemBase> toMake, boolean ena) {
        if (ena) {
            toMake.stream().forEach((it) -> {
                it.setCanEdit(true);
            });
            toMake.stream().forEach((it) -> {
                it.RefreshGrupoCanEdit();
            });
        } else {
            toMake.stream().forEach((it) -> {
                it.setCanEdit(false);
            });
        }
    }

    public void RefreshAllCanEdit() {
        //tem mesmo que ficar habilitado?
        for (InspectorItemBase it : getItens()) {
            it.RefreshGrupoCanEdit();
        }
        revalidate();
    }

    /**
     * @return the dicas
     */
    public InspectorDicas getDicas() {
        return dicas;
    }

    /**
     * @param dicas the dicas to set
     */
    public void setDicas(InspectorDicas dicas) {
        this.dicas = dicas;
    }

    public void HideShow(InspectorItemSeparador item, char estado) {
        int i = getItens().indexOf(item);
        if ('+' == estado) {
            for (int j = i + 1; j < gerado.size(); j++) {
                if (gerado.get(j).tipo == InspectorProperty.TipoDeProperty.tpSeparador) {
                    RePosicionar();
                    return;
                }
                getItens().get(j).setVisible(false);
            }
        } else {
            for (int j = i + 1; j < gerado.size(); j++) {
                if (gerado.get(j).tipo == InspectorProperty.TipoDeProperty.tpSeparador) {
                    RePosicionar();
                    return;
                }
                getItens().get(j).setVisible(true);
            }
        }
        RePosicionar();
    }

    public void RePosicionar() {
        // Only expanded rows participate in layout; items remain in the original list.
        box.removeAll();
        for (InspectorItemBase item : getItens()) {
            if (item.isVisible() && !(item instanceof InspectorItemSeparador header && header.endOFF)) {
                this.box.add(item);
            }
        }
        box.revalidate();
        box.repaint();
    }

    public InspectorItemBase FindByProperty(String pprt) {
        for (InspectorItemBase it : getItens()) {
            if (it.getPropriedade().isMe(pprt)) {
                return it;
            }
        }
        return null;
    }

    @Override
    public void paint(Graphics grphcs) {
        super.paint(grphcs); //To change body of generated methods, choose Tools | Templates.
        // Row height used to be patched here after layout; rows now size themselves.
    }

    private double divisor = 0.5;

    public double getDivisor() {
        return divisor;
    }

    public void setDivisor(double divisor) {
        this.divisor = divisor;
        DoResize();
    }

}
