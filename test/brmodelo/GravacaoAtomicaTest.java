package brmodelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.GravacaoAtomica;
import static org.junit.jupiter.api.Assertions.*;

/** Saving replaces a file whole or not at all: a failed write keeps the previous file. */
class GravacaoAtomicaTest {
    @TempDir Path dir;

    @Test
    void failedWriteKeepsThePreviousFileAndLeavesNoTemporary() throws Exception {
        Path alvo = Files.writeString(dir.resolve("aula.brM3"), "versão anterior");
        IOException erro = assertThrows(IOException.class, () -> GravacaoAtomica.gravar(alvo, saida -> {
            saida.write("metade do novo".getBytes(StandardCharsets.UTF_8));
            throw new IOException("disco cheio");
        }));
        assertEquals("disco cheio", erro.getMessage());
        assertEquals("versão anterior", Files.readString(alvo));
        try (Stream<Path> s = Files.list(dir)) { assertEquals(List.of(alvo), s.toList()); }
    }

    @Test
    void successReplacesTheContentEvenWhenTheWriterClosesTheStream() throws Exception {
        Path alvo = Files.writeString(dir.resolve("aula.xml"), "um texto anterior bem mais comprido");
        // ObjectOutputStream and Writer close what they wrap; the sync must still happen after.
        GravacaoAtomica.gravar(alvo, saida -> {
            try (var w = new java.io.OutputStreamWriter(saida, StandardCharsets.UTF_8)) { w.write("novo"); }
        });
        assertEquals("novo", Files.readString(alvo));
        GravacaoAtomica.gravar(dir.resolve("novo.sql"), saida -> saida.write(1));
        try (Stream<Path> s = Files.list(dir)) { assertEquals(2, s.count()); }
    }

    @Test
    void permissionsAndSymbolicLinksAreKept() throws Exception {
        if (!dir.getFileSystem().supportedFileAttributeViews().contains("posix")) return;
        Path real = Files.writeString(dir.resolve("real.brMj"), "antes");
        Files.setPosixFilePermissions(real, PosixFilePermissions.fromString("rw-r-----"));
        Path link = Files.createSymbolicLink(dir.resolve("atalho.brMj"), real.getFileName());
        GravacaoAtomica.gravar(link, saida -> saida.write("depois".getBytes(StandardCharsets.UTF_8)));
        assertTrue(Files.isSymbolicLink(link), "The link stays a link");
        assertEquals("depois", Files.readString(real));
        assertEquals("rw-r-----", PosixFilePermissions.toString(Files.getPosixFilePermissions(real)));
    }

    @Test
    void aWritableFileInAReadOnlyFolderIsStillSaved() throws Exception {
        if (!dir.getFileSystem().supportedFileAttributeViews().contains("posix")) return;
        Path pasta = Files.createDirectory(dir.resolve("compartilhada"));
        Path alvo = Files.writeString(pasta.resolve("turma.brM3"), "antes");
        Files.setPosixFilePermissions(pasta, PosixFilePermissions.fromString("r-x------"));
        try {
            if (Files.isWritable(pasta)) return; // Running as root.
            GravacaoAtomica.gravar(alvo, saida -> saida.write("depois".getBytes(StandardCharsets.UTF_8)));
            assertEquals("depois", Files.readString(alvo));
        } finally {
            Files.setPosixFilePermissions(pasta, PosixFilePermissions.fromString("rwx------"));
        }
    }

    // Every save, export and settings write goes through GravacaoAtomica.
    private static final Pattern ESCRITA_DIRETA = Pattern.compile(
            "new\\s+(FileOutputStream|FileWriter|PrintWriter\\s*\\(\\s*\\w+\\.getAbsoluteFile)"
            + "|Files\\.(write|writeString|newOutputStream|newBufferedWriter)\\s*\\("
            + "|ImageIO\\.write\\s*\\((?![^;]*,\\s*(saida|baos)\\s*\\))");

    @Test
    void everyDiskWriteGoesThroughTheAtomicWriter() throws Exception {
        List<String> diretas = new ArrayList<>();
        try (Stream<Path> fontes = Files.walk(Path.of("src"))) {
            for (Path f : fontes.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (f.endsWith(Path.of("util", "GravacaoAtomica.java"))) continue;
                List<String> linhas = Files.readAllLines(f, StandardCharsets.UTF_8);
                for (int i = 0; i < linhas.size(); i++) {
                    if (ESCRITA_DIRETA.matcher(linhas.get(i)).find()) diretas.add(f + ":" + (i + 1) + ": " + linhas.get(i).strip());
                }
            }
        }
        assertEquals(List.of(), diretas);
    }
}
