package brmodelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a main class in a separate JVM with the test classpath. For process-wide settings
 * that cannot be undone (the URL handler factory, UI defaults) and for inputs meant to
 * exhaust memory or CPU, so a failure cannot take down the Gradle test worker.
 */
final class JvmFilha {
    private JvmFilha() {}

    record Resultado(int saida, String texto, boolean expirou) {}

    static Resultado rodar(long segundos, List<String> opcoes, Class<?> principal, String... args)
            throws IOException, InterruptedException {
        List<String> comando = new ArrayList<>();
        comando.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        comando.add("-Djava.awt.headless=true");
        comando.add("-Duser.home=" + System.getProperty("user.home"));
        comando.addAll(opcoes);
        comando.add("-cp");
        comando.add(System.getProperty("java.class.path"));
        comando.add(principal.getName());
        comando.addAll(List.of(args));
        Process processo = new ProcessBuilder(comando).redirectErrorStream(true).start();
        processo.getOutputStream().close();
        // Read concurrently: a child that fills the pipe would otherwise block forever.
        StringBuilder saida = new StringBuilder();
        Thread leitor = new Thread(() -> {
            try { saida.append(new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8)); }
            catch (IOException ignored) { }
        });
        leitor.start();
        boolean terminou = processo.waitFor(segundos, TimeUnit.SECONDS);
        if (!terminou) processo.destroyForcibly().waitFor();
        leitor.join();
        return new Resultado(terminou ? processo.exitValue() : -1, saida.toString(), !terminou);
    }
}
