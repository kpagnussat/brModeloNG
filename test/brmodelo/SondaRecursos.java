package brmodelo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/**
 * Child-JVM probe for RecursosTest: each case asks for one image the size of a large
 * shape or declared size, which a small heap cannot hold. The parent runs this with
 * -Xmx256m and reads one line per case.
 */
public final class SondaRecursos {
    static final int GRANDE = 30_000;

    public static void main(String[] args) throws Exception {
        // The PNG reader wraps its own OutOfMemoryError in an IOException, which also ends
        // in null, so the log tells a refusal by size from a failed allocation.
        caso("imagem", () -> util.TratadorDeImagens.fromByteArray(pngDeclarando(20_000, 20_000)) == null
                ? "recusada " + ultimoLog() : "lida");
        // Shapes built the way a file load builds them: an owner editor, repaints off.
        var diagrama = Fixtures.load("livre");
        diagrama.setMaster(new controlador.Editor());
        diagrama.isCarregando = true;
        caso("seta", () -> {
            var seta = new desenho.formas.Desenhador(diagrama);
            seta.setTipoSeta();
            seta.SetBounds(0, 0, GRANDE, GRANDE);
            pintar(seta);
            return "pintada";
        });
        caso("item", () -> {
            var item = new desenho.formas.Desenhador(diagrama);
            item.setTipoImg();
            item.setImagem(new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB));
            item.SetBounds(0, 0, GRANDE, GRANDE);
            pintar(item);
            return "pintado";
        });
        caso("exportar", () -> {
            try {
                util.ImageGenerate.geraImagemForPrn(diagrama, GRANDE, GRANDE);
                return "gerada";
            } catch (Exception e) {
                if (e instanceof util.LimitesEntrada.Excedido) return "recusada " + e.getMessage();
                throw e;
            }
        });
        System.out.println("fim");
    }

    interface Caso { String rodar() throws Exception; }

    private static void caso(String nome, Caso caso) {
        try {
            System.out.println(nome + ": " + caso.rodar());
        } catch (Throwable e) {
            System.out.println(nome + ": " + e);
        }
    }

    private static String ultimoLog() {
        synchronized (util.BrLogger.Logs) {
            return util.BrLogger.Logs.isEmpty() ? "" : util.BrLogger.Logs.get(util.BrLogger.Logs.size() - 1).Valor;
        }
    }

    private static void pintar(desenho.formas.Desenhador forma) {
        BufferedImage tela = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = tela.createGraphics();
        try { forma.DoPaint(g); } finally { g.dispose(); }
    }

    /** A valid 1x1 PNG whose header claims another size; the pixel data stays 1x1. */
    static byte[] pngDeclarando(int largura, int altura) throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", saida);
        byte[] png = saida.toByteArray();
        // Signature (8), IHDR length (4), "IHDR" at 12, width at 16, height at 20, CRC at 29.
        escreverInt(png, 16, largura);
        escreverInt(png, 20, altura);
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        escreverInt(png, 29, (int) crc.getValue());
        return png;
    }

    private static void escreverInt(byte[] b, int pos, int v) {
        b[pos] = (byte) (v >>> 24); b[pos + 1] = (byte) (v >>> 16); b[pos + 2] = (byte) (v >>> 8); b[pos + 3] = (byte) v;
    }
}
