package brmodelo;

import controlador.editores.MostradorDeCodigo;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
class MostradorDeCodigoGuiTest {
    // Guards the SQL viewer: names from the diagram are shown as text inside the viewer's
    // own HTML, keyword highlighting still applies, and copy/save keep the raw SQL.
    @Test @Timeout(30)
    void diagramNamesAreEscapedInsideTheHighlightedSql() throws Exception {
        String sql = "/* <img src='http://127.0.0.1:9/x'>: */\nCREATE TABLE <b>Aluno</b> (\n    id INT\n);";
        SwingUtilities.invokeAndWait(() -> {
            MostradorDeCodigo viewer = new MostradorDeCodigo(null, true);
            try {
                viewer.setTexto(sql);
                String html = viewer.lblHtml.getText();
                assertTrue(html.startsWith("<html>"), "Viewer keeps its own HTML");
                assertFalse(html.contains("<img") || html.contains("<b>"), html);
                assertTrue(html.contains("&lt;img src='http://127.0.0.1:9/x'&gt;"), html);
                assertTrue(html.contains("&lt;b&gt;Aluno&lt;/b&gt;"), html);
                assertTrue(html.contains("CREATE</font>"), "Keywords are still highlighted: " + html);
                assertEquals(sql, viewer.getTexto(), "Copy and save use the raw SQL");
            } finally {
                viewer.dispose();
            }
        });
    }
}
