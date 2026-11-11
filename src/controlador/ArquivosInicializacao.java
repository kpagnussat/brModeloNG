package controlador;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;

/** Opens launcher arguments through the same persistence path as the editor menu. */
public final class ArquivosInicializacao {
    private ArquivosInicializacao() {}

    /** Opens the files one after another, in order; completes on the EDT after the last one. */
    public static CompletableFuture<Void> abrir(Editor editor, String[] argumentos) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Open files on the EDT");
        CompletableFuture<Void> fila = CompletableFuture.completedFuture(null);
        for (String argumento : argumentos) fila = fila.thenCompose(x -> abrir(editor, argumento));
        return fila;
    }

    private static CompletableFuture<Void> abrir(Editor editor, String argumento) {
        File file = new File(argumento).getAbsoluteFile();
        if (!file.isFile()) {
            System.getLogger(ArquivosInicializacao.class.getName()).log(System.Logger.Level.WARNING,
                    "Cannot open file: " + file);
            return CompletableFuture.completedFuture(null);
        }
        var opened = editor.getDiagramas().stream().filter(d -> file.getPath().equals(d.getArquivo())).findFirst();
        if (opened.isPresent()) {
            editor.setSelected(opened.get());
            return CompletableFuture.completedFuture(null);
        }
        return ArquivosEditor.AbrirDiagramaFromFile(editor, file).thenAccept(aberto -> {
            if (aberto != null && file.getPath().equals(aberto.getArquivo())) {
                System.getLogger(ArquivosInicializacao.class.getName()).log(System.Logger.Level.INFO,
                        "Opened diagram: " + file + " (" + aberto.getListaDeItens().size() + " elements)");
            }
        });
    }
}
