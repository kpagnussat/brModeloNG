package brmodelo;

import java.awt.event.InputEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import util.Atalhos;
import static org.junit.jupiter.api.Assertions.*;

/** Issue #27 of the original brModelo: the shortcut modifier follows the platform. */
class AtalhosTest {

    @AfterEach void reset() {
        Atalhos.usarMascara(null);
    }

    @Test void withCtrlEveryConfiguredShortcutIsUnchanged() throws Exception {
        Atalhos.usarMascara(InputEvent.CTRL_DOWN_MASK);
        Properties p = propriedades();
        int n = 0;
        for (String chave : p.stringPropertyNames()) {
            if (!chave.endsWith(".tecla")) continue;
            String spec = p.getProperty(chave);
            assertEquals(KeyStroke.getKeyStroke(spec), Atalhos.tecla(spec), chave);
            n++;
        }
        assertTrue(n > 40, "shortcuts found: " + n);
    }

    @Test void withCommandOnlyCtrlIsReplaced() throws Exception {
        Atalhos.usarMascara(InputEvent.META_DOWN_MASK);
        Properties p = propriedades();
        for (String chave : p.stringPropertyNames()) {
            if (!chave.endsWith(".tecla")) continue;
            KeyStroke antes = KeyStroke.getKeyStroke(p.getProperty(chave)), depois = Atalhos.tecla(p.getProperty(chave));
            if (antes == null) { assertNull(depois, chave); continue; }
            int resto = InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK;
            assertEquals(antes.getKeyCode(), depois.getKeyCode(), chave);
            assertEquals(antes.getModifiers() & resto, depois.getModifiers() & resto, chave);
            assertEquals((antes.getModifiers() & InputEvent.CTRL_DOWN_MASK) != 0, (depois.getModifiers() & InputEvent.META_DOWN_MASK) != 0, chave);
            assertEquals(0, depois.getModifiers() & InputEvent.CTRL_DOWN_MASK, chave);
        }
        assertEquals(KeyStroke.getKeyStroke("shift meta pressed C"), Atalhos.tecla("shift ctrl pressed C"));
        assertEquals(KeyStroke.getKeyStroke("pressed DELETE"), Atalhos.tecla("pressed DELETE"));
        assertNull(Atalhos.tecla((String) null));
    }

    private static Properties propriedades() throws Exception {
        Properties p = new Properties();
        try (InputStream in = AtalhosTest.class.getResourceAsStream("/principal/Propriedades_pt_BR.properties")) {
            assertNotNull(in);
            p.load(new InputStreamReader(in, StandardCharsets.ISO_8859_1));
        }
        return p;
    }
}
