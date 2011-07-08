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
    public  String localDerbyDBPath;
    public  String localGOslimDBPath;
    public  String selectedMappingID;
    public  String selectedMappingType;
    public  String ensemblIDType;
    private JPanel optionPane;
    public  JLabel messageLabel;
    public  JButton comfirmButton;
    
    public AnnotationDialog(JFrame aFrame, String localDerbyDB, String localGOslimDB,
            String mappingID, String mappingType, String ensemblType) {
        super(aFrame, true);
        localDerbyDBPath = localDerbyDB;
        localGOslimDBPath = localGOslimDB;
        selectedMappingID = mappingID;
        selectedMappingType = mappingType;
        ensemblIDType = ensemblType;
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
            System.out.println("starting annotating");            
            sp.messageLabel.setText("Starting annotating");
            if(sp.selectedMappingType.indexOf("ensembl")!=-1) {
                IdMapping.mapID(sp.localDerbyDBPath, sp.selectedMappingID, sp.ensemblIDType);
                IdMapping.mapAnnotation(sp.localGOslimDBPath, sp.ensemblIDType);
            } else {
                IdMapping.mapAnnotation(sp.localGOslimDBPath, sp.selectedMappingID);
            }
            // must wait for completion of uncompression before giving up thread
            //this.waitFor();
            sp.comfirmButton.setText("Finished!");
            sp.comfirmButton.setEnabled(true);
//        } catch (MalformedURLException e1) {
//            e1.printStackTrace();
//        } catch (IOException e1) {
//            e1.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}