package util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Every file the program saves is written here: diagrams, parts, recovery data, settings
 * and exports. The content goes to a temporary file in the same folder, is synced to disk
 * and then renamed over the target, so a crash, a full disk or an error while encoding
 * leaves the previous file whole instead of a truncated one.
 */
public final class GravacaoAtomica {
    private GravacaoAtomica() {}

    /** Writes the content to a stream; closing that stream is allowed and harmless. */
    @FunctionalInterface
    public interface Escritor {
        void escrever(OutputStream saida) throws IOException;
    }

    public static void gravar(Path destino, Escritor escritor) throws IOException {
        Path alvo = destino.toAbsolutePath();
        // Replace the file a link points to, not the link itself.
        if (Files.isSymbolicLink(alvo)) alvo = alvo.toRealPath();
        Path temp;
        try {
            temp = criarTemporario(alvo);
        } catch (AccessDeniedException e) {
            // A writable file in a folder the user cannot write to (a shared class folder,
            // for instance) can still be saved, only not atomically.
            BrLogger.Logger("WARN_SAVE_NOT_ATOMIC", alvo.toString());
            try (FileOutputStream saida = new FileOutputStream(alvo.toFile())) {
                escreverESincronizar(saida, escritor);
            }
            return;
        }
        try {
            try (FileOutputStream saida = new FileOutputStream(temp.toFile())) {
                escreverESincronizar(saida, escritor);
            }
            copiarPermissoes(alvo, temp);
            try {
                Files.move(temp, alvo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, alvo, StandardCopyOption.REPLACE_EXISTING);
            }
            sincronizarPasta(alvo.getParent());
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // Created like a plain new file, so its permissions follow the user's umask; a
    // createTempFile would make every saved file private to the owner.
    private static Path criarTemporario(Path alvo) throws IOException {
        for (int tentativa = 0; ; tentativa++) {
            Path temp = alvo.resolveSibling("." + alvo.getFileName() + "."
                    + Long.toHexString(ThreadLocalRandom.current().nextLong() >>> 1) + ".tmp");
            try {
                Files.newByteChannel(temp, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).close();
                return temp;
            } catch (FileAlreadyExistsException e) {
                if (tentativa == 9) throw e;
            }
        }
    }

    private static void escreverESincronizar(FileOutputStream arquivo, Escritor escritor) throws IOException {
        BufferedOutputStream buffer = new BufferedOutputStream(arquivo, 1 << 16);
        escritor.escrever(new FilterOutputStream(buffer) {
            @Override public void write(byte[] b, int off, int len) throws IOException { out.write(b, off, len); }
            @Override public void close() throws IOException { flush(); }
        });
        buffer.flush();
        arquivo.getFD().sync();
    }

    // Keeps the mode of the file being replaced; the rename would otherwise bring the
    // temporary file's mode.
    private static void copiarPermissoes(Path alvo, Path temp) {
        try {
            PosixFileAttributeView origem = Files.getFileAttributeView(alvo, PosixFileAttributeView.class);
            if (origem == null || !Files.exists(alvo)) return;
            Files.setPosixFilePermissions(temp, origem.readAttributes().permissions());
        } catch (IOException | UnsupportedOperationException e) {
            BrLogger.Logger("WARN_SAVE_PERMISSIONS", e.getMessage());
        }
    }

    // Makes the rename itself durable on Linux; other systems refuse to open a folder.
    private static void sincronizarPasta(Path pasta) {
        try (FileChannel canal = FileChannel.open(pasta, StandardOpenOption.READ)) {
            canal.force(true);
        } catch (IOException | UnsupportedOperationException e) {
            // Best effort: the file content is already on disk.
        }
    }
}
