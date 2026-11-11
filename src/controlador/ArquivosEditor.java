package controlador;

import java.io.File;
import java.awt.Point;
import java.util.concurrent.CompletableFuture;
import java.awt.image.BufferedImage;
import desenho.FormaElementar;
import javax.swing.JOptionPane;

/** Owns open/save/export commands, loaded-diagram installation and duplicate-path checks. */
final class ArquivosEditor {
    private ArquivosEditor() {}

    static boolean IsOpen(Editor editor, File arq) {
        String tmp = arq.getAbsolutePath();
        for (Diagrama d : editor.getDiagramas()) {
            if (d.getArquivo().equals(tmp)) {
                editor.setSelected(d);
                JOptionPane.showMessageDialog(editor.getParent(), Editor.fromConfiguracao.getValor("Controler.interface.mensagem.msg01"),
                        Editor.fromConfiguracao.getValor("Controler.interface.mensagem.tit_informacao"), JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        }
        return false;
    }

    static void ChecarArquivosBiAbertos(Editor editor, Diagrama diag) {
        editor.getDiagramas().stream().filter(d -> !d.getArquivo().isEmpty() && d != diag).forEach(d -> {
            if (d.getArquivo().equals(diag.getArquivo())) {
                util.BrLogger.Logger("ERRO_SAME_FILE", " (" + d.getNome() + ")", null);
            }
        });
    }

    /**
     * Opens a diagram file; completes on the EDT with the diagram installed, or null. Model
     * files (.brM3, .brMj) are read in the background (AberturaEmSegundoPlano); the legacy
     * XML format, parsed here under XMLGenerate's limits, still opens before this returns.
     */
    static CompletableFuture<Diagrama> AbrirDiagramaFromFile(Editor editor, File arq) {
        if (arq != null && util.Arquivo.IsModelo(arq)) {
            if (editor.IsOpen(arq)) return CompletableFuture.completedFuture(null);
            return AberturaEmSegundoPlano.abrir(editor, arq);
        }
        Diagrama res = Diagrama.LoadFromFile(arq, editor);
        if (res != null) {
            editor.ProcessePosOpen(res, true);
        }
        return CompletableFuture.completedFuture(res);
    }

    static void ProcessePosOpen(Editor editor, Diagrama res, boolean isXml) {
        if (res != null) {
            res.setMaster(editor);
            int idx = -1;
            if (!editor.diagramaAtual.isAlterado()) {
                idx = editor.historicos.getDiagramas().indexOf(editor.diagramaAtual);
                editor.FechaDiagrama(editor.diagramaAtual, res);
            }
            editor.validate();
            editor.diagramaAtual = res;
            res.setMudou(false);
            if (idx > -1) {
                editor.historicos.add(editor.diagramaAtual, idx);
            } else {
                editor.historicos.add(editor.diagramaAtual);
            }
            res.OnAfterLoad(isXml);
            editor.prepareDiagramaAtual();
            editor.RePopuleBarraDiagramas(true);
        }
    }

    static boolean processeComando(Editor editor, Controler.menuComandos cmd, java.awt.event.ActionEvent ev) throws java.io.IOException {
        switch (cmd) {
            case cmdSave:
                editor.diagramaAtual.Salvar(editor.diagramaAtual.getArquivo());
                editor.RePopuleBarraDiagramas(false);
                editor.controler.makeEnableComands();
                editor.ChecarArquivosBiAbertos(editor.diagramaAtual);
                break;
            case cmdSaveAs:
                editor.diagramaAtual.Salvar();
                editor.RePopuleBarraDiagramas(false);
                editor.controler.makeEnableComands();
                editor.ChecarArquivosBiAbertos(editor.diagramaAtual);
                break;

            case cmdSaveAll:
                editor.getDiagramas().stream().forEach(d -> {
                    d.Salvar(d.getArquivo());
                });
                editor.getDiagramas().stream().forEach(d -> {
                    editor.ChecarArquivosBiAbertos(d);
                });
                editor.RePopuleBarraDiagramas(false);
                editor.controler.makeEnableComands();
                break;

            case cmdOpen:
                File arq = util.Dialogos.ShowDlgLoadDiagrama(editor.diagramaAtual.getArquivo(), editor);
                AbrirDiagramaFromFile(editor, arq).thenAccept(aberto -> {
                    if (aberto != null) editor.ChecarArquivosBiAbertos(aberto);
                });
                break;
            case cmdCopyImg:
                final int borda = 2;
                Point p2 = editor.diagramaAtual.getPontoExtremoSelecionado();
                int minX = p2.x;
                int minY = p2.y;

                for (int i = editor.diagramaAtual.getItensSelecionados().size() - 1; i > -1; i--) {
                    FormaElementar el = editor.diagramaAtual.getItensSelecionados().get(i);
                    minX = Math.min(minX, el.getLeft());
                    minY = Math.min(minY, el.getTop());
                }

                minX = Math.max(minX - borda, 0);
                minY = Math.max(minY - borda, 0);

                BufferedImage cp_img = util.ImageGenerate.geraImagemForPrnSelecao(editor.diagramaAtual, p2.x + borda, p2.y + borda);
                BufferedImage cp_img2 = cp_img.getSubimage(minX, minY, p2.x - minX, p2.y - minY);
                editor.diagramaAtual.doCopy(cp_img2);
                break;

            case cmdExport:
                File arqui = util.Dialogos.ShowDlgSaveAsImg(editor, editor.diagramaAtual);
                if (arqui != null) {
                    Point p = editor.diagramaAtual.getPontoExtremo();
                    BufferedImage img = util.ImageGenerate.geraImagemForPrn(editor.diagramaAtual, p.x, p.y);
                    String formato = (util.Arquivo.IsBMP(arqui) ? util.Arquivo.bmp : util.Arquivo.png).toUpperCase();
                    util.GravacaoAtomica.gravar(arqui.toPath(), saida -> javax.imageio.ImageIO.write(img, formato, saida));
                }
                break;

            default:
                return false;
        }
        return true;
    }
}
