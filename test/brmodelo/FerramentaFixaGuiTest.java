package brmodelo;

import controlador.Controler.Comandos;
import controlador.Diagrama;
import controlador.Editor;
import diagramas.conceitual.Atributo;
import diagramas.conceitual.Entidade;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #5 of the original brModelo: inserting several attributes meant choosing the tool
 * again for each one. A double-clicked palette tool stays active until Escape or the arrow.
 */
@Tag("gui")
class FerramentaFixaGuiTest {
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

    @Test void aDoubleClickedToolKeepsCreatingUntilEscape() throws Exception {
        onEdt(() -> {
            JToggleButton entidade = botao(Comandos.cmdEntidade), atributo = botao(Comandos.cmdAtributo);
            entidade.doClick();
            clicar(new Point(400, 300));
            Entidade e = (Entidade) diagram.getListaDeItens().stream().filter(i -> i instanceof Entidade).findFirst().orElseThrow();
            assertNull(diagram.getComando(), "a single click on a tool still creates one object");

            cliqueDuplo(atributo);
            for (int i = 0; i < 3; i++) {
                clicar(new Point(e.getLeft() + 10 + i * 25, e.getTop() + 5));
                assertEquals(Comandos.cmdAtributo, diagram.getComando(), "the tool stays after attribute " + (i + 1));
            }
            assertEquals(3, atributos(), "three attributes, one tool choice");
            assertTrue(atributo.isSelected());

            editor.keyPressed(new KeyEvent(editor, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
            assertNull(diagram.getComando(), "Escape releases the tool");
            clicar(new Point(900, 600));
            assertEquals(3, atributos(), "no object after Escape");

            atributo.doClick();
            clicar(new Point(e.getLeft() + 10, e.getTop() + 40));
            assertEquals(4, atributos());
            assertNull(diagram.getComando(), "after Escape, a single click is single again");
        });
    }

    @Test void theArrowReleasesAFixedTool() throws Exception {
        onEdt(() -> {
            cliqueDuplo(botao(Comandos.cmdEntidade));
            clicar(new Point(300, 300));
            clicar(new Point(700, 300));
            assertEquals(Comandos.cmdEntidade, diagram.getComando());
            editor.getControler().BtnNothing.doClick();
            assertNull(diagram.getComando());
            botao(Comandos.cmdEntidade).doClick();
            clicar(new Point(300, 600));
            assertNull(diagram.getComando(), "choosing the tool once creates one object");
            assertEquals(3, diagram.getListaDeItens().stream().filter(i -> i instanceof Entidade).count());
        });
    }

    private long atributos() {
        return diagram.getListaDeItens().stream().filter(i -> i instanceof Atributo).count();
    }

    private static void cliqueDuplo(JToggleButton b) {
        long t = System.currentTimeMillis();
        b.doClick();
        b.doClick();
        MouseEvent duplo = new MouseEvent(b, MouseEvent.MOUSE_CLICKED, t, 0, 5, 5, 2, false, MouseEvent.BUTTON1);
        for (var l : b.getMouseListeners()) l.mouseClicked(duplo);
    }

    private void clicar(Point p) {
        var box = editor.getBox();
        long t = System.currentTimeMillis();
        diagram.mouseMoved(new MouseEvent(box, MouseEvent.MOUSE_MOVED, t, 0, p.x, p.y, 0, false, MouseEvent.NOBUTTON));
        diagram.mousePressed(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, t, InputEvent.BUTTON1_DOWN_MASK, p.x, p.y, 1, false, MouseEvent.BUTTON1));
        diagram.mouseReleased(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, t, 0, p.x, p.y, 1, false, MouseEvent.BUTTON1));
    }

    private JToggleButton botao(Comandos cmd) {
        JToggleButton b = procurar(frame.getContentPane(), cmd.name());
        assertNotNull(b, "palette button " + cmd);
        return b;
    }

    private static JToggleButton procurar(Container c, String cmd) {
        for (Component x : c.getComponents()) {
            if (x instanceof JToggleButton b && cmd.equals(b.getActionCommand()) && b.isVisible()) return b;
            if (x instanceof Container k) {
                JToggleButton r = procurar(k, cmd);
                if (r != null) return r;
            }
        }
        return null;
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
