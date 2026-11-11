package brmodelo;

import controlador.Controler.Comandos;
import controlador.Diagrama;
import controlador.Editor;
import controlador.conversor.conversorConceitualParaLogico;
import controlador.conversor.conversorOpcoes;
import desenho.formas.Forma;
import desenho.preAnyDiagrama.PreCardinalidade.TiposCard;
import diagramas.conceitual.Atributo;
import diagramas.conceitual.DiagramaConceitual;
import diagramas.conceitual.Entidade;
import diagramas.logico.Campo;
import diagramas.logico.DiagramaLogico;
import diagramas.logico.LogicoLinha;
import diagramas.logico.Tabela;
import java.awt.Point;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Conceptual to logical conversion, run step by step with "yes to all" so no dialog opens. */
@Tag("gui")
class ConversaoLogicoGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private DiagramaConceitual conceitual;

    @BeforeEach void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            conceitual = (DiagramaConceitual) editor.AddAsAtual("tpConceitual");
        });
    }

    @AfterEach void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
        });
    }

    // Issue #13 of the original brModelo: a multivalued attribute became a table whose
    // key went to the entity. The entity's key must go to the new table instead.
    @ParameterizedTest @CsvSource({"1, -1, C1N", "0, 3, C0N"})
    void multivaluedAttributeTableReceivesTheEntityKey(int min, int max, String cardFoto) throws Exception {
        onEdt(() -> {
            Entidade produto = (Entidade) shape(Comandos.cmdEntidade, "Produto", 400, 300);
            Atributo id = (Atributo) shape(Comandos.cmdAtributo, "id_prod", produto.getLeft() + 2, produto.getTop() + 10);
            id.setIdentificador(true);
            shape(Comandos.cmdAtributo, "nome", produto.getLeft() + 30, produto.getTop() + 2);
            Atributo foto = (Atributo) shape(Comandos.cmdAtributo, "foto", produto.getLeft() + 2, produto.getTop() + 40);
            foto.setMultivalorado(true);
            foto.setCardMaxima(max);
            foto.setCardMinima(min);
            assertEquals(3, produto.getListaDePontosLigados().size(), "attributes linked to Produto");

            DiagramaLogico logico = converter();
            Tabela tProduto = tabela(logico, "Produto");
            Tabela tFoto = tabela(logico, "foto");

            assertEquals("[PK id_prod, nome]", nomes(tProduto), "Produto must not reference the attribute table");
            Campo fk = tFoto.getCampos().stream().filter(Campo::isFkey).findFirst()
                    .orElseThrow(() -> new AssertionError("foto has no foreign key: " + nomes(tFoto)));
            assertTrue(fk.isKey(), "The entity key is part of the attribute table key");
            Campo valor = tFoto.getCampos().stream().filter(c -> c.getTexto().equals("foto")).findFirst()
                    .orElseThrow(() -> new AssertionError("foto has no value column: " + nomes(tFoto)));
            assertTrue(valor.isKey(), "One row per value: the value is part of the key");
            assertEquals(2, tFoto.getCampos().size(), nomes(tFoto));

            LogicoLinha linha = (LogicoLinha) tFoto.getListaDeLigacoes().get(0);
            boolean fotoEmB = linha.getFormaPontaB() == tFoto;
            assertEquals(TiposCard.C11, (fotoEmB ? linha.getCardA() : linha.getCardB()).getCard(), "each value belongs to one Produto");
            assertEquals(TiposCard.valueOf(cardFoto), (fotoEmB ? linha.getCardB() : linha.getCardA()).getCard());

            String sql = logico.ScriptDDL();
            assertTrue(sql.matches("(?s).*CREATE TABLE foto \\(.*FOREIGN KEY \\(\\w+\\)\\s*REFERENCES Produto \\(id_prod\\).*"), sql);
            assertFalse(sql.matches("(?s).*CREATE TABLE Produto \\([^;]*FOREIGN KEY.*"), sql);
        });
    }

    private DiagramaLogico converter() {
        editor.AddAsAtual(Diagrama.TipoDeDiagrama.tpLogico.name());
        DiagramaLogico logico = (DiagramaLogico) editor.diagramaAtual;
        conversorConceitualParaLogico conv = new conversorConceitualParaLogico();
        conv.Opcoes.Resultado = conversorOpcoes.resultOfQuestion.resOkToAll;
        conv.origem = conceitual;
        conv.destino = logico;
        logico.isCarregando = true;
        assertTrue(conv.converterEntidades() && conv.converterAtributos() && conv.converterEspecializacao()
                && conv.converterUniao() && conv.converterAutoRelacionamento() && conv.converterRelacionamento()
                && conv.converterEntidadeAssossiativa() && conv.exportarAssessorios() && conv.processeConstraints(),
                String.join("\n", conv.Opcoes.Erros));
        logico.isCarregando = false;
        return logico;
    }

    private static Tabela tabela(DiagramaLogico d, String nome) {
        return d.getListaDeTabelas().stream().filter(t -> t.getTexto().equals(nome)).findFirst()
                .orElseThrow(() -> new AssertionError("No table " + nome + " in "
                        + d.getListaDeTabelas().stream().map(t -> t.getTexto() + nomes(t)).toList()));
    }

    private static String nomes(Tabela t) {
        return t.getCampos().stream().map(c -> (c.isKey() ? "PK " : "") + (c.isFkey() ? "FK " : "") + c.getTexto()).toList().toString();
    }

    // The palette gesture, as GeraFixtures uses it: an attribute clicked on an entity is linked to it.
    private Forma shape(Comandos command, String text, int x, int y) {
        conceitual.getEditor().DoAction(new ActionEvent(new JButton(), ActionEvent.ACTION_PERFORMED, command.name()));
        Forma result = (Forma) conceitual.ExternalRealiseComando(command, new Point(x, y));
        assertNotNull(result, command.name());
        result.setTexto(text);
        return result;
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
