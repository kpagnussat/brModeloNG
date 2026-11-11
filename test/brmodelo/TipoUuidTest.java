package brmodelo;

import diagramas.logico.Campo;
import diagramas.logico.DataBaseModel;
import diagramas.logico.DiagramaLogico;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Issue #11 of the original brModelo: UUID in the list of field types. The list travels
 * inside .brM3 files, so files saved before it existed must offer it too.
 */
class TipoUuidTest {

    @Test void aNewLogicalModelListsUuid() {
        assertTrue(new DataBaseModel().getDataTypes().contains("UUID"));
    }

    @Test void aFileSavedWithoutUuidListsItToo() throws Exception {
        DiagramaLogico d = (DiagramaLogico) Fixtures.load("logico");
        DataBaseModel m = d.getDataModel();
        assertFalse(m.getDataTypesToJava().containsKey("UUID"), "the fixture predates UUID");
        assertTrue(m.getDataTypes().contains("UUID"), m.getDataTypes().toString());
        assertTrue(m.getDataTypes().containsAll(m.getDataTypesToJava().keySet()), "the stored types are all still there");
        // Saved again, the file still opens and still lists it.
        DiagramaLogico relido = (DiagramaLogico) Fixtures.load(Fixtures.save(d));
        assertTrue(relido.getDataModel().getDataTypes().contains("UUID"));
    }

    // A diagram with UUID in the stored map, the cached list and a field still opens in the
    // official jar, from .brM3 and after a .brMj round trip.
    @Test void aDiagramWithUuidOpensInTheOfficialJar() throws Exception {
        String jar = System.getProperty("oficialJar");
        assumeTrue(jar != null && !jar.isBlank(), "Set -PoficialJar=/path/to/official/brModelo.jar");
        DiagramaLogico d = (DiagramaLogico) Fixtures.load("logico");
        d.setMaster(new controlador.Editor()); // setTipo repaints the field
        d.setDataModel(new DataBaseModel());
        assertTrue(d.getDataModel().getDataTypes().contains("UUID"));
        d.getListaDeTabelas().get(0).getCampos().get(0).setTipo("UUID");
        OfficialCompatibilityTest.verify(d, jar);
        var convertido = util.FormatoBrMj.ler(util.FormatoBrMj.escrever(d));
        assertTrue(((DiagramaLogico) convertido.diagrama()).getDataModel().getDataTypes().contains("UUID"));
        OfficialCompatibilityTest.verify(convertido.diagrama(), jar, Fixtures.save(convertido.paraBrM3()));
    }

    @Test void theScriptKeepsUuidAsTheType() throws Exception {
        DiagramaLogico d = DdlTest.carregar(false);
        Campo c = d.getListaDeTabelas().get(0).getCampos().get(0);
        c.setTipo("UUID");
        String script = d.ScriptDDL();
        assertTrue(script.contains(c.getTexto() + " UUID"), script);
    }
}
