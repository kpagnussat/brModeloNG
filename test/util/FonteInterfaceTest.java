package util;

import java.awt.Font;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FonteInterfaceTest {
    // adwaita-fonts 50.0 from download.gnome.org (tarball sha256 4c927fbf…), identical to Fedora's package.
    private static final String SHA256 = "8381c33b9a44f066f2b99dba3d416a2342891e28c956a35dfd8d16ee2987e6d4";

    @Test void theBundledTypefaceIsTheReleasedAdwaitaSansWithItsLicense() throws Exception {
        byte[] ttf;
        try (InputStream in = getClass().getResourceAsStream("/fontes/AdwaitaSans-Regular.ttf")) {
            assertNotNull(in);
            ttf = in.readAllBytes();
        }
        assertEquals(SHA256, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ttf)));
        assertEquals(FonteInterface.FAMILIA, Font.createFont(Font.TRUETYPE_FONT, new java.io.ByteArrayInputStream(ttf)).getFamily());
        try (InputStream in = getClass().getResourceAsStream("/fontes/LICENSE-adwaita-fonts.txt")) {
            assertNotNull(in);
            assertTrue(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("SIL Open Font License, Version 1.1"));
        }
    }

    @Test void linuxFallsBackOnlyWhenTheDesktopFontIsMissing() {
        assertTrue(FonteInterface.faltaFonteDoDesktop(true, new Font("Fonte Que Nao Existe", Font.PLAIN, 13)));
        assertTrue(FonteInterface.faltaFonteDoDesktop(true, new Font(Font.DIALOG, Font.PLAIN, 13)));
        assertFalse(FonteInterface.faltaFonteDoDesktop(true, new Font(Font.SANS_SERIF, Font.PLAIN, 13)));
        assertFalse(FonteInterface.faltaFonteDoDesktop(false, new Font(Font.DIALOG, Font.PLAIN, 13)));
        assertFalse(FonteInterface.faltaFonteDoDesktop(true, null));
    }
}
