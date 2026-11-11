/*
 * Aplicacao.java
 */

/*
Copyright do brModelo
============================================================================

Copyright (c) 2019 SIS4.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

============================================================================
 */

package principal;

import controlador.Diagrama;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * The main class of the application.
 */
public class Aplicacao {

    public static FramePrincipal fmPrincipal;
    public static final String VERSAO_A = util.InformacoesAplicacao.versao().split("\\.")[0];
    public static final String VERSAO_B = util.InformacoesAplicacao.versao().split("\\.")[1];
    public static final String VERSAO_C = util.InformacoesAplicacao.versao().split("\\.")[2];
    public static final String VERSAO_DATA = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        util.RedeDesligada.instalar(); // before any library creates an http URL
        util.Renderizacao.configurar(); // before anything starts AWT
        util.Navegador.registrarIsolamento(); // logging starts AWT
        // Command-line theme > saved preference > desktop; decorations share a
        // native fallback on every OS until the WM interaction audit can be completed.
        initLookAndFeel();
        util.InterfaceSemHtml.instalar(); // before any window shows diagram text

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                fmPrincipal = new FramePrincipal();
                fmPrincipal.setVisible(true);
                controlador.ArquivosInicializacao.abrir(fmPrincipal.getEditor(), args);
                // Queued after the opening starts, which reads in the background meanwhile.
                java.awt.EventQueue.invokeLater(() -> util.SessaoX11.avisarUmaVez(fmPrincipal));
            }
        });
    }
    
    //Versao 3.2
    public static Diagrama getDiagramaSelecionado() {
        if (fmPrincipal != null) {
            return fmPrincipal.getEditor().diagramaAtual;
        }
        return null;
    }

    private static void initLookAndFeel() {
        util.TemaAplicacao.decoracoes();
        if (util.TemaAplicacao.iniciar()) return;
        initLookAndFeelPlataforma();
    }

    /** The platform look-and-feel: GTK on any Linux desktop, the native one elsewhere. */
    private static void initLookAndFeelPlataforma() {
        String sistema = UIManager.getSystemLookAndFeelClassName();
        // Java only picks the GTK look-and-feel on GNOME. Other Linux desktops (KDE Plasma,
        // Xfce, Cinnamon, MATE...) also publish a GTK theme (Plasma: Breeze / Breeze Dark), so
        // try GTK there too instead of falling back to Metal; Metal stays the fallback when
        // GTK is unavailable. An explicit -Dswing.systemlaf still wins.
        if (System.getProperty("swing.systemlaf") == null
                && sistema.equals(UIManager.getCrossPlatformLookAndFeelClassName())
                && System.getProperty("os.name", "").toLowerCase().contains("linux")) {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                return;
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                // No usable GTK: keep the cross-platform look-and-feel below.
            }
        }
        try {
            UIManager.setLookAndFeel(sistema);
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            util.BrLogger.Logger("ERROR_APP_LOAD_UI", ex.getMessage());
        }

    }

    //Apagar
}
