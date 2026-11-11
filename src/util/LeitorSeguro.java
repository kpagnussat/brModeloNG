package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads the existing Java serialization format without accepting arbitrary classes.
 * Diagrams are exchanged between users, so every descriptor (including array components)
 * is checked before class resolution and object construction. Only brModelo's packages
 * and the JDK value types stored by the model are accepted; dynamic proxies are never
 * part of a diagram. Sizes declared in the stream are bounded too (see the constructor).
 * This keeps the original file format.
 */
public class LeitorSeguro extends ObjectInputStream {

    private static final Set<String> PACOTES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("controlador", "desenho", "diagramas", "helper", "partepronta",
                    "principal", "util")));

    private static final Set<String> CLASSES_JDK = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(
                    // Scalar values, their serializable bases, and the model's Class[] registry.
                    "java.lang.String", "java.lang.Boolean", "java.lang.Byte",
                    "java.lang.Short", "java.lang.Integer", "java.lang.Long",
                    "java.lang.Float", "java.lang.Double", "java.lang.Character",
                    "java.lang.Number", "java.lang.Enum", "java.lang.Class",
                    // Model lists/maps and Font's serialized attribute table.
                    "java.util.ArrayList", "java.util.HashMap", "java.util.Hashtable",
                    // Values observed in real files, including Font's attribute hierarchy.
                    "java.awt.Color", "java.awt.Font", "java.awt.Point",
                    "java.awt.Rectangle", "java.awt.Dimension", "java.awt.Cursor",
                    "java.awt.font.TextAttribute", "java.text.AttributedCharacterIterator$Attribute",
                    // Stored regions/arrows; GeneralPath also serializes its Path2D.Float base.
                    "java.awt.Polygon", "java.awt.geom.Path2D$Double",
                    "java.awt.geom.Path2D$Float", "java.awt.geom.GeneralPath",
                    "java.awt.geom.Ellipse2D$Float", "java.awt.geom.Rectangle2D$Float",
                    "java.awt.geom.RoundRectangle2D$Float")));

    /** Declared sizes up to this many elements are always accepted. */
    static final int MINIMO_ELEMENTOS = 1 << 20;

    public LeitorSeguro(InputStream entrada) throws IOException {
        super(entrada);
        // The reader allocates an array, and the table of an ArrayList, HashMap or Hashtable,
        // at the size the stream declares, before reading the elements. Every element takes
        // at least one byte of input, so a size beyond what is left to read can only make a
        // small file claim a large allocation. The floor covers sources that report no size
        // and the spare capacity of hash tables.
        long limite = Math.max(entrada.available(), MINIMO_ELEMENTOS);
        setObjectInputFilter(info -> info.arrayLength() > limite
                ? ObjectInputFilter.Status.REJECTED : ObjectInputFilter.Status.UNDECIDED);
    }

    /** Shared name-only policy for binary and JSON diagrams; call before class resolution. */
    public static boolean permitido(String nome) {
        if (nome.startsWith("[")) {
            int i = 0;
            while (i < nome.length() && nome.charAt(i) == '[') {
                i++;
            }
            if (i == nome.length()) {
                return false;
            }
            if (nome.charAt(i) == 'L' && nome.endsWith(";")) {
                return permitido(nome.substring(i + 1, nome.length() - 1));
            }
            return i == nome.length() - 1 && "ZBCSIJFD".indexOf(nome.charAt(i)) >= 0;
        }
        int ponto = nome.indexOf('.');
        return CLASSES_JDK.contains(nome)
                || (ponto > 0 && PACOTES.contains(nome.substring(0, ponto)));
    }

    private static InvalidClassException rejeitado(String nome) {
        BrLogger.Logger("ERROR_UNSAFE_CLASS", nome);
        return new InvalidClassException(nome, "Class is not allowed in brModelo files");
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass descritor)
            throws IOException, ClassNotFoundException {
        // Check the name before asking the class loader to resolve it.
        if (!permitido(descritor.getName())) {
            throw rejeitado(descritor.getName());
        }
        return super.resolveClass(descritor);
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException {
        throw rejeitado("java.lang.reflect.Proxy");
    }
}
