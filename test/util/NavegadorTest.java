package util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static util.Navegador.Via.*;

class NavegadorTest {
    private static final URI WEB = URI.create("https://github.com/kpagnussat/brModeloNG/releases");
    private static final URI AJUDA = Path.of("/home/aluno/.local/state/brmodelo-ng/ajuda-1/index.html").toUri();

    @Test void underTheLinuxLauncherTheBrowserStartsOutsideTheProgramsNamespace() {
        // The portal refuses file: URIs, so the help page goes straight to the user's service manager.
        assertEquals(List.of(PORTAL, SERVICO_USUARIO, DESKTOP, XDG_OPEN), Navegador.vias(WEB, true, false, true));
        assertEquals(List.of(SERVICO_USUARIO, DESKTOP, XDG_OPEN), Navegador.vias(AJUDA, true, false, true));
    }

    @Test void elsewhereTheUsualWaysAreKept() {
        assertEquals(List.of(DESKTOP, XDG_OPEN), Navegador.vias(WEB, true, false, false));
        assertEquals(List.of(DESKTOP, XDG_OPEN), Navegador.vias(AJUDA, true, false, false));
        assertEquals(List.of(DESKTOP), Navegador.vias(WEB, false, false, false));
        assertEquals(List.of(DESKTOP), Navegador.vias(AJUDA, false, false, false));
        assertEquals(List.of(DESKTOP, XDG_OPEN), Navegador.vias(WEB, true, true, false));
        assertEquals(List.of(XDG_OPEN), Navegador.vias(AJUDA, true, true, false));
    }

    @Test void flatpakXdgOpenReceivesTheHelpFileByPath() {
        assertEquals(List.of("xdg-open", Path.of(AJUDA).toString()), Navegador.comandoXdgOpen(AJUDA, true));
        assertEquals(List.of("xdg-open", AJUDA.toASCIIString()), Navegador.comandoXdgOpen(AJUDA, false));
        assertEquals(List.of("xdg-open", WEB.toASCIIString()), Navegador.comandoXdgOpen(WEB, true));
    }

    @Test void theServiceManagerReportsXdgOpensOwnResultAndKeepsDollarsLiteral() {
        URI comDolar = Path.of("/home/aluno/R$ 10/ajuda/index.html").toUri();
        List<String> c = Navegador.comandoServicoUsuario(comDolar, Map.of());
        assertTrue(c.containsAll(List.of("--wait", "--property=Type=exec")), c.toString());
        assertEquals(comDolar.toASCIIString().replace("$", "$$"), c.getLast());
        assertTrue(c.getLast().contains("R$$"), c.getLast());
    }

    @Test void theServiceManagerReceivesTheSessionsDisplay() {
        assertEquals(List.of("systemd-run", "--user", "--quiet", "--collect", "--wait", "--property=Type=exec", "--setenv=DISPLAY=:0",
                "--setenv=WAYLAND_DISPLAY=wayland-0", "--", "xdg-open", WEB.toASCIIString()),
                Navegador.comandoServicoUsuario(WEB, Map.of("DISPLAY", ":0", "WAYLAND_DISPLAY", "wayland-0",
                        "XAUTHORITY", "", "HOME", "/home/aluno")));
    }

    @Test void onlyWebAndLocalPagesAreOpened() {
        for (String u : List.of("http://example.com/", "javascript:alert(1)", "smb://servidor/x", "relativo.html")) {
            assertThrows(IOException.class, () -> Navegador.abrir(URI.create(u)), u);
        }
    }
}
