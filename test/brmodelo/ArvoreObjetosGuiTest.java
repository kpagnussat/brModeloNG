package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import controlador.apoios.TreeItem;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import util.BrLogger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #31 of the original brModelo: the object tree asked for an icon that does not
 * exist and the log filled with ERROR_GET_RESOURCE_IMG. Every row needs an icon.
 */
@Tag("gui")
class ArvoreObjetosGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;

    @BeforeEach void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            BrLogger.Clean();
        });
    }

    @AfterEach void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
        });
    }

    static List<String> fixtures() { return Fixtures.names().toList(); }

    @ParameterizedTest @MethodSource("fixtures")
    void everyRowOfTheObjectTreeHasAnIcon(String name) throws Exception {
        Diagrama d = Fixtures.load(name);
        d.setNome(name); // set by the editor when a file is opened
        onEdt(() -> conferirIcones(d.AtualizeTreeNavegacao(true)));
    }

    @Test void theSelectionRectangleIsNotAnObject() throws Exception {
        onEdt(() -> {
            Diagrama d = editor.AddAsAtual("tpConceitual");
            editor.getControler().BtnNothing.doClick();
            var box = editor.getBox();
            long t = System.currentTimeMillis();
            d.mousePressed(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, t, InputEvent.BUTTON1_DOWN_MASK, 100, 100, 1, false, MouseEvent.BUTTON1));
            assertNotNull(editor.getMultSel(), "a press on empty space starts the selection rectangle");
            List<String> linhas = conferirIcones(d.AtualizeTreeNavegacao(true));
            assertFalse(linhas.contains("Selecionador"), linhas.toString());
            d.mouseReleased(new MouseEvent(box, MouseEvent.MOUSE_RELEASED, t, 0, 300, 300, 1, false, MouseEvent.BUTTON1));
        });
    }

    private List<String> conferirIcones(TreeItem raiz) {
        List<String> semIcone = new ArrayList<>(), linhas = new ArrayList<>();
        for (Object o : Collections.list(raiz.depthFirstEnumeration())) {
            TreeItem it = (TreeItem) o;
            if (it == raiz) continue;
            linhas.add(it.getExtraInfo());
            if (editor.getControler().getIconeInterface(it.getExtraInfo()) == null) semIcone.add(it.getExtraInfo());
        }
        assertEquals(List.of(), semIcone, "rows without an icon");
        List<String> erros;
        synchronized (BrLogger.Logs) {
            erros = BrLogger.Logs.stream().filter(x -> x.Tipo.startsWith("ERROR_GET_RESOURCE")).map(Object::toString).toList();
        }
        assertEquals(List.of(), erros);
        return linhas;
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
