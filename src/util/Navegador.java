package util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.Variant;

/**
 * The single way the program opens a web link or its local help in the user's browser.
 *
 * <p>The Linux packages start the program inside a network namespace of its own (see
 * packaging/linux/brmodelo-ng, which sets {@value #VARIAVEL}={@value #ISOLADO}). A browser
 * started from there as a child process would inherit that namespace and stay offline for
 * the rest of the session, so in that case the browser is started by the desktop instead:
 * web links through the XDG OpenURI portal (it refuses file: URIs), and the help page as a
 * transient service of the user's systemd manager. Only when both are missing does the
 * browser start from here, and the log says it has no network.
 */
public final class Navegador {
    private Navegador() {}

    /** Set by the Linux launcher: {@value #ISOLADO}, or "indisponivel: reason" when it could not isolate. */
    public static final String VARIAVEL = "BRMODELO_ISOLAMENTO";
    public static final String ISOLADO = "rede";
    private static final long ESPERA_SEGUNDOS = 15;

    enum Via { PORTAL, SERVICO_USUARIO, DESKTOP, XDG_OPEN }

    @DBusInterfaceName("org.freedesktop.portal.OpenURI")
    public interface OpenURI extends DBusInterface {
        DBusPath OpenURI(String parent, String uri, Map<String, Variant<?>> options);
    }

    static boolean linux() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux"); }
    static boolean flatpak() { return System.getenv("FLATPAK_ID") != null || Files.exists(Path.of("/.flatpak-info")); }
    static boolean isolado() { return ISOLADO.equals(System.getenv(VARIAVEL)); }

    /** The ways to try, in order; the first that succeeds opens the link. */
    static List<Via> vias(URI uri, boolean linux, boolean flatpak, boolean isolado) {
        boolean web = "https".equalsIgnoreCase(uri.getScheme());
        // Inside Flatpak, xdg-open is the portal client: it passes a local file as a descriptor.
        if (flatpak) return web ? List.of(Via.DESKTOP, Via.XDG_OPEN) : List.of(Via.XDG_OPEN);
        if (!linux) return List.of(Via.DESKTOP);
        if (!isolado) return List.of(Via.DESKTOP, Via.XDG_OPEN);
        return web ? List.of(Via.PORTAL, Via.SERVICO_USUARIO, Via.DESKTOP, Via.XDG_OPEN)
                : List.of(Via.SERVICO_USUARIO, Via.DESKTOP, Via.XDG_OPEN);
    }

    /** Flatpak's xdg-open exports a single local file by its path; elsewhere it takes the URI. */
    static List<String> comandoXdgOpen(URI uri, boolean flatpak) {
        return List.of("xdg-open", flatpak && "file".equals(uri.getScheme()) ? Path.of(uri).toString() : uri.toASCIIString());
    }

    /**
     * xdg-open as a transient unit of the user's service manager, outside this namespace.
     * --wait with Type=exec returns xdg-open's own exit status, so a failure falls through
     * to the next way; systemd turns "$$" into "$" in the command, so dollars are doubled.
     */
    static List<String> comandoServicoUsuario(URI uri, Map<String, String> ambiente) {
        List<String> res = new ArrayList<>(List.of("systemd-run", "--user", "--quiet", "--collect", "--wait",
                "--property=Type=exec"));
        // The manager usually has the session's display variables; pass ours in case it does not.
        for (String nome : List.of("DISPLAY", "WAYLAND_DISPLAY", "XAUTHORITY", "XDG_CURRENT_DESKTOP")) {
            String valor = ambiente.get(nome);
            if (valor != null && !valor.isEmpty()) res.add("--setenv=" + nome + "=" + valor);
        }
        res.addAll(List.of("--", "xdg-open", uri.toASCIIString().replace("$", "$$")));
        return res;
    }

    /** Logs, once at startup, that the Linux launcher ran the program with the network. */
    public static void registrarIsolamento() {
        String estado = System.getenv(VARIAVEL);
        if (estado != null && !estado.equals(ISOLADO)) BrLogger.Logger("WARN_NETWORK_ISOLATION_UNAVAILABLE", estado);
    }

    /** Opens an https or file URI; blocks while launching, so callers stay off the EDT. */
    public static void abrir(URI uri) throws IOException {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("file")) throw new IOException("Not opened: " + uri);
        boolean isolado = isolado();
        IOException falha = null;
        boolean avisado = false;
        for (Via via : vias(uri, linux(), flatpak(), isolado)) {
            if (isolado && !avisado && (via == Via.DESKTOP || via == Via.XDG_OPEN)) {
                // A browser not yet running would start inside the namespace, offline.
                BrLogger.Logger("WARN_BROWSER_WITHOUT_NETWORK", falha == null ? null : falha.getMessage());
                avisado = true;
            }
            try {
                if (tentar(via, uri)) return;
            } catch (IOException | RuntimeException e) {
                falha = new IOException(via + ": " + e.getMessage(), e);
            }
        }
        throw falha != null ? falha : new IOException("No browser available for " + uri);
    }

    /** Opens the link on a background thread and logs a failure; for clicks on the EDT. */
    public static void abrirDepois(URI uri) {
        Thread t = new Thread(() -> {
            try { abrir(uri); }
            catch (IOException e) { BrLogger.Logger("ERROR_BROWSER_OPEN", e.getMessage()); }
        }, "brModelo-navegador");
        t.setDaemon(true);
        t.start();
    }

    private static boolean tentar(Via via, URI uri) throws IOException {
        switch (via) {
            case PORTAL -> {
                try (var bus = DBusConnectionBuilder.forSessionBus().withShared(false).build()) {
                    OpenURI portal = bus.getRemoteObject(PortalDeArquivos.DESTINO, PortalDeArquivos.CAMINHO, OpenURI.class);
                    String token = PortalDeArquivos.token();
                    // With a default browser the link opens at once; otherwise the desktop asks
                    // which application to use, and the connection stays open until it answers.
                    var resposta = PortalDeArquivos.requisitar(bus, token,
                            () -> portal.OpenURI("", uri.toASCIIString(), Map.of("handle_token", new Variant<>(token))));
                    if (resposta == null) throw new IOException("the OpenURI portal went away");
                    int codigo = resposta.response.intValue();
                    if (codigo == 2) throw new IOException("the OpenURI portal could not open the link");
                    return true; // 0 opened, 1 the user dismissed the application chooser
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
            case SERVICO_USUARIO -> {
                // Some xdg-open fallbacks keep the browser in the foreground: still running means opened.
                return executar(comandoServicoUsuario(uri, System.getenv()), true);
            }
            case DESKTOP -> {
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) return false;
                Desktop.getDesktop().browse(uri);
                return true;
            }
            case XDG_OPEN -> { return executar(comandoXdgOpen(uri, flatpak()), false); }
        }
        return false;
    }

    private static boolean executar(List<String> comando, boolean demoraAbriu) throws IOException {
        Process process = new ProcessBuilder(comando).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!process.waitFor(ESPERA_SEGUNDOS, TimeUnit.SECONDS)) {
                if (demoraAbriu) return true;
                process.destroy();
                throw new IOException(comando.getFirst() + " did not answer");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
        if (process.exitValue() != 0) throw new IOException(comando.getFirst() + " exited with " + process.exitValue());
        return true;
    }
}
