package util;

import controlador.Diagrama;
import controlador.apoios.GuardaPadraoBrM;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Diagram files (.brM3, .brMj) are decoded in a separate JVM with no display, a bounded heap
 * and a deadline; on Linux it also runs without network, home folder or session bus. The
 * worker reads the file from its standard input and answers with the same diagram as .brMj,
 * which this process decodes with its own input limits. The interface therefore never runs
 * Java deserialization on a file received from someone else, and a file built to exhaust
 * memory or CPU ends the worker instead of the program.
 *
 * When a level cannot start, opening falls back to the next one and logs it: isolated
 * worker, plain worker, then decoding in this process. A file that makes a started worker
 * fail is refused; it never falls back.
 */
public final class TrabalhadorImportacao {
    private TrabalhadorImportacao() {}

    /** Re-encoding the largest real diagram (2,125 shapes) needs 256 MB; three times that. */
    static final String MEMORIA = "-Xmx768m";
    static final long PRAZO_SEGUNDOS = 30;
    // Written before the worker reads its input, so it only proves the sandbox and the JVM
    // started: its absence means the level is unavailable, never that the file failed.
    static final byte[] PRONTO = "brModelo-trabalhador 1\n".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_ERRO = 4096;
    // HotSpot's exit status under -XX:+ExitOnOutOfMemoryError, whichever thread ran out.
    private static final int SEM_MEMORIA = 3;
    // A per-run JVM argument that finds the worker among all processes: under Flatpak it is
    // started by the portal, not as a child of this process.
    static final String MARCA = "-Dbrmodelo.trabalhador=";

    enum Nivel { ISOLADO, PROCESSO, LOCAL }

    private static Nivel nivel;

    /** The level files are opened at, once one has been tried. */
    static synchronized Nivel nivel() { return nivel; }

    /** Opens a .brM3 (or .brMj when {@code brMj}) file already read into memory. */
    public static synchronized Diagrama abrir(byte[] arquivo, boolean brMj) throws IOException {
        if (nivel == null) {
            nivel = melhorNivel();
            if (nivel == Nivel.LOCAL) BrLogger.Logger("WARN_IMPORT_WORKER_UNAVAILABLE", "LOCAL: no " + java());
        }
        while (nivel != Nivel.LOCAL) {
            Resultado r = executar(comando(nivel, MEMORIA, TrabalhadorImportacao.class.getName(), brMj ? "brMj" : "brM3",
                    Long.toString(PRAZO_SEGUNDOS)), arquivo, PRAZO_SEGUNDOS, LimitesEntrada.JSON_BYTES);
            if (r.iniciou()) return FormatoBrMj.ler(r.respostaDe(PRAZO_SEGUNDOS)).diagrama();
            BrLogger.Logger("WARN_IMPORT_WORKER_UNAVAILABLE", nivel + ": " + r.erro());
            nivel = Nivel.values()[nivel.ordinal() + 1];
        }
        return decodificar(arquivo, brMj).diagrama();
    }

    /** Worker side: args are the format ("brM3" or "brMj") and the deadline in seconds. */
    public static void main(String[] args) {
        RedeDesligada.instalar(); // a plain worker still has the network
        long prazo = Long.parseLong(args[1]);
        Thread relogio = new Thread(() -> {
            try { Thread.sleep(TimeUnit.SECONDS.toMillis(prazo)); } catch (InterruptedException e) { return; }
            Runtime.getRuntime().halt(4); // in case the parent cannot kill this process
        });
        relogio.setDaemon(true);
        relogio.start();
        try {
            System.out.write(PRONTO);
            System.out.flush();
            FormatoBrMj.Documento doc = decodificar(System.in.readAllBytes(), "brMj".equals(args[0]));
            System.out.write(FormatoBrMj.escrever(doc.diagrama(), doc.envelope()));
            System.out.flush();
            System.exit(0);
        } catch (Throwable e) {
            System.err.println(e);
            System.exit(2);
        }
    }

    private static FormatoBrMj.Documento decodificar(byte[] arquivo, boolean brMj) throws IOException {
        if (brMj) return FormatoBrMj.ler(arquivo);
        try (ObjectInputStream in = new LeitorSeguro(new ByteArrayInputStream(arquivo))) {
            GuardaPadraoBrM envelope = (GuardaPadraoBrM) in.readObject();
            Diagrama diagrama = envelope.getDiagrama();
            if (diagrama == null) throw new IOException("Invalid diagram data");
            return new FormatoBrMj.Documento(diagrama, envelope);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException(e.toString(), e);
        }
    }

