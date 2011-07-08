/*******************************************************************************
 * Copyright 2011 Chao Zhang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.genmapp.golayout.download;

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

/**
 *
 * @author Chao
 */
public class FileDownloadDialog extends JDialog implements ActionListener {
    public  List<String> downloadFileList;
    private JPanel optionPane;
    public  JLabel messageLabel;
    public  JButton comfirmButton;

    public FileDownloadDialog(JFrame aFrame, List<String> downloadDBList) {
        super(aFrame, true);
        downloadFileList = downloadDBList;
        setTitle("Downloading databases");

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
        new DownloadThread(this).start();
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
class DownloadThread extends Thread {

    private FileDownloadDialog sp;

    public DownloadThread(FileDownloadDialog sp) {
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
                sp.messageLabel.setText("Uncompressing " + fileName + "...");
                
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