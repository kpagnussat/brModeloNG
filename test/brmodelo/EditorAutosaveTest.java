package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import util.LeitorSeguro;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
@Timeout(30)
class EditorAutosaveTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private Path recovery;

    @BeforeEach void start() throws Exception {
        recovery = Path.of(Editor.fromConfiguracao.getAutoSaveFile());
        Files.deleteIfExists(recovery);
        edt(() -> {
            frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
            editor = frame.getEditor();
        });
    }

    @AfterEach void stop() throws Exception {
        if (editor != null) {
            CountDownLatch finished = new CountDownLatch(1);
            edt(() -> editor.EndAutoSave(finished::countDown));
            assertTrue(finished.await(10, TimeUnit.SECONDS), "Shutdown drains the writer");
            ExecutorService writer = (ExecutorService) field("autoSaveWriter").get(editor);
            if (writer != null) assertTrue(writer.awaitTermination(10, TimeUnit.SECONDS));
        }
        edt(() -> {
            for (Window window : Window.getWindows()) window.dispose();
            principal.Aplicacao.fmPrincipal = null;
        });
        Files.deleteIfExists(recovery);
    }

    @Test void statusAndIntervalChangesUseBoundedDaemonThreads() throws Exception {
        AtomicBoolean offEdt = new AtomicBoolean();
        edt(() -> {
            JLabel label = new JLabel("");
            label.addPropertyChangeListener("text", event -> {
                if (!SwingUtilities.isEventDispatchThread()) offEdt.set(true);
            });
            editor.setLblStatus(label);
            editor.DoStatus("warm up");
        });
        ExecutorService writer = writer();
        writer.submit(() -> {}).get(10, TimeUnit.SECONDS);
        Timer schedule = (Timer) field("autoSaveTimer").get(editor);
        long baseline = schedulerThreads();
        for (int i = 0; i < 200; i++) {
            // Exercise public entry points from a non-EDT caller too.
            editor.DoStatus("status " + i);
            editor.setAutoSaveInterval(i % 2 + 1);
        }
        editor.setAutoSaveInterval(0);
        writer.submit(() -> {}).get(10, TimeUnit.SECONDS);
        CountDownLatch blinkFinished = new CountDownLatch(1);
        edt(() -> {
            assertSame(schedule, get("autoSaveTimer"), "Interval changes reuse one schedule");
            assertFalse(schedule.isRunning());
            Timer status = (Timer) get("statusTimer");
            status.setDelay(1);
            Timer poll = new Timer(10, event -> {
                if (!status.isRunning()) {
                    ((Timer) event.getSource()).stop();
                    blinkFinished.countDown();
                }
            });
            poll.start();
        });
        assertTrue(blinkFinished.await(10, TimeUnit.SECONDS));
        assertFalse(offEdt.get(), "Every JLabel mutation must be on the EDT");
        assertEquals(baseline, schedulerThreads(), "200 messages/interval changes must not grow threads");
        assertTrue(Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.isAlive() && t.getName().equals("brModelo-autosave-writer"))
                .allMatch(Thread::isDaemon));
    }

    @Test void scheduledSnapshotsRunOnEdtAndExitClearsAfterQueuedWrites() throws Exception {
        CountDownLatch captured = new CountDownLatch(1);
        AtomicBoolean snapshotOnEdt = new AtomicBoolean();
        ExecutorService writer = writer();
        CountDownLatch release = new CountDownLatch(1);
        writer.submit(() -> await(release));
        CountDownLatch exited = new CountDownLatch(1);
        try {
            edt(() -> {
                Diagrama probe = new Diagrama(editor) {
                    private static final long serialVersionUID = 1L;
                    @Override public boolean AutoSalvar(ArrayList<byte[]> bytes) {
                        snapshotOnEdt.set(SwingUtilities.isEventDispatchThread());
                        bytes.add(new byte[]{1, 2, 3});
                        captured.countDown();
                        return true;
                    }
                };
                ((controlador.apoios.Historico) get("historicos")).add(probe);
                probe.setMudou(true);
                editor.DoDiagramaMuda();
                editor.setAutoSaveInterval(1); // The same immediate first tick as before.
            });
            assertTrue(captured.await(10, TimeUnit.SECONDS));
            assertTrue(snapshotOnEdt.get());
            edt(() -> editor.EndAutoSave(() -> {
                assertTrue(SwingUtilities.isEventDispatchThread());
                exited.countDown();
            }));
            assertEquals(1, exited.getCount(), "Exit waits for the blocked disk worker");
            assertFalse(((Timer) field("autoSaveTimer").get(editor)).isRunning());
        } finally { release.countDown(); }
        assertTrue(exited.await(10, TimeUnit.SECONDS));
        assertTrue(writer.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(saved().isEmpty(), "The exit clear follows the snapshot in FIFO order");
        edt(() -> frame.dispose());
        editor = null;
    }

    @ParameterizedTest(name = "{0}") @MethodSource("brmodelo.Fixtures#names")
    void detachedBytesAreRecoveredByEditor(String name) throws Exception {
        ExecutorService writer = writer();
        CountDownLatch release = new CountDownLatch(1);
        writer.submit(() -> await(release));
        String[] before = new String[1];
        try {
            assertNotNull(AberturaNoEditor.abrir(editor, Path.of("test-resources/fixtures", name + ".brM3").toFile()));
            edt(() -> {
                try {
                    editor.diagramaAtual.SetNome("snapshot");
                    editor.diagramaAtual.setMudou(true);
                    before[0] = CanonicalDump.dump(editor.diagramaAtual);
                    editor.DoAutoSaveCompleto();
                    // The writer is blocked, but the EDT remains available for further edits.
                    editor.diagramaAtual.SetNome("edited after snapshot");
                    var shape = editor.diagramaAtual.getListaDeItens().get(0);
                    shape.setLeft(shape.getLeft() + 75);
                    assertNotEquals(before[0], CanonicalDump.dump(editor.diagramaAtual));
                    editor.DoDiagramaMuda();
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        } finally { release.countDown(); }
        writer.submit(() -> {}).get(10, TimeUnit.SECONDS);
        ArrayList<byte[]> bytes = saved();
        assertEquals(1, bytes.size());
        assertFalse(field("doneAutoSave").getBoolean(editor), "Edits after capture remain dirty after writing");
        assertEquals(before[0], CanonicalDump.dump(Fixtures.load(bytes.get(0))), "Worker writes the captured state");
        edt(() -> {
            Timer accept = new Timer(10, event -> {
                for (Window window : Window.getWindows()) {
                    if (window.isShowing()) acceptOption(window);
                }
            });
            accept.start();
            try {
                assertTrue(editor.LoadAutoSave(), "Use the actual Editor recovery path");
                assertEquals("snapshot", editor.diagramaAtual.getNome());
                assertTrue(editor.diagramaAtual.getMudou());
                assertEquals(before[0], CanonicalDump.dump(editor.diagramaAtual));
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { accept.stop(); }
        });
    }

    // Guards the size limit on the editor's own entry points: opening .brM3/.brMj and the
    // recovery file. Sparse files report their size without being written.
    @Test void oversizedFilesAreRefusedOnEveryOpenPath() throws Exception {
        Path dir = Files.createTempDirectory("limites");
        Path brm3 = esparso(dir.resolve("grande.brM3"), util.LimitesEntrada.BINARIO_BYTES + 1);
        Path brmj = esparso(dir.resolve("grande.brMj"), util.LimitesEntrada.JSON_BYTES + 1);
        esparso(recovery, util.LimitesEntrada.BINARIO_BYTES + 1);
        Diagrama[] inicial = new Diagrama[1];
        edt(() -> inicial[0] = editor.diagramaAtual);
        for (Path p : new Path[]{brm3, brmj}) {
            assertNull(AberturaNoEditor.abrir(editor, p.toFile()), p.getFileName() + " is not opened");
            edt(() -> {
                assertSame(inicial[0], editor.diagramaAtual, p.getFileName() + " is not opened");
                assertTrue(ultimoLog().contains(p.getFileName() + ": "), ultimoLog());
            });
        }
        edt(() -> {
            try {
                // Refused before reading, so no recovery prompt is shown.
                assertFalse(editor.LoadAutoSave());
                assertTrue(ultimoLog().contains(" > " + util.LimitesEntrada.BINARIO_BYTES), ultimoLog());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    private static Path esparso(Path p, long tamanho) throws Exception {
        try (var f = new java.io.RandomAccessFile(p.toFile(), "rw")) { f.setLength(tamanho); }
        return p;
    }

    private static String ultimoLog() {
        synchronized (util.BrLogger.Logs) {
            var e = util.BrLogger.Logs.get(util.BrLogger.Logs.size() - 1);
            return e.Tipo + e.Complemento + e.Valor;
        }
    }

    private static void acceptOption(Container container) {
        if (container instanceof JOptionPane option) option.setValue(JOptionPane.YES_OPTION);
        else for (var child : container.getComponents())
            if (child instanceof Container nested) acceptOption(nested);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<byte[]> saved() throws Exception {
        try (LeitorSeguro in = new LeitorSeguro(Files.newInputStream(recovery))) {
            return (ArrayList<byte[]>) in.readObject();
        }
    }

    private ExecutorService writer() throws Exception {
        edt(() -> {
            try {
                Method method = Editor.class.getDeclaredMethod("autoSaveWriter");
                method.setAccessible(true);
                method.invoke(editor);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        return (ExecutorService) field("autoSaveWriter").get(editor);
    }

    private Object get(String name) {
        try { return field(name).get(editor); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static Field field(String name) throws Exception {
        Field field = Editor.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static long schedulerThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter(t -> t.isAlive()
                && (t.getName().startsWith("Timer-") || t.getName().equals("TimerQueue")
                || t.getName().equals("brModelo-autosave-writer"))).count();
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    private static void edt(Runnable action) throws Exception { SwingUtilities.invokeAndWait(action); }
}
