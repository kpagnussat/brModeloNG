package controlador;

import controlador.apoios.GuardaPadraoBrM;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Schedules EDT snapshots, serial writes, recovery and orderly autosave shutdown. */
final class AutoSalvamentoEditor {
    private AutoSalvamentoEditor() {}

    static void setAutoSaveInterval(Editor editor, int autoSaveInterval) {
        if (!SwingUtilities.isEventDispatchThread()) {
            onEdt(() -> editor.setAutoSaveInterval(autoSaveInterval));
            return;
        }
        if (editor.shuttingDown) {
            return;
        }
        if (autoSaveInterval > -1 && autoSaveInterval < 30) {
            editor.autoSaveInterval = autoSaveInterval;
        }
        editor.autoSaveAtivo = editor.autoSaveInterval > 0;
        editor.InicieAutoSave();
    }

    static boolean InicieAutoSave(Editor editor) {
        if (!editor.autoSaveAtivo) {
            editor.EndAutoSave();
            return false;
        }
        editor.startAutoSave(0);
        return true;
    }

    static void startAutoSave(Editor editor, int initialDelay) {
        // One Swing timer owns the schedule. Snapshots and all scheduling state belong to
        // the EDT, so serializing a diagram cannot race mouse/keyboard edits.
        if (editor.autoSaveTimer == null) {
            editor.tempoAs = editor.new Temporizador(editor);
            editor.autoSaveTimer = new Timer(0, event -> editor.tempoAs.run());
        }
        editor.autoSaveTimer.stop();
        editor.autoSaveTimer.setDelay(editor.autoSaveInterval * 1000 * 60);
        editor.autoSaveTimer.setInitialDelay(initialDelay);
        editor.autoSaveIniciado = true;
        editor.autoSaveTimer.start();
    }

    static boolean PreInicieAutoSave(Editor editor, int iter) {
        editor.autoSaveInterval = iter;
        editor.autoSaveAtivo = editor.autoSaveInterval > 0;
        if (editor.autoSaveAtivo) {
            editor.startAutoSave(editor.autoSaveInterval * 1000 * 60);
            return true;
        }
        return false;
    }

