package brmodelo;

import java.io.*;
import java.util.HashMap;
import util.LeitorSeguro;

/**
 * Child-JVM probe for LimitesEntradaTest: a stream of a few dozen bytes whose array, or
 * whose HashMap, declares about two billion elements. Without a bound the reader allocates
 * that up front and runs out of memory; the parent runs this with a small heap.
 */
public final class SondaLeitor {
    public static void main(String[] args) throws Exception {
        // An empty array ends with its 4-byte length.
        byte[] array = serializar(new byte[0]);
        escreverInt(array, array.length - 4, Integer.MAX_VALUE - 8);
        ler("array", array);
        // An empty HashMap ends with its custom data: buckets, mappings, TC_ENDBLOCKDATA.
        byte[] mapa = serializar(new HashMap<String, String>());
        escreverInt(mapa, mapa.length - 5, 1 << 30);
        ler("hashmap", mapa);
        System.out.println("fim");
    }

    private static void ler(String nome, byte[] bytes) {
        try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(bytes))) {
            in.readObject();
            System.out.println(nome + ": lido");
        } catch (InvalidClassException e) {
            System.out.println(nome + ": recusado " + e.getMessage());
        } catch (Throwable e) {
            System.out.println(nome + ": " + e);
        }
    }

    private static byte[] serializar(Object o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(o); }
        return bytes.toByteArray();
    }

    private static void escreverInt(byte[] b, int pos, int v) {
        b[pos] = (byte) (v >>> 24); b[pos + 1] = (byte) (v >>> 16); b[pos + 2] = (byte) (v >>> 8); b[pos + 3] = (byte) v;
    }
}
