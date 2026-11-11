package helper;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BundledHelpTest {
    @TempDir Path temporary;

    @Test void packagedSiteResolvesWithoutExternalFilesAndRepairsItsCache() throws Exception {
        Path distribution = Files.createDirectory(temporary.resolve("distribution"));
        Path jar = Files.copy(Path.of(System.getProperty("bundledHelp.jar")), distribution.resolve("brModelo.jar"));
        Path state = temporary.resolve("state with spaces and acentuação");
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> opener = loader.loadClass("util.AjudaNavegador");
            var resolve = opener.getMethod("resolverIndice", ClassLoader.class, Path.class);
            Path index = (Path)resolve.invoke(null, loader, state);
            assertTrue(index.startsWith(state));
            assertTrue(Files.readString(index).contains("Bem-vindo"));
            assertEquals(index.toUri(), opener.getMethod("endereco", Path.class, boolean.class).invoke(null, index, false));
            Path css = index.resolveSibling("ajuda.css");
            assertTrue(Files.readString(css).contains("prefers-color-scheme"));
            Files.writeString(css, "corrupt cache");
            assertEquals(index, resolve.invoke(null, loader, state));
            assertTrue(Files.readString(css).contains("prefers-color-scheme"));
            assertTrue(Files.isRegularFile(index.getParent().resolve("img/atributos.png")));
            try (var files = Files.list(distribution)) {
                assertEquals(1, files.count(), "Only the jar is required");
            }
            for (String removed : new String[]{"FormHelp", "AjudaManager", "ParteAjuda", "LabelNav"})
                assertThrows(ClassNotFoundException.class, () -> loader.loadClass("helper." + removed));
            assertNotNull(loader.loadClass("partepronta.GerenciadorSubParte"));
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            assertNull(zip.getEntry("Ajuda.brMh"));
            assertNotNull(zip.getEntry("ajuda/index.html"));
            assertNotNull(zip.getEntry("ajuda/ajuda-completa.html"));
            assertTrue(zip.stream().noneMatch(e -> e.getName().startsWith("org/commonmark/")), "Markdown compiler is build only");
            for (var entry : zip.stream().filter(e -> e.getName().endsWith(".class")).toList()) {
                try (var in = zip.getInputStream(entry)) {
                    assertFalse(new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).contains("com/sun/net/httpserver/HttpServer"), entry.getName());
                }
            }
        }
    }

    @Test void flatpakExtractsOnlyOneFileAndPassesItsPathToThePortalLauncher() throws Exception {
        Path state = temporary.resolve("portal with spaces and acentuação");
        Path jar = Path.of(System.getProperty("bundledHelp.jar"));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class<?> opener = loader.loadClass("util.AjudaNavegador");
            var resolve = opener.getMethod("resolverIndice", ClassLoader.class, Path.class, boolean.class);
            var address = opener.getMethod("endereco", Path.class, boolean.class);
            Path index = (Path)resolve.invoke(null, loader, state, true);
            assertTrue(index.startsWith(state));
            assertEquals("ajuda-completa.html", index.getFileName().toString());
            var uri = (java.net.URI)address.invoke(null, index, true);
            assertEquals("file", uri.getScheme());
            assertNull(uri.getHost());
            assertEquals(index.toUri(), uri);
            assertEquals(uri, address.invoke(null, index.resolveSibling("index.html"), true));
            var command = loader.loadClass("util.Navegador").getDeclaredMethod("comandoXdgOpen", java.net.URI.class, boolean.class);
            command.setAccessible(true);
            assertEquals(java.util.List.of("xdg-open", index.toString()), command.invoke(null, uri, true));
            assertEquals(java.util.List.of("xdg-open", uri.toASCIIString()), command.invoke(null, uri, false));
            byte[] expected;
            try (var in = loader.getResourceAsStream("ajuda/ajuda-completa.html")) {
                assertNotNull(in);
                expected = in.readAllBytes();
            }
            assertArrayEquals(expected, Files.readAllBytes(index));
            try (var files = Files.walk(state)) {
                assertEquals(java.util.List.of(index), files.filter(Files::isRegularFile).toList(), "One file is sufficient for the host browser");
            }
            Files.writeString(index, "corrupt cache");
            assertEquals(index, resolve.invoke(null, loader, state, true));
            assertArrayEquals(expected, Files.readAllBytes(index));
        }
    }

    @Test void flatpakManifestDoesNotGrantNetworkAccess() throws Exception {
        assertFalse(Files.readString(Path.of(System.getProperty("help.flatpakManifest"))).contains("--share=network"));
    }

    @Test void creditsUsePeopleLinksAndSeparateProjectLinks() {
        String html = RepositorioHtml.generateSobre();
        assertTrue(html.contains("brModelo NG " + util.InformacoesAplicacao.versao()));
        assertTrue(html.contains("href='https://github.com/kpagnussat'>Kristofer Pagnussat"));
        assertTrue(html.contains("href='https://github.com/chcandido'>Carlos Henrique Cândido"));
        assertTrue(html.contains("href='https://www.inf.ufsc.br/~r.mello/'>Dr. Ronaldo dos Santos Mello"));
        assertTrue(html.contains("href='https://github.com/kpagnussat/brModeloNG'>brModelo NG"));
        assertTrue(html.contains("href='https://github.com/chcandido/brModelo'>brModelo original"));
        assertTrue(html.contains("GPL-3.0"));
        assertFalse(html.contains("sis4"));
        assertFalse(html.contains("3.32"));
        assertFalse(html.contains("file:"));
    }
}
