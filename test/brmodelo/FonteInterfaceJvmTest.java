package brmodelo;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The theme's typeface, set up in a fresh JVM: preferred fonts are process-wide. */
class FonteInterfaceJvmTest {
    @Test void windowsUsesTheBundledTypeface() throws Exception {
        var r = JvmFilha.rodar(60, List.of("-Xmx128m", "-Dos.name=Windows 11"), SondaFonte.class);
        assertEquals(0, r.saida(), r.texto());
        // The bundled copy, or this machine's own Adwaita Sans when it has one.
        assertTrue(r.texto().contains("familia=Adwaita Sans"), r.texto());
    }

    @Test void linuxKeepsTheDesktopFontWhenJavaFindsIt() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("linux"));
        var r = JvmFilha.rodar(60, List.of("-Xmx128m"), SondaFonte.class);
        assertEquals(0, r.saida(), r.texto());
        // Headless FlatLaf reports SansSerif, a family Java has: no fallback.
        assertTrue(r.texto().contains("embutida=false"), r.texto());
    }
}
