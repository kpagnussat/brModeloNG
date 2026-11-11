package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/** Opens a file through the editor's own open command and waits for it, off the EDT. */
final class AberturaNoEditor {
    private AberturaNoEditor() {}

    /** The diagram installed, or null when the file was refused; call off the EDT. */
    static Diagrama abrir(Editor editor, File arq) throws Exception {
        CompletableFuture<?>[] aberto = new CompletableFuture<?>[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                Method open = Editor.class.getDeclaredMethod("AbrirDiagramaFromFile", File.class);
                open.setAccessible(true);
                aberto[0] = (CompletableFuture<?>) open.invoke(editor, arq);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        // Model files are read in the background and installed on the EDT.
        return (Diagrama) aberto[0].get(20, TimeUnit.SECONDS);
    }
}
