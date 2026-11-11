package brmodelo;

import diagramas.logico.Campo;
import diagramas.logico.Constraint;
import diagramas.logico.DiagramaLogico;
import diagramas.logico.Tabela;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The generated SQL script: usual names stay as they were, hostile ones stay names. */
class DdlTest {
    // logico.sql was produced by the code before names were checked, for both variants.
    @Test
    void usualDiagramsKeepTheSameScript() throws Exception {
        String esperado = Files.readString(Path.of("test-resources/fixtures/logico.sql"), StandardCharsets.UTF_8);
        assertEquals(esperado, carregar(false).ScriptDDL() + "\n=====\n" + carregar(true).ScriptDDL() + "\n");
    }

    @Test
    void namesFromAReceivedFileCannotAddStatements() throws Exception {
        DiagramaLogico d = carregar(true);
        String ataque = "x; DROP TABLE notas; --";
        d.SetNome("Aula */ DROP TABLE notas; /*");
        d.setPrefixo("esquema; DROP TABLE notas; --.");
        d.setSeparatorSQL("; DROP TABLE notas;");
        for (Tabela t : d.getListaDeTabelas()) {
            t.setTexto(t.getTexto() + "\"); " + ataque);
            for (Campo c : t.getCampos()) c.setTexto(c.getTexto() + "\" " + ataque);
            for (Constraint c : t.getConstraints()) {
                if (c.isNomeada()) c.setNome(c.getNome() + " " + ataque);
                if (!c.getDdlOnDelete().isEmpty()) c.setDdlOnDelete("CASCADE; DROP TABLE notas");
            }
        }
        String script = d.ScriptDDL();
        String fora = foraDeNomesEComentarios(script);
        assertFalse(fora.contains("DROP"), script);
        // One terminator per statement: 3 CREATE TABLE, 2 named UNIQUE/PRIMARY KEY, 2 FOREIGN KEY.
        assertEquals(7, fora.chars().filter(ch -> ch == ';').count(), script);
        assertTrue(script.contains("CREATE TABLE \"esquema; DROP TABLE notas; --\".\"Aluno\"\"); x; DROP TABLE notas; --\" ("), script);
        assertTrue(script.contains("    \"id_aluno\"\" x; DROP TABLE notas; --\" INTEGER"), script);
        assertTrue(script.startsWith("/* Aula * / DROP TABLE notas; /*: */"), script);
        assertFalse(script.contains("ON DELETE"), script);
    }

    @Test
    void usualVariationsAreNotQuoted() throws Exception {
        DiagramaLogico d = carregar(false);
        d.setSeparatorSQL("\nGO");
        d.getListaDeTabelas().get(0).setTexto("Situação_2");
        String script = d.ScriptDDL();
        assertTrue(script.contains("CREATE TABLE Situação_2 ("), script);
        assertTrue(script.contains(")\nGO"), script);
        assertFalse(script.contains("\""), script);
    }

    // Removes "quoted identifiers" and /* comments */, so what remains is the SQL a
    // database would read as keywords and punctuation.
    static String foraDeNomesEComentarios(String sql) {
        StringBuilder fora = new StringBuilder();
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '"') {
                for (i++; i < sql.length(); i++) {
                    if (sql.charAt(i) == '"') {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == '"') i++;
                        else break;
                    }
                }
            } else if (sql.startsWith("/*", i)) {
                int fim = sql.indexOf("*/", i + 2);
                i = fim < 0 ? sql.length() : fim + 1;
            } else {
                fora.append(ch);
            }
        }
        return fora.toString();
    }

    static DiagramaLogico carregar(boolean variante) throws Exception {
        DiagramaLogico d = (DiagramaLogico) Fixtures.load("logico");
        d.setMaster(new controlador.Editor());
        d.SetNome("logico");
        if (variante) {
            d.SetNome("Aula BD");
            d.setPrefixo("esquema.");
            for (Tabela t : d.getListaDeTabelas()) for (Constraint c : t.getConstraints()) {
                String tabela = t.getTexto(); int i = t.getConstraints().indexOf(c);
                if (tabela.equals("Matricula") && i == 0) { c.setNomeada(true); c.setNome("pk_matricula"); }
                if (tabela.equals("Aluno") && i == 1) { c.setNomeada(true); c.setNome("uq_email"); }
                if (tabela.equals("Matricula") && i == 1) { c.setDdlOnDelete("CASCADE"); c.setDdlOnUpdate("NO ACTION"); }
                if (tabela.equals("Matricula") && i == 2) { c.setNomeada(true); c.setNome("fk_curso"); c.setDdlOnDelete("SET NULL"); }
            }
        }
        return d;
    }
}
