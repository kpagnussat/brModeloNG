package util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The X11 session notice: only an explicit X11 session, and only once. */
class SessaoX11Test {
    @AfterEach void esquecer() {
        controlador.Editor.fromConfiguracao.setValor(SessaoX11.CHAVE, "");
    }

    @Test void onlyAnExplicitX11SessionIsWarned() {
        assertTrue(SessaoX11.deveAvisar(Map.of("XDG_SESSION_TYPE", "x11"), false));
        assertTrue(SessaoX11.deveAvisar(Map.of("XDG_SESSION_TYPE", "X11"), false));
        assertFalse(SessaoX11.deveAvisar(Map.of("XDG_SESSION_TYPE", "wayland"), false));
        assertFalse(SessaoX11.deveAvisar(Map.of("XDG_SESSION_TYPE", "tty"), false));
        // Windows, macOS and sessions that do not say: no notice.
        assertFalse(SessaoX11.deveAvisar(Map.of("DISPLAY", ":0"), false));
        assertFalse(SessaoX11.deveAvisar(Map.of("XDG_SESSION_TYPE", "x11"), true));
    }

    @Test void theNoticeIsShownOnceAndRemembered() {
        AtomicInteger mostrado = new AtomicInteger();
        Map<String, String> x11 = Map.of("XDG_SESSION_TYPE", "x11");
        assertTrue(SessaoX11.avisarUmaVez(x11, mostrado::incrementAndGet));
        assertEquals("true", controlador.Editor.fromConfiguracao.getValor(SessaoX11.CHAVE));
        assertFalse(SessaoX11.avisarUmaVez(x11, mostrado::incrementAndGet));
        assertEquals(1, mostrado.get());
        synchronized (BrLogger.Logs) {
            assertEquals("WARN_X11_SESSION", BrLogger.Logs.get(BrLogger.Logs.size() - 1).Tipo);
        }
    }

    @Test void waylandShowsNothingAndRemembersNothing() {
        AtomicInteger mostrado = new AtomicInteger();
        assertFalse(SessaoX11.avisarUmaVez(Map.of("XDG_SESSION_TYPE", "wayland"), mostrado::incrementAndGet));
        assertEquals(0, mostrado.get());
        assertNotEquals("true", controlador.Editor.fromConfiguracao.getValor(SessaoX11.CHAVE));
    }
}
