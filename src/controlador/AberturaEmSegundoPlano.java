package controlador;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 * Opens a .brM3 or .brMj file without holding the EDT. The import worker
 * (util.TrabalhadorImportacao) may run until its deadline on a hostile file, and the window
 * keeps painting meanwhile. If reading is not over after {@link #ATRASO_DIALOGO_MS}, a modal
 * dialog blocks further commands and offers Cancel, which interrupts the reading thread (and
 * so kills the worker) and gives the window back at once. The diagram read is installed on
 * the EDT, unless the same file was opened in the meantime.
 */
final class AberturaEmSegundoPlano {
    private AberturaEmSegundoPlano() {}

    /** Most files open within this; only slower ones show the dialog. */
    static final int ATRASO_DIALOGO_MS = 500;
    static final String NOME_THREAD = "brModelo-abrir";

    interface Leitor {
        Diagrama ler(File arq) throws IOException;
    }

    /** Reads a file off the EDT; tests replace it to control timing. */
    static volatile Leitor leitor = PersistenciaDiagrama::lerModelo;

    /**
     * Starts opening {@code arq}; call on the EDT. The result completes on the EDT with the
     * installed diagram, or null when the file was refused, the user cancelled or the file was
     * already open. It never completes exceptionally.
     */
    static CompletableFuture<Diagrama> abrir(Editor editor, File arq) {
        return new Abertura(editor, arq).iniciar();
    }

    private static final class Abertura {
        private final Editor editor;
        private final File arq;
        private final CompletableFuture<Diagrama> fim = new CompletableFuture<>();
        private final Leitor leitura = leitor;
        private Thread thread;
        private Timer atraso;
        private JDialog dialogo;

        Abertura(Editor editor, File arq) {
            this.editor = editor;
            this.arq = arq;
        }

        CompletableFuture<Diagrama> iniciar() {
            thread = new Thread(this::ler, NOME_THREAD);
            thread.setDaemon(true);
            thread.start();
            if (!GraphicsEnvironment.isHeadless()) {
                atraso = new Timer(ATRASO_DIALOGO_MS, e -> mostrarDialogo());
                atraso.setRepeats(false);
                atraso.start();
            }
            return fim;
        }

        private void ler() {
            Diagrama lido = null;
            Throwable erro = null;
            try {
                lido = leitura.ler(arq);
            } catch (Throwable e) { // reported on the EDT, as the synchronous open did
                erro = e;
            }
            Diagrama res = lido;
            Throwable falha = erro;
            SwingUtilities.invokeLater(() -> concluir(res, falha));
        }

        // EDT. A cancelled opening has already completed: its late result is dropped.
        private void concluir(Diagrama lido, Throwable erro) {
            if (fim.isDone()) return;
            fecharDialogo();
            Diagrama res = null;
            try {
                if (erro != null) {
                    util.BrLogger.Logger("ERROR_DIAGRAMA_LOAD_FILE_BRM",
                            erro instanceof IOException ? erro.getMessage() : erro.toString());
                } else if (aberto() == null) {
                    res = PersistenciaDiagrama.prepararModelo(lido, arq, editor);
                    editor.ProcessePosOpen(res, false);
                }
            } finally {
                fim.complete(res);
            }
        }

        private Diagrama aberto() {
            String caminho = arq.getAbsolutePath();
            return editor.getDiagramas().stream().filter(d -> caminho.equals(d.getArquivo())).findFirst().orElse(null);
        }

        private void cancelar() {
            if (fim.isDone()) return;
            thread.interrupt(); // the worker is killed by TrabalhadorImportacao on interruption
            fecharDialogo();
            util.BrLogger.Logger("INFO_DIAGRAMA_LOAD_CANCELLED", arq.getName(), null);
            fim.complete(null);
        }

        private void fecharDialogo() {
            if (atraso != null) atraso.stop();
            if (dialogo != null) dialogo.dispose();
        }

        private void mostrarDialogo() {
            if (fim.isDone()) return;
            dialogo = new JDialog(SwingUtilities.getWindowAncestor(editor), texto("titulo"), Dialog.ModalityType.DOCUMENT_MODAL);
            dialogo.setName("abrindoDiagrama");
            dialogo.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialogo.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) { cancelar(); }
            });
            JPanel corpo = new JPanel();
            corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
            corpo.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));
            corpo.add(new JLabel(texto("msg")));
            corpo.add(new JLabel(arq.getName()));
            corpo.add(Box.createVerticalStrut(8));
            JProgressBar progresso = new JProgressBar();
            progresso.setIndeterminate(true);
            progresso.setAlignmentX(0f);
            // Wide enough for a usual file name; a longer one widens the dialog.
            progresso.setPreferredSize(util.Escala.ajuste(progresso, 280, progresso.getPreferredSize().height));
            corpo.add(progresso);
            JButton cancelar = new JButton(texto("cancelar"));
            cancelar.setName("cancelarAbertura");
            cancelar.addActionListener(e -> cancelar());
            JPanel botoes = new JPanel();
            botoes.add(cancelar);
            dialogo.getContentPane().add(corpo, BorderLayout.CENTER);
            dialogo.getContentPane().add(botoes, BorderLayout.SOUTH);
            dialogo.getRootPane().setDefaultButton(cancelar);
            dialogo.pack();
            dialogo.setResizable(false);
            dialogo.setLocationRelativeTo(dialogo.getOwner());
            dialogo.setVisible(true); // modal: returns once concluir or cancelar disposes it
        }

        private static String texto(String chave) {
            return Editor.fromConfiguracao.getValor("Controler.interface.abrindo." + chave);
        }
    }
}
