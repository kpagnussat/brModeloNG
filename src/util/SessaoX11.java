package util;

import controlador.Editor;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * Tells the user, once, that their desktop runs an X11 session. Under X11 any client may send
 * keystrokes to other windows (XTEST), so code that ever ran inside this window could type into
 * a terminal, also from inside the Flatpak sandbox. Java 21 Swing draws only through X11; under
 * Wayland it goes through XWayland, which reaches only other X11 windows, so only the X11
 * session gets the notice. Closing the gap needs a Java with a native Wayland toolkit.
 */
public final class SessaoX11 {
    private SessaoX11() {}

    /** Config entry set once the notice has been shown. */
    static final String CHAVE = "cfg.avisoSessaoX11";

    /**
     * Whether the notice is due. Only an explicit "x11" session counts: the variable is absent on
     * Windows and macOS, and an unknown session gets no notice. Flatpak passes it through.
     */
    static boolean deveAvisar(Map<String, String> ambiente, boolean jaAvisado) {
        return !jaAvisado && "x11".equalsIgnoreCase(ambiente.getOrDefault("XDG_SESSION_TYPE", "").trim());
    }

    /** On the EDT, after the main window shows. */
    public static void avisarUmaVez(Component janela) {
        if (GraphicsEnvironment.isHeadless()) return;
        avisarUmaVez(System.getenv(), () -> JOptionPane.showMessageDialog(janela, texto("msg"), texto("titulo"),
                JOptionPane.WARNING_MESSAGE));
    }

    /** Shows the notice through {@code mostrar} when due; true when it did. */
    static boolean avisarUmaVez(Map<String, String> ambiente, Runnable mostrar) {
        if (!deveAvisar(ambiente, "true".equals(Editor.fromConfiguracao.getValor(CHAVE)))) return false;
        BrLogger.Logger("WARN_X11_SESSION", null);
        // Saved before showing, so a crash or kill while it is open does not repeat it.
        Editor.fromConfiguracao.SetAndSaveIfNeed(CHAVE, "true");
        mostrar.run();
        return true;
    }

    private static String texto(String chave) {
        return Editor.fromConfiguracao.getValor("Controler.interface.sessaoX11." + chave);
    }
}
