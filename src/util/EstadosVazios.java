package util;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/** Standard Swing labels in empty viewports; no custom painting or serialized subclasses. */
public final class EstadosVazios {
    private EstadosVazios() {}
    public static void instalar(JScrollPane scroll) {
        Component view = scroll.getViewport().getView();
        if (!(view instanceof JTable || view instanceof JList || view instanceof JToolBar || view instanceof JPanel panel && panel.getLayout() == null)) return;
        if (scroll.getClientProperty("brmodelo.vazio") == view) return;
        scroll.putClientProperty("brmodelo.vazio", view);
        JLabel hint = InterfaceSemHtml.permitir(new JLabel("", SwingConstants.CENTER));
        hint.setText(view instanceof JTable ? "Nenhum item disponível." : view instanceof JToolBar ? "<html><center>Nenhuma parte<br>salva.</center></html>" : "<html><center>Nenhum item<br>adicionado.</center></html>");
        hint.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
        JPanel empty = new JPanel(new BorderLayout());
        empty.add(hint);
        empty.setPreferredSize(new Dimension(AcabamentoDialogos.px(view instanceof JTable ? 360 : 160), AcabamentoDialogos.px(160)));
        Runnable refresh = () -> {
            boolean isEmpty = view instanceof JTable table ? table.getRowCount() == 0
                    : view instanceof JList<?> list ? list.getModel().getSize() == 0
                    : ((Container)view).getComponentCount() == 0;
            scroll.setViewportView(isEmpty ? empty : view);
            if (view instanceof JTable table) scroll.setColumnHeaderView(table.getTableHeader());
            scroll.revalidate(); scroll.repaint();
        };
        if (view instanceof JTable table) {
            TableModelListener listener = event -> refresh.run();
            table.getModel().addTableModelListener(listener);
            table.addPropertyChangeListener("model", event -> {
                ((javax.swing.table.TableModel)event.getOldValue()).removeTableModelListener(listener);
                table.getModel().addTableModelListener(listener); refresh.run();
            });
        } else if (view instanceof JList<?> list) {
            ListDataListener listener = new ListDataListener() {
                public void intervalAdded(ListDataEvent e) { refresh.run(); }
                public void intervalRemoved(ListDataEvent e) { refresh.run(); }
                public void contentsChanged(ListDataEvent e) { refresh.run(); }
            };
            list.getModel().addListDataListener(listener);
            list.addPropertyChangeListener("model", event -> {
                ((ListModel<?>)event.getOldValue()).removeListDataListener(listener);
                list.getModel().addListDataListener(listener); refresh.run();
            });
        } else {
            ((Container)view).addContainerListener(new java.awt.event.ContainerAdapter() {
                public void componentAdded(java.awt.event.ContainerEvent e) { refresh.run(); }
                public void componentRemoved(java.awt.event.ContainerEvent e) { refresh.run(); }
            });
        }
        scroll.putClientProperty("brmodelo.vazio.texto", hint);
        refresh.run();
    }
}
