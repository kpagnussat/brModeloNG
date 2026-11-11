package brmodelo;

import controlador.Controler;
import controlador.Controler.Comandos;
import controlador.Diagrama;
import controlador.Editor;
import controlador.inspector.Inspector;
import controlador.inspector.InspectorItemBase;
import desenho.formas.Forma;
import diagramas.conceitual.Entidade;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #7 of the original brModelo: a name typed in the Inspector was kept only when
 * Enter was pressed. Leaving the field, clicking the diagram or saving must keep it too.
 */
@Tag("gui")
class InspectorTextoGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private Diagrama diagram;
    @TempDir Path dir;

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

    @Test void leavingTheFieldKeepsTheName() throws Exception {
        onEdt(() -> {
            Entidade e = (Entidade) palette(Comandos.cmdEntidade, new Point(300, 200));
            JTextField campo = digitarNome(e, "Cliente");
            // The focus manager drops a synthetic FOCUS_LOST for a field that never had focus.
            FocusEvent saida = new FocusEvent(campo, FocusEvent.FOCUS_LOST, false, null);
            for (var l : campo.getFocusListeners()) l.focusLost(saida);
            assertEquals("Cliente", e.getTexto());
        });
    }

    @Test void escapeStillDiscardsTheTyping() throws Exception {
        onEdt(() -> {
            Entidade e = (Entidade) palette(Comandos.cmdEntidade, new Point(300, 200));
            String antes = e.getTexto();
            JTextField campo = digitarNome(e, "Cliente");
            java.awt.event.KeyEvent esc = new java.awt.event.KeyEvent(campo, java.awt.event.KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, java.awt.event.KeyEvent.VK_ESCAPE, java.awt.event.KeyEvent.CHAR_UNDEFINED);
            for (var l : campo.getKeyListeners()) l.keyPressed(esc);
            FocusEvent saida = new FocusEvent(campo, FocusEvent.FOCUS_LOST, false, null);
            for (var l : campo.getFocusListeners()) l.focusLost(saida);
            assertEquals(antes, e.getTexto());
        });
    }

    @Test void clickingAnotherEntityKeepsTheNameOnTheEditedOne() throws Exception {
        onEdt(() -> {
            Entidade a = (Entidade) palette(Comandos.cmdEntidade, new Point(300, 200));
            Entidade b = (Entidade) palette(Comandos.cmdEntidade, new Point(700, 200));
            String nomeB = b.getTexto();
            digitarNome(a, "Cliente");
            clicar(new Point(b.getLeft() + b.getWidth() / 2, b.getTop() + b.getHeight() / 2));
            assertEquals("Cliente", a.getTexto());
            assertEquals(nomeB, b.getTexto(), "the other entity is not renamed");
            assertSame(b, diagram.getSelecionado());
        });
    }

    @Test void savingWithTheFieldOpenSavesTheName() throws Exception {
        Path arquivo = dir.resolve("nome.brM3");
        onEdt(() -> {
            Entidade e = (Entidade) palette(Comandos.cmdEntidade, new Point(300, 200));
            diagram.setArquivo(arquivo.toString());
            digitarNome(e, "Cliente");
            editor.DoActionExtender(new ActionEvent(new JButton(), ActionEvent.ACTION_PERFORMED,
                    Controler.menuComandos.cmdSave.name()));
            assertEquals("Cliente", e.getTexto());
        });
        Diagrama salvo = Fixtures.load(Files.readAllBytes(arquivo));
        assertTrue(salvo.getListaDeItens().stream().anyMatch(i -> i instanceof Entidade x && x.getTexto().equals("Cliente")),
                "the saved file has the typed name");
    }

    private JTextField digitarNome(Forma f, String nome) {
        selecionar(f);
        diagram.PerformInspector();
        Inspector inspector = editor.getInspectorEditor();
        InspectorItemBase item = inspector.getItens().stream()
                .filter(i -> "nome".equals(i.getPropriedade().configuracaoStr)).findFirst()
                .orElseThrow(() -> new AssertionError("No name row"));
        inspector.PerformSelect(item);
        JTextField campo = (JTextField) item.getOndeEditar();
        assertSame(item, campo.getParent(), "the field opens inside the row");
        campo.setText(nome);
        assertNotEquals(nome, f.getTexto(), "nothing is applied while typing");
        return campo;
    }

    private void selecionar(Forma f) {
        diagram.ClearSelect();
        diagram.DiagramaDoSelecao(f, false, false);
    }

    private void clicar(Point p) {
        editor.getControler().BtnNothing.doClick(); // the selection tool, not the last palette shape
        var box = editor.getBox();
        long t = System.currentTimeMillis();
        // The diagram finds what is under the mouse when it moves.
        diagram.mouseMoved(new MouseEvent(box, MouseEvent.MOUSE_MOVED, t, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
        diagram.mousePressed(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, t, InputEvent.BUTTON1_DOWN_MASK, p.x, p.y, 1, false, MouseEvent.BUTTON1));
        diagram.mouseReleased(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, t, 0, p.x, p.y, 1, false, MouseEvent.BUTTON1));
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
