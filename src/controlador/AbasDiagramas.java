package controlador;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.*;

/** Runtime adapter; no fields or method signatures change in the serialized controllers. */
public final class AbasDiagramas {
    private final Mostrador host;
    private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    private final List<Diagrama> documents = new ArrayList<>();
    private boolean syncing;

    private AbasDiagramas(Mostrador host) {
        this.host = host;
        tabs.setInheritsPopupMenu(true);
        tabs.putClientProperty("JTabbedPane.tabType", "underlined");
        tabs.putClientProperty("JTabbedPane.tabClosable", true);
        tabs.putClientProperty("JTabbedPane.tabCloseToolTipText", "Fechar diagrama");
        util.DicasInterface.abas(tabs);
        tabs.putClientProperty("JTabbedPane.maximumTabWidth", 240);
        tabs.putClientProperty("JTabbedPane.scrollButtonsPolicy", "asNeeded");
        tabs.putClientProperty("JTabbedPane.tabCloseCallback", (IntConsumer) index -> {
            tabs.setSelectedIndex(index);
            host.getMaster().FechaDiagrama(index);
            sincronizar();
        });
        tabs.addChangeListener(event -> {
            int index = tabs.getSelectedIndex();
            if (!syncing && index >= 0 && host.getMaster() != null) {
                host.setSelectedIndex(index);
                host.getMaster().AtiveDiagrama(index);
            }
        });
        tabs.addPropertyChangeListener("UI", event -> instalarUI());
        instalarUI();
    }

    public static AbasDiagramas de(Mostrador host) {
        Object value = host.getClientProperty(AbasDiagramas.class);
        if (value instanceof AbasDiagramas adapter) return adapter;
        AbasDiagramas adapter = new AbasDiagramas(host);
        host.putClientProperty(AbasDiagramas.class, adapter);
        return adapter;
    }

    private void instalarUI() {
        if (tabs.getUI() instanceof FlatTabbedPaneUI && !(tabs.getUI() instanceof Aparencia))
            tabs.setUI(new Aparencia());
    }

    public Dimension tamanho() {
        // FlatLaf explicitly supports a tab-only pane with null content components.
        if (tabs.getTabCount() > 0) return new Dimension(100, tabs.getPreferredSize().height);
        Insets insets = UIManager.getInsets("TabbedPane.tabInsets");
        int padding = insets == null ? 16 : insets.top + insets.bottom;
        return new Dimension(100, Math.max(com.formdev.flatlaf.util.UIScale.scale(UIManager.getInt("TabbedPane.tabHeight")),
                tabs.getFontMetrics(tabs.getFont()).getHeight() + com.formdev.flatlaf.util.UIScale.scale(padding))
                + com.formdev.flatlaf.util.UIScale.scale(UIManager.getInt("TabbedPane.contentSeparatorHeight")));
    }

    public void sincronizar() {
        if (syncing) return;
        syncing = true;
        try {
            if (tabs.getParent() != host || !(host.getLayout() instanceof BorderLayout)) {
                host.setLayout(new BorderLayout());
                host.add(tabs, BorderLayout.CENTER);
                host.revalidate();
            }
            List<Diagrama> current = host.getDiagramas();
            if (current == null) return;
            if (!documents.equals(current)) {
                tabs.removeAll();
                documents.clear();
                documents.addAll(current);
                for (Diagrama diagram : documents)
                    tabs.addTab("", host.getMaster().getControler().getIconeDoTipo(diagram.getTipo()), null);
                host.revalidate();
            }
            for (int i = 0; i < documents.size(); i++) {
                Diagrama diagram = documents.get(i);
                String name = util.InterfaceSemHtml.texto((diagram.getMudou() ? "*" : "") + diagram.getNomeFormatado());
                if (!name.equals(tabs.getTitleAt(i))) tabs.setTitleAt(i, name);
                String path = diagram.getArquivo();
                String tooltip = path == null || path.isEmpty() ? util.InterfaceSemHtml.texto(diagram.getNomeFormatado())
                        : new java.io.File(path).getAbsolutePath();
                if (!tooltip.equals(tabs.getToolTipTextAt(i))) tabs.setToolTipTextAt(i, tooltip);
            }
            selecionar(host.getSelectedIndex());
        } finally {
            syncing = false;
        }
    }

    public void selecionar(int index) {
        boolean previous = syncing;
        syncing = true;
        try {
            if (index >= -1 && index < tabs.getTabCount() && tabs.getSelectedIndex() != index)
                tabs.setSelectedIndex(index);
        } finally {
            syncing = previous;
        }
    }

    /** UI delegates aren't Serializable. Geometry and hit testing stay entirely in FlatLaf. */
    private static final class Aparencia extends FlatTabbedPaneUI {
        @Override protected void paintTabCloseButton(Graphics g, int index, int x, int y, int w, int h) {
            if (index == getRolloverTab()) super.paintTabCloseButton(g, index, x, y, w, h);
        }
    }
}
