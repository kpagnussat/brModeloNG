package util;

import java.net.URI;

/** Releases are opened only on an explicit user action; startup never checks the network. */
public final class AtualizacoesNG {
    public static final String RELEASES = "https://github.com/kpagnussat/brModeloNG/releases";
    private AtualizacoesNG() {}

    public static void abrirReleases() {
        Navegador.abrirDepois(URI.create(RELEASES));
    }
}
