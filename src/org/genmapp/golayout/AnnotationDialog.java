/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genmapp.golayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.genmapp.golayout.download.Downloader;

/**
 *
 * @author Chao
 */
public class AnnotationDialog extends JDialog implements ActionListener {
    public  List<String> downloadFileList;
    private JPanel optionPane;
    public  JLabel messageLabel;
    public  JButton comfirmButton;
    
    public AnnotationDialog(JFrame aFrame, List<String> downloadDBList) {
        super(aFrame, true);
        downloadFileList = downloadDBList;
        setTitle("Annotating current network");

        optionPane = new JPanel();
        messageLabel = new JLabel();
        optionPane.add(messageLabel);
        comfirmButton = new JButton("Please wait ......");
        comfirmButton.setEnabled(false);
        comfirmButton.addActionListener(this);
        optionPane.add(comfirmButton);
        this.add(optionPane);
        //Handle window closing correctly.
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        new AnnotationThread(this).start();
    }

    /** This method handles events for the text field. */
    public void actionPerformed(ActionEvent e) {
        this.dispose();
    }
}

/**
 *
 * @author Anurag Sharma
 */
class AnnotationThread extends Thread {

    private AnnotationDialog sp;

    public AnnotationThread(AnnotationDialog sp) {
        this.sp = sp;
    }

    public void run() {
        try {
            System.out.println("starting download");
            for(String fileName:sp.downloadFileList) {
                Downloader d = new Downloader();
                d.download(fileName);
                int progress = d.getProgress();
                while (progress < 99) {
                    System.out.println(fileName + ": " + progress + "%");
                    sp.messageLabel.setText(fileName + ": " + progress + "%");
                    Thread.sleep(500);
                    progress = d.getProgress();

                }
                sp.messageLabel.setText(fileName + ": 100%");

                // must wait for completion of uncompression before giving up thread
                d.waitFor();
            }
            sp.comfirmButton.setText("Finished!");
            sp.comfirmButton.setEnabled(true);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}