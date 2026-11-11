package brmodelo;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards that opening a received diagram makes no network request through Swing's HTML
 * rendering or library URL loading. Each layer is checked on its own in a fresh JVM,
 * against a control run that proves every probe really reaches the network.
 */
class HtmlInertTest {
    private static final Set<String> COMPONENTES = Set.of("label", "arvore", "combo", "checkbox", "parte",
            "recente", "dialogo", "aba");
    private static HttpServer server;
    private static final Set<String> pedidos = new TreeSet<>();

    @BeforeAll
    static void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            synchronized (pedidos) { pedidos.add(exchange.getRequestURI().getPath().substring(1)); }
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterAll
    static void stop() { server.stop(0); }

    private static Set<String> rodar(String modo) throws Exception {
        synchronized (pedidos) { pedidos.clear(); }
        var r = JvmFilha.rodar(60, List.of(), SondaHtml.class, modo, String.valueOf(server.getAddress().getPort()));
        assertTrue(r.texto().contains("fim"), r.texto());
        synchronized (pedidos) { return new TreeSet<>(pedidos); }
    }

    @Test
    void controlReachesTheNetworkFromEveryProbe() throws Exception {
        Set<String> esperado = new TreeSet<>(COMPONENTES);
        esperado.addAll(Set.of("xxe", "imageio"));
        assertEquals(esperado, rodar("nada"));
    }

    @Test
    void diagramTextIsNeverRenderedAsHtml() throws Exception {
        // Only the component that opted in for the program's own HTML may load its image.
        assertEquals(Set.of("permitido"), rodar("html"));
    }

    @Test
    void libraryUrlLoadingIsRefused() throws Exception {
        assertEquals(Set.of(), rodar("rede"));
    }
}
