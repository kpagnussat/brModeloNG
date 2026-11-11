package brmodelo;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.BrLogger;
import util.LimitesEntrada;
import static org.junit.jupiter.api.Assertions.*;

/** Guards the size limits on files read from outside the program and on declared sizes. */
class LimitesEntradaTest {
    @TempDir Path dir;

    // Sparse: the size is reported without writing the bytes.
    private Path arquivo(String nome, long tamanho) throws Exception {
        Path p = dir.resolve(nome);
        try (RandomAccessFile f = new RandomAccessFile(p.toFile(), "rw")) { f.setLength(tamanho); }
        return p;
    }

    @Test
    void brMjAboveTheLimitIsRefusedBeforeReading() throws Exception {
        Path p = arquivo("grande.brMj", LimitesEntrada.JSON_BYTES + 1);
        var e = assertThrows(LimitesEntrada.Excedido.class, () -> util.FormatoBrMj.ler(p));
        assertTrue(e.getMessage().contains("grande.brMj"), e.getMessage());
    }

    @Test
    void partsFileAboveTheLimitIsRefusedBeforeReading() throws Exception {
        Path p = arquivo("Template.brMt", LimitesEntrada.BINARIO_BYTES + 1);
        assertNull(partepronta.GerenciadorPartes.LoadData(p.toString()));
        String ultimo = BrLogger.Logs.get(BrLogger.Logs.size() - 1).Valor;
        assertTrue(ultimo.contains(" > " + LimitesEntrada.BINARIO_BYTES), ultimo);
    }

    // A fresh JVM with a small heap: without the bound the declared size is allocated.
    @Test
    void arraysAndMapsDeclaredLargerThanTheInputAreRefused() throws Exception {
        var r = JvmFilha.rodar(60, List.of("-Xmx256m"), SondaLeitor.class);
        assertFalse(r.expirou(), r.texto());
        assertTrue(r.texto().contains("array: recusado"), r.texto());
        assertTrue(r.texto().contains("hashmap: recusado"), r.texto());
        assertTrue(r.texto().contains("fim"), r.texto());
    }
}
