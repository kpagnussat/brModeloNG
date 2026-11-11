package brmodelo;

import java.awt.Component;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import util.InterfaceSemHtml;
import util.RedeDesligada;

/**
 * Child-JVM probe for HtmlInertTest: shows diagram-like text in the components the program
 * uses, each image pointing to its own path on the test server, so the parent sees which
 * component made a request. Mode "nada" installs nothing (the control), "html" installs
 * InterfaceSemHtml as the application does, "rede" installs RedeDesligada.
 */
public final class SondaHtml {
    public static void main(String[] args) throws Exception {
        String modo = args[0], base = "http://127.0.0.1:" + args[1] + "/";
        if (modo.equals("rede")) RedeDesligada.instalar();
        com.formdev.flatlaf.FlatLightLaf.setup();
        if (modo.equals("html")) {
            InterfaceSemHtml.instalar();
            // A theme change after installation must keep the protection.
            com.formdev.flatlaf.FlatDarkLaf.setup();
        }
        SwingUtilities.invokeAndWait(() -> {
            medir(new JLabel(img(base, "label")));
            DefaultTreeCellRenderer arvore = new DefaultTreeCellRenderer();
            medir(arvore.getTreeCellRendererComponent(new JTree(), img(base, "arvore"), false, false, true, 0, false));
            JComboBox<String> combo = new JComboBox<>(new String[]{img(base, "combo")});
            medir(combo.getRenderer().getListCellRendererComponent(new JList<>(), img(base, "combo"), 0, false, false));
            medir(new JCheckBox(img(base, "checkbox")));
            medir(new JToggleButton(img(base, "parte")));
            medir(new JMenuItem(img(base, "recente")));
            medir(new JOptionPane("Salvar alterações?\n" + img(base, "dialogo")));
            JTabbedPane abas = new JTabbedPane();
            String titulo = img(base, "aba");
            abas.addTab(modo.equals("nada") ? titulo : InterfaceSemHtml.texto(titulo), new JPanel());
            abas.setSize(400, 300);
            abas.doLayout();
            medir(abas);
            if (modo.equals("html")) {
                // The program's own HTML still renders where it opts in.
                medir(InterfaceSemHtml.permitir(new JLabel()), img(base, "permitido"));
            }
        });
        if (!modo.equals("html")) {
            try {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new java.io.StringReader(
                        "<!DOCTYPE DIAGRAMA SYSTEM '" + base + "xxe'><DIAGRAMA/>")));
            } catch (Exception ignored) { }
            try { ImageIO.read(new URL(base + "imageio")); } catch (Exception ignored) { }
        }
        System.out.println("fim");
    }

    private static String img(String base, String caminho) {
        return "<html><img src='" + base + caminho + "'></html>";
    }

    private static void medir(Component c) {
        c.getPreferredSize();
    }

    private static void medir(JLabel label, String texto) {
        label.setText(texto);
        label.getPreferredSize();
    }
}
