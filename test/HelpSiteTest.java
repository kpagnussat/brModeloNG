import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HelpSiteTest {
    @TempDir Path temporary;
    private Map<String, byte[]> files(Path root) throws Exception {
        Map<String, byte[]> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) result.put(root.relativize(file).toString(), Files.readAllBytes(file));
        }
        return result;
    }
    @Test void buildsByteIdenticalSitesAndEveryLinkAndImageResolves() throws Exception {
        Path sources = Path.of(System.getProperty("help.sources"));
        Path first = temporary.resolve("one"), second = temporary.resolve("two");
        HelpSite.build(sources, first);
        HelpSite.build(sources, second);
        Map<String, byte[]> a = files(first), b = files(second);
        assertEquals(a.keySet(), b.keySet());
        for (String file : a.keySet()) assertArrayEquals(a.get(file), b.get(file), file);
        assertEquals(12, a.keySet().stream().filter(f -> f.endsWith(".html")).count());
        HelpSite.validate(first);
        String conceptual = Files.readString(first.resolve("modelo-conceitual.html"));
        assertTrue(conceptual.contains("id=\"entidade-fraca\""));
        assertTrue(conceptual.contains("rel=\"prev\""));
        assertTrue(conceptual.contains("rel=\"next\""));
        assertFalse(conceptual.contains("href=\"modelo-logico.md"));
    }
    @Test void singleFileContainsEveryTopicInOrderWithOnlyAnchorsAndEmbeddedImages() throws Exception {
        Path sources = Path.of(System.getProperty("help.sources"));
        Path site = temporary.resolve("single");
        HelpSite.build(sources, site);
        Path single = site.resolve(HelpSite.SINGLE_FILE);
        HelpSite.validateSingleFile(single);
        String html = Files.readString(single);
        assertTrue(html.contains("<style>" + Files.readString(sources.resolve("ajuda.css")) + "</style>"));
        assertTrue(html.contains("prefers-color-scheme:dark"));
        assertFalse(Pattern.compile("(?i)file:|<link\\b|<script\\b|@import|url\\s*\\(").matcher(html).find());
        // Web addresses appear only as clickable link targets, never as something the page loads.
        assertEquals(html.split("(?i)https?:", -1).length - 1, html.split("href=\"https://", -1).length - 1);
        assertTrue(html.contains("href=\"https://projbd.heuser.pro.br/\""));
        assertTrue(html.contains("href=\"https://github.com/kpagnussat/brModeloNG/releases\""));
        List<String> order = new ArrayList<>(List.of("index"));
        var topicLinks = Pattern.compile("\\]\\(([a-z-]+)\\.md\\)").matcher(Files.readString(sources.resolve("index.md")));
        while (topicLinks.find()) order.add(topicLinks.group(1));
        int position = -1;
        int imageCount = 0;
        for (String topic : order) {
            String markdown = Files.readString(sources.resolve(topic + ".md"));
            String title = markdown.lines().findFirst().orElseThrow().substring(2);
            int section = html.indexOf("<section id=\"topico-" + topic + "\">");
            assertTrue(section > position, topic);
            position = section;
            String sectionHtml = html.substring(section, html.indexOf("</section>", section));
            assertTrue(sectionHtml.contains(">" + title + "</h1>"), title);
            String article = sectionHtml.substring(sectionHtml.indexOf("<article>") + 9, sectionHtml.indexOf("</article>"));
            String page = Files.readString(site.resolve(topic + ".html"));
            String original = page.substring(page.indexOf("<article>") + 9, page.indexOf("</article>"));
            assertEquals(content(original), content(article), "All topic content is preserved: " + topic);
            var sourceImages = Pattern.compile("!\\[[^]]*]\\((img/[^)]+)\\)").matcher(markdown);
            while (sourceImages.find()) {
                String data = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(sources.resolve(sourceImages.group(1))));
                assertTrue(html.contains("src=\"" + data + "\""), sourceImages.group(1));
                imageCount++;
            }
        }
        assertTrue(imageCount > 0);
        assertEquals(imageCount, Pattern.compile("<img\\b").matcher(html).results().count());
        Set<String> ids = new HashSet<>();
        var identifiers = Pattern.compile("\\bid=\"([^\"]+)\"").matcher(html);
        while (identifiers.find()) assertTrue(ids.add(identifiers.group(1)), "Unique anchors");
        var links = Pattern.compile("href=\"([^\"]+)\"").matcher(html);
        while (links.find()) {
            String href = links.group(1);
            if (href.startsWith("https://")) continue; // web links stay clickable, nothing is loaded
            assertTrue(href.startsWith("#"), href);
            assertTrue(ids.contains(java.net.URI.create(href).getFragment()), href);
        }
        assertTrue(html.contains("href=\"#primeiros-passos--onde-ficam-as-preferências\""));
        assertTrue(html.contains("Kristofer Pagnussat"));
        assertTrue(html.contains("site do autor e do livro"));
        // The existing site still retains the bibliography and credits' external destinations.
        assertTrue(Files.readString(site.resolve("index.html")).contains("https://projbd.heuser.pro.br/"));
    }
    private static String content(String article) {
        // Only destinations and heading IDs differ between editions; retain text, alt text and markup.
        return article.replaceAll("</?a(?:\\s[^>]*)?>", "")
                .replaceAll("\\s(?:id|src)=\"[^\"]*\"", "");
    }
    @Test void singleFileNamespacesRepeatedHeadingsAndTopicLocalLinks() throws Exception {
        Path sources = temporary.resolve("sources"); Files.createDirectories(sources.resolve("img"));
        Files.writeString(sources.resolve("ajuda.css"), "body { color:black; }");
        Files.writeString(sources.resolve("index.md"), "# Início\n\n[Outro](outro.md)\n\n## Mesmo\n\n[Local](#mesmo)\n");
        Files.writeString(sources.resolve("outro.md"), "# Outro\n\n## Mesmo\n\n## Mesmo\n\n[Local](#mesmo-1)\n\n[Cruzado](index.md#mesmo)\n");
        Path site = temporary.resolve("duplicates"); HelpSite.build(sources, site);
        String html = Files.readString(site.resolve(HelpSite.SINGLE_FILE));
        assertTrue(html.contains("id=\"index--mesmo\""));
        assertTrue(html.contains("id=\"outro--mesmo\""));
        assertTrue(html.contains("id=\"outro--mesmo-1\""));
        assertTrue(html.contains("href=\"#outro--mesmo-1\""));
        assertTrue(html.contains("href=\"#index--mesmo\""));
    }
    @Test void singleFileValidatorRejectsExternalAssetsLinksCssAndBrokenOrDuplicateAnchors() throws Exception {
        Path single = temporary.resolve(HelpSite.SINGLE_FILE);
        for (String invalid : List.of("<a href=\"http://example.org\">X</a>", "<a href=\"file:///tmp/x\">X</a>",
                "<img src=\"https://example.org/x.png\">",
                "<a href=\"//example.org\">X</a>", "<img src=\"img/x.png\">", "<link href=\"ajuda.css\">",
                "<style>@import 'remote.css';</style>", "<style>body{background:url(image.png)}</style>",
                "<a href=\"#missing\">X</a>", "<h1 id=\"x\">A</h1><h2 id=\"x\">B</h2>",
                "<img src=\"data:image/png;base64,invalid!\">")) {
            Files.writeString(single, invalid);
            assertThrows(java.io.IOException.class, () -> HelpSite.validateSingleFile(single), invalid);
        }
        Files.writeString(single, "<a href=\"https://example.org/\">Web</a>");
        HelpSite.validateSingleFile(single); // a link is only followed on a click
    }
    @Test void validatorRejectsBrokenImagesAndFragments() throws Exception {
        Path site = temporary.resolve("bad"); Files.createDirectories(site);
        Path index = site.resolve("index.html");
        Files.writeString(index, "<img src=\"missing.png\">");
        assertThrows(java.io.IOException.class, () -> HelpSite.validate(site));
        Files.writeString(index, "<a href=\"#missing\">Broken</a>");
        assertThrows(java.io.IOException.class, () -> HelpSite.validate(site));
    }
}
