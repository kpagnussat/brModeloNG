package brmodelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** packaging/launcher/brmodelo-ng: the Linux packages' entry point, run against a stand-in program. */
@EnabledOnOs(OS.LINUX)
class LancadorLinuxTest {
    private static final Path SCRIPT = Path.of("packaging/launcher/brmodelo-ng");
    // The stand-in reports what the real program would see: the variable, the network
    // interfaces of its namespace (/proc/self/net follows the namespace, /sys does not)
    // and each argument.
    private static final String PROGRAMA = """
            #!/bin/sh
            echo "estado=$BRMODELO_ISOLAMENTO"
            echo "interfaces=$(tail -n +3 /proc/self/net/dev | cut -d: -f1 | tr -d ' ' | LC_ALL=C sort | tr '\\n' ' ')"
            for a in "$@"; do echo "arg=[$a]"; done
            """;
    private static final List<String> ARGS = List.of("diagrama com espaços.brM3", "", "--x=$HOME");

    @TempDir Path temp;
    private Path lancador;

    record Saida(int status, List<String> linhas, String erro) {}

    @BeforeEach void instalar() throws IOException {
        Path bin = Files.createDirectories(temp.resolve("opt/brmodelo-ng/bin"));
        lancador = bin.resolve("brmodelo-ng");
        Files.copy(SCRIPT, lancador);
        executavel(bin.resolve("brmodelo-ng-app"), PROGRAMA);
        lancador.toFile().setExecutable(true);
    }

    private static void executavel(Path p, String conteudo) throws IOException {
        Files.writeString(p, conteudo);
        assertTrue(p.toFile().setExecutable(true));
    }

    private Saida rodar(Path comando, Map<String, String> ambiente) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(comando.toString()));
        cmd.addAll(ARGS);
        // Output goes to files, so a hung launcher meets the timeout instead of a blocking read.
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectOutput(temp.resolve("saida.txt").toFile())
                .redirectError(temp.resolve("erro.txt").toFile());
        pb.environment().remove("BRMODELO_ISOLAMENTO");
        pb.environment().putAll(ambiente);
        Process p = pb.start();
        if (!p.waitFor(20, TimeUnit.SECONDS)) {
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
            fail("the launcher did not finish in 20 s");
        }
        return new Saida(p.exitValue(), Files.readString(temp.resolve("saida.txt"), StandardCharsets.UTF_8).lines().toList(),
                Files.readString(temp.resolve("erro.txt")));
    }

    private static String valor(Saida s, String chave) {
        return s.linhas().stream().filter(l -> l.startsWith(chave + "=")).findFirst().orElseThrow().substring(chave.length() + 1);
    }

    private static List<String> argumentos(Saida s) {
        return s.linhas().stream().filter(l -> l.startsWith("arg=")).map(l -> l.substring(5, l.length() - 1)).toList();
    }

    private static String interfacesDaqui() throws IOException {
        return String.join(" ", Files.readAllLines(Path.of("/proc/self/net/dev")).stream().skip(2)
                .map(l -> l.substring(0, l.indexOf(':')).trim()).sorted().toList()) + " ";
    }

    private static boolean bwrapIsolaRede() throws Exception {
        if (!Files.isExecutable(Path.of("/usr/bin/bwrap"))) return false;
        Process p = new ProcessBuilder("/usr/bin/bwrap", "--unshare-net", "--dev-bind", "/", "/", "/usr/bin/true")
                .redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        return p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
    }

    /** A PATH with the tools the script uses, plus {@code ferramentas} from /usr/bin. */
    private Path caminho(String... ferramentas) throws IOException {
        Path dir = Files.createDirectories(temp.resolve("path"));
        for (String f : List.of("dirname", "readlink", "head", "tail", "cut", "tr", "sort", "cat")) {
            Path alvo = Path.of("/usr/bin", f);
            if (!Files.exists(dir.resolve(f))) Files.createSymbolicLink(dir.resolve(f), alvo);
        }
        for (String f : ferramentas) if (!Files.exists(dir.resolve(f))) Files.createSymbolicLink(dir.resolve(f), Path.of("/usr/bin", f));
        return dir;
    }

    @Test void runsTheProgramWithoutNetworkWhenBubblewrapWorks() throws Exception {
        Sandbox.disponivel(bwrapIsolaRede(), "bubblewrap cannot create a network namespace on this machine");
        Saida s = rodar(lancador, Map.of());
        assertEquals(0, s.status(), s.erro());
        assertEquals("rede", valor(s, "estado"));
        assertEquals("lo ", valor(s, "interfaces"));
        assertEquals(ARGS, argumentos(s));
        assertEquals("", s.erro());
    }

    @Test void followsTheSymbolicLinkPackagesInstallInUsrBin() throws Exception {
        Path usrBin = Files.createDirectories(temp.resolve("usr/bin")).resolve("brmodelo-ng");
        Files.createSymbolicLink(usrBin, lancador);
        Saida s = rodar(usrBin, Map.of("BRMODELO_ISOLAMENTO", "desligado"));
        assertEquals(0, s.status(), s.erro());
        assertEquals(ARGS, argumentos(s));
    }

    @Test void runsAsBeforeAndSaysWhyWhenBubblewrapFails() throws Exception {
        Path path = caminho();
        // A bwrap that fails the way it does where unprivileged user namespaces are restricted.
        executavel(path.resolve("bwrap"), "#!/bin/sh\necho 'bwrap: setting up uid map: Permission denied' >&2\nexit 1\n");
        Saida s = rodar(lancador, Map.of("PATH", path.toString()));
        assertEquals(0, s.status(), s.erro());
        assertEquals("indisponivel: bwrap: setting up uid map: Permission denied", valor(s, "estado"));
        assertEquals(interfacesDaqui(), valor(s, "interfaces"));
        assertEquals(ARGS, argumentos(s));
        assertTrue(s.erro().contains("running without network isolation"), s.erro());
    }

    @Test void runsAsBeforeWithoutBubblewrap() throws Exception {
        Saida s = rodar(lancador, Map.of("PATH", caminho().toString()));
        assertEquals(0, s.status(), s.erro());
        assertEquals("indisponivel: bwrap is not installed", valor(s, "estado"));
        assertEquals(ARGS, argumentos(s));
    }

    @Test void theUserCanTurnIsolationOff() throws Exception {
        Saida s = rodar(lancador, Map.of("BRMODELO_ISOLAMENTO", "desligado"));
        assertEquals(0, s.status(), s.erro());
        assertEquals("indisponivel: disabled by the user", valor(s, "estado"));
        assertEquals(interfacesDaqui(), valor(s, "interfaces"));
    }

    @Test void passesTheProgramsExitStatusOn() throws Exception {
        executavel(lancador.resolveSibling("brmodelo-ng-app"), "#!/bin/sh\nexit 7\n");
        assertEquals(7, rodar(lancador, Map.of()).status());
        assertEquals(7, rodar(lancador, Map.of("BRMODELO_ISOLAMENTO", "desligado")).status());
    }
}
