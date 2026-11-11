package util;

import com.formdev.flatlaf.FlatLaf;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.Locale;
import javax.swing.UIManager;

/**
 * The interface typeface. Windows and macOS use the bundled Adwaita Sans (GNOME's interface
 * font, SIL Open Font License 1.1, src/fontes/), so the program looks the same as on GNOME.
 * Linux keeps the desktop's own font. Inside Flatpak on image-based systems (Silverblue,
 * Kinoite), whose host font folders share the runtime's zero timestamp, fontconfig reuses the
 * runtime's cache and the host's fonts disappear: there the bundled copy stands in for a
 * missing Adwaita Sans, and for any other missing desktop font the theme is set up again with
 * it. Diagram fonts are not affected.
 */
public final class FonteInterface {
    private FonteInterface() {}

    static final String FAMILIA = "Adwaita Sans";
    private static final String RECURSO = "/fontes/AdwaitaSans-Regular.ttf";
    private static boolean registrada, preferida;

    static boolean linux() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux"); }

    /**
     * Before the first FlatLaf setup. The family is registered first, everywhere: Swing caches
     * each family lookup, so a name looked up while missing would stay a fallback.
     */
    public static void antesDoTema() {
        // An installed Adwaita Sans makes the copy unnecessary (about 0.1 s of startup).
        if (!linux() || !FAMILIA.equals(new Font(FAMILIA, Font.PLAIN, 12).getFamily())) registrar();
        if (!linux()) preferir();
    }

    /**
     * After a FlatLaf setup on Linux: true when the desktop font resolved to Java's logical
     * fallback, the bundled typeface is now preferred and the theme must be set up again.
     */
    public static boolean reservaNecessaria() {
        return !preferida && faltaFonteDoDesktop(linux(), UIManager.getFont("defaultFont")) && preferir();
    }

    /** Java names a family it could not find "Dialog", its logical fallback. */
    static boolean faltaFonteDoDesktop(boolean linux, Font atual) {
        return linux && atual != null && Font.DIALOG.equals(atual.getFamily());
    }

    /** Whether the interface is drawn with the bundled copy rather than an installed one. */
    public static boolean embutida() {
        Font atual = UIManager.getFont("defaultFont");
        return registrada && atual != null && FAMILIA.equals(atual.getFamily());
    }

    private static void registrar() {
        try (InputStream in = FonteInterface.class.getResourceAsStream(RECURSO)) {
            if (in == null) throw new java.io.IOException("missing " + RECURSO);
            // False when the system already has the family: Java keeps the installed one.
            registrada = GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
        } catch (Exception e) {
            BrLogger.Logger("WARN_UI_FONT", e.getMessage());
        }
    }

    private static boolean preferir() {
        if (!registrada && !java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames()).contains(FAMILIA)) return false;
        FlatLaf.setPreferredFontFamily(FAMILIA);
        preferida = true;
        return true;
    }
}
