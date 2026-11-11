package util;

import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicHTML;

/**
 * Keeps text that comes from diagrams from being rendered as HTML.
 *
 * Swing renders any label or button text that starts with "<html>", and an
 * <img src='http://...'> in it is fetched as soon as the component is measured. Diagram
 * names reach the navigation tree, editors, combo lists, menus and dialogs, so opening a
 * received file could make network requests. Every label and button gets "html.disable"
 * when its UI is created, before any text is rendered; the tree, list, table and combo
 * renderers are labels, and JOptionPane builds its message lines from labels, so they are
 * covered too. The UI classes are swapped in the developer defaults, which survive theme
 * changes, and each creation is delegated to the current look and feel's own UI.
 *
 * The few components that show the program's own HTML opt in with {@link #permitir}.
 * Tooltips are not covered: the program builds them with escaped text. Tab titles ignore
 * "html.disable" in Swing, so they go through {@link #texto}.
 */
public final class InterfaceSemHtml {
    private InterfaceSemHtml() {}

    private static final String CHAVE = "html.disable";
    private static final List<String> TIPOS = List.of("LabelUI", "ButtonUI", "ToggleButtonUI", "CheckBoxUI",
            "RadioButtonUI", "MenuItemUI", "CheckBoxMenuItemUI", "RadioButtonMenuItemUI", "MenuUI");

    public static void instalar() {
        for (String tipo : TIPOS) UIManager.put(tipo, InterfaceSemHtml.class.getName());
    }

    /** Called by Swing for the swapped UI classes. */
    public static ComponentUI createUI(JComponent c) {
        if (c.getClientProperty(CHAVE) == null) c.putClientProperty(CHAVE, Boolean.TRUE);
        try {
            Class<? extends ComponentUI> original = UIManager.getLookAndFeelDefaults().getUIClass(c.getUIClassID());
            Method criar = original.getMethod("createUI", JComponent.class);
            return (ComponentUI) criar.invoke(null, c);
        } catch (ReflectiveOperationException | NullPointerException e) {
            throw new IllegalStateException("No look and feel UI for " + c.getUIClassID(), e);
        }
    }

    /** Lets a component show the program's own HTML; call before setting its text. */
    public static <T extends JComponent> T permitir(T c) {
        c.putClientProperty(CHAVE, Boolean.FALSE);
        return c;
    }

    /** Text for places Swing renders as HTML regardless of "html.disable" (tab titles, tooltips). */
    public static String texto(String s) {
        return s != null && BasicHTML.isHTMLString(s) ? " " + s : s;
    }
}
