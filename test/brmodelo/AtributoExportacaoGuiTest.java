package brmodelo;

import controlador.Controler.Comandos;
import controlador.Diagrama;
import controlador.Editor;
import desenho.formas.Forma;
import diagramas.conceitual.Atributo;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #25 of the original brModelo: attribute names broke into two lines in the exported
 * image. Autosize measured them at the screen's scale (zoom, or 125%/150% displays), and the
 * export draws at scale 1, where the same name can be wider than its box.
 */
@Tag("gui")
class AtributoExportacaoGuiTest {
    private static final String[] NOMES = {
        "codigo", "nome", "data_de_nascimento", "endereco_completo_do_cliente", "telefone_residencial",
        "quantidade_minima_em_estoque", "nome_fantasia_da_empresa", "iiiiiiiillllllll", "WWWWMMMMWWWW",
        "valor_unitario_com_desconto", "email", "numero_do_documento_fiscal"};

    private principal.FramePrincipal frame;
    private Editor editor;
    private Diagrama diagram;

    @BeforeEach void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            diagram = editor.AddAsAtual("tpConceitual");
        });
    }

    @AfterEach void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
        });
    }

    @ParameterizedTest(name = "screen scale {0}")
    @ValueSource(doubles = {0.5, 0.67, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0})
    void exportedAttributeNamesStayOnOneLine(double escala) throws Exception {
        onEdt(() -> {
            List<Atributo> atributos = new ArrayList<>();
            for (int i = 0; i < NOMES.length; i++) {
                Atributo a = (Atributo) palette(Comandos.cmdAtributo, new Point(60 + (i % 2) * 420, 40 + (i / 2) * 50));
                a.setTexto(NOMES[i]);
                assertTrue(a.isAutosize());
                atributos.add(a);
            }
            pintarNaTela(escala);
            pintarNaTela(escala); // autosize settles on the first paint

            BufferedImage img = util.ImageGenerate.geraImagem(diagram);
            assertNotNull(img);
            // The export's own context: scale 1, antialiased text, no fractional metrics.
            FontRenderContext frc = new FontRenderContext(null, true, false);
            List<String> quebrados = new ArrayList<>();
            for (Atributo a : atributos) {
                // Broken into lines, the widest line drawn is narrower than the whole name.
                int inteiro = (int) new TextLayout(a.getTexto(), a.getFont(), frc).getBounds().getWidth();
                int desenhado = a.getTextoFormatado().getMaxWidth();
                if (desenhado < inteiro) quebrados.add(a.getTexto() + " " + desenhado + "<" + inteiro);
            }
            assertEquals(List.of(), quebrados, "attribute names broken in the exported image");
        });
    }

    // The editor canvas: QuadroDeEdicao's hints, the screen scale applied by Swing.
    private void pintarNaTela(double escala) {
        BufferedImage tela = new BufferedImage((int) (1000 * escala) + 1, (int) (400 * escala) + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tela.createGraphics();
        try {
            g.scale(escala, escala);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            diagram.ProcessPaint(g);
        } finally {
            g.dispose();
        }
    }

    private Forma palette(Comandos command, Point click) {
        diagram.getEditor().DoAction(new ActionEvent(new JButton(), ActionEvent.ACTION_PERFORMED, command.name()));
        Forma result = (Forma) diagram.ExternalRealiseComando(command, click);
        assertNotNull(result, command.name());
        return result;
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
