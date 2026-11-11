package util;

import java.awt.*;
import java.net.URI;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

/** Accessible credits page for the About dialog; browser help carries the same credits. */
public final class ConteudoSobre {
    private ConteudoSobre() {}
    public static String html() {
        int icon = AcabamentoDialogos.px(96);
        // Pick the raster that is at least as large as the drawn size, so it is never upscaled.
        String source = icon <= 128 ? "brModelo-128.png" : "brModelo-256.png";
        return "<html><body><div align='center'><img src='" + source + "' width='" + icon
                + "' height='" + icon + "' alt='brModelo NG'><h1>brModelo NG " + InformacoesAplicacao.versao()
                + "</h1></div><p><b>Créditos:</b></p>"
                + "<p>brModelo NG — <a href='https://github.com/kpagnussat'>Kristofer Pagnussat</a></p>"
                + "<p>Baseado no brModelo 3.3.2 de <a href='https://github.com/chcandido'>Carlos Henrique Cândido</a>,<br>"
                + "sob orientação do <a href='https://www.inf.ufsc.br/~r.mello/'>Dr. Ronaldo dos Santos Mello</a></p>"
                + "<p>Licença GPL-3.0</p><p><a href='https://github.com/kpagnussat/brModeloNG'>brModelo NG</a>"
                + " · <a href='https://github.com/chcandido/brModelo'>brModelo original</a></p></body></html>";
    }

    public static JEditorPane pagina(String html) {
        JEditorPane page = new JEditorPane();
        page.setEditable(false);
        page.setContentType("text/html");
        page.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        page.setFont(UIManager.getFont("Label.font"));
        page.setBackground(EstiloUI.fundo(page, "Panel.background"));
        page.setForeground(EstiloUI.texto(page, "Label.foreground"));
        page.setBorder(BorderFactory.createEmptyBorder());
        page.addHyperlinkListener(event -> {
            if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
            try {
                URI link = URI.create(event.getDescription());
                // The page is the program's own; still, only web links leave it for the browser.
                if ("https".equalsIgnoreCase(link.getScheme())) Navegador.abrirDepois(link);
            } catch (IllegalArgumentException e) {
                BrLogger.Logger("ERROR_BROWSER_OPEN", e.getMessage());
            }
        });
        page.putClientProperty("brmodelo.creditos", true);
        estilizar(page);
        if (page.getDocument() instanceof javax.swing.text.html.HTMLDocument document)
            document.setBase(ConteudoSobre.class.getResource("/imagens/app/brModelo-128.png"));
        page.setText(html);
        page.setCaretPosition(0);
        return page;
    }

    public static String cssCor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static void estilizar(JEditorPane page) {
        if (page.getEditorKit() instanceof javax.swing.text.html.HTMLEditorKit kit) {
            kit.getStyleSheet().addRule("a { color: " + cssCor(EstiloUI.cor("Component.linkColor")) + "; }");
            kit.getStyleSheet().addRule("h1 { font-size: " + (UIManager.getFont("Label.font").getSize() + 2) + "pt; margin: 8px 0 0 0; }");
        }
        page.setFont(UIManager.getFont("Label.font"));
    }

    public static void mostrar(JPanel panel) {
        panel.removeAll();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(null);
        panel.add(pagina(html()));
        panel.revalidate(); panel.repaint();
    }

    public static void dialogo(JDialog dialog, JPanel panel, JScrollPane scroll, JPanel actions, JButton close) {
        dialog.setTitle("Sobre o brModelo NG");
        close.setText("Fechar");
        DicasInterface.dica(close, "close");
        mostrar(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        Container root = dialog.getContentPane();
        root.removeAll(); root.setLayout(new BorderLayout(0, AcabamentoDialogos.px(16)));
        root.add(panel, BorderLayout.CENTER); root.add(actions, BorderLayout.SOUTH);
        AcabamentoDialogos.aplicar(dialog);
        dialog.setResizable(false);
    }
}
