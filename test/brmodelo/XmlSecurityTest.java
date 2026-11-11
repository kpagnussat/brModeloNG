package brmodelo;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import util.XMLGenerate;
import static org.junit.jupiter.api.Assertions.*;

class XmlSecurityTest {
    private static final String SECRET = "invented-secret-4711";
    private static HttpServer server;
    private static final AtomicInteger requests = new AtomicInteger();
    @TempDir static Path dir;
    private static Path secret;

    @BeforeAll
    static void start() throws IOException {
        secret = Files.writeString(dir.resolve("secret.txt"), SECRET);
        // Serves an external DTD that would send the secret back as a query string.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] body = ("<!ENTITY % d SYSTEM '" + secret.toUri() + "'>"
                    + "<!ENTITY % w \"<!ENTITY &#x25; x SYSTEM 'http://127.0.0.1:"
                    + server.getAddress().getPort() + "/leak?%d;'>\">%w;%x;")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
        });
        server.start();
    }

    @AfterAll
    static void stop() { server.stop(0); }

    static Stream<String> attacks() {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/ext.dtd";
        return Stream.of(
                // Local file pulled into the diagram text.
                "<!DOCTYPE DIAGRAMA [<!ENTITY e SYSTEM '" + secret.toUri() + "'>]><DIAGRAMA>&e;</DIAGRAMA>",
                // External DTD fetched over the network (out-of-band exfiltration).
                "<!DOCTYPE DIAGRAMA [<!ENTITY % p SYSTEM '" + url + "'> %p;]><DIAGRAMA/>",
                "<!DOCTYPE DIAGRAMA SYSTEM '" + url + "'><DIAGRAMA/>",
                // Exponential entity expansion with no external access at all.
                "<!DOCTYPE DIAGRAMA [<!ENTITY a 'aaaaaaaaaa'><!ENTITY b '&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;'>"
                        + "<!ENTITY c '&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;'>]><DIAGRAMA>&c;</DIAGRAMA>",
                // XInclude of a local file.
                "<DIAGRAMA xmlns:xi='http://www.w3.org/2001/XInclude'><xi:include href='"
                        + secret.toUri() + "' parse='text'/></DIAGRAMA>");
    }

    // Guards both entry points: pasted text (String) and opened .xml files (File).
    @ParameterizedTest
    @MethodSource("attacks")
    void refusesDtdsAndIncludes(String xml) throws IOException {
        requests.set(0);
        Path file = Files.writeString(dir.resolve("attack.xml"), xml);
        for (Document doc : new Document[]{XMLGenerate.LoadDocument(xml), XMLGenerate.LoadDocument(file.toFile())}) {
            if (xml.contains("<!DOCTYPE")) {
                assertNull(doc, "brModelo never writes a DOCTYPE, so any DTD is refused");
            } else {
                // XInclude parses, but must stay an inert element, never the file's text.
                assertFalse(doc.getDocumentElement().getTextContent().contains(SECRET));
            }
        }
        assertEquals(0, requests.get(), "No network request may leave the parser");
    }

    // Guards copy/paste and XML export of real diagrams: the hardened parser reads
    // brModelo's own XML exactly as the default parser did before.
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void readsOwnXml(String name) throws Exception {
        String xml = XMLGenerate.GeraXMLFrom(Fixtures.load(name), false);
        Path file = Files.writeString(dir.resolve(name + ".xml"), xml);
        Document legacy = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
        legacy.getDocumentElement().normalize();
        assertTrue(legacy.getElementsByTagName("*").getLength() > 1, "Fixture exports its items");
        for (Document doc : new Document[]{XMLGenerate.LoadDocument(xml), XMLGenerate.LoadDocument(file.toFile())}) {
            assertNotNull(doc);
            assertTrue(legacy.isEqualNode(doc));
        }
    }

    // Guards the size limit on both entry points: input at the limit is read, one unit
    // above is refused before parsing. A ready-made part is pasted through the same path.
    @Test
    void refusesXmlAboveTheSizeLimit() throws Exception {
        int limite = Math.toIntExact(util.LimitesEntrada.XML_BYTES);
        String inicio = "<DIAGRAMA TIPO='";
        String fim = "'/>";
        String noLimite = inicio + "a".repeat(limite - inicio.length() - fim.length()) + fim;
        String acima = inicio + "a".repeat(limite - inicio.length() - fim.length() + 1) + fim;
        assertEquals(limite, noLimite.length());

        Path arquivo = dir.resolve("limite.xml");
        Files.writeString(arquivo, noLimite);
        assertNotNull(XMLGenerate.LoadDocument(noLimite));
        assertNotNull(XMLGenerate.LoadDocument(arquivo.toFile()));

        Files.writeString(arquivo, acima);
        assertNull(XMLGenerate.LoadDocument(acima));
        assertNull(XMLGenerate.LoadDocument(arquivo.toFile()));
        assertFalse(Fixtures.load("conceitual").doPaste(acima), "Ready-made part above the limit is not pasted");
    }
}
