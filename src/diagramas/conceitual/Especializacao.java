/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package diagramas.conceitual;

import controlador.Controler;
import controlador.Diagrama;
import controlador.Editor;
import controlador.inspector.InspectorProperty;
import desenho.formas.Forma;
import desenho.linhas.PontoDeLinha;
import desenho.preAnyDiagrama.PreEntidade;
import desenho.preAnyDiagrama.PreEspecializacao;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author ccandido
 */
public class Especializacao extends PreEspecializacao {

    private static final long serialVersionUID = 3595682719755602909L;

    public Especializacao(Diagrama modelo) {
        super(modelo);
        nodic = false;
        paintParcial = true;
    }

    public Especializacao(Diagrama modelo, String texto) {
        super(modelo, texto);
        nodic = false;
        paintParcial = true;
    }

    // <editor-fold defaultstate="collapsed" desc="Campos">
    @Override
    public boolean isParcial() {
        return (this.getListaDeFormasLigadas().size() > 1 && LigadaAoPontoPrincipal() != null) && super.isParcial();
    }

    public boolean isTotal() {
        return (!isParcial() &&  LigadaAoPontoPrincipal() != null);
    }
    
    public void setTotal(boolean sn) {
        setParcial(!sn);
    }

    public boolean isNaoExclusiva() {
        final PreEntidade tmp = LigadaAoPontoPrincipal();
        return this.getListaDeFormasLigadas().size() > 1 && tmp != null && (
                    tmp.getListaDeFormasLigadas().stream().filter(f -> f instanceof Especializacao).map(e -> (Especializacao)e).filter(e -> e.LigadaAoPontoPrincipal() == tmp).count() > 1
                );
    }

    public boolean isExclusiva() {
        final PreEntidade tmp = LigadaAoPontoPrincipal();
        return this.getListaDeFormasLigadas().size() > 1 && tmp != null && (
                    tmp.getListaDeFormasLigadas().stream().filter(f -> f instanceof Especializacao).map(e -> (Especializacao)e).filter(e -> e.LigadaAoPontoPrincipal() == tmp).count() == 1
                );
    }
    
    // </editor-fold>

    // Exclusivity is drawn, not stored: one triangle shared by the subtypes is exclusive,
    // one triangle per subtype from the same supertype is not (issue #33 of the original
    // brModelo, where the Inspector only showed it). Changing it redraws the triangles.
    public void setNaoExclusiva(boolean sn) {
        setExclusiva(!sn);
    }

    public void setExclusiva(boolean sn) {
        PreEntidade mae = LigadaAoPontoPrincipal();
        if (mae == null || sn == isExclusiva()) {
            return;
        }
        if (sn) {
            juntarRamos(mae);
        } else {
            separarFilhas(mae);
        }
        getMaster().repaint();
    }

    /** Lines from this triangle to its subtypes (every line except the one to the supertype). */
    private List<PontoDeLinha> filhas() {
        PreEntidade mae = LigadaAoPontoPrincipal();
        return getListaDePontosLigados().stream()
                .filter(p -> p.getDono().getOutraPonta(p).getEm() != mae)
                .collect(Collectors.toList());
    }

    private void separarFilhas(PreEntidade mae) {
        List<PontoDeLinha> pontas = filhas();
        for (PontoDeLinha ponta : pontas.subList(1, pontas.size())) {
            Forma filha = ponta.getDono().getOutraPonta(ponta).getEm();
            Especializacao ramo = new Especializacao(getMaster(), getTexto());
            ramo.setDirecaoTriangulo(getDirecaoTriangulo());
            ramo.setParcial(super.isParcial());
            ramo.SetBounds(filha.getLeft() + filha.getWidth() / 2 - getWidth() / 2, getTop(), getWidth(), getHeight());
            ramo.Reenquadre();
            Point[] pts = ramo.getPontosDoTriangulo();
            Ligacao linha = new Ligacao(getMaster());
            linha.setInteligente(false);
            linha.Inicie(0, pts[0], mae.getMelhorPontoDeLigacao(pts[0]));
            linha.getPontaA().LigarA(ramo); // at the apex: the supertype's place
            linha.getPontaB().LigarA(mae);
            ponta.setEm(null); // detach first: the subtype must not be reachable through this triangle
            ponta.setCentro(pts[3]);
            ponta.LigarA(ramo);
        }
    }

