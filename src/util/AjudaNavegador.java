package util;

import java.awt.Component;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/** Opens offline help; neither extraction nor browser launching blocks the Swing EDT. */
public final class AjudaNavegador {
    private AjudaNavegador() {}
    private static final String SINGLE_FILE = "ajuda-completa.html";

    private static byte[] resource(ClassLoader loader, String name) throws IOException {
        try (var in = loader.getResourceAsStream("ajuda/" + name)) {
            if (in == null) throw new IOException("Recurso de ajuda ausente: " + name);
            return in.readAllBytes();
        }
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** Explicit destination/loader seam: tests resolve the fat jar without touching user state. */
    public static Path resolverIndice(ClassLoader loader, Path state) throws IOException {
        return resolverIndice(loader, state, false);
    }

    /** The portal exports one file; Flatpak only extracts the self-contained edition. */
    public static Path resolverIndice(ClassLoader loader, Path state, boolean flatpak) throws IOException {
        byte[] manifest = resource(loader, "manifest.sha256");
        Path root = state.toAbsolutePath().normalize().resolve("ajuda-" + sha256(manifest));
        Files.createDirectories(root);
        for (String entry : new String(manifest, StandardCharsets.UTF_8).lines().toList()) {
            if (!entry.matches("[a-f0-9]{64} [a-zA-Z0-9/_.-]+")) throw new IOException("Manifesto de ajuda inválido");
            String name = entry.substring(65);
            if (flatpak && !name.equals(SINGLE_FILE)) continue;
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) throw new IOException("Caminho de ajuda inválido");
            String hash = entry.substring(0, 64);
            if (Files.isRegularFile(target) && sha256(Files.readAllBytes(target)).equals(hash)) continue;
            byte[] content = resource(loader, name);
            if (!sha256(content).equals(hash)) throw new IOException("Recurso de ajuda corrompido: " + name);
            Files.createDirectories(target.getParent());
            GravacaoAtomica.gravar(target, saida -> saida.write(content));
        }
        Path index = root.resolve(flatpak ? SINGLE_FILE : "index.html");
        if (!Files.isRegularFile(index)) throw new IOException("Índice de ajuda ausente");
        return index;
    }

    /** A file URI in both environments: Flatpak's xdg-open exports the single file via OpenURI. */
    public static URI endereco(Path index, boolean flatpak) {
        return (flatpak ? index.resolveSibling(SINGLE_FILE) : index).toUri();
    }
    public static void abrir(Component parent) {
        new SwingWorker<Void, Void>() {
            private URI url;
            private Path index;
            @Override protected Void doInBackground() throws Exception {
                Path state = Pastas.arquivo(Pastas.Tipo.ESTADO, "ajuda").toPath();
                boolean flatpak = Navegador.flatpak();
                index = resolverIndice(AjudaNavegador.class.getClassLoader(), state, flatpak);
                url = endereco(index, flatpak);
                Navegador.abrir(url); return null;
            }
            @Override protected void done() {
                try { get(); }
                catch (Exception e) {
                    BrLogger.Logger("ERROR_BROWSER_OPEN", e.getMessage());
                    // Selectable text lets the student copy the local URL/path.
                    javax.swing.JTextArea details = new javax.swing.JTextArea("Não foi possível abrir a ajuda no navegador.\n"
                            + (url == null ? "" : "Abra este endereço: " + url + "\n")
                            + (index == null ? "Verifique a instalação: o site de ajuda não pôde ser extraído."
                            : "Arquivo local: " + index));
                    details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
                    details.setColumns(60); details.setRows(5);
                    JOptionPane.showMessageDialog(parent, new javax.swing.JScrollPane(details), "Ajuda do brModelo NG", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }
}
