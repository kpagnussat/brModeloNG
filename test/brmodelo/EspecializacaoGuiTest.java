package brmodelo;

import controlador.Controler.Comandos;
import controlador.Diagrama;
import controlador.Editor;
import controlador.inspector.InspectorProperty;
import desenho.formas.Forma;
import desenho.preAnyDiagrama.PreEntidade;
import diagramas.conceitual.Entidade;
import diagramas.conceitual.Especializacao;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** Issue #33 of the original brModelo: exclusive or not is chosen in the Inspector. */
@Tag("gui")
class EspecializacaoGuiTest {
    private principal.FramePrincipal frame;
    private Editor editor;
    private Diagrama diagram;

    @BeforeEach void start() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new principal.FramePrincipal();
            editor = frame.getEditor();
            editor.setAutoSaveInterval(0);
            frame.setSize(1300, 900);
            frame.addNotify();
            frame.validate();
            diagram = editor.AddAsAtual("tpConceitual");
        });
    }

    @AfterEach void stop() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (editor != null) editor.EndAutoSave();
            if (frame != null) frame.dispose();
        });
    }

    @Test void inspectorSwitchesBetweenOneTriangleAndOneTrianglePerSubtype() throws Exception {
        onEdt(() -> {
            Entidade pessoa = (Entidade) palette(Comandos.cmdEntidade, new Point(500, 150));
            pessoa.setTexto("Pessoa");
            Especializacao esp = (Especializacao) palette(Comandos.cmdEspecializacao_Dupla, centro(pessoa));
            esp.setTotal(true);
            Set<Forma> filhas = filhas(esp);
            assertEquals(2, filhas.size());
            assertEquals(3, esp.getListaDePontosLigados().size());
            assertTrue(esp.isExclusiva());
            diagram.DoMuda(null);
            String exclusiva = CanonicalDump.dump(diagram);

            set(esp, "especializacao.naoexclusiva", true);
            List<Especializacao> ramos = especializacoes(pessoa);
            assertEquals(2, ramos.size(), "one triangle per subtype");
            for (Especializacao r : ramos) {
                assertTrue(r.isNaoExclusiva());
                assertFalse(r.isExclusiva());
                assertTrue(r.isTotal(), "totality is kept");
                assertEquals(1, filhas(r).size());
            }
            assertEquals(filhas, ramos.stream().flatMap(r -> filhas(r).stream()).collect(Collectors.toSet()));
            String naoExclusiva = CanonicalDump.dump(diagram);
            assertEquals(naoExclusiva, CanonicalDump.dump(Fixtures.load(Fixtures.save(diagram))), "saved and reopened");

            assertTrue(editor.desfazer());
            assertEquals(exclusiva, CanonicalDump.dump(editor.diagramaAtual), "one undo restores the single triangle");
            assertTrue(editor.refazer());
            assertEquals(naoExclusiva, CanonicalDump.dump(editor.diagramaAtual));

            diagram = editor.diagramaAtual;
            pessoa = (Entidade) diagram.getListaDeItens().stream()
                    .filter(i -> i instanceof Entidade e && e.getTexto().equals("Pessoa")).findFirst().orElseThrow();
            Especializacao ramo = especializacoes(pessoa).get(1);
            set(ramo, "especializacao.exclusiva", true);
            ramos = especializacoes(pessoa);
            assertEquals(1, ramos.size(), "the branches merge into the edited triangle");
            assertSame(ramo, ramos.get(0));
            assertTrue(ramo.isExclusiva());
            assertEquals(2, filhas(ramo).size());
        });
    }

    @Test void aSingleSubtypeHasNothingToSplit() throws Exception {
        onEdt(() -> {
            Entidade pessoa = (Entidade) palette(Comandos.cmdEntidade, new Point(500, 150));
            Especializacao esp = (Especializacao) palette(Comandos.cmdEspecializacao_Exclusiva, centro(pessoa));
            InspectorProperty p = propriedade(esp, "especializacao.naoexclusiva");
            assertEquals(InspectorProperty.TipoDeProperty.tpApenasLeituraTexto, p.tipo);
        });
    }

    private void set(Especializacao esp, String nome, boolean valor) {
        diagram.ClearSelect();
        diagram.DiagramaDoSelecao(esp, false, false);
        InspectorProperty p = propriedade(esp, nome);
        assertEquals(InspectorProperty.TipoDeProperty.tpBooleano, p.tipo, nome + " must be editable");
        assertTrue(diagram.AceitaEdicao(p, Boolean.toString(valor)));
    }

    private static InspectorProperty propriedade(Especializacao esp, String nome) {
        return esp.GenerateProperty().stream().filter(p -> nome.equals(p.configuracaoStr)).findFirst()
                .orElseThrow(() -> new AssertionError("No property " + nome));
    }

    private static Set<Forma> filhas(Especializacao esp) {
        PreEntidade mae = esp.LigadaAoPontoPrincipal();
        return esp.getListaDeFormasLigadas().stream().filter(f -> f != mae).collect(Collectors.toSet());
    }

    private List<Especializacao> especializacoes(Entidade mae) {
        return diagram.getListaDeItens().stream().filter(i -> i instanceof Especializacao e && e.LigadaAoPontoPrincipal() == mae)
                .map(i -> (Especializacao) i).toList();
    }

    private Forma palette(Comandos command, Point click) {
        diagram.getEditor().DoAction(new ActionEvent(new JButton(), ActionEvent.ACTION_PERFORMED, command.name()));
        Forma result = (Forma) diagram.ExternalRealiseComando(command, click);
        assertNotNull(result, command.name());
        return result;
    }

    private static Point centro(Forma f) {
        return new Point(f.getLeft() + f.getWidth() / 2, f.getTop() + f.getHeight() / 2);
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
    private static void onEdt(CheckedAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try { action.run(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
