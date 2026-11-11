package brmodelo;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Inventories compiled main classes, including nested and anonymous classes. */
public final class SerializedForm {
    private SerializedForm() {}

    /** Names of all compiled main classes, from the directories the build passes in. */
    static TreeSet<String> classNames() throws Exception {
        TreeSet<String> names = new TreeSet<>();
        String roots = System.getProperty("serializedForm.classes");
        if (roots == null || roots.isBlank()) throw new IllegalStateException("Missing main class directories");
        for (String root : roots.split(Pattern.quote(java.io.File.pathSeparator))) {
            Path directory = Path.of(root);
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.filter(p -> p.toString().endsWith(".class")).forEach(p ->
                        names.add(directory.relativize(p).toString()
                                .replace(java.io.File.separatorChar, '.').replaceFirst("\\.class$", "")));
            }
        }
        if (names.isEmpty()) throw new IllegalStateException("No compiled main classes found");
        return names;
    }

    static String dump() throws Exception {
        StringBuilder out = new StringBuilder();
        for (String name : classNames()) {
            Class<?> type = Class.forName(name, false, SerializedForm.class.getClassLoader());
            if (!Serializable.class.isAssignableFrom(type)) continue;
            out.append("class ").append(name).append('\n');
            for (Class<?> ancestor = type; ancestor != null; ancestor = ancestor.getSuperclass()) {
                ObjectStreamClass descriptor = ObjectStreamClass.lookup(ancestor);
                out.append("  ").append(ancestor.getName());
                if (descriptor == null) {
                    out.append(" non-serializable\n");
                    continue;
                }
                out.append(" uid=").append(descriptor.getSerialVersionUID()).append('\n');
                for (ObjectStreamField field : descriptor.getFields()) {
                    out.append("    ").append(field.getName()).append(' ')
                            .append(field.isPrimitive() ? String.valueOf(field.getTypeCode()) : field.getTypeString())
                            .append('\n');
                }
            }
        }
        return out.toString();
    }

    /** Explicit baseline writer; tests never update the golden file. */
    public static void main(String[] args) throws Exception {
        Path golden = Path.of("test-resources/serialized-form.txt");
        Files.writeString(golden, dump());
        System.out.println("Wrote " + golden);
    }
}