    private void juntarRamos(PreEntidade mae) {
        List<Especializacao> ramos = mae.getListaDeFormasLigadas().stream()
                .filter(f -> f instanceof Especializacao && f != this).map(f -> (Especializacao) f)
                .filter(e -> e.LigadaAoPontoPrincipal() == mae).collect(Collectors.toList());
        List<PontoDeLinha> movidas = new ArrayList<>();
        for (Especializacao ramo : ramos) {
            for (PontoDeLinha ponta : ramo.filhas()) {
                ponta.setEm(null);
                movidas.add(ponta);
            }
            for (PontoDeLinha ponta : ramo.getListaDePontosLigados()) {
                desenho.linhas.Linha linha = ponta.getDono();
                linha.getPontaA().setEm(null); // the supertype must stop counting this triangle
                linha.getPontaB().setEm(null);
                getMaster().Remove(linha, true);
            }
            getMaster().Remove(ramo, true);
        }
        for (PontoDeLinha ponta : movidas) {
            Forma filha = ponta.getDono().getOutraPonta(ponta).getEm();
            ponta.setCentro(getMelhorPontoDeLigacao(new Point(filha.getLeft() + filha.getWidth() / 2, filha.getTop() + filha.getHeight() / 2)));
            ponta.LigarA(this);
        }
    }

    @Override
    public ArrayList<InspectorProperty> GenerateProperty() {
        ArrayList<InspectorProperty> res = super.GenerateProperty();
        res.add(InspectorProperty.PropertyFactoryMenu("direcao", "setDirecaoFromInspector", getDirecaoForInspector(), Editor.fromConfiguracao.getLstDirecao(Controler.Comandos.cmdEspecializacao)));

        if ((!isExclusiva() && !isNaoExclusiva()) ||(!isParcial() && !isTotal())) {
            res.add(InspectorProperty.PropertyFactoryApenasLeituraTexto("especializacao.formacao", Editor.fromConfiguracao.getValor("Inspector.obj.especializacao.malformada")));
        } else {
            res.add(InspectorProperty.PropertyFactorySN("especializacao.parcial", "setParcial", isParcial()));
            res.add(InspectorProperty.PropertyFactorySN("especializacao.total", "setTotal", isTotal()));
            
            // A single subtype under a single triangle has nothing to split or merge.
            if (isNaoExclusiva() || filhas().size() > 1) {
                res.add(InspectorProperty.PropertyFactorySN("especializacao.exclusiva", "setExclusiva", isExclusiva()));
                res.add(InspectorProperty.PropertyFactorySN("especializacao.naoexclusiva", "setNaoExclusiva", isNaoExclusiva()));
            } else {
                res.add(InspectorProperty.PropertyFactoryApenasLeituraSN("especializacao.exclusiva", isExclusiva()));
                res.add(InspectorProperty.PropertyFactoryApenasLeituraSN("especializacao.naoexclusiva", isNaoExclusiva()));
            }
        }
        PreEntidade pe = LigadaAoPontoPrincipal();
        if (pe != null) {
            res.add(
                    InspectorProperty.PropertyFactoryActionSelect(Editor.fromConfiguracao.getValor("Inspector.obj.especializacao.apartirde"),
                    pe.getTexto(),
                    String.valueOf(pe.getID()))
                    );
        } else {
            res.add(InspectorProperty.PropertyFactoryApenasLeituraTexto("especializacao.apartirde", "{}"));
        }
        return res;
    }
    
    
    @Override
    protected boolean FinderLinked(Forma quem, Forma origem) {
        ArrayList<Forma> outras = getListaDeFormasLigadas(origem);
        for (Forma f : outras) {
            if (f == quem) {
                return true;
            }
            List<Especializacao> esp = f.getListaDeFormasLigadas(Especializacao.class).stream().filter(o -> o != this).map(o -> (Especializacao)o).collect(Collectors.toList());
            
            if (esp.stream().anyMatch((f2) -> (f2.FinderLinked(quem, f)))) {
                return true;
            }
        }
        return false;
    }
    
}