    private static Nivel melhorNivel() {
        if (!Files.isExecutable(java())) return Nivel.LOCAL;
        if (Files.exists(Path.of("/.flatpak-info")) ? programa("flatpak-spawn") != null
                : System.getProperty("os.name", "").toLowerCase().contains("linux") && programa("bwrap") != null) {
            return Nivel.ISOLADO;
        }
        return Nivel.PROCESSO;
    }

    private static Path java() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().startsWith("windows");
        Path bin = Path.of(System.getProperty("java.home"), "bin");
        if (!windows) return bin.resolve("java");
        // javaw: a console program started from the window would open a console window.
        return Files.isExecutable(bin.resolve("javaw.exe")) ? bin.resolve("javaw.exe") : bin.resolve("java.exe");
    }

    private static String programa(String nome) {
        for (String dir : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
            if (!dir.isEmpty() && Files.isExecutable(Path.of(dir, nome))) return Path.of(dir, nome).toString();
        }
        return null;
    }

    /** The command that runs {@code classe} at the given level, with the program's classpath. */
    static List<String> comando(Nivel nivel, String memoria, String classe, String... args) throws IOException {
        return comando(nivel, Files.exists(Path.of("/.flatpak-info")), memoria, classe, args);
    }

    static List<String> comando(Nivel nivel, boolean flatpak, String memoria, String classe, String... args)
            throws IOException {
        List<String> classpath = new ArrayList<>();
        for (String entrada : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entrada.isEmpty() && Files.exists(Path.of(entrada))) classpath.add(Path.of(entrada).toRealPath().toString());
        }
        Path jvm = java();
        List<String> res = new ArrayList<>();
        if (nivel == Nivel.ISOLADO && flatpak) {
            // A new sandbox of this app: /app and the runtime only, no network or display.
            res.addAll(List.of(Objects.requireNonNullElse(programa("flatpak-spawn"), "flatpak-spawn"), "--sandbox",
                    "--no-network", "--watch-bus", "--clear-env", "--directory=/"));
            // The portal sends signals to its bwrap, which does not pass them on; with the
            // worker's pid namespace nested in this one, this process can kill the worker
            // itself, while the worker sees none of this sandbox's processes.
            if (FlatpakPortal.expoePids()) res.add(1, "--expose-pids");
            // The portal starts the sandbox with the session's environment even under
            // --clear-env (Flatpak 1.18), so the worker's JVM starts from an empty one.
            res.addAll(List.of("/usr/bin/env", "-i"));
        } else if (nivel == Nivel.ISOLADO) {
            jvm = jvm.toRealPath();
            res.addAll(List.of(programa("bwrap"), "--unshare-all", "--new-session", "--die-with-parent", "--clearenv",
                    "--proc", "/proc", "--dev", "/dev", "--tmpfs", "/tmp", "--chdir", "/"));
            // Then the system's programs and libraries, the JVM and the program's jars, read-only
            // (after the /tmp mount, which would hide a jar run from /tmp); nothing else of the
            // file system: no home, no X11 or session bus socket.
            for (String sistema : List.of("/usr", "/lib", "/lib64", "/bin", "/sbin")) {
                Path p = Path.of(sistema);
                if (Files.isSymbolicLink(p)) res.addAll(List.of("--symlink", Files.readSymbolicLink(p).toString(), sistema));
                else if (Files.isDirectory(p)) res.addAll(List.of("--ro-bind", sistema, sistema));
            }
            Path javaHome = Path.of(System.getProperty("java.home")).toRealPath();
            res.addAll(List.of("--ro-bind", javaHome.toString(), javaHome.toString()));
            for (String entrada : classpath) res.addAll(List.of("--ro-bind", entrada, entrada));
        }
        res.addAll(List.of(jvm.toString(), MARCA + UUID.randomUUID(), "-Djava.awt.headless=true", memoria, "-XX:+UseSerialGC",
                "-XX:+ExitOnOutOfMemoryError", "-XX:-UsePerfData", "-cp", String.join(File.pathSeparator, classpath), classe));
        res.addAll(Arrays.asList(args));
        return res;
    }

    record Resultado(boolean iniciou, boolean expirou, boolean excedeu, int saida, byte[] dados, String erro) {
        /** The worker's answer, or why the file was refused. */
        byte[] respostaDe(long prazo) throws IOException {
            if (expirou) throw new IOException("import worker exceeded " + prazo + " s");
            if (excedeu) throw new IOException("import worker answer exceeds " + LimitesEntrada.JSON_BYTES + " bytes");
            if (saida == SEM_MEMORIA) throw new IOException("import worker ran out of memory");
            if (saida != 0) throw new IOException("import worker: " + (erro.isBlank() ? "exit " + saida : erro.strip()));
            return dados;
        }
    }

    /** Runs a command with {@code entrada} on its standard input, killing it at the deadline. */
    static Resultado executar(List<String> comando, byte[] entrada, long prazoSegundos, long maxResposta)
            throws IOException {
        String marca = comando.stream().filter(a -> a.startsWith(MARCA)).findFirst().orElse(null);
        Process processo;
        try {
            processo = new ProcessBuilder(comando).start();
        } catch (IOException e) {
            return new Resultado(false, false, false, -1, null, e.getMessage());
        }
        Thread escritor = new Thread(() -> {
            try (OutputStream o = processo.getOutputStream()) { o.write(entrada); }
            catch (IOException ignored) { } // the worker refused early or died: its exit tells why
        });
        ByteArrayOutputStream erro = new ByteArrayOutputStream();
        Thread leitorErro = new Thread(() -> copiar(processo.getErrorStream(), erro, MAX_ERRO, null));
        ByteArrayOutputStream dados = new ByteArrayOutputStream();
        AtomicBoolean iniciou = new AtomicBoolean(), excedeu = new AtomicBoolean();
        Thread leitor = new Thread(() -> {
            InputStream saida = processo.getInputStream();
            try { iniciou.set(Arrays.equals(PRONTO, saida.readNBytes(PRONTO.length))); }
            catch (IOException ignored) { } // killed before announcing itself
            // The marker is read apart so the answer is held once; a process that did not
            // announce itself is drained, not kept.
            copiar(saida, dados, iniciou.get() ? maxResposta : 0, !iniciou.get() ? null : () -> {
                excedeu.set(true);
                encerrar(processo, marca);
            });
        });
        for (Thread t : List.of(escritor, leitorErro, leitor)) { t.setDaemon(true); t.start(); }
        boolean terminou;
        try {
            terminou = processo.waitFor(prazoSegundos, TimeUnit.SECONDS);
            if (!terminou) {
                encerrar(processo, marca);
                processo.waitFor(2, TimeUnit.SECONDS);
            }
            leitor.join(TimeUnit.SECONDS.toMillis(5));
            leitorErro.join(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
            encerrar(processo, marca);
            Thread.currentThread().interrupt();
            throw new IOException("import interrupted", e);
        }
        String textoErro = erro.toString(StandardCharsets.UTF_8);
        if (!iniciou.get()) return new Resultado(false, !terminou, false, terminou ? processo.exitValue() : -1, null,
                textoErro.isBlank() ? (terminou ? "exit " + processo.exitValue() : "no answer in " + prazoSegundos + " s") : textoErro.strip());
        return new Resultado(true, !terminou, excedeu.get(), terminou ? processo.exitValue() : -1,
                dados.toByteArray(), textoErro);
    }

    /**
     * Kills the worker wherever it runs: its descendants of this process (bwrap, a plain JVM)
     * and any process carrying its {@link #MARCA} argument (under Flatpak, where flatpak-spawn
     * cannot pass SIGKILL on and the portal's SIGTERM stops at its bwrap). The worker's own
     * clock remains as the last resort.
     */
    static void encerrar(Process processo, String marca) {
        processo.descendants().forEach(ProcessHandle::destroyForcibly);
        if (marca != null) {
            ProcessHandle.allProcesses()
                    .filter(h -> h.info().arguments().map(a -> Arrays.asList(a).contains(marca)).orElse(false))
                    .forEach(ProcessHandle::destroyForcibly);
        }
        processo.destroyForcibly();
    }

    private static void copiar(InputStream de, ByteArrayOutputStream para, long maximo, Runnable aoExceder) {
        byte[] buffer = new byte[1 << 16];
        try (de) {
            for (int n; (n = de.read(buffer)) > 0; ) {
                int cabe = (int) Math.max(0, Math.min(n, maximo - para.size()));
                para.write(buffer, 0, cabe);
                if (cabe < n && aoExceder != null) { aoExceder.run(); return; }
            }
        } catch (IOException ignored) {
            // The process was killed; what was read so far is all there is.
        }
    }
}
