package brmodelo;

import javax.swing.UIManager;

/** Sets the theme up in a fresh JVM (FonteInterfaceJvmTest) and reports the interface typeface. */
public final class SondaFonte {
    public static void main(String[] args) throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(util.TemaAplicacao::iniciar);
        System.out.println("familia=" + UIManager.getFont("defaultFont").getFamily());
        System.out.println("embutida=" + util.FonteInterface.embutida());
    }
}
