package controlador;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opening a diagram file reads it in the background, so a slow or hostile file never freezes the window. */
@Tag("gui")
@Timeout(60)
class AberturaEmSegundoPlanoGuiTest {
    private static final File FIXTURE = new File("test-resources/fixtures/conceitual.brM3").getAbsoluteFile();

    private principal.FramePrincipal frame;
    private Editor editor;
    private final CountDownLatch leituraIniciou = new CountDownLatch(1);
    private final CountDownLatch liberar = new CountDownLatch(1);
    private final AtomicBoolean interrompida = new AtomicBoolean();
    private final AtomicReference<Thread> leitura = new AtomicReference<>();

    @BeforeEach void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.addNotify();
            frame.validate();
        });
        // Holds the read until the test releases it, then reads the file for real (worker included).
        AberturaEmSegundoPlano.leitor = arq -> {
            leitura.set(Thread.currentThread());
            leituraIniciou.countDown();
            try {
                assertTrue(liberar.await(30, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                interrompida.set(true);
            }
            return PersistenciaDiagrama.lerModelo(arq);
        };
    }

    @AfterEach void stop() throws Exception {
        liberar.countDown();
        AberturaEmSegundoPlano.leitor = PersistenciaDiagrama::lerModelo;
        SwingUtilities.invokeAndWait(() -> {
            if (editor != null) editor.EndAutoSave();
            for (Window window : Window.getWindows()) window.dispose();
            principal.Aplicacao.fmPrincipal = null;
        });
    }

    /** Starts the open command from the EDT without waiting there; the future it returns. */
    private CompletableFuture<Diagrama> abrir(File arq) throws Exception {
        AtomicReference<CompletableFuture<Diagrama>> res = new AtomicReference<>();
        SwingUtilities.invokeLater(() -> res.set(ArquivosEditor.AbrirDiagramaFromFile(editor, arq)));
        assertTrue(leituraIniciou.await(10, TimeUnit.SECONDS), "the read started");
        return esperar(res::get);
    }

    private static <T> T esperar(java.util.function.Supplier<T> valor) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        for (T v; System.nanoTime() < limite; Thread.sleep(20)) if ((v = valor.get()) != null) return v;
        return fail("timed out");
    }

    /** The EDT runs a task within a second, while the read is still held. */
    private static void edtLivre() throws InterruptedException {
        CountDownLatch rodou = new CountDownLatch(1);
        SwingUtilities.invokeLater(rodou::countDown);
        assertTrue(rodou.await(1, TimeUnit.SECONDS), "the EDT is blocked by the open");
    }

    private static JDialog dialogoAberto() throws Exception {
        AtomicReference<JDialog> res = new AtomicReference<>();
        Runnable procurar = () -> {
            for (Window w : Window.getWindows())
                if (w instanceof JDialog d && "abrindoDiagrama".equals(d.getName()) && d.isShowing()) res.set(d);
        };
        return esperar(() -> {
            try { SwingUtilities.invokeAndWait(procurar); } catch (Exception e) { throw new RuntimeException(e); }
            return res.get();
        });
    }

    private static <T extends Component> T componente(Container c, String nome, Class<T> tipo) {
        for (Component filho : c.getComponents()) {
            if (tipo.isInstance(filho) && nome.equals(filho.getName())) return tipo.cast(filho);
            if (filho instanceof Container sub && componente(sub, nome, tipo) != null) return componente(sub, nome, tipo);
        }
        return null;
    }

    private Diagrama naEdt(java.util.concurrent.Callable<Diagrama> c) throws Exception {
        AtomicReference<Diagrama> res = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { res.set(c.call()); } catch (Exception e) { throw new RuntimeException(e); }
        });
        return res.get();
    }

    @Test void theWindowKeepsWorkingWhileAFileIsReadAndShowsTheDiagramAfter() throws Exception {
        Diagrama antes = naEdt(() -> editor.diagramaAtual);
        CompletableFuture<Diagrama> aberto = abrir(FIXTURE);
        edtLivre();
        assertFalse(aberto.isDone());
        dialogoAberto(); // a read longer than the delay tells the user what is going on
        edtLivre();
        liberar.countDown();
        Diagrama res = aberto.get(20, TimeUnit.SECONDS);
        assertNotNull(res);
        assertSame(res, naEdt(() -> editor.diagramaAtual));
        assertNotSame(antes, res);
        assertEquals(FIXTURE.getPath(), res.getArquivo());
        assertEquals("conceitual", res.getNome());
        assertFalse(res.getListaDeItens().isEmpty());
        semDialogo();
    }

    private static void semDialogo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (Window w : Window.getWindows())
                assertFalse(w instanceof JDialog d && "abrindoDiagrama".equals(d.getName()) && d.isShowing(), "dialog left open");
        });
    }

    @Test void cancelGivesTheWindowBackAtOnceAndDropsTheLateResult() throws Exception {
        Diagrama antes = naEdt(() -> editor.diagramaAtual);
        java.util.concurrent.atomic.AtomicInteger abertos = new java.util.concurrent.atomic.AtomicInteger();
        SwingUtilities.invokeAndWait(() -> abertos.set(editor.getDiagramas().size()));
        CompletableFuture<Diagrama> aberto = abrir(FIXTURE);
        JDialog dialogo = dialogoAberto();
        SwingUtilities.invokeAndWait(() -> componente(dialogo.getContentPane(), "cancelarAbertura", JButton.class).doClick());
        assertNull(aberto.get(1, TimeUnit.SECONDS), "cancelled without waiting for the read");
        // The interrupt is what kills the import worker (TrabalhadorImportacaoTest).
        assertTrue(esperar(() -> interrompida.get() ? true : null));
        // The read still finishes here (the held reader then reads the file); nothing is installed.
        leitura.get().join(20_000);
        assertFalse(leitura.get().isAlive());
        SwingUtilities.invokeAndWait(() -> {}); // its result, posted to the EDT, has been handled
        assertSame(antes, naEdt(() -> editor.diagramaAtual));
        SwingUtilities.invokeAndWait(() -> assertEquals(abertos.get(), editor.getDiagramas().size()));
        semDialogo();
        synchronized (util.BrLogger.Logs) {
            var ultimo = util.BrLogger.Logs.get(util.BrLogger.Logs.size() - 1);
            assertEquals("INFO_DIAGRAMA_LOAD_CANCELLED", ultimo.Tipo);
        }
    }

    @Test void aFileOpenedTwiceWhileReadingIsInstalledOnce() throws Exception {
        CompletableFuture<Diagrama> primeiro = abrir(FIXTURE);
        AtomicReference<CompletableFuture<Diagrama>> segundo = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> segundo.set(ArquivosEditor.AbrirDiagramaFromFile(editor, FIXTURE)));
        liberar.countDown();
        // Either read may finish first; the other finds the file open and installs nothing.
        Diagrama a = primeiro.get(30, TimeUnit.SECONDS), b = segundo.get().get(30, TimeUnit.SECONDS);
        assertTrue(a == null ^ b == null, a + " / " + b);
        SwingUtilities.invokeAndWait(() -> assertEquals(1,
                editor.getDiagramas().stream().filter(d -> FIXTURE.getPath().equals(d.getArquivo())).count()));
    }

    @Test void aRefusedFileIsLoggedAndOpensNothing() throws Exception {
        AberturaEmSegundoPlano.leitor = arq -> {
            leituraIniciou.countDown();
            throw new IOException("import worker: refused");
        };
        Diagrama antes = naEdt(() -> editor.diagramaAtual);
        assertNull(abrir(FIXTURE).get(10, TimeUnit.SECONDS));
        assertSame(antes, naEdt(() -> editor.diagramaAtual));
        synchronized (util.BrLogger.Logs) {
            var ultimo = util.BrLogger.Logs.get(util.BrLogger.Logs.size() - 1);
            assertEquals("ERROR_DIAGRAMA_LOAD_FILE_BRM", ultimo.Tipo);
            assertTrue(ultimo.Valor.contains("import worker: refused"), ultimo.Valor);
        }
    }
}
