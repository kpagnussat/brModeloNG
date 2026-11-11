package util;

import controlador.Diagrama;
import controlador.apoios.GuardaPadraoBrM;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import sun.reflect.ReflectionFactory;

/** Versioned, allowlisted graph codec; no reflective access to JDK implementation fields. */
public final class FormatoBrMj {
    private FormatoBrMj() {}
    private static final String APP = Diagrama.VERSAO_A + "." + Diagrama.VERSAO_B + "." + Diagrama.VERSAO_C;

    public record Documento(Diagrama diagrama, GuardaPadraoBrM envelope) {
        /** Rebuilds the historical nested stream while retaining envelope metadata. */
        public GuardaPadraoBrM paraBrM3() {
            GuardaPadraoBrM result = new GuardaPadraoBrM(diagrama);
            result.versao = envelope.versao; result.versaoDiagrama = envelope.versaoDiagrama;
            result.autor = envelope.autor; result.data = envelope.data; result.Tag = envelope.Tag;
            return result;
        }
    }

    public static byte[] escrever(Diagrama diagram) throws IOException {
        GuardaPadraoBrM metadata = new GuardaPadraoBrM((byte[]) null);
        metadata.versaoDiagrama = APP; metadata.Tag = diagram.getVersao();
        return escrever(diagram, metadata);
    }

    public static byte[] escrever(Diagrama diagram, GuardaPadraoBrM envelope) throws IOException {
        return codificar(diagram, envelope);
    }

    public static void salvar(Path path, Diagrama diagram) throws IOException {
        // Encode completely before touching the destination.
        byte[] json = escrever(diagram);
        GravacaoAtomica.gravar(path, saida -> saida.write(json));
    }

    public static Documento ler(Path path) throws IOException {
        return ler(LimitesEntrada.ler(path.toFile(), LimitesEntrada.JSON_BYTES));
    }

    public static Documento ler(byte[] bytes) throws IOException {
        Map<String, Object> doc = documento(bytes);
        Reader reader = new Reader(doc);
        reader.validateRoot(doc.get("raiz"), Diagrama.class);
        Diagrama diagram = (Diagrama) reader.read(doc.get("raiz"));
        Map<String, Object> data = map(doc.get("envelope"));
        GuardaPadraoBrM envelope = new GuardaPadraoBrM((byte[]) null);
        envelope.versao = nullableString(data.get("versao"));
        envelope.versaoDiagrama = nullableString(data.get("versaoDiagrama"));
        envelope.autor = nullableString(data.get("autor")); envelope.data = nullableString(data.get("data"));
        envelope.Tag = nullableString(data.get("Tag"));
        return new Documento(diagram, envelope);
    }

    // Package-private entry points let tests exercise graphs beyond the shipped fixtures.
    static byte[] codificar(Object root, GuardaPadraoBrM envelope) throws IOException {
        try {
            Writer writer = new Writer();
            Object ref = writer.value(root);
            return Json.escrever(obj("formato", "brModelo-json", "versao", 1, "app", APP,
                    "envelope", obj("versao", envelope.versao, "versaoDiagrama", envelope.versaoDiagrama,
                            "autor", envelope.autor, "data", envelope.data, "Tag", envelope.Tag),
                    "raiz", ref, "objetos", writer.objects)).getBytes(StandardCharsets.UTF_8);
        } catch (ReflectiveOperationException | RuntimeException e) { throw failure(e); }
    }

    static Object decodificar(byte[] bytes) throws IOException {
        Map<String, Object> doc = documento(bytes);
        return new Reader(doc).read(doc.get("raiz"));
    }

