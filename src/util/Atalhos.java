package util;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * The platform's shortcut modifier: Command on macOS, Ctrl elsewhere (issue #27 of the
 * original brModelo, where every shortcut was hardwired to Ctrl).
 *
 * Shortcuts are written with "ctrl" in the properties files and in code; every place that
 * turns them into a KeyStroke, or asks whether the modifier is held, goes through here.
 */
public final class Atalhos {

    private static volatile Integer mascara;

    private Atalhos() {
    }

    /** The modifier mask for shortcuts: META_DOWN_MASK on macOS, CTRL_DOWN_MASK elsewhere. */
    public static int mascara() {
        Integer m = mascara;
        if (m == null) {
            m = InputEvent.CTRL_DOWN_MASK;
            // Toolkit refuses to answer without a display; Ctrl is the right answer there anyway.
            if (!GraphicsEnvironment.isHeadless()) {
                try {
                    m = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                } catch (RuntimeException e) {
                    m = InputEvent.CTRL_DOWN_MASK;
                }
            }
            mascara = m;
        }
        return m;
    }

    /**
     * Overrides the detected mask, so the macOS behaviour can be tested on any system.
     * Null goes back to detection.
     */
    public static void usarMascara(Integer m) {
        mascara = m;
    }

    /** The key that holds the modifier down: VK_META with Command, VK_CONTROL with Ctrl. */
    public static int tecla() {
        return mascara() == InputEvent.META_DOWN_MASK ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
    }

    /**
     * KeyStroke.getKeyStroke(spec), with Ctrl replaced by the platform's modifier.
     * Null or unparsable specs give null, as KeyStroke.getKeyStroke does.
     */
    public static KeyStroke tecla(String spec) {
        KeyStroke k;
        try {
            k = KeyStroke.getKeyStroke(spec);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return comMascara(k);
    }

    /** The same keystroke with Ctrl replaced by the platform's modifier. */
    public static KeyStroke comMascara(KeyStroke k) {
        int m = mascara();
        if (k == null || m == InputEvent.CTRL_DOWN_MASK || (k.getModifiers() & InputEvent.CTRL_DOWN_MASK) == 0) {
            return k;
        }
        int mods = (k.getModifiers() & ~(InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) | m;
        if (k.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            return KeyStroke.getKeyStroke(Character.valueOf(k.getKeyChar()), mods);
        }
        return KeyStroke.getKeyStroke(k.getKeyCode(), mods, k.isOnKeyRelease());
    }

    /** Shortcut for a key plus the platform's modifier, as with "ctrl pressed" + key. */
    public static KeyStroke tecla(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, mascara());
    }

    /** Whether the platform's modifier is held in this event. */
    public static boolean pressionado(InputEvent e) {
        return (e.getModifiersEx() & mascara()) != 0;
    }
}
