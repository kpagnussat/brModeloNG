package util;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import controlador.Editor;
import java.awt.Component;
import java.util.Locale;
import javax.swing.*;

/** Appearance state belongs to helpers, never to serialized editors or diagrams. */
public final class TemaAplicacao {
    public static final String CHAVE = "cfg.aparencia.tema";
    public static final String CLARO = "cfg.aparencia.temaClaro";
    public static final String ESCURO = "cfg.aparencia.temaEscuro";
    private static String escolha = "auto", claro = "claro", escuro = "escuro";
    private static String ativo;
    private static Timer monitor;
    private static boolean consultando;
    private TemaAplicacao() {}

    public static String normalizar(String value) {
        if (value == null) return "auto";
        String id = value.toLowerCase(Locale.ROOT);
        return switch (id) {
            case "light" -> "claro";
            case "dark" -> "escuro";
            case "sistema", "auto" -> "auto";
            default -> CatalogoTemas.buscar(id) != null ? id : "auto";
        };
    }
    public static String escolhaInicial(String command, String saved) {
        return normalizar(command != null ? command : saved);
    }
    private static String salvo(String key) {
        return Editor.fromConfiguracao.hasValor(key) ? Editor.fromConfiguracao.getValor(key) : null;
    }
    public static boolean iniciar() {
        EstiloUI.iniciar();
        escolha = escolhaInicial(System.getProperty("brmodelo.tema"), salvo(CHAVE));
        claro = CatalogoTemas.preferido(salvo(CLARO), false);
        escuro = CatalogoTemas.preferido(salvo(ESCURO), true);
        if (System.getProperty("swing.systemlaf") != null) return false;
        FonteInterface.antesDoTema();
        CatalogoTemas.Tema tema = CatalogoTemas.resolver(escolha, claro, escuro, escolha.equals("auto") && TemaDoSistema.escuroDetectado());
        boolean ok = instalarTema(tema);
        // Once, when the desktop font turned out to be missing: again with the bundled one.
        if (ok && FonteInterface.reservaNecessaria()) ok = instalarTema(tema);
        return ok;
    }
    private static boolean instalarTema(CatalogoTemas.Tema tema) {
        boolean ok = FlatLaf.setup(tema.fabrica().get());
        if (ok) ativo = tema.id();
        return ok;
    }
    public static String escolha() { return escolha; }
    public static String preferido(boolean dark) { return dark ? escuro : claro; }
    public static String ativo() { return ativo; }

    public static void aplicar(String value) {
        String nova = normalizar(value);
        trocar(CatalogoTemas.resolver(nova, claro, escuro, nova.equals("auto") && TemaDoSistema.escuroDetectado()));
        escolha = nova;
        Editor.fromConfiguracao.SetAndSaveIfNeed(CHAVE, escolha);
    }
    /** A card changes that half of the preferred pair when following the desktop. */
    public static void escolher(CatalogoTemas.Tema tema) {
        if (escolha.equals("auto")) preferir(tema.id(), tema.escuro());
        else aplicar(tema.id());
    }
    public static void preferir(String id, boolean dark) {
        String novo = CatalogoTemas.preferido(id, dark);
        String novoClaro = dark ? claro : novo, novoEscuro = dark ? novo : escuro;
        if (escolha.equals("auto"))
            trocar(CatalogoTemas.resolver(escolha, novoClaro, novoEscuro, TemaDoSistema.escuroDetectado()));
        claro = novoClaro;
        escuro = novoEscuro;
        Editor.fromConfiguracao.SetAndSaveIfNeed(dark ? ESCURO : CLARO, novo);
    }
    private static void trocar(CatalogoTemas.Tema tema) {
        EstiloUI.iniciar();
        if (tema.id().equals(ativo) && UIManager.getLookAndFeel().getClass() == tema.fabrica().get().getClass()) return;
        FlatAnimatedLafChange.showSnapshot();
        try {
            if (!instalarTema(tema)) throw new IllegalStateException("Não foi possível aplicar o tema");
            FlatLaf.updateUI();
        } finally { FlatAnimatedLafChange.hideSnapshotWithAnimation(); }
    }

