package brmodelo;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import util.LimitesEntrada;
import static org.junit.jupiter.api.Assertions.*;

/** Guards the bounds on images, shape painting and expressions that a diagram file controls. */
class RecursosTest {
    // A fresh JVM with a small heap: without the bounds each case allocates gigabytes.
    @Test
    void imagesShapesAndExportsAreBoundedInPixels() throws Exception {
        var r = JvmFilha.rodar(60, List.of("-Xmx256m"), SondaRecursos.class);
        assertFalse(r.expirou(), r.texto());
        assertTrue(r.texto().contains("imagem 20000x20000: " + 20_000L * 20_000 + " > " + LimitesEntrada.PIXELS_IMAGEM), r.texto());
        assertTrue(r.texto().contains("seta: pintada"), r.texto());
        assertTrue(r.texto().contains("item: pintado"), r.texto());
        assertTrue(r.texto().contains("exportar: recusada imagem " + SondaRecursos.GRANDE + "x" + SondaRecursos.GRANDE
                + ": " + (long) SondaRecursos.GRANDE * SondaRecursos.GRANDE + " > " + LimitesEntrada.PIXELS_IMAGEM), r.texto());
        assertTrue(r.texto().contains("fim"), r.texto());
    }

    @Test
    void imagesWithinTheLimitAreRead() throws Exception {
        BufferedImage original = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB);
        original.setRGB(299, 199, 0xff123456);
        BufferedImage lida = util.TratadorDeImagens.fromByteArray(util.TratadorDeImagens.toByteArray(original));
        assertEquals(300, lida.getWidth());
        assertEquals(200, lida.getHeight());
        assertEquals(0xff123456, lida.getRGB(299, 199));
    }

    @Test
    void exportAtTheLimitIsAllowed() throws Exception {
        // Only the size check runs: a 1-pixel-high image at the limit's width would be too
        // wide for the JDK, so check the boundary on the bound itself.
        assertDoesNotThrow(() -> LimitesEntrada.conferirPixels("imagem", LimitesEntrada.PIXELS_IMAGEM, 1));
        assertThrows(LimitesEntrada.Excedido.class,
                () -> LimitesEntrada.conferirPixels("imagem", LimitesEntrada.PIXELS_IMAGEM + 1, 1));
    }

    @Test
    void powersAreBoundedBeforeComputing() {
        var expr = new controlador.apoios.ProcessadorExprSimples();
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertThrows(Exception.class, () -> expr.processaExprInt("9^100000000"));
            assertThrows(Exception.class, () -> expr.processaExprInt("((9^20)^20)^20"));
        });
        assertDoesNotThrow(() -> assertEquals(1024, expr.processaExprInt("2^10")));
        assertDoesNotThrow(() -> assertEquals(400, expr.processaExprInt("(10+10)^2")));
    }
}
