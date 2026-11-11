package brmodelo;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import javax.management.BadAttributeValueExpException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import util.LeitorSeguro;
import util.ReadWitness;
import static org.junit.jupiter.api.Assertions.*;

class SafeReaderTest {
    // Guards rejection at descriptor resolution, before queue/comparator or proxy-handler callbacks.
    @ParameterizedTest
    @ValueSource(strings = {"queue", "exception", "proxy"})
    void rejectsBeforeConstruction(String kind) throws Exception {
        Object payload = switch (kind) {
            case "queue" -> {
                PriorityQueue<String> queue = new PriorityQueue<>(new ReadWitness());
                queue.add("Aluno"); queue.add("Curso");
                yield queue;
            }
            case "exception" -> new BadAttributeValueExpException("invented");
            case "proxy" -> Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Runnable.class}, new ReadWitness());
            default -> throw new AssertionError(kind);
        };
        String rejected = switch (kind) {
            case "queue" -> "java.util.PriorityQueue";
            case "exception" -> "javax.management.BadAttributeValueExpException";
            default -> "java.lang.reflect.Proxy";
        };
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(payload); }
        ReadWitness.reads = 0;
        List<String> resolved = new ArrayList<>();
        try (LeitorSeguro in = new LeitorSeguro(new ByteArrayInputStream(bytes.toByteArray())) {
            @Override protected Class<?> resolveClass(ObjectStreamClass descriptor)
                    throws IOException, ClassNotFoundException {
                Class<?> result = super.resolveClass(descriptor);
                resolved.add(descriptor.getName());
                return result;
            }
        }) {
            InvalidClassException failure = assertThrows(InvalidClassException.class, in::readObject);
            assertEquals(rejected, failure.classname);
            assertFalse(resolved.contains(rejected), "Disallowed class was never resolved");
            assertEquals(0, ReadWitness.reads, "No nested callback may run");
        }
    }

    // Guards the classes a file can actually instantiate. The allowlist admits whole packages,
    // but every serializable ancestor descriptor is resolved too, so a class extending a Swing
    // or other unlisted JDK type is unreachable. The reachable ones must stay plain data:
    // no deserialization callbacks, and no hashCode/equals/compareTo, which HashMap and
    // Hashtable call on their keys while reading. The listed JDK types keep their own
    // well-known readObject methods (Font, collections, Path2D); none of them runs model code.
    @Test
    void reachableClassesHaveNoReadCallbacks() throws Exception {
        Set<String> reachable = new TreeSet<>();
        List<String> problems = new ArrayList<>();
        for (String name : SerializedForm.classNames()) {
            Class<?> type = Class.forName(name, false, getClass().getClassLoader());
            if (!Serializable.class.isAssignableFrom(type) || !reachable(type)) continue;
            reachable.add(name);
            if (Externalizable.class.isAssignableFrom(type)) problems.add(name + " is Externalizable");
            if (ObjectInputValidation.class.isAssignableFrom(type)) problems.add(name + " validates objects");
            for (Method method : type.getDeclaredMethods()) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                if (Set.of("readObject[class java.io.ObjectInputStream]", "readObjectNoData[]", "readResolve[]",
                        "finalize[]", "hashCode[]", "equals[class java.lang.Object]").contains(signature)
                        || (method.getName().equals("compareTo") && !type.isEnum())) {
                    problems.add(name + "." + signature);
                }
            }
        }
        assertTrue(reachable.contains("controlador.Diagrama"), "Diagram model is reachable");
        assertTrue(reachable.contains("controlador.apoios.GuardaPadraoBrM"), "File envelope is reachable");
        assertFalse(reachable.contains("controlador.Acao"), "Swing-based classes are unreachable");
        assertEquals(List.of(), problems, "Review these before allowing them in files");
    }

    private static boolean reachable(Class<?> type) {
        for (Class<?> c = type; c != null && Serializable.class.isAssignableFrom(c); c = c.getSuperclass()) {
            if (!LeitorSeguro.permitido(c.getName())) return false;
        }
        return true;
    }

    // Guards acceptance of both envelope and nested model descriptors for all supported diagram types.
    @ParameterizedTest(name = "{0}")
    @MethodSource("brmodelo.Fixtures#names")
    void acceptsFixtures(String name) throws Exception {
        assertNotNull(Fixtures.load(name));
    }
}