    public static void instalar(JToolBar toolbar, JMenu editar) {
        if (!(UIManager.getLookAndFeel() instanceof FlatLaf)) return;
        editar.setMnemonic('E');
        JMenuItem aparencia = new JMenuItem("Aparência…", Icones.de("/imagens/sun-moon.svg"));
        aparencia.setMnemonic('A');
        DicasInterface.dica(aparencia, "appearance");
        aparencia.addActionListener(event -> Aparencia.abrir(editar));
        editar.addSeparator();
        editar.add(aparencia);
        JButton button = new JButton(Icones.de("/imagens/sun-moon.svg"));
        button.setToolTipText("Aparência: sistema, claro, escuro e mais temas");
        button.getAccessibleContext().setAccessibleName("Aparência");
        EstiloUI.toolbar(button);
        JPopupMenu popup = menuRapido(button);
        button.addActionListener(event -> popup.show(button, 0, button.getHeight()));
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(button);
        monitorarSistema();
    }
    public static JPopupMenu menuRapido(Component owner) {
        JPopupMenu popup = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        String[] values = {"auto", "claro", "escuro"};
        String[] labels = {"Seguir o sistema", "Claro", "Escuro"};
        for (int index = 0; index < values.length; index++) {
            String value = values[index];
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(labels[index]);
            item.setActionCommand(value);
            item.setMnemonic(index == 0 ? 'S' : index == 1 ? 'C' : 'E');
            item.addActionListener(event -> aplicar(value.equals("auto") ? value : preferido(value.equals("escuro"))));
            group.add(item);
            popup.add(item);
        }
        popup.addSeparator();
        JMenuItem more = new JMenuItem("Mais temas…");
        more.setMnemonic('M');
        DicasInterface.dica(more, "appearance");
        more.addActionListener(event -> Aparencia.abrir(owner));
        popup.add(more);
        atualizarMenu(popup, group);
        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { atualizarMenu(popup, group); }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
        return popup;
    }
    private static void atualizarMenu(JPopupMenu popup, ButtonGroup group) {
        group.clearSelection();
        for (Component component : popup.getComponents()) if (component instanceof JRadioButtonMenuItem item) {
            String value = item.getActionCommand();
            item.setSelected(value.equals("auto") ? escolha.equals("auto") : escolha.equals(preferido(value.equals("escuro"))));
            item.setToolTipText(value.equals("auto") ? DicasInterface.texto("themeAuto")
                    : DicasInterface.texto("themeApply") + ": " + CatalogoTemas.buscar(preferido(value.equals("escuro"))).nome());
        }
    }
    static void sistemaAlterado(boolean dark) {
        if (escolha.equals("auto") && UIManager.getLookAndFeel() instanceof FlatLaf)
            trocar(CatalogoTemas.resolver(escolha, claro, escuro, dark));
    }
    /** Query off the EDT; a single timer also serves multiple editor windows. */
    private static void monitorarSistema() {
        if (monitor != null) return;
        monitor = new Timer(5000, event -> {
            boolean windows = java.util.Arrays.stream(java.awt.Window.getWindows()).anyMatch(java.awt.Window::isDisplayable);
            if (!windows) { monitor.stop(); monitor = null; return; }
            if (!escolha.equals("auto") || consultando || !(UIManager.getLookAndFeel() instanceof FlatLaf)) return;
            consultando = true;
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() { return TemaDoSistema.escuroDetectado(); }
                @Override protected void done() {
                    consultando = false;
                    try {
                        sistemaAlterado(get());
                    } catch (Exception e) { java.util.logging.Logger.getLogger(TemaAplicacao.class.getName()).fine(e.toString()); }
                }
            }.execute();
        });
        monitor.start();
    }
    public static void decoracoes() { DecoracoesJanela.iniciar(); }
}
