package util;

import controlador.apoios.GuardaPadraoBrM;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonGraphTest {
    public static class Base {
        static int calls;
        public Base() { calls++; }
    }
    public static class Parent extends Base implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int repeated = 1;
        transient int transientValue = 99;
    }
    public static class Node extends Parent {
        private static final long serialVersionUID = 1L;
        private final long repeated = Long.MIN_VALUE;
        ArrayList<Object> data;
        Node self;
        static int calls;
        public Node() { calls++; }
    }
    private byte[] encode(Object value) throws IOException {
        return FormatoBrMj.codificar(value, new GuardaPadraoBrM((byte[]) null));
    }
    private Object roundTrip(Object v) throws IOException { return FormatoBrMj.decodificar(encode(v)); }

    @Test void cyclesSharedReferencesFinalHiddenFieldsAndSerializationConstruction() throws Exception {
        Node n = new Node(); n.self = n;
        ArrayList<Object> list = new ArrayList<>(); list.add(n); list.add(list);
        HashMap<String, Object> map = new HashMap<>(); map.put("z", n); map.put("a", list);
        Hashtable<String, Object> table = new Hashtable<>(); table.put("b", list); table.put("a", n);
        n.data = new ArrayList<>(Arrays.asList(list, list, map, table, n));
        Base.calls = 0; Node.calls = 0;
        byte[] json = encode(n);
        String text = new String(json, StandardCharsets.UTF_8);
        assertTrue(text.contains(Parent.class.getName() + ".repeated"));
        assertTrue(text.contains(Node.class.getName() + ".repeated"));
        Node copy = (Node) FormatoBrMj.decodificar(json);
        assertEquals(1, Base.calls, "First non-serializable constructor must run");
        assertEquals(0, Node.calls, "Serializable constructor must not run");
        assertEquals(0, copy.transientValue);
        assertEquals(Long.MIN_VALUE, copy.repeated);
        assertSame(copy, copy.self);
        assertSame(copy.data.get(0), copy.data.get(1));
        var copiedList = (ArrayList<?>) copy.data.get(0);
        assertSame(copy, copiedList.get(0)); assertSame(copiedList, copiedList.get(1));
        assertSame(copy, ((Map<?, ?>) copy.data.get(2)).get("z"));
        assertSame(copy.data.get(0), ((Map<?, ?>) copy.data.get(3)).get("b"));
        assertArrayEquals(json, encode(copy));
    }

    @Test void rejectionPrecedesAnyModelConstruction() throws Exception {
        Node n = new Node(); n.self = n; n.data = new ArrayList<>(java.util.List.of(new Node()));
        String json = new String(encode(n), StandardCharsets.UTF_8);
        Base.calls = 0; Node.calls = 0;
        String invalid = json.replace("\"self\":", "\"unknown\":");
        assertTrue(assertThrows(IOException.class, () -> FormatoBrMj.decodificar(invalid.getBytes(StandardCharsets.UTF_8)))
                .getMessage().contains("Unknown field"));
        assertEquals(0, Base.calls); assertEquals(0, Node.calls);
        String denied = json.replace(Node.class.getName(), "java.util.PriorityQueue");
        assertThrows(InvalidClassException.class, () -> FormatoBrMj.decodificar(denied.getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, Base.calls); assertEquals(0, Node.calls);
        String dangling = json.replace("\"ref\": 1", "\"ref\": 999999");
        assertThrows(IOException.class, () -> FormatoBrMj.decodificar(dangling.getBytes(StandardCharsets.UTF_8)));
        assertEquals(0, Base.calls);
    }

    @Test void exactScalarsAndArrays() throws Exception {
        Object[] input = {Long.MAX_VALUE, Long.MIN_VALUE, Byte.MIN_VALUE, Short.MAX_VALUE, Integer.MIN_VALUE,
                Double.MIN_VALUE, Double.MAX_VALUE, Math.PI, -0.0d, Float.MIN_VALUE, Float.MAX_VALUE, -0.0f,
                Double.NaN, Float.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 'ç', "UTF-8 🚀", true, null,
                Node.class, int[].class, new Class<?>[]{Node.class, String.class}, TextAttribute.WEIGHT,
                new byte[]{0, 1, -1}, new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}, new boolean[]{true, false},
                new char[]{'\u0000', '\ud800'}, new long[]{Long.MIN_VALUE, Long.MAX_VALUE},
                new float[]{Float.MIN_VALUE, -0.0f}, new double[]{Double.MIN_VALUE, -0.0d},
                new short[]{Short.MIN_VALUE}, new byte[][]{{1}, {2}}};
        Object[] copy = ((java.util.List<?>) roundTrip(new ArrayList<>(Arrays.asList(input)))).toArray();
        assertTrue(Arrays.deepEquals(input, copy));
        assertEquals(Double.doubleToRawLongBits((Double) input[8]), Double.doubleToRawLongBits((Double) copy[8]));
        assertArrayEquals(encode(new ArrayList<>(Arrays.asList(input))), encode(new ArrayList<>(Arrays.asList(copy))));
    }

    @Test void awtValuesPreservePublicStateAndPathSegments() throws Exception {
        Font font = new Font("Dialog", Font.BOLD | Font.ITALIC, 12).deriveFont(12.5f)
                .deriveFont(Map.of(TextAttribute.TRACKING, 0.1f, TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        path.moveTo(-0.0, Double.MIN_VALUE); path.lineTo(1.5, 2.75);
        path.quadTo(3, 4, 5, 6); path.curveTo(7, 8, 9, 10, 11, 12); path.closePath();
        Object[] values = {new Color(10, 20, 30, 40), new Point(-3, 5), new Rectangle(1, 2, 3, 4),
                new Dimension(10, 20), font, new Cursor(Cursor.HAND_CURSOR),
                new Polygon(new int[]{1, 2, 3}, new int[]{4, 5, 6}, 3), path, new Path2D.Float(path), new GeneralPath(path),
                new Ellipse2D.Float(-0.0f, 2.5f, 3.75f, 4.5f), new Rectangle2D.Float(1, 2, 3, 4),
                new RoundRectangle2D.Float(1, 2, 3, 4, 5, 6)};
        Object[] copy = ((java.util.List<?>) roundTrip(new ArrayList<>(Arrays.asList(values)))).toArray();
        for (int i = 0; i < 5; i++) assertEquals(values[i], copy[i]);
        assertEquals(font.getAttributes(), ((Font) copy[4]).getAttributes());
        assertEquals(Cursor.HAND_CURSOR, ((Cursor) copy[5]).getType());
        assertEquals(Long.MIN_VALUE, Double.doubleToRawLongBits(firstX((Path2D) copy[7])));
        assertArrayEquals(encode(new ArrayList<>(Arrays.asList(values))), encode(new ArrayList<>(Arrays.asList(copy))));
    }
    private double firstX(Path2D path) { double[] coords = new double[6]; path.getPathIterator(null).currentSegment(coords); return coords[0]; }

    @Test void mapsAreOrderedBeforeIdsAreAssigned() throws Exception {
        HashMap<String, Object> a = new HashMap<>(), b = new HashMap<>();
        // These strings collide in HashMap, so insertion order would otherwise affect serialization.
        a.put("Aa", new Point(1, 2)); a.put("BB", new Point(3, 4));
        b.put("BB", new Point(3, 4)); b.put("Aa", new Point(1, 2));
        assertArrayEquals(encode(a), encode(b));
    }
    @Test void objectMapKeysAreSortedAndShared() throws Exception {
        HashMap<Point, Object> a = new HashMap<>(), b = new HashMap<>();
        a.put(new Point(1, 2), "first"); a.put(new Point(3, 4), "second");
        b.put(new Point(3, 4), "second"); b.put(new Point(1, 2), "first");
        assertArrayEquals(encode(a), encode(b));
        assertEquals(a, roundTrip(a));
        Node key = new Node(); key.self = key;
        HashMap<Node, Node> map = new HashMap<>(); map.put(key, key);
        var copy = (Map<?, ?>) roundTrip(map);
        Object copiedKey = copy.keySet().iterator().next();
        assertSame(copiedKey, copy.get(copiedKey));
        assertSame(copiedKey, ((Node) copiedKey).self);
    }
    @Test void collectionsAreRefusedAsMapKeysBeforeConstruction() throws Exception {
        // Hashing a list or map key walks its content; the reader refuses them while validating.
        HashMap<Object, Object> map = new HashMap<>();
        map.put(new ArrayList<>(Arrays.asList("a")), "valor");
        IOException erro = assertThrows(IOException.class, () -> roundTrip(map));
        assertEquals("Map key cannot be a collection: java.util.ArrayList", erro.getMessage());
        HashMap<Object, Object> aninhado = new HashMap<>();
        aninhado.put(new HashMap<>(Map.of("k", "v")), "valor");
        assertThrows(IOException.class, () -> roundTrip(aninhado));
    }

}
