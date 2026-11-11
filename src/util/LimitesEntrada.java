package util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Upper bounds for input that comes from outside the program: opened files, pasted text,
 * ready-made parts and recovery data. Every reader checks against these values, so the
 * limits live in one place. Each one is far above what a real diagram needs; measurements
 * are noted next to it.
 */
public final class LimitesEntrada {
    private LimitesEntrada() {}

    /**
     * XML text, in bytes for files and in characters for pasted text. The largest real
     * diagram measured (2,125 items, a 1.9 MB .brM3) exports 1.3 MB of XML. Parsed, the
     * worst case measured is about 21 times the input in memory (16 MiB of empty
     * elements took 340 MB), which still fits the default heap of a 4 GB machine.
     */
    public static final long XML_BYTES = 16L << 20;

    /**
     * Java-serialized files: .brM3 diagrams, the ready-made parts file (.brMt) and the
     * recovery file. The largest real diagram is a 1.9 MB .brM3 that takes about 6 MB
     * once read. Arrays and collection sizes declared inside are bounded separately by
     * LeitorSeguro, so a small file cannot claim a large allocation.
     */
    public static final long BINARIO_BYTES = 64L << 20;

    /**
     * .brMj diagrams. The format is indented JSON that repeats every Font, so it is about
     * 16 times the .brM3: the largest real diagram is 31.7 MB and needs 256 to 512 MB of
     * heap to read. Nesting depth is bounded by Json itself.
     */
    public static final long JSON_BYTES = 128L << 20;

    /**
     * Pixels of one image the program decodes or creates: pictures embedded in a diagram,
     * scaled copies cached for painting, and exported, copied or printed diagrams. The
     * largest real diagram measured spans 4,334 x 2,159 (9.4 Mpx); 64 Mpx (8,192 x 8,192)
     * takes 256 MB as ARGB, which still fits the default heap of a 4 GB machine.
     */
    public static final long PIXELS_IMAGEM = 1L << 26;

    /** Input refused for its size; the message names the input and both sizes. */
    public static final class Excedido extends IOException {
        Excedido(String origem, long tamanho, long limite) {
            super(origem + ": " + tamanho + " > " + limite);
        }
    }

    /** Checks a file's size before anything reads it. */
    public static void conferir(File arquivo, long limite) throws Excedido {
        long tamanho = arquivo.length();
        if (tamanho > limite) throw new Excedido(arquivo.getName(), tamanho, limite);
    }

    /**
     * Reads a whole file within the limit. The size is checked first and the read stops at
     * one byte past the limit, so a file that grows or is replaced in between (a shared
     * folder, for instance) is refused too.
     */
    public static byte[] ler(File arquivo, long limite) throws IOException {
        conferir(arquivo, limite);
        try (InputStream in = Files.newInputStream(arquivo.toPath())) {
            byte[] dados = in.readNBytes((int) Math.min(limite + 1, Integer.MAX_VALUE - 8));
            if (dados.length > limite) throw new Excedido(arquivo.getName(), dados.length, limite);
            return dados;
        }
    }

    /** Checks an image's dimensions before anything allocates it. */
    public static void conferirPixels(String origem, long largura, long altura) throws Excedido {
        if (largura * altura > PIXELS_IMAGEM) {
            throw new Excedido(origem + " " + largura + "x" + altura, largura * altura, PIXELS_IMAGEM);
        }
    }

    /** Log entry for refused input; the value says which limit and how large it was. */
    public static void recusar(String origem, long tamanho, long limite) {
        BrLogger.Logger("ERROR_INPUT_TOO_LARGE", " (" + origem + ")",
                tamanho + " > " + limite);
    }
}
