package brmodelo;

import controlador.Diagrama;
import controlador.Editor;
import java.awt.Window;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import util.FormatoBrMj;
import static org.junit.jupiter.api.Assertions.*;

@Tag("gui")
@Timeout(30)
class JsonEditorTest {
    private static principal.FramePrincipal frame;
    private static Path output;

    @BeforeAll static void start() throws Exception {
        output = Files.createTempDirectory("brmodelo-json-editor-");
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            principal.Aplicacao.fmPrincipal = frame;
        });
    }

    @AfterAll static void stop() throws Exception {
        CountDownLatch drained = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> frame.getEditor().EndAutoSave(drained::countDown));
        assertTrue(drained.await(10, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) window.dispose();
            principal.Aplicacao.fmPrincipal = null;
        });
    }

    @ParameterizedTest(name = "{0}") @MethodSource("brmodelo.Fixtures#names")
    void opensJsonAsNativeDiagramAndKeepsFormatOnSave(String name) throws Exception {
        Diagrama original = Fixtures.load(name);
        Path json = output.resolve(name + ".brMj");
        FormatoBrMj.salvar(json, original);
        String before = CanonicalDump.dump(original);
        Editor editor = frame.getEditor();
        Diagrama opened = AberturaNoEditor.abrir(editor, json.toFile());
        SwingUtilities.invokeAndWait(() -> {
            try {
                Diagrama loaded = editor.diagramaAtual;
                assertSame(opened, loaded);
                assertEquals(json.toString(), loaded.getArquivo());
                assertEquals(before, CanonicalDump.dump(loaded));
                assertFalse(loaded.getMudou());
                var shape = loaded.getListaDeItens().get(0);
                shape.setLeft(shape.getLeft() + 10); loaded.setMudou(true);
                String edited = CanonicalDump.dump(loaded);
                assertFalse(loaded.Salvar(output.resolve("missing").resolve(name + ".brMj").toFile(), false));
                assertTrue(loaded.getMudou(), "Failed save retains unsaved changes");
                assertEquals(json.toString(), loaded.getArquivo());
                assertTrue(loaded.Salvar(loaded.getArquivo()));
                assertEquals(edited, CanonicalDump.dump(FormatoBrMj.ler(json).diagrama()));
                byte[] saved = Files.readAllBytes(json);
                assertTrue(loaded.Salvar(loaded.getArquivo()));
                assertArrayEquals(saved, Files.readAllBytes(json));
                Path binary = output.resolve(name + ".brM3");
                assertTrue(loaded.Salvar(binary.toFile(), false));
                assertEquals(binary.toString(), loaded.getArquivo());
                assertEquals(edited, CanonicalDump.dump(Fixtures.load(Files.readAllBytes(binary))));
                assertTrue(loaded.Salvar(loaded.getArquivo()));
                assertEquals((byte) 0xac, Files.readAllBytes(binary)[0]);
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
