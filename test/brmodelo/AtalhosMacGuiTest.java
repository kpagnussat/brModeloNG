package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import controlador.Acao;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import util.Atalhos;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #27 of the original brModelo: on macOS every shortcut asked for Ctrl, where the
 * system uses Command. The tests force the macOS modifier, so they run on any system.
 */
@Tag("gui")
class AtalhosMacGuiTest {
    private static final int CMD = InputEvent.META_DOWN_MASK;

    private principal.FramePrincipal frame;
    private Editor editor;
    private Diagrama diagram;

    @BeforeEach void start() throws Exception {
        Atalhos.usarMascara(CMD);
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
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (editor != null) editor.EndAutoSave();
                if (frame != null) frame.dispose();
            });
        } finally {
            Atalhos.usarMascara(null);
        }
    }

    @Test void menuShortcutsUseCommand() throws Exception {
        onEdt(() -> {
            List<KeyStroke> atalhos = new ArrayList<>();
            for (MenuElement m : frame.getJMenuBar().getSubElements()) coletar(m, atalhos);
            for (Acao a : editor.getControler().ListaDeAcoesEditaveis) {
                if (a.getValue(Action.ACCELERATOR_KEY) instanceof KeyStroke k) atalhos.add(k);
            }
            List<String> comCtrl = atalhos.stream().filter(k -> (k.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0).map(Object::toString).toList();
            assertEquals(List.of(), comCtrl, "shortcuts still on Ctrl");
            for (KeyStroke esperado : List.of(
                    KeyStroke.getKeyStroke(KeyEvent.VK_B, CMD),                              // Save
                    KeyStroke.getKeyStroke(KeyEvent.VK_Z, CMD),                              // Undo
                    KeyStroke.getKeyStroke(KeyEvent.VK_Q, CMD),                              // Quit
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, CMD | InputEvent.SHIFT_DOWN_MASK))) { // New conceptual
                assertTrue(atalhos.contains(esperado), esperado + " in " + atalhos);
            }
        });
    }

    @Test void holdingCommandIsTheHeldModifier() throws Exception {
        onEdt(() -> {
            InputMap im = editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            disparar(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_META, CMD)));
            assertTrue(editor.isControlDown(), "Command held adds to the selection and keeps the tool");
            disparar(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_META, 0, true)));
            assertFalse(editor.isControlDown());
        });
    }

    @Test void commandWheelZooms() throws Exception {
        onEdt(() -> {
            double antes = editor.getZoom();
            diagram.mouseWheelMoved(new MouseWheelEvent(editor.getBox(), MouseEvent_WHEEL, System.currentTimeMillis(), CMD,
                    200, 200, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, -1));
            assertTrue(editor.getZoom() > antes, "Command + wheel zooms in");
        });
    }

    @Test void dialogsConfirmWithCommandEnter() throws Exception {
        onEdt(() -> {
            List<JDialog> dialogos = List.of(
                    new principal.FrameSobre(frame, false),
                    new principal.FormaLogs(frame, false),
                    new util.DlgExecutor(frame, false),
                    new controlador.editores.EditorTexto(frame, false),
                    new controlador.editores.MostradorDeCodigo(frame, false));
            try {
                for (JDialog d : dialogos) {
                    InputMap im = d.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                    String nome = d.getClass().getSimpleName();
                    // Ctrl+Enter may still be there: the look and feel binds it to the default button.
                    assertNotNull(im.get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, CMD)), nome + ": Command+Enter");
                }
            } finally {
                dialogos.forEach(JDialog::dispose);
            }
        });
    }

    private static final int MouseEvent_WHEEL = java.awt.event.MouseEvent.MOUSE_WHEEL;

    private void disparar(Object chave) {
        assertNotNull(chave, "binding");
        Action a = editor.getActionMap().get(chave);
        assertNotNull(a, "action " + chave);
        a.actionPerformed(new ActionEvent(editor, ActionEvent.ACTION_PERFORMED, ""));
    }

    private static void coletar(MenuElement m, List<KeyStroke> atalhos) {
        if (m instanceof JMenuItem mi && !(m instanceof JMenu) && mi.getAccelerator() != null) atalhos.add(mi.getAccelerator());
        for (MenuElement s : m.getSubElements()) coletar(s, atalhos);
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