    static boolean DoAutoSave(Editor editor) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Autosave snapshots must run on the EDT");
        }
        //evita reescrita repetitiva!
        if (editor.doneAutoSave || editor.shuttingDown) {
            return true;
        }
        editor.DoStatus(Editor.fromConfiguracao.getValor("Controler.MSG_STATUS_AUTOSAVE"));
        editor.autoSave.clear();
        for (Diagrama diagram : editor.getDiagramas()) {
            if (diagram.getMudou() && !diagram.AutoSalvar(editor.autoSave)) {
                return false;
            }
        }
        editor.doneAutoSave = true;
        editor.queueAutoSave();
        return true;
    }

    static boolean AutoSalveToFile(Editor editor, ArrayList<byte[]> snapshot, String file) {
        try {
            util.GravacaoAtomica.gravar(new File(file).toPath(), saida -> {
                try (ObjectOutput out = new ObjectOutputStream(saida)) {
                    out.writeObject(snapshot);
                }
            });
            return true;
        } catch (IOException iOException) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_AUTOSAVE_WIRTE", iOException.getMessage());
            return false;
        }
    }

    static ExecutorService autoSaveWriter(Editor editor) {
        if (editor.autoSaveWriter == null) {
            editor.autoSaveWriter = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "brModelo-autosave-writer");
                thread.setDaemon(true);
                return thread;
            });
        }
        return editor.autoSaveWriter;
    }

    static void queueAutoSave(Editor editor) {
        // The worker only sees detached bytes, never diagrams or the EDT's mutable list.
        // A single FIFO writer also prevents older snapshots overwriting newer ones or the
        // empty recovery file queued when autosave is disabled/the application exits.
        ArrayList<byte[]> snapshot = new ArrayList<>(editor.autoSave);
        String file = Editor.fromConfiguracao.getAutoSaveFile();
        editor.autoSaveWriter().execute(() -> {
            if (!editor.AutoSalveToFile(snapshot, file)) {
                SwingUtilities.invokeLater(() -> editor.doneAutoSave = false);
            }
        });
    }

    static void onEdt(Runnable action) {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    static void EndAutoSave(Editor editor) {
        if (!SwingUtilities.isEventDispatchThread()) {
            onEdt(editor::EndAutoSave);
            return;
        }
        if (editor.autoSaveTimer != null) {
            editor.autoSaveTimer.stop();
        }
        if (editor.autoSaveIniciado) {
            editor.autoSaveIniciado = false;
            editor.autoSave.clear();
            editor.queueAutoSave();
        }
    }

    static void EndAutoSave(Editor editor, Runnable afterWrites) {
        if (!SwingUtilities.isEventDispatchThread()) {
            onEdt(() -> editor.EndAutoSave(afterWrites));
            return;
        }
        if (editor.shuttingDown) {
            return;
        }
        editor.EndAutoSave();
        editor.shuttingDown = true;
        util.MensagensStatus.parar(editor.getLblStatus());
        if (editor.statusTimer != null) {
            editor.statusTimer.stop();
        }
        if (editor.autoSaveWriter != null) {
            editor.autoSaveWriter.execute(() -> SwingUtilities.invokeLater(afterWrites));
            editor.autoSaveWriter.shutdown();
        } else {
            SwingUtilities.invokeLater(afterWrites);
        }
    }

    static void DoAutoSaveCompleto(Editor editor) {
        if (!SwingUtilities.isEventDispatchThread()) {
            onEdt(editor::DoAutoSaveCompleto);
            return;
        }
        if (editor.autoSaveIniciado) {
            editor.doneAutoSave = false;
            editor.DoAutoSave();
        }
    }

    static boolean LoadAutoSave(Editor editor) {

        File f = new File(Editor.fromConfiguracao.getAutoSaveFile());
        if (!f.exists()) {
            return false;
        }

        try {
            util.LimitesEntrada.conferir(f, util.LimitesEntrada.BINARIO_BYTES);
            FileInputStream fi = new FileInputStream(f);
            try (ObjectInput in = new util.LeitorSeguro(fi)) {
                ArrayList<byte[]> salvado = (ArrayList<byte[]>) in.readObject();
                if (!salvado.isEmpty()) {
                    if (!util.Dialogos.ShowMessageConfirmYES(editor.getRootPane(),
                            Editor.fromConfiguracao.getValor(salvado.size() > 1 ? "Controler.MSG_CONFIRM_LOAD_AUTOSAVE_PLURAL" : "Controler.MSG_CONFIRM_LOAD_AUTOSAVE"),
                            false)) {
                        in.close();
                        return false;
                    }
                    // Each recovered diagram is read through the allowlist on its own: one that
                    // is damaged or rejected is skipped (and logged), the others still come back,
                    // as before.
                    for (byte[] k : salvado) {
                        try (ObjectInputStream inb = new util.LeitorSeguro(new ByteArrayInputStream(k))) {
                            GuardaPadraoBrM seguranca = (GuardaPadraoBrM) inb.readObject();
                            Diagrama res = Diagrama.LoadFromBrm(seguranca, editor);
                            if (res != null) {
                                editor.ProcessePosOpen(res, false);
                                res.setMudou(true);
                            }
                        } catch (ClassCastException | NullPointerException | IOException | ClassNotFoundException iOException) {
                            util.BrLogger.Logger("ERROR_DIAGRAMA_AUTOSAVE_LOAD", "STREAM LENGTH: " + String.valueOf(k.length), iOException.getMessage());
                        }
                    }
                    in.close();
                    editor.doneAutoSave = false;
                    editor.controler.makeEnableComands();
                    editor.AtualizeTreeNavegacao();
                    return true;
                }
            }
        } catch (ClassCastException | NullPointerException | IOException | ClassNotFoundException iOException) {
            util.BrLogger.Logger("ERROR_DIAGRAMA_AUTOSAVE_LOAD", iOException.getMessage());
        }
        return false;
    }
}
