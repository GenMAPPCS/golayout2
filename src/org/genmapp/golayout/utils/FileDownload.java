/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genmapp.golayout.utils;

import java.awt.BorderLayout;
import org.genmapp.golayout.utils.Downloader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import org.genmapp.golayout.GOLayout;
import org.genmapp.golayout.GOLayoutStaticValues;

/**
 *
 * @author Chao
 */
public class FileDownload {
    private static final String bridgedbSpecieslist = GOLayoutStaticValues.bridgedbSpecieslist;
    //private static final String bridgedbDatasourcelist = "http://svn.bigcat.unimaas.nl/bridgedb/trunk/org.bridgedb.bio/resources/org/bridgedb/bio/datasources.txt";
    public static final String bridgedbDerbyDir = GOLayoutStaticValues.bridgedbDerbyDir;
//    /public static String genmappcsdir = "/GenMAPP-CS-Data/";
    public static String genmappcsdatabasedir = GOLayout.GOLayoutBaseDir;
//    private static String localSpecieslist;
//    private String speciesState = null;
//    private String connState = null;
//    private String derbyState = null;
//    private String latestLocalState = null;
//    private static boolean onlineState = true;
    String downloadFile = null;

    public FileDownload(String fileName) {
        this.downloadFile = fileName;
        new DownloadThread(this).start();
    }


}

/**
 *
 * @author Anurag Sharma
 */
class DownloadThread extends Thread {

    private FileDownload sp;

    public DownloadThread(FileDownload sp) {
        this.sp = sp;
    }

    public void run() {
        try {
            System.out.println("starting download");
            Downloader d = new Downloader();
            d.download(GOLayoutStaticValues.genmappcsDatabaseDir + sp.downloadFile);
            int progress = d.getProgress();
            while (progress < 99) {
                System.out.println(sp.downloadFile + ": " + progress + "%");
                //sp.dbConnection.setText(sp.downloadFile + ": " + progress + "%");
                Thread.sleep(500);
                progress = d.getProgress();

            }
            System.out.println(sp.downloadFile + ": 100%");
            System.out.println("Uncompressing " + sp.downloadFile + "...");
            //sp.dbConnection.setText(sp.downloadFile + ": 100%");
            //sp.dbConnection.setText("Uncompressing " + sp.downloadFile + "...");

            // must wait for completion of uncompression before giving up thread
            d.waitFor();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}