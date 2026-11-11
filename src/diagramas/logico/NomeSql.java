package diagramas.logico;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Text from the diagram that goes into the generated SQL script. A diagram received from
 * someone else can carry names such as {@code x; DROP TABLE y; --}, which would otherwise
 * become statements when the script is run.
 *
 * Names made of letters (accented ones included), digits and underscores are written as
 * they are, so the script stays the same for every usual diagram and keeps working in
 * MySQL's default mode and with PostgreSQL's case folding. Any other name is delimited
 * with double quotes, which turns it into a single identifier.
 *
 * Field types and complements stay free text on purpose: they are SQL written by the
 * user ({@code VARCHAR(80)}, {@code NOT NULL DEFAULT 0}) and are shown on the table.
 */
final class NomeSql {
    private NomeSql() {}

    private static final Pattern SIMPLES = Pattern.compile("[\\p{L}\\p{N}_]*");

    /**
     * A table, field or constraint name, possibly after the diagram prefix. The prefix may
     * name a schema ({@code esquema.}), so each part between dots is checked on its own.
     */
    static String nome(String nome) {
        if (SIMPLES.matcher(nome.replace('.', '_')).matches()) return nome;
        StringBuilder res = new StringBuilder();
        for (String parte : nome.split("\\.", -1)) {
            if (res.length() > 0) res.append('.');
            res.append(SIMPLES.matcher(parte).matches() ? parte : '"' + parte.replace("\"", "\"\"") + '"');
        }
        return res.toString();
    }

    /** Text placed inside a block comment, which must not be able to close it. */
    static String comentario(String texto) {
        return texto.replace("*/", "* /");
    }

    private static final Set<String> ACOES = Set.of("CASCADE", "SET NULL", "SET DEFAULT", "RESTRICT", "NO ACTION");

    /** ON DELETE/ON UPDATE action: one of the five SQL actions, otherwise none. */
    static String acao(String acao) {
        if (acao == null) return "";
        String normal = acao.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        return ACOES.contains(normal) ? acao : "";
    }

    // Statement terminators in use: ";", "GO" (SQL Server), "/" (Oracle), on their own or
    // on the next line.
    private static final Pattern SEPARADOR = Pattern.compile("[;/\\s]*((?i:GO)[;\\s]*)?");

    /** Statement separator; anything that could carry a statement falls back to ";". */
    static String separador(String separador) {
        return separador != null && SEPARADOR.matcher(separador).matches() ? separador : ";";
    }
}
