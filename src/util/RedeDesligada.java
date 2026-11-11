package util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Set;

/**
 * The program needs no network: links open in the system browser, a separate process.
 * Libraries can still fetch URLs on their own (an XML parser resolving an external entity,
 * Swing loading an <img> in HTML text, ImageIO reading a URL), so http, https and ftp URLs
 * refuse to connect for the whole process. A proxy setting would not be enough: the JDK's
 * HTTP client falls back to a direct connection when every proxy fails. This blocks those
 * library paths, not code that opens sockets itself; isolation for that comes from the
 * operating system (Flatpak, launcher sandbox).
 */
public final class RedeDesligada {
    private RedeDesligada() {}

    private static final Set<String> BLOQUEADOS = Set.of("http", "https", "ftp");
    private static boolean instalada;

    /** Runs before anything creates a URL for these protocols; the JVM accepts one factory. */
    public static synchronized void instalar() {
        if (instalada) return;
        try {
            URL.setURLStreamHandlerFactory(protocolo -> BLOQUEADOS.contains(protocolo) ? new Recusa() : null);
            instalada = true;
        } catch (Error e) {
            // Another factory is already set (an embedding host); keep running without this layer.
            BrLogger.Logger("ERROR_NETWORK_BLOCK", e.getMessage());
        }
    }

    private static final class Recusa extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            throw new IOException("brModelo NG does not access the network: " + u.getProtocol() + "://" + u.getHost());
        }

        // URL.equals/hashCode would otherwise resolve the host name, a DNS request.
        @Override
        protected InetAddress getHostAddress(URL u) {
            return null;
        }
    }
}