    private static Map<String, Object> documento(byte[] bytes) throws IOException {
        // REPORT avoids silently replacing invalid UTF-8 in student documents.
        String text;
        try { text = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
        catch (java.nio.charset.CharacterCodingException e) { throw new IOException("Invalid brMj UTF-8", e); }
        Map<String, Object> doc = map(Json.ler(text));
        keys(doc, "formato", "versao", "app", "envelope", "raiz", "objetos");
        if (!"brModelo-json".equals(doc.get("formato")) || integer(doc.get("versao")) != 1)
            throw new IOException("Unsupported brModelo JSON format/version");
        string(doc.get("app"));
        Map<String, Object> envelope = map(doc.get("envelope"));
        keys(envelope, "versao", "versaoDiagrama", "autor", "data", "Tag");
        for (Object value : envelope.values()) nullableString(value);
        return doc;
    }

    private static IOException failure(Exception e) { return new IOException("Invalid brMj: " + e.getMessage(), e); }
    private static Map<String, Object> obj(Object... pairs) {
        Map<String, Object> result = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    private static Map<String, Object> map(Object value) throws IOException {
        if (!(value instanceof Map<?, ?> raw)) throw new IOException("Expected JSON object");
        Map<String, Object> result = new TreeMap<>();
        for (var e : raw.entrySet()) {
            if (!(e.getKey() instanceof String key)) throw new IOException("Expected string key");
            result.put(key, e.getValue());
        }
        return result;
    }
    private static List<?> list(Object value) throws IOException {
        if (!(value instanceof List<?> result)) throw new IOException("Expected JSON array"); return result;
    }
    private static String string(Object value) throws IOException {
        if (!(value instanceof String s)) throw new IOException("Expected string"); return s;
    }
    private static String nullableString(Object value) throws IOException { return value == null ? null : string(value); }
    private static void keys(Map<String, Object> value, String... names) throws IOException {
        if (!value.keySet().equals(new TreeSet<>(List.of(names))))
            throw new IOException("Unexpected/missing fields: " + value.keySet() + "; expected " + List.of(names));
    }
    private static int integer(Object value) throws IOException {
        try { return decimal(value).intValueExact(); }
        catch (ArithmeticException e) { throw new IOException("Expected exact int", e); }
    }
    private static BigDecimal decimal(Object value) throws IOException {
        if (!(value instanceof Number)) throw new IOException("Expected number");
        try { return new BigDecimal(value.toString()); }
        catch (NumberFormatException e) { throw new IOException("Invalid number", e); }
    }
    private static double real(Object value) throws IOException {
        double result = value instanceof Double d && d == 0 ? d : decimal(value).doubleValue();
        if (!Double.isFinite(result)) throw new IOException("Number exceeds double range"); return result;
    }
    private static Class<?> allowed(String name) throws IOException {
        if (!LeitorSeguro.permitido(name)) throw new InvalidClassException(name, "Class is not allowed in brModelo files");
        try { return Class.forName(name, false, FormatoBrMj.class.getClassLoader()); }
        catch (ClassNotFoundException e) { throw failure(e); }
    }
    private static Class<?> classValue(String name) throws IOException { return allowed(name); }
    private static boolean model(Class<?> type) {
        return !type.isArray() && !type.getName().startsWith("java.") && Serializable.class.isAssignableFrom(type)
                && !Enum.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers());
    }
    private static Map<String, Field> fields(Class<?> type) throws IOException {
        List<Field> all = new ArrayList<>(); Map<String, Integer> counts = new HashMap<>();
        for (Class<?> c = type; Serializable.class.isAssignableFrom(c); c = c.getSuperclass()) {
            allowed(c.getName());
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) continue;
                all.add(f); counts.merge(f.getName(), 1, Integer::sum);
            }
        }
        Map<String, Field> result = new TreeMap<>();
        for (Field f : all) {
            if (!f.trySetAccessible()) throw new IOException("Inaccessible serialized field: " + f);
            String key = counts.get(f.getName()) > 1 ? f.getDeclaringClass().getName() + "." + f.getName() : f.getName();
            result.put(key, f);
        }
        return result;
    }

    private static Object scalar(Object value) throws IOException {
        if (value == null || value instanceof String || value instanceof Boolean) return value;
        if (value instanceof Number n) {
            allowed(n.getClass().getName());
            Object number = n;
            if (n instanceof Float f && (!Float.isFinite(f) || Float.floatToRawIntBits(f) == 0x80000000)
                    || n instanceof Double d && (!Double.isFinite(d) || Double.doubleToRawLongBits(d) == Long.MIN_VALUE))
                number = n.toString();
            return obj("numero", n.getClass().getName(), "valor", number);
        }
        if (value instanceof Character c) return obj("caractere", c.toString());
        if (value instanceof Enum<?> e) {
            allowed(e.getDeclaringClass().getName()); return obj("enum", e.getDeclaringClass().getName(), "nome", e.name());
        }
        if (value instanceof Class<?> c) { classValue(c.getName()); return obj("class", c.getName()); }
        if (value instanceof TextAttribute a) {
            for (Field f : TextAttribute.class.getFields()) {
                try { if (f.getType() == TextAttribute.class && f.get(null) == a) return obj("atributoTexto", f.getName()); }
                catch (IllegalAccessException e) { throw failure(e); }
            }
            throw new IOException("Unknown TextAttribute");
        }
        return null;
    }

    private static final class Writer {
        final IdentityHashMap<Object, Integer> ids = new IdentityHashMap<>();
        final List<Object> objects = new ArrayList<>();
        Object value(Object value) throws IOException, ReflectiveOperationException { return value(value, null); }
        Object value(Object value, Class<?> declared) throws IOException, ReflectiveOperationException {
            if (value instanceof Number n && numeric(declared)
                    && !(n instanceof Float f && !Float.isFinite(f))
                    && !(n instanceof Double d && !Double.isFinite(d))) return n;
            Object scalar = scalar(value);
            if (value == null || scalar != null) return scalar;
            Integer known = ids.get(value); if (known != null) return obj("ref", known);
            Class<?> type = value.getClass(); allowed(type.getName());
            int id = objects.size() + 1; ids.put(value, id);
            Map<String, Object> record = obj("id", id, "classe", type.getName()); objects.add(record);
            if (value instanceof byte[] bytes) record.put("base64", Base64.getEncoder().encodeToString(bytes));
            else if (type.isArray()) {
                List<Object> elements = new ArrayList<>();
                for (int i = 0; i < Array.getLength(value); i++) elements.add(value(Array.get(value, i), type.getComponentType()));
                record.put("itens", elements);
            } else if (type == ArrayList.class) {
                List<Object> elements = new ArrayList<>();
                for (Object element : (List<?>) value) elements.add(value(element)); record.put("itens", elements);
            } else if (type == HashMap.class || type == Hashtable.class) {
                List<Map.Entry<?, ?>> entries = new ArrayList<>(((Map<?, ?>) value).entrySet());
                // Sort before assigning ids, so hash iteration order never influences the graph.
                Map<Object, String> order = new IdentityHashMap<>();
                for (var e : entries) {
                    Object key = scalar(e.getKey());
                    String sortedKey = key != null || e.getKey() == null ? Json.escrever(key)
                            : Json.escrever(sortShape(e.getKey(), new IdentityHashMap<>()));
                    order.put(e.getKey(), sortedKey);
                }
                // Structurally equal object keys use their values and already visited ids as tie breakers.
                Map<Map.Entry<?, ?>, String> ties = new IdentityHashMap<>();
                Map<String, Integer> counts = new HashMap<>();
                for (var e : entries) counts.merge(order.get(e.getKey()), 1, Integer::sum);
                for (var e : entries) {
                    if (counts.get(order.get(e.getKey())) > 1)
                        ties.put(e, Json.escrever(sortShape(e.getValue(), new IdentityHashMap<>()))
                                + ":" + ids.getOrDefault(e.getKey(), 0));
                }
                entries.sort(Comparator.<Map.Entry<?, ?>, String>comparing(e -> order.get(e.getKey()))
                        .thenComparing(e -> ties.getOrDefault(e, "")));
                List<Object> pairs = new ArrayList<>();
                for (var e : entries) pairs.add(obj("chave", value(e.getKey()), "valor", value(e.getValue())));
                record.put("entradas", pairs);
            } else {
                Object data = awt(value);
                if (data != null) record.put("valor", data);
                else {
                    if (!model(type)) throw new IOException("Unsupported serialized class: " + type.getName());
                    Map<String, Object> dataFields = new TreeMap<>();
                    for (var e : fields(type).entrySet()) dataFields.put(e.getKey(), value(e.getValue().get(value), e.getValue().getType()));
                    record.put("campos", dataFields);
                }
            }
            return obj("ref", id);
        }
        Object sortShape(Object v, IdentityHashMap<Object, Integer> seen) throws IOException, ReflectiveOperationException {
            Object simple = scalar(v);
            if (v == null || simple != null) return simple;
            if (seen.containsKey(v)) return obj("ciclo", seen.get(v));
            seen.put(v, seen.size() + 1);
            Class<?> type = v.getClass(); allowed(type.getName());
            Object data;
            if (type.isArray() || type == ArrayList.class) {
                List<Object> values = new ArrayList<>();
                int size = type.isArray() ? Array.getLength(v) : ((List<?>) v).size();
                for (int i = 0; i < size; i++) values.add(sortShape(type.isArray() ? Array.get(v, i) : ((List<?>) v).get(i), seen));
                data = values;
            } else if (type == HashMap.class || type == Hashtable.class) {
                List<String> pairs = new ArrayList<>();
                for (var entry : ((Map<?, ?>) v).entrySet()) {
                    IdentityHashMap<Object, Integer> branch = new IdentityHashMap<>(seen);
                    pairs.add(Json.escrever(obj("chave", sortShape(entry.getKey(), branch), "valor", sortShape(entry.getValue(), branch))));
                }
                Collections.sort(pairs); data = pairs;
            } else if (model(type)) {
                Map<String, Object> values = new TreeMap<>();
                for (var entry : fields(type).entrySet()) values.put(entry.getKey(), sortShape(entry.getValue().get(v), seen));
                data = values;
            } else {
                Writer adapter = new Writer(); Object ref = adapter.value(v);
                data = obj("raiz", ref, "objetos", adapter.objects);
            }
            return obj("classe", type.getName(), "valor", data);
        }
        Object awt(Object v) throws IOException, ReflectiveOperationException {
            if (v instanceof Color c) return obj("rgba", c.getRGB());
            if (v instanceof Rectangle r) return obj("x", r.x, "y", r.y, "width", r.width, "height", r.height);
            if (v instanceof Point p) return obj("x", p.x, "y", p.y);
            if (v instanceof Dimension d) return obj("width", d.width, "height", d.height);
            if (v instanceof Cursor c) return obj("tipo", c.getType());
            if (v instanceof Font f) {
                List<Object> attrs = new ArrayList<>();
                List<Map.Entry<TextAttribute, ?>> entries = new ArrayList<>(f.getAttributes().entrySet());
                entries.sort(Comparator.comparing(e -> e.getKey().toString()));
                for (var e : entries) attrs.add(obj("chave", value(e.getKey()), "valor", value(e.getValue())));
                return obj("nome", f.getName(), "estilo", f.getStyle(), "tamanho", f.getSize2D(), "atributos", attrs);
            }
            if (v instanceof Polygon p) {
                List<Object> points = new ArrayList<>();
                for (int i = 0; i < p.npoints; i++) points.add(List.of(p.xpoints[i], p.ypoints[i]));
                return obj("pontos", points);
            }
            if (v instanceof Path2D path) {
                List<Object> segments = new ArrayList<>(); double[] coords = new double[6];
                for (PathIterator it = path.getPathIterator(null); !it.isDone(); it.next()) {
                    int kind = it.currentSegment(coords); List<Object> segment = new ArrayList<>(); segment.add(kind);
                    for (int i = 0; i < segmentSize(kind); i++) segment.add(coords[i]); segments.add(segment);
                }
                return obj("regra", path.getWindingRule(), "segmentos", segments);
            }
            if (v instanceof RoundRectangle2D.Float r)
                return obj("x", r.x, "y", r.y, "width", r.width, "height", r.height, "arcwidth", r.arcwidth, "archeight", r.archeight);
            if (v instanceof Rectangle2D.Float r) return obj("x", r.x, "y", r.y, "width", r.width, "height", r.height);
            if (v instanceof Ellipse2D.Float r) return obj("x", r.x, "y", r.y, "width", r.width, "height", r.height);
            return null;
        }
    }

    private static int segmentSize(int kind) throws IOException {
        return switch (kind) {
            case PathIterator.SEG_MOVETO, PathIterator.SEG_LINETO -> 2;
            case PathIterator.SEG_QUADTO -> 4; case PathIterator.SEG_CUBICTO -> 6; case PathIterator.SEG_CLOSE -> 0;
            default -> throw new IOException("Unknown path segment: " + kind);
        };
    }

    private static final class Reader {
        final Map<Integer, Map<String, Object>> records = new TreeMap<>();
        final Map<Integer, Class<?>> types = new TreeMap<>();
        final Map<Integer, Object> instances = new HashMap<>();
        final Set<Integer> building = new HashSet<>();
        Reader(Map<String, Object> doc) throws IOException {
            try {
                for (Object raw : list(doc.get("objetos"))) {
                    Map<String, Object> record = map(raw); int id = integer(record.get("id"));
                    if (id < 1 || records.putIfAbsent(id, record) != null) throw new IOException("Duplicate/invalid object id: " + id);
                    types.put(id, allowed(string(record.get("classe"))));
                }
                // Validate every record and reference before allocating any model instance.
                for (var e : records.entrySet()) validateRecord(e.getValue(), types.get(e.getKey()));
                valueType(doc.get("raiz"));
            } catch (ReflectiveOperationException | RuntimeException e) { throw failure(e); }
        }
        void validateRoot(Object root, Class<?> expected) throws IOException {
            try { compatible(valueType(root), expected); }
            catch (ReflectiveOperationException e) { throw failure(e); }
            if (root == null) throw new IOException("Empty diagram root");
        }
        void validateRecord(Map<String, Object> r, Class<?> type) throws IOException, ReflectiveOperationException {
            if (type == byte[].class) {
                keys(r, "id", "classe", "base64"); Base64.getDecoder().decode(string(r.get("base64")));
            } else if (type.isArray() || type == ArrayList.class) {
                keys(r, "id", "classe", "itens");
                for (Object v : list(r.get("itens"))) {
                    if (type.isArray()) validateTyped(v, type.getComponentType());
                    else valueType(v);
                }
            } else if (type == HashMap.class || type == Hashtable.class) {
                keys(r, "id", "classe", "entradas"); Set<String> seen = new HashSet<>();
                for (Object p : list(r.get("entradas"))) {
                    Map<String, Object> pair = map(p); keys(pair, "chave", "valor");
                    Object key = pair.get("chave");
                    // Lists and maps hash their whole content, recursively, when used as a key;
                    // every other allowed key hashes in constant time. Diagrams only use text keys.
                    Class<?> keyType = valueType(key); valueType(pair.get("valor"));
                    if (keyType != null && (Collection.class.isAssignableFrom(keyType) || Map.class.isAssignableFrom(keyType)))
                        throw new IOException("Map key cannot be a collection: " + keyType.getName());
                    if (!seen.add(Json.escrever(key))) throw new IOException("Duplicate map key");
                    if (type == Hashtable.class && (key == null || pair.get("valor") == null)) throw new IOException("Hashtable cannot contain null");
                }
            } else if (model(type)) {
                keys(r, "id", "classe", "campos");
                Map<String, Field> fields = fields(type); Map<String, Object> data = map(r.get("campos"));
                for (String key : data.keySet()) {
                    if (!fields.containsKey(key)) throw new IOException("Unknown field: " + type.getName() + "." + key);
                }
                if (!fields.keySet().equals(data.keySet())) throw new IOException("Missing fields for " + type.getName());
                for (var e : fields.entrySet()) validateTyped(data.get(e.getKey()), e.getValue().getType());
            } else {
                keys(r, "id", "classe", "valor"); validateAwt(type, map(r.get("valor")));
            }
        }
        Class<?> valueType(Object value) throws IOException, ReflectiveOperationException {
            if (value == null) return null;
            if (value instanceof String) return String.class;
            if (value instanceof Boolean) return Boolean.class;
            if (value instanceof Number) { integer(value); return Integer.class; }
            Map<String, Object> tag = map(value);
            if (tag.containsKey("ref")) {
                keys(tag, "ref"); int id = integer(tag.get("ref")); Class<?> type = types.get(id);
                if (type == null) throw new IOException("Dangling reference: " + id); return type;
            }
            Object scalar = decodeScalar(tag); return scalar.getClass();
        }
        void validateTyped(Object raw, Class<?> expected) throws IOException, ReflectiveOperationException {
            if (raw instanceof Number && numeric(expected)) convertNumber(raw, expected);
            else compatible(valueType(raw), expected);
        }
        Object value(Object raw, Class<?> expected) throws IOException, ReflectiveOperationException {
            return raw instanceof Number && numeric(expected) ? convertNumber(raw, expected) : value(raw);
        }
        void compatible(Class<?> actual, Class<?> expected) throws IOException {
            Class<?> boxed = expected.isPrimitive() ? switch (expected.getName()) {
                case "boolean" -> Boolean.class; case "byte" -> Byte.class; case "short" -> Short.class;
                case "int" -> Integer.class; case "long" -> Long.class; case "float" -> Float.class;
                case "double" -> Double.class; case "char" -> Character.class;
                default -> throw new IOException("Invalid primitive type");
            } : expected;
            if (actual == null ? expected.isPrimitive() : !boxed.isAssignableFrom(actual))
                throw new IOException("Value type " + actual + " does not match " + expected.getName());
        }
        void validateAwt(Class<?> type, Map<String, Object> d) throws IOException, ReflectiveOperationException {
            switch (type.getName()) {
                case "java.awt.Color" -> { keys(d, "rgba"); integer(d.get("rgba")); }
                case "java.awt.Point" -> { keys(d, "x", "y"); ints(d); }
                case "java.awt.Dimension" -> { keys(d, "width", "height"); ints(d); }
                case "java.awt.Rectangle" -> { keys(d, "x", "y", "width", "height"); ints(d); }
                case "java.awt.Cursor" -> {
                    keys(d, "tipo"); int kind = integer(d.get("tipo"));
                    if (kind < 0 || kind > Cursor.MOVE_CURSOR) throw new IOException("Unsupported cursor type: " + kind);
                }
                case "java.awt.Font" -> {
                    keys(d, "nome", "estilo", "tamanho", "atributos"); string(d.get("nome"));
                    int style = integer(d.get("estilo")); if (style < 0 || style > 3) throw new IOException("Invalid font style");
                    finiteFloat(d.get("tamanho"));
                    Set<String> seen = new HashSet<>();
                    for (Object raw : list(d.get("atributos"))) {
                        Map<String, Object> p = map(raw); keys(p, "chave", "valor");
                        compatible(valueType(p.get("chave")), TextAttribute.class); valueType(p.get("valor"));
                        if (!seen.add(Json.escrever(p.get("chave")))) throw new IOException("Duplicate font attribute");
                    }
                }
                case "java.awt.Polygon" -> {
                    keys(d, "pontos");
                    for (Object raw : list(d.get("pontos"))) {
                        List<?> point = list(raw); if (point.size() != 2) throw new IOException("Invalid polygon point");
                        integer(point.get(0)); integer(point.get(1));
                    }
                }
                case "java.awt.geom.Path2D$Float", "java.awt.geom.Path2D$Double", "java.awt.geom.GeneralPath" -> {
                    keys(d, "regra", "segmentos"); int rule = integer(d.get("regra"));
                    if (rule != Path2D.WIND_EVEN_ODD && rule != Path2D.WIND_NON_ZERO) throw new IOException("Invalid winding rule");
                    boolean moved = false;
                    for (Object raw : list(d.get("segmentos"))) {
                        List<?> s = list(raw); if (s.isEmpty()) throw new IOException("Empty path segment");
                        int kind = integer(s.get(0));
                        if (s.size() != segmentSize(kind) + 1) throw new IOException("Invalid path coordinates");
                        if (!moved && kind != PathIterator.SEG_MOVETO) throw new IOException("Path must start with moveTo");
                        moved = true;
                        for (int i = 1; i < s.size(); i++) {
                            if (type == Path2D.Double.class) real(s.get(i)); else finiteFloat(s.get(i));
                        }
                    }
                }
                case "java.awt.geom.Ellipse2D$Float", "java.awt.geom.Rectangle2D$Float" -> {
                    keys(d, "x", "y", "width", "height"); floats(d);
                }
                case "java.awt.geom.RoundRectangle2D$Float" -> {
                    keys(d, "x", "y", "width", "height", "arcwidth", "archeight"); floats(d);
                }
                default -> throw new IOException("Unsupported serialized class: " + type.getName());
            }
        }
        Object read(Object root) throws IOException {
            try {
                // Allocate models and containers first: final fields, aliases and cycles work as in serialization.
                for (var e : types.entrySet()) {
                    int id = e.getKey(); Class<?> type = e.getValue(); Map<String, Object> r = records.get(id);
                    Object instance = null;
                    if (model(type)) {
                        Class<?> base = type;
                        while (Serializable.class.isAssignableFrom(base)) base = base.getSuperclass();
                        Constructor<?> ctor = ReflectionFactory.getReflectionFactory()
                                .newConstructorForSerialization(type, base.getDeclaredConstructor());
                        ctor.setAccessible(true); instance = ctor.newInstance();
                    } else if (type == ArrayList.class) instance = new ArrayList<>();
                    else if (type == HashMap.class) instance = new HashMap<>();
                    else if (type == Hashtable.class) instance = new Hashtable<>();
                    else if (type == byte[].class) instance = Base64.getDecoder().decode(string(r.get("base64")));
                    else if (type.isArray()) instance = Array.newInstance(type.getComponentType(), list(r.get("itens")).size());
                    if (instance != null) instances.put(id, instance);
                }
                for (var e : types.entrySet()) {
                    int id = e.getKey(); Class<?> type = e.getValue(); Map<String, Object> r = records.get(id);
                    Object instance = instances.get(id);
                    if (model(type)) {
                        Map<String, Object> data = map(r.get("campos"));
                        for (var f : fields(type).entrySet()) f.getValue().set(instance, value(data.get(f.getKey()), f.getValue().getType()));
                    } else if (type.isArray() && type != byte[].class) {
                        List<?> data = list(r.get("itens"));
                        for (int i = 0; i < data.size(); i++) Array.set(instance, i, value(data.get(i), type.getComponentType()));
                    } else if (type == ArrayList.class) {
                        @SuppressWarnings("unchecked") List<Object> target = (List<Object>) instance;
                        for (Object v : list(r.get("itens"))) target.add(value(v));
                    }
                }
                // Populate maps after model fields, so value-based keys see their restored state.
                for (var e : types.entrySet()) {
                    if (e.getValue() != HashMap.class && e.getValue() != Hashtable.class) continue;
                    @SuppressWarnings("unchecked") Map<Object, Object> target = (Map<Object, Object>) instances.get(e.getKey());
                    for (Object raw : list(records.get(e.getKey()).get("entradas"))) {
                        Map<String, Object> p = map(raw); Object key = value(p.get("chave"));
                        if (target.containsKey(key)) throw new IOException("Duplicate map key");
                        target.put(key, value(p.get("valor")));
                    }
                }
                return value(root);
            } catch (ReflectiveOperationException | RuntimeException e) { throw failure(e); }
        }
        Object value(Object raw) throws IOException, ReflectiveOperationException {
            if (raw == null || raw instanceof String || raw instanceof Boolean) return raw;
            if (raw instanceof Number) return integer(raw);
            Map<String, Object> tag = map(raw);
            if (!tag.containsKey("ref")) return decodeScalar(tag);
            int id = integer(tag.get("ref"));
            if (instances.containsKey(id)) return instances.get(id);
            if (!building.add(id)) throw new IOException("Cycle between AWT values");
            Object v = createAwt(types.get(id), map(records.get(id).get("valor")));
            instances.put(id, v); building.remove(id); return v;
        }
        Object createAwt(Class<?> type, Map<String, Object> d) throws IOException, ReflectiveOperationException {
            return switch (type.getName()) {
                case "java.awt.Color" -> new Color(integer(d.get("rgba")), true);
                case "java.awt.Point" -> new Point(integer(d.get("x")), integer(d.get("y")));
                case "java.awt.Dimension" -> new Dimension(integer(d.get("width")), integer(d.get("height")));
                case "java.awt.Rectangle" -> new Rectangle(integer(d.get("x")), integer(d.get("y")), integer(d.get("width")), integer(d.get("height")));
                case "java.awt.Cursor" -> new Cursor(integer(d.get("tipo")));
                case "java.awt.Font" -> {
                    Map<TextAttribute, Object> attrs = new HashMap<>();
                    for (Object raw : list(d.get("atributos"))) {
                        Map<String, Object> p = map(raw); attrs.put((TextAttribute) value(p.get("chave")), value(p.get("valor")));
                    }
                    Font font = new Font(string(d.get("nome")), integer(d.get("estilo")), 1).deriveFont(finiteFloat(d.get("tamanho")));
                    yield font.deriveFont(attrs);
                }
                case "java.awt.Polygon" -> {
                    Polygon p = new Polygon();
                    for (Object raw : list(d.get("pontos"))) {
                        List<?> point = list(raw); p.addPoint(integer(point.get(0)), integer(point.get(1)));
                    }
                    yield p;
                }
                case "java.awt.geom.Path2D$Float", "java.awt.geom.Path2D$Double", "java.awt.geom.GeneralPath" -> {
                    int rule = integer(d.get("regra"));
                    Path2D path = type == Path2D.Double.class ? new Path2D.Double(rule)
                            : type == GeneralPath.class ? new GeneralPath(rule) : new Path2D.Float(rule);
                    for (Object raw : list(d.get("segmentos"))) {
                        List<?> s = list(raw); int kind = integer(s.get(0)); double[] c = new double[6];
                        for (int i = 1; i < s.size(); i++) c[i - 1] = real(s.get(i));
                        switch (kind) {
                            case PathIterator.SEG_MOVETO -> path.moveTo(c[0], c[1]);
                            case PathIterator.SEG_LINETO -> path.lineTo(c[0], c[1]);
                            case PathIterator.SEG_QUADTO -> path.quadTo(c[0], c[1], c[2], c[3]);
                            case PathIterator.SEG_CUBICTO -> path.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]);
                            case PathIterator.SEG_CLOSE -> path.closePath();
                            default -> throw new IOException("Unknown path segment");
                        }
                    }
                    yield path;
                }
                case "java.awt.geom.Ellipse2D$Float" -> new Ellipse2D.Float(finiteFloat(d.get("x")), finiteFloat(d.get("y")), finiteFloat(d.get("width")), finiteFloat(d.get("height")));
                case "java.awt.geom.Rectangle2D$Float" -> new Rectangle2D.Float(finiteFloat(d.get("x")), finiteFloat(d.get("y")), finiteFloat(d.get("width")), finiteFloat(d.get("height")));
                case "java.awt.geom.RoundRectangle2D$Float" -> new RoundRectangle2D.Float(finiteFloat(d.get("x")), finiteFloat(d.get("y")), finiteFloat(d.get("width")), finiteFloat(d.get("height")), finiteFloat(d.get("arcwidth")), finiteFloat(d.get("archeight")));
                default -> throw new IOException("Unsupported AWT value: " + type.getName());
            };
        }
    }
    private static void ints(Map<String, Object> d) throws IOException { for (Object v : d.values()) integer(v); }
    private static void floats(Map<String, Object> d) throws IOException { for (Object v : d.values()) finiteFloat(v); }
    private static float finiteFloat(Object value) throws IOException {
        float f = (float) real(value); if (!Float.isFinite(f)) throw new IOException("Number exceeds float range"); return f;
    }
    private static boolean numeric(Class<?> type) {
        return type != null && List.of(byte.class, short.class, int.class, long.class, float.class, double.class,
                Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class).contains(type);
    }
    private static Object convertNumber(Object raw, Class<?> type) throws IOException, ReflectiveOperationException {
        String name = type.isPrimitive() ? switch (type.getName()) {
            case "byte" -> "java.lang.Byte"; case "short" -> "java.lang.Short"; case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long"; case "float" -> "java.lang.Float"; case "double" -> "java.lang.Double";
            default -> throw new IOException("Not a numeric field");
        } : type.getName();
        if (raw instanceof Double d && Double.doubleToRawLongBits(d) == Long.MIN_VALUE
                && (type == float.class || type == Float.class || type == double.class || type == Double.class))
            return decodeScalar(obj("numero", name, "valor", "-0.0"));
        return decodeScalar(obj("numero", name, "valor", raw));
    }
    private static Object decodeScalar(Map<String, Object> tag) throws IOException, ReflectiveOperationException {
        if (tag.containsKey("numero")) {
            keys(tag, "numero", "valor"); String kind = string(tag.get("numero")); allowed(kind);
            Object value = tag.get("valor");
            if (value instanceof String special) {
                if (!List.of("NaN", "Infinity", "-Infinity", "-0.0").contains(special)) throw new IOException("Invalid special number");
                if (kind.equals("java.lang.Float")) return Float.valueOf(special);
                if (kind.equals("java.lang.Double")) return Double.valueOf(special);
                throw new IOException("Special number requires float/double");
            }
            BigDecimal n = decimal(value);
            try {
                return switch (kind) {
                    case "java.lang.Byte" -> n.byteValueExact(); case "java.lang.Short" -> n.shortValueExact();
                    case "java.lang.Integer" -> n.intValueExact(); case "java.lang.Long" -> n.longValueExact();
                    case "java.lang.Float" -> finiteFloat(n); case "java.lang.Double" -> real(n);
                    default -> throw new IOException("Unsupported numeric type: " + kind);
                };
            } catch (ArithmeticException e) { throw new IOException("Number outside " + kind + " range", e); }
        }
        if (tag.containsKey("caractere")) {
            keys(tag, "caractere"); String s = string(tag.get("caractere"));
            if (s.length() != 1) throw new IOException("Expected one UTF-16 character"); return s.charAt(0);
        }
        if (tag.containsKey("class")) { keys(tag, "class"); return classValue(string(tag.get("class"))); }
        if (tag.containsKey("enum")) {
            keys(tag, "enum", "nome"); Class<?> type = allowed(string(tag.get("enum")));
            if (!type.isEnum()) throw new IOException("Expected enum class");
            String name = string(tag.get("nome"));
            for (Object constant : type.getEnumConstants()) if (((Enum<?>) constant).name().equals(name)) return constant;
            throw new IOException("Unknown enum constant: " + type.getName() + "." + name);
        }
        if (tag.containsKey("atributoTexto")) {
            keys(tag, "atributoTexto");
            Field f = TextAttribute.class.getField(string(tag.get("atributoTexto")));
            if (f.getType() != TextAttribute.class || !Modifier.isStatic(f.getModifiers())) throw new IOException("Unknown TextAttribute");
            return f.get(null);
        }
        throw new IOException("Unknown value tag: " + tag.keySet());
    }
}
