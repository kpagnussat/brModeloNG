package util;

import brmodelo.CanonicalDump;
import controlador.Diagrama;
import controlador.apoios.GuardaPadraoBrM;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Diagram files are decoded by a separate, bounded worker process. */
class TrabalhadorImportacaoTest {
    private static final String SONDA = SondaTrabalhador.class.getName();

    private static byte[] fixture(String nome) throws IOException {
        try (InputStream in = TrabalhadorImportacaoTest.class.getResourceAsStream("/fixtures/" + nome + ".brM3")) {
            return in.readAllBytes();
        }
    }

    // The oracle decodes in this process, the way files were opened before the worker.
    private static Diagrama direto(byte[] brM3) throws Exception {
        try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(brM3))) {
            return ((GuardaPadraoBrM) in.readObject()).getDiagrama();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"conceitual", "logico", "fluxo", "atividade", "eap", "livre"})
    void fixturesOpenThroughTheWorkerUnchanged(String nome) throws Exception {
        byte[] brM3 = fixture(nome);
        String esperado = CanonicalDump.dump(direto(brM3));
        assertEquals(esperado, CanonicalDump.dump(TrabalhadorImportacao.abrir(brM3, false)));
        byte[] brMj = FormatoBrMj.escrever(direto(brM3));
        assertEquals(esperado, CanonicalDump.dump(TrabalhadorImportacao.abrir(brMj, true)));
    }

    @Test
    void aFileTheWorkerRejectsIsRefusedWithoutFallingBack() {
        IOException erro = assertThrows(IOException.class,
                () -> TrabalhadorImportacao.abrir("não é um diagrama".getBytes(StandardCharsets.UTF_8), false));
        assertTrue(erro.getMessage().startsWith("import worker: "), erro.getMessage());
    }

    @Test
    void aWorkerPastItsDeadlineIsKilled() throws Exception {
        long inicio = System.nanoTime();
        var r = TrabalhadorImportacao.executar(TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.PROCESSO,
                "-Xmx64m", SONDA, "lento"), new byte[0], 2, 1024);
        assertTrue(r.iniciou());
        assertTrue(r.expirou());
        assertTrue(System.nanoTime() - inicio < 6_000_000_000L, "killed soon after the deadline");
        IOException erro = assertThrows(IOException.class, () -> r.respostaDe(2));
        assertEquals("import worker exceeded 2 s", erro.getMessage());
    }

    @Test
    void underFlatpakTheWorkerStartsInANewSandboxWithAnEmptyEnvironment() throws Exception {
        List<String> c = TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.ISOLADO, true, "-Xmx64m", SONDA, "lento");
        assertTrue(c.getFirst().endsWith("flatpak-spawn"), c.toString());
        assertTrue(c.containsAll(List.of("--sandbox", "--no-network", "--watch-bus", "--directory=/")), c.toString());
        // The JVM follows env -i directly (java, or javaw.exe/java.exe when this runs on Windows).
        int env = c.indexOf("/usr/bin/env");
        assertEquals("-i", c.get(env + 1), c.toString());
        assertTrue(Path.of(c.get(env + 2)).startsWith(Path.of(System.getProperty("java.home"), "bin")), c.toString());
        assertTrue(c.get(env + 3).startsWith(TrabalhadorImportacao.MARCA), c.toString());
        assertTrue(c.indexOf("--directory=/") < env, "flatpak-spawn options come before the command");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void aWorkerOutsideThisProcessTreeIsKilledByItsMark() throws Exception {
        // As under Flatpak, where the portal starts the worker: the process started here
        // (flatpak-spawn there, a shell here) is not its parent and killing it leaves the worker.
        List<String> trabalhador = TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.PROCESSO, "-Xmx64m", SONDA, "lento");
        String marca = trabalhador.stream().filter(a -> a.startsWith(TrabalhadorImportacao.MARCA)).findFirst().orElseThrow();
        List<String> comando = new ArrayList<>(List.of("sh", "-c", "(setsid \"$@\" &); exec sleep 600", "sh"));
        comando.addAll(trabalhador);
        long inicio = System.nanoTime();
        var r = TrabalhadorImportacao.executar(comando, new byte[0], 2, 1024);
        assertTrue(r.iniciou());
        assertTrue(r.expirou());
        assertTrue(System.nanoTime() - inicio < 6_000_000_000L, "killed soon after the deadline");
        long limite = System.nanoTime() + 2_000_000_000L;
        while (vivo(marca) && System.nanoTime() < limite) Thread.sleep(50);
        assertFalse(vivo(marca), "the worker outlived its deadline");
    }

    @Test
    void cancellingAnImportKillsItsWorker() throws Exception {
        // Cancel in the opening dialog interrupts the thread waiting for the worker.
        List<String> comando = TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.PROCESSO, "-Xmx64m", SONDA, "lento");
        String marca = comando.stream().filter(a -> a.startsWith(TrabalhadorImportacao.MARCA)).findFirst().orElseThrow();
        java.util.concurrent.atomic.AtomicReference<Throwable> erro = new java.util.concurrent.atomic.AtomicReference<>();
        Thread espera = new Thread(() -> {
            try { TrabalhadorImportacao.executar(comando, new byte[0], 30, 1024); }
            catch (Throwable e) { erro.set(e); }
        });
        espera.start();
        long limite = System.nanoTime() + 10_000_000_000L;
        while (!vivo(marca) && System.nanoTime() < limite) Thread.sleep(50);
        assertTrue(vivo(marca), "the worker started");
        long inicio = System.nanoTime();
        espera.interrupt();
        espera.join(5_000);
        assertFalse(espera.isAlive());
        assertTrue(System.nanoTime() - inicio < 3_000_000_000L, "stopped at once, not at the deadline");
        assertEquals("import interrupted", erro.get().getMessage());
        limite = System.nanoTime() + 2_000_000_000L;
        while (vivo(marca) && System.nanoTime() < limite) Thread.sleep(50);
        assertFalse(vivo(marca), "the worker outlived the cancel");
    }

    private static boolean vivo(String marca) {
        return ProcessHandle.allProcesses().anyMatch(h -> h.info().arguments().map(a -> List.of(a).contains(marca)).orElse(false));
    }

    @Test
    void aWorkerOutOfMemoryEndsAloneAndTheFileIsRefused() throws Exception {
        var r = TrabalhadorImportacao.executar(TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.PROCESSO,
                "-Xmx64m", SONDA, "memoria"), new byte[0], 30, 1024);
        assertTrue(r.iniciou());
        assertFalse(r.expirou());
        IOException erro = assertThrows(IOException.class, () -> r.respostaDe(30));
        assertEquals("import worker ran out of memory", erro.getMessage());
    }

    @Test
    void anAnswerAboveTheLimitIsCut() throws Exception {
        var r = TrabalhadorImportacao.executar(TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.PROCESSO,
                "-Xmx64m", SONDA, "eco"), new byte[100_000], 30, 1000);
        assertTrue(r.excedeu());
        assertEquals(1000, r.dados().length);
        assertThrows(IOException.class, () -> r.respostaDe(30));
    }

    @Test
    void aLevelThatCannotStartIsToldApartFromAFailingFile() throws Exception {
        var ausente = TrabalhadorImportacao.executar(List.of("/nonexistent/java"), new byte[0], 5, 1024);
        assertFalse(ausente.iniciou());
        // A JVM that exits before announcing itself: a level that does not work here.
        var semAnuncio = TrabalhadorImportacao.executar(TrabalhadorImportacao.comando(TrabalhadorImportacao.Nivel.PROCESSO,
                "-Xmx64m", "classe.Inexistente"), new byte[0], 30, 1024);
        assertFalse(semAnuncio.iniciou());
        assertTrue(semAnuncio.erro().contains("classe.Inexistente"), semAnuncio.erro());
    }

    // Whether bubblewrap can create a sandbox on this machine at all, apart from the
    // program's own arguments: a failure of those must fail the test, not skip it.
    private static boolean bwrapFunciona() throws Exception {
        if (!System.getProperty("os.name").toLowerCase().contains("linux") || Files.exists(Path.of("/.flatpak-info"))) return false;
        if (!Files.isExecutable(Path.of("/usr/bin/bwrap"))) return false;
        Process p = new ProcessBuilder("/usr/bin/bwrap", "--unshare-all", "--ro-bind", "/", "/", "/usr/bin/true")
                .redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        return p.waitFor() == 0;
    }

    @Test
    void filesOpenAtTheBestLevelThisMachineOffers() throws Exception {
        TrabalhadorImportacao.abrir(fixture("livre"), false);
        assertEquals(bwrapFunciona() ? TrabalhadorImportacao.Nivel.ISOLADO : TrabalhadorImportacao.Nivel.PROCESSO,
                TrabalhadorImportacao.nivel());
    }

    @Test
    void theIsolatedWorkerReachesNoUserFilesNetworkDisplayOrSessionBus() throws Exception {
        brmodelo.Sandbox.disponivel(bwrapFunciona(), "bubblewrap cannot create a sandbox on this machine");
        // A file of the user's that the worker has no reason to see (its jars and classes are
        // bound read-only, the rest of the project folder is not).
        Path arquivoDoUsuario = Path.of("build.gradle.kts").toRealPath();
        Path bus = Path.of("/run/user", String.valueOf(Files.getAttribute(Path.of(System.getProperty("user.home")), "unix:uid")), "bus");
        try (ServerSocket servidor = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            String[] args = {"isolamento", arquivoDoUsuario.toString(), String.valueOf(servidor.getLocalPort()), bus.toString()};
            // Control: a plain worker reaches all of it, so the probe can tell.
            String livre = new String(TrabalhadorImportacao.executar(TrabalhadorImportacao.comando(
                    TrabalhadorImportacao.Nivel.PROCESSO, "-Xmx64m", SONDA, args), new byte[0], 30, 1024).respostaDe(30), StandardCharsets.UTF_8);
            assertTrue(livre.contains("arquivo: " + Files.size(arquivoDoUsuario) + " bytes"), livre);
            assertTrue(livre.contains("rede: conectou"), livre);
            if (System.getenv("DISPLAY") != null) assertTrue(livre.contains("tela: " + System.getenv("DISPLAY") + " "), livre);
            if (Files.exists(bus)) assertTrue(livre.lines().anyMatch(l -> l.startsWith("dbus: ") && l.endsWith(" true")), livre);
            var r = TrabalhadorImportacao.executar(TrabalhadorImportacao.comando(
                    TrabalhadorImportacao.Nivel.ISOLADO, "-Xmx64m", SONDA, args), new byte[0], 30, 1024);
            assertTrue(r.iniciou(), "bubblewrap works here, so the worker's sandbox must start: " + r.erro());
            String isolado = new String(r.respostaDe(30), StandardCharsets.UTF_8);
            assertTrue(isolado.contains("arquivo: bloqueado (NoSuchFileException)"), isolado);
            assertTrue(isolado.contains("rede: bloqueado"), isolado);
            assertTrue(isolado.contains("tela: null false"), isolado);
            assertTrue(isolado.contains("dbus: null false"), isolado);
        }
    }
}
