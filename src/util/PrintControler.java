package util;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.awt.print.*;
import java.util.Locale;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.PageRanges;

/**
 * Copiado da ineternet
 */
public class PrintControler implements Printable {

    public PrintControler() {
        super();
        this.printJob = PrinterJob.getPrinterJob();
        page = printJob.defaultPage();
        page.setOrientation(PageFormat.PORTRAIT);
    }

    private final PrinterJob printJob;

    private PageFormat page;

    int idx = 0;

    public void print() {
        if (imgs == null) {
            return;
        }
        getPrintJob().setPrintable(this, getPage());

        if (Atributos.isEmpty()) {
            Atributos.add(jbn);
        }
        Atributos.remove(pgr);
        pgr = new PageRanges(page_range.x, page_range.y);
        Atributos.add(pgr);

        if (getPrintJob().printDialog(Atributos)) {
            try {
                getPrintJob().print(Atributos);
            } catch (PrinterException pe) {
                util.BrLogger.Logger("ERROR_PRINTING", pe.getMessage());
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
        if (imgs != null && pageIndex < imgs.length) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            RenderingHints renderHints
                    = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
            renderHints.put(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            g2d.addRenderingHints(renderHints);
            int w = getPageWidth();
            int h = getPageHeigth();
            g2d.drawImage(imgs[pageIndex], 0, 0, w, h, null);

            return (PAGE_EXISTS);
        } else {
            return (NO_SUCH_PAGE);
        }
    }

    public final void setJobName(String jobName) {
        getPrintJob().setJobName(jobName);
    }

    public void pageSetup() {
        page = getPrintJob().pageDialog(getPage());
    }

    private Point page_range = new Point(0, 0);
    private PrintRequestAttributeSet Atributos = new HashPrintRequestAttributeSet();
    private PageRanges pgr = null;
    private JobName jbn = new JobName("brModelo", Locale.getDefault());

    public void printSetup() {
        if (Atributos.isEmpty()) {
            Atributos.add(jbn);
        }
        Atributos.remove(pgr);
        pgr = new PageRanges(page_range.x, page_range.y);
        Atributos.add(pgr);

        if (getPrintJob().printDialog(Atributos)) {
            page = getPrintJob().getPageFormat(Atributos);
        }

    }

    public static void disableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(false);
    }

    public static void enableDoubleBuffering(Component c) {
        RepaintManager currentManager = RepaintManager.currentManager(c);
        currentManager.setDoubleBufferingEnabled(true);
    }

    public PrinterJob getPrintJob() {
        return printJob;
    }

    public PageFormat getPage() {
        return page;
    }

    public int getPageWidth() {
        return (int) getPage().getImageableWidth();
    }

    public int getPageHeigth() {
        return (int) getPage().getImageableHeight();
    }

    public boolean isLandscape() {
        return (getPage().getOrientation() == PageFormat.LANDSCAPE);
    }

    public int getRealFolhaWidth() {
        return (int) getPage().getWidth();
    }

    public int getRealFolhaHeigth() {
        return (int) getPage().getHeight();
    }

    BufferedImage[] imgs = null;

    public void setPaginas(BufferedImage[] imgs) {
        this.imgs = imgs;
        if (imgs == null) {
            page_range = new Point(0, 0);
            return;
        }
        page_range = new Point(1, imgs.length);
    }
}//class  

