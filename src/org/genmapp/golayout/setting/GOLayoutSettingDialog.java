/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GOLayoutSettingPanel.java
 *
 * Created on Aug 2, 2011, 3:52:32 PM
 */

package org.genmapp.golayout.setting;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.genmapp.golayout.GOLayout;
import org.genmapp.golayout.layout.CellAlgorithm;
import org.genmapp.golayout.layout.PartitionNetworkVisualStyleFactory;
import org.genmapp.golayout.partition.PartitionAlgorithm;
import org.genmapp.golayout.utils.GOLayoutStaticValues;
import org.genmapp.golayout.utils.GOLayoutUtil;
import org.genmapp.golayout.utils.IdMapping;

/**
 *
 * @author Administrator
 */
public class GOLayoutSettingDialog extends JDialog
        implements ActionListener {
    private String annotationSpeciesCode = "";
    private String annotationButtonLabel = "Annotate";
    private List<String> speciesValues = new ArrayList<String>();
    private List<String> downloadDBList = new ArrayList<String>();
    private List<String> currentAttributeList = new ArrayList<String>();
    private List<String> rAnnIdeValues = new ArrayList<String>();
    private List<String> idMappingTypeValues = new ArrayList<String>();

    /** Creates new form GOLayoutSettingPanel */
    public GOLayoutSettingDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.setTitle("GOLayout Settings");
        loadCurrentValues();
        initComponents();
        initValues();
//        this.repaint();
//        this.pack();
    }

    private void loadCurrentValues() {       
    }

    private void initValues() {
        System.out.println("**************initialize values*************");
        speciesValues = Arrays.asList(GOLayoutStaticValues.speciesList);
        currentAttributeList = Arrays.asList(cytoscape.Cytoscape
                .getNodeAttributes().getAttributeNames());
        Collections.sort(currentAttributeList);
        
        //Guess species of current network for annotation
        String[] defaultSpecies = getSpeciesCommonName(CytoscapeInit
                .getProperties().getProperty("defaultSpeciesName"));
        System.out.println("Guess species: "+defaultSpecies[0]);
        if(!defaultSpecies[0].equals("")) {
            annotationSpeciesCode = defaultSpecies[1];
            rAnnSpeComboBox.setModel(new DefaultComboBoxModel(speciesValues.toArray()));
            rAnnSpeComboBox.setSelectedIndex(speciesValues.indexOf(defaultSpecies[0]));
            downloadDBList = checkMappingResources(annotationSpeciesCode);
            System.out.println(downloadDBList);
            //checkDownloadStatus();
            rAnnIdeValues.add("ID");
            rAnnIdeValues.addAll(currentAttributeList);
            rAnnIdeComboBox.setModel(new DefaultComboBoxModel(rAnnIdeValues.toArray()));
            if(downloadDBList.isEmpty()) {
                idMappingTypeValues = IdMapping.getSourceTypes(GOLayout.GOLayoutDatabaseDir
                        +identifyLatestVersion(GOLayoutUtil.retrieveLocalFiles(
                        GOLayout.GOLayoutDatabaseDir), annotationSpeciesCode+
                        "_Derby", ".bridge")+".bridge");
                rAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
                setDefaultAttType("ID");
            }            
        }
//        rAnnIdeComboBox.setEnabled(false);
//        rAnnTypComboBox.setEnabled(false);

        //updates ui based on current network attributes
        checkAnnotationStatus();
        //checkDownloadStatus();
            
        aAttParComboBox.setModel(new DefaultComboBoxModel(
                checkAttributes(GOLayoutStaticValues.BP_ATTNAME).toArray()));
        aAttParComboBox.setSelectedItem(GOLayoutStaticValues.BP_ATTNAME);
        aAttParComboBox.addActionListener(this);
        aAttLayComboBox.setModel(new DefaultComboBoxModel(
                checkAttributes(GOLayoutStaticValues.CC_ATTNAME).toArray()));
        aAttLayComboBox.setSelectedItem(GOLayoutStaticValues.CC_ATTNAME);
        aAttNodComboBox.setModel(new DefaultComboBoxModel(
                checkAttributes(GOLayoutStaticValues.MF_ATTNAME).toArray()));
        aAttNodComboBox.setSelectedItem(GOLayoutStaticValues.MF_ATTNAME);
        
        if (!GOLayoutUtil.checkGPMLPlugin()) {
            lTepPreRadioButton.setEnabled(false);
            lTepPreComboBox.setEnabled(false);
            lTepCusRadioButton.setEnabled(false);
            lTepCusTextField.setEnabled(false);
            lTepCusButton.setEnabled(false);
        }
    }

    private String[] getSpeciesCommonName(String speName) {
        String[] result = {"", ""};
        for (String line : GOLayout.speciesMappinglist) {
            String tempMappingString = line.replace("\t", " ").toUpperCase();
            if(tempMappingString.indexOf(speName.toUpperCase())!=-1) {
                String[] s = line.split("\t");
                result[0] = s[2].trim();
                result[1] = s[3].trim();
                return result;
            }
        }
        return null;
    }

    private List<String> checkMappingResources(String species){
        List<String> downloadList = new ArrayList<String>();
        List<String> localFileList = new ArrayList<String>();

        String latestDerbyDB = identifyLatestVersion(GOLayout.derbyRemotelist,
                species+"_Derby", ".zip");
        String latestGOslimDB = identifyLatestVersion(GOLayout.goslimRemotelist,
                species+"_GOslim", ".zip");

        localFileList = GOLayoutUtil.retrieveLocalFiles(GOLayout.GOLayoutDatabaseDir);
        if(localFileList==null || localFileList.isEmpty()) {
            downloadList.add(GOLayoutStaticValues.bridgedbDerbyDir+latestDerbyDB+".zip");
            downloadList.add(GOLayoutStaticValues.genmappcsDatabaseDir+latestGOslimDB+".zip");
            System.out.println("No any local db, need download all");
        }  else {
            String localDerbyDB = identifyLatestVersion(localFileList,
                    species+"_Derby", ".bridge");
            if(latestDerbyDB.equals("")&&!localDerbyDB.equals(""))
                latestDerbyDB = localDerbyDB;
            //System.out.println("latestDerbyDB: "+latestDerbyDB);
            //System.out.println("localDerbyDB: "+localDerbyDB);
            if(localDerbyDB.equals("")||!localDerbyDB.equals(latestDerbyDB))
                downloadList.add(GOLayoutStaticValues.bridgedbDerbyDir+latestDerbyDB+".zip");
            String localGOslimDB = identifyLatestVersion(localFileList,
                    species+"_GOslim", ".tab");
            if(latestGOslimDB.equals("")&&!localGOslimDB.equals(""))
                latestGOslimDB = localGOslimDB;
            //System.out.println("latestGOslimDB: "+latestGOslimDB);
            //System.out.println("localGOslimDB: "+localGOslimDB);
            if(localGOslimDB.equals("")||!localGOslimDB.equals(latestGOslimDB))
                downloadList.add(GOLayoutStaticValues.genmappcsDatabaseDir+latestGOslimDB+".zip");
        }
        return downloadList;
    }

    private String identifyLatestVersion(List<String> dbList, String prefix, String surfix) {
        String result = "";
        int latestdate = 0;
        for (String filename : dbList) {
            Pattern p = Pattern.compile(prefix+"_\\d{8}\\"+surfix);
            Matcher m = p.matcher(filename);
            if(m.find()) {
                filename = m.group();
                String datestr = filename.substring(filename.lastIndexOf("_")+1,
                        filename.indexOf("."));
                if (datestr.matches("^\\d{8}$")) {
                    int date = new Integer(datestr);
                    if (date > latestdate) {
                        latestdate = date;
                        result = filename.substring(0,filename.lastIndexOf("."));
                    }
                }
            }
        }
        return result;
    }
    
    private void checkDownloadStatus() {
        if(downloadDBList.isEmpty()) {
            rAnnIdeComboBox.setEnabled(true);
            rAnnTypComboBox.setEnabled(true);
            rAnnMesButton.setText(this.annotationButtonLabel);
            if(this.annotationButtonLabel == "Re-annotate") {
                rAnnMesButton.setForeground(Color.BLACK);
                rAnnMesLabel.setText("You can re-annotate this network and old annotation will be replaced.");
                rAnnMesLabel.setForeground(Color.BLACK);
                submitButton.setEnabled(true);
            } else {
                rAnnMesButton.setForeground(Color.RED);
                rAnnMesLabel.setText("You need to first annotate this network with the GO-Slim terms selected above!");
                rAnnMesLabel.setForeground(Color.RED);
                submitButton.setEnabled(false);
            }
        } else {
            rAnnIdeComboBox.setEnabled(false);
            rAnnTypComboBox.setEnabled(false);
            if(!GOLayout.tagInternetConn) {
                rAnnMesButton.setText("Help!");
                rAnnMesButton.setForeground(Color.RED);
                rAnnMesLabel.setText("Please check internet connection!");
                rAnnMesLabel.setForeground(Color.RED);
            } else {
                System.out.println("it is right");
                rAnnMesButton.setText("Download");
                rAnnMesButton.setForeground(Color.RED);
                rAnnMesLabel.setText("You need to first download necessary databases for selected species!");
                rAnnMesLabel.setForeground(Color.RED);
            }
            submitButton.setEnabled(false);
        }
    }
    
    private void checkAnnotationStatus() {
        String partitionAttr = this.aAttParComboBox.getSelectedItem().toString();
        String layoutAttr = this.aAttLayComboBox.getSelectedItem().toString();
        String colorAttr = this.aAttNodComboBox.getSelectedItem().toString();
        List CurrentNetworkAtts = Arrays.asList(Cytoscape.getNodeAttributes()
                .getAttributeNames());
        int numberOfNodes = Cytoscape.getCurrentNetwork().nodesList().size();
        //If user didn't choose GO attribute for partition, disable 'The deepest level of GO term for partition'
        if(!isGOAttr(partitionAttr)) {
            sParLevComboBox.setEnabled(false);
            sParPatComboBox.setEnabled(false);
        } else {
            sParLevComboBox.setEnabled(true);
            sParPatComboBox.setEnabled(true);
        }
        if((isGOAttr(partitionAttr)&&(!CurrentNetworkAtts.contains(partitionAttr)||
                checkAnnotationRate(partitionAttr)==0))||
                (isGOAttr(layoutAttr)&&(!CurrentNetworkAtts.contains(layoutAttr)||
                checkAnnotationRate(layoutAttr)==0))||
                (isGOAttr(colorAttr)&&(!CurrentNetworkAtts.contains(colorAttr)||
                checkAnnotationRate(colorAttr)==0))) {
            //Any of three global settings is GO attribute and annotation rate equls 0.
            //Force user to fetch the annotations, and user can not turn off the annotation panel.
            submitButton.setEnabled(false);
            rAnnSpeComboBox.setEnabled(true);
            rAnnIdeComboBox.setEnabled(true);
            rAnnTypComboBox.setEnabled(true);
            rAnnMesButton.setEnabled(true);
            this.annotationButtonLabel = "Annotate";
            rAnnMesButton.setText(this.annotationButtonLabel);
            rAnnMesButton.setForeground(Color.RED);
            rAnnMesLabel.setText("You need to first annotate this network with the GO-Slim terms selected above!");
            rAnnMesLabel.setForeground(Color.RED);
        } else if(!(isGOAttr(partitionAttr)||isGOAttr(layoutAttr)||isGOAttr(colorAttr))) {
            //None of three global settings is GO attribute, user can not turn on the annotattion panel.
            submitButton.setEnabled(true);
            rAnnSpeComboBox.setEnabled(false);
            rAnnIdeComboBox.setEnabled(false);
            rAnnTypComboBox.setEnabled(false);
            rAnnMesButton.setEnabled(false);            
        } else {
            submitButton.setEnabled(true);
            rAnnSpeComboBox.setEnabled(true);
            rAnnIdeComboBox.setEnabled(true);
            rAnnTypComboBox.setEnabled(true);
            rAnnMesButton.setEnabled(true);
            this.annotationButtonLabel = "Re-annotate";
            rAnnMesButton.setText(this.annotationButtonLabel);
            rAnnMesButton.setForeground(Color.BLACK);
            rAnnMesLabel.setText("You can re-annotate this network and old annotation wiil be replaced.");
            rAnnMesLabel.setForeground(Color.BLACK);
        }

        aAttParTextField.setText(checkAnnotationRate(partitionAttr)+"/"+numberOfNodes+" attribute values");
        aAttLayTextField.setText(checkAnnotationRate(layoutAttr)+"/"+numberOfNodes+" attribute values");
        aAttNodTextField.setText(checkAnnotationRate(colorAttr)+"/"+numberOfNodes+" attribute values");
        checkDownloadStatus();
    }
    
    private boolean isGOAttr(String selectedAttribute) {
        if(selectedAttribute.equals(GOLayoutStaticValues.BP_ATTNAME)||
                selectedAttribute.equals(GOLayoutStaticValues.CC_ATTNAME)||
                selectedAttribute.equals(GOLayoutStaticValues.MF_ATTNAME)) {
            return true;
        } else {
            return false;
        }
    }
    
    private int checkAnnotationRate(String goAttribute) {
        int count = 0;
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        CyAttributes currentAttrs = Cytoscape.getNodeAttributes();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            if (currentAttrs.hasAttribute(cn.getIdentifier(), goAttribute)) {
                byte type = currentAttrs.getType(goAttribute);
                if (type == CyAttributes.TYPE_SIMPLE_LIST) {
                    List list = currentAttrs.getListAttribute(
                        cn.getIdentifier(), goAttribute);
                    if (list.size() > 1){
                        count++;
                    } else if (list.size() == 1){
                        if (list.get(0) != null)
                            if (!list.get(0).equals(""))
                                count++;
                    }
                } else if (type == CyAttributes.TYPE_STRING) {
                    if (!currentAttrs.getStringAttribute(cn.getIdentifier(),
                            goAttribute).equals("null"))
                        count++;
                } else {
                    //we don't have to be as careful with other attribute types
                    if (!currentAttrs.getAttribute(cn.getIdentifier(),
                            goAttribute).equals(null))
                        count++;
                }
            }
        }
        return count;
    }

    private List<String> checkAttributes(String goAttribute){
        List<String> result = new ArrayList();
        result.add("none");
        if(!currentAttributeList.contains(goAttribute))
            result.add(goAttribute);
        result.addAll(currentAttributeList);
        return result;
    }
    
    private void setDefaultAttType(String idName) {
        String sampleID = Cytoscape.getCurrentNetwork().nodesList().get(0)
                .toString();
        if(!idName.equals("ID"))
            sampleID = Cytoscape.getNodeAttributes().getAttribute(sampleID,
                    idName).toString();
        Set<String> guessResult = IdMapping.guessIdType(sampleID);
        if(guessResult.isEmpty()) {
            rAnnTypComboBox.setSelectedIndex(findMatchType("Ensembl"));
        } else {
            rAnnTypComboBox.setSelectedIndex(findMatchType(guessResult.toArray()[0]
                    .toString()));
        }
    }
    
    private int findMatchType(String matchSeq) {
        int i = idMappingTypeValues.indexOf(matchSeq);
        if(i==-1) {
            int n=0;
            for(String type:idMappingTypeValues) {
                if(type.trim().toLowerCase().indexOf("ensembl")!=-1)
                    return n;
                n++;
            }
            return 0;
        } else {
            return i;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        javax.swing.JPanel aSelPanel = new javax.swing.JPanel();
        aAttParLabel = new javax.swing.JLabel();
        aAttLayLabel = new javax.swing.JLabel();
        aAttNodLabel = new javax.swing.JLabel();
        aAttParComboBox = new javax.swing.JComboBox();
        aAttLayComboBox = new javax.swing.JComboBox();
        aAttNodComboBox = new javax.swing.JComboBox();
        aAttParTextField = new javax.swing.JTextField();
        aAttLayTextField = new javax.swing.JTextField();
        aAttNodTextField = new javax.swing.JTextField();
        rAnnPanel = new javax.swing.JPanel();
        rAnnMesLabel = new javax.swing.JLabel();
        rAnnMesButton = new javax.swing.JButton();
        rAnnSpeLabel = new javax.swing.JLabel();
        rAnnSpeComboBox = new javax.swing.JComboBox();
        rAnnIdeComboBox = new javax.swing.JComboBox();
        rAnnTypComboBox = new javax.swing.JComboBox();
        rAnnIdeLabel = new javax.swing.JLabel();
        rAnnTypLabel = new javax.swing.JLabel();
        lTepPanel = new javax.swing.JPanel();
        lTepPreRadioButton = new javax.swing.JRadioButton();
        lTepCusRadioButton = new javax.swing.JRadioButton();
        lTepPreComboBox = new javax.swing.JComboBox();
        lTepCusTextField = new javax.swing.JTextField();
        lTepCusButton = new javax.swing.JButton();
        ButtonPanel = new javax.swing.JPanel();
        submitButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        sParPanel = new javax.swing.JPanel();
        sParFewLabel = new javax.swing.JLabel();
        sParMorLabel = new javax.swing.JLabel();
        sParLevLabel = new javax.swing.JLabel();
        sParPatLabel = new javax.swing.JLabel();
        sParFewTextField = new javax.swing.JTextField();
        sParMorTextField = new javax.swing.JTextField();
        sParLevComboBox = new javax.swing.JComboBox();
        sParPatComboBox = new javax.swing.JComboBox();
        sParSpaLabel = new javax.swing.JLabel();
        sParSpaTextField = new javax.swing.JTextField();
        sParCroCheckBox = new javax.swing.JCheckBox();
        sParCroLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        aSelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Attributes"));

        aAttParLabel.setText("The attribute to use for partitioning");

        aAttLayLabel.setText("The attribute to use for the layout");

        aAttNodLabel.setText("The attribute to use for node color");

        aAttParComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "annotation.GO BIOLOGICAL_PROCESS" }));
        aAttParComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttParComboBoxActionPerformed(evt);
            }
        });

        aAttLayComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "annotation.GO CELLULAR_COMPONENT" }));
        aAttLayComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttLayComboBoxActionPerformed(evt);
            }
        });

        aAttNodComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "annotation.GO MOLECULAR_FUNCTION" }));
        aAttNodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttNodComboBoxActionPerformed(evt);
            }
        });

        aAttParTextField.setColumns(20);
        aAttParTextField.setEditable(false);
        aAttParTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        aAttParTextField.setText(" 0/100000 attribute values");
        aAttParTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aAttParTextFieldActionPerformed(evt);
            }
        });

        aAttLayTextField.setColumns(20);
        aAttLayTextField.setEditable(false);
        aAttLayTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        aAttLayTextField.setText("0/100000 attribute values");

        aAttNodTextField.setColumns(20);
        aAttNodTextField.setEditable(false);
        aAttNodTextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        aAttNodTextField.setText("0/100000 attribute values");

        javax.swing.GroupLayout aSelPanelLayout = new javax.swing.GroupLayout(aSelPanel);
        aSelPanel.setLayout(aSelPanelLayout);
        aSelPanelLayout.setHorizontalGroup(
            aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aSelPanelLayout.createSequentialGroup()
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(aAttParLabel)
                    .addComponent(aAttLayLabel)
                    .addComponent(aAttNodLabel))
                .addGap(18, 18, 18)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(aAttNodTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttLayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttParTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(aAttParComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttNodComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttLayComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        aSelPanelLayout.setVerticalGroup(
            aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(aSelPanelLayout.createSequentialGroup()
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aAttParLabel)
                    .addComponent(aAttParTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttParComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aAttLayLabel)
                    .addComponent(aAttLayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttLayComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(aSelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aAttNodLabel)
                    .addComponent(aAttNodTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(aAttNodComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        rAnnPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Retrieve GO-Slim Annotations (Optional)"));
        rAnnPanel.setPreferredSize(new java.awt.Dimension(562, 139));

        rAnnMesLabel.setForeground(java.awt.Color.red);
        rAnnMesLabel.setText("You need to first annotate this network with the GO-Slim terms selected above!");

        rAnnMesButton.setForeground(java.awt.Color.red);
        rAnnMesButton.setText("Annotate");
        rAnnMesButton.setPreferredSize(new java.awt.Dimension(75, 23));
        rAnnMesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rAnnMesButtonActionPerformed(evt);
            }
        });

        rAnnSpeLabel.setText("Species");
        rAnnSpeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        rAnnSpeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yeast" }));
        rAnnSpeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rAnnSpeComboBoxActionPerformed(evt);
            }
        });

        rAnnIdeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ID" }));
        rAnnIdeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rAnnIdeComboBoxActionPerformed(evt);
            }
        });

        rAnnTypComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Ensembl Yeast" }));

        rAnnIdeLabel.setText("The identifier to use for annotation retrieval");
        rAnnIdeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        rAnnTypLabel.setText("Type of identifier, e.g., Entrez Gene");
        rAnnTypLabel.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        javax.swing.GroupLayout rAnnPanelLayout = new javax.swing.GroupLayout(rAnnPanel);
        rAnnPanel.setLayout(rAnnPanelLayout);
        rAnnPanelLayout.setHorizontalGroup(
            rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rAnnPanelLayout.createSequentialGroup()
                .addComponent(rAnnMesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 389, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 152, Short.MAX_VALUE)
                .addComponent(rAnnMesButton, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(rAnnPanelLayout.createSequentialGroup()
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(rAnnIdeLabel)
                    .addComponent(rAnnTypLabel)
                    .addComponent(rAnnSpeLabel))
                .addGap(26, 26, 26)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rAnnSpeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnIdeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnTypComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(303, Short.MAX_VALUE))
        );
        rAnnPanelLayout.setVerticalGroup(
            rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rAnnPanelLayout.createSequentialGroup()
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnMesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rAnnMesButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnSpeLabel)
                    .addComponent(rAnnSpeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnIdeLabel)
                    .addComponent(rAnnIdeComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rAnnPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rAnnTypLabel)
                    .addComponent(rAnnTypComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lTepPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Layout Template"));

        buttonGroup1.add(lTepPreRadioButton);
        lTepPreRadioButton.setSelected(true);
        lTepPreRadioButton.setText("Choose prebuilt template");
        lTepPreRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lTepPreRadioButtonActionPerformed(evt);
            }
        });

        buttonGroup1.add(lTepCusRadioButton);
        lTepCusRadioButton.setText("Upload custom template");
        lTepCusRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lTepCusRadioButtonActionPerformed(evt);
            }
        });

        lTepPreComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lTepCusTextField.setText("C:\\test.gpml");
        lTepCusTextField.setEnabled(false);

        lTepCusButton.setText("upload");
        lTepCusButton.setEnabled(false);

        javax.swing.GroupLayout lTepPanelLayout = new javax.swing.GroupLayout(lTepPanel);
        lTepPanel.setLayout(lTepPanelLayout);
        lTepPanelLayout.setHorizontalGroup(
            lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lTepPanelLayout.createSequentialGroup()
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lTepCusRadioButton)
                    .addComponent(lTepPreRadioButton))
                .addGap(10, 10, 10)
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lTepPanelLayout.createSequentialGroup()
                        .addComponent(lTepCusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 408, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lTepCusButton))
                    .addComponent(lTepPreComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 479, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        lTepPanelLayout.setVerticalGroup(
            lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lTepPanelLayout.createSequentialGroup()
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lTepPreRadioButton)
                    .addComponent(lTepPreComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(lTepPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lTepCusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lTepCusRadioButton)
                    .addComponent(lTepCusButton)))
        );

        submitButton.setText("Run");
        submitButton.setMaximumSize(new java.awt.Dimension(70, 23));
        submitButton.setMinimumSize(new java.awt.Dimension(70, 23));
        submitButton.setPreferredSize(new java.awt.Dimension(70, 23));
        submitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                submitButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.setPreferredSize(new java.awt.Dimension(70, 23));

        javax.swing.GroupLayout ButtonPanelLayout = new javax.swing.GroupLayout(ButtonPanel);
        ButtonPanel.setLayout(ButtonPanelLayout);
        ButtonPanelLayout.setHorizontalGroup(
            ButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ButtonPanelLayout.createSequentialGroup()
                .addContainerGap(482, Short.MAX_VALUE)
                .addComponent(submitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        ButtonPanelLayout.setVerticalGroup(
            ButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ButtonPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(ButtonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(submitButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        sParPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Set Parameters"));

        sParFewLabel.setText("Don't show subnetworks with fewer nodes than");

        sParMorLabel.setText("Don't show subnetworks with more nodes than");

        sParLevLabel.setText("The deepest level of GO term for partition");

        sParPatLabel.setText("Path type");

        sParFewTextField.setColumns(5);
        sParFewTextField.setText("5");
        sParFewTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sParFewTextFieldActionPerformed(evt);
            }
        });

        sParMorTextField.setColumns(5);
        sParMorTextField.setText("200");

        sParLevComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Deepest Level" }));

        sParPatComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Relative Path", "Absolute Path" }));

        sParSpaLabel.setText("Spacing between nodes");

        sParSpaTextField.setColumns(5);
        sParSpaTextField.setText("30.0");

        sParCroLabel.setText("Prune cross- region edges?");

        javax.swing.GroupLayout sParPanelLayout = new javax.swing.GroupLayout(sParPanel);
        sParPanel.setLayout(sParPanelLayout);
        sParPanelLayout.setHorizontalGroup(
            sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sParPanelLayout.createSequentialGroup()
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParFewLabel)
                    .addComponent(sParMorLabel)
                    .addComponent(sParLevLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParLevComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(sParMorTextField, javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(sParFewTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 48, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 56, Short.MAX_VALUE)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParSpaLabel)
                    .addComponent(sParPatLabel)
                    .addComponent(sParCroLabel))
                .addGap(25, 25, 25)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sParSpaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParCroCheckBox)
                    .addComponent(sParPatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        sParPanelLayout.setVerticalGroup(
            sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sParPanelLayout.createSequentialGroup()
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sParFewLabel)
                        .addComponent(sParFewTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sParSpaLabel)
                        .addComponent(sParSpaTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sParMorLabel)
                        .addComponent(sParMorTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(sParCroLabel))
                    .addComponent(sParCroCheckBox))
                .addGap(5, 5, 5)
                .addGroup(sParPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sParLevLabel)
                    .addComponent(sParLevComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sParPatLabel)
                    .addComponent(sParPatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(sParPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lTepPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(rAnnPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 650, Short.MAX_VALUE)
                    .addComponent(aSelPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ButtonPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(aSelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(rAnnPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sParPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lTepPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void aAttParTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttParTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_aAttParTextFieldActionPerformed

    private void sParFewTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sParFewTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sParFewTextFieldActionPerformed

    private void aAttParComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttParComboBoxActionPerformed
        // TODO add your handling code here:
        checkAnnotationStatus();
    }//GEN-LAST:event_aAttParComboBoxActionPerformed

    private void aAttLayComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttLayComboBoxActionPerformed
        // TODO add your handling code here:
        checkAnnotationStatus();
    }//GEN-LAST:event_aAttLayComboBoxActionPerformed

    private void aAttNodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aAttNodComboBoxActionPerformed
        // TODO add your handling code here:
        checkAnnotationStatus();
    }//GEN-LAST:event_aAttNodComboBoxActionPerformed

    private void rAnnSpeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rAnnSpeComboBoxActionPerformed
        // TODO add your handling code here:
        //Regenerate list of ID types when user select another species.
        String[] speciesCode = getSpeciesCommonName(rAnnSpeComboBox.getSelectedItem().toString());
        annotationSpeciesCode = speciesCode[1];
        downloadDBList = checkMappingResources(annotationSpeciesCode);
        checkDownloadStatus();
        if(downloadDBList.isEmpty()) {
            idMappingTypeValues = IdMapping.getSourceTypes(GOLayout.GOLayoutDatabaseDir
                    +identifyLatestVersion(GOLayoutUtil.retrieveLocalFiles(
                    GOLayout.GOLayoutDatabaseDir), annotationSpeciesCode+
                    "_Derby", ".bridge")+".bridge");
            rAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
            }
        rAnnIdeComboBox.setSelectedItem("ID");
        setDefaultAttType("ID");
    }//GEN-LAST:event_rAnnSpeComboBoxActionPerformed

    private void rAnnIdeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rAnnIdeComboBoxActionPerformed
        // TODO add your handling code here:
        setDefaultAttType(rAnnIdeComboBox.getSelectedItem().toString());
    }//GEN-LAST:event_rAnnIdeComboBoxActionPerformed

    private void rAnnMesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rAnnMesButtonActionPerformed
        // TODO add your handling code here:
        if(((JButton)evt.getSource()).getText().equals("Download")) {
            System.out.println("download buttion on click");
//            FileDownloadDialog annDownloadDialog
//                = new FileDownloadDialog(Cytoscape.getDesktop(), downloadDBList);
//            annDownloadDialog.setLocationRelativeTo(Cytoscape.getDesktop());
//            annDownloadDialog.setSize(450, 100);
//            annDownloadDialog.setVisible(true);
            final JTaskConfig jTaskConfig = new JTaskConfig();
            jTaskConfig.setOwner(cytoscape.Cytoscape.getDesktop());
            jTaskConfig.displayCloseButton(true);
            jTaskConfig.displayCancelButton(false);
            jTaskConfig.displayStatus(true);
            jTaskConfig.setAutoDispose(true);
            jTaskConfig.setMillisToPopup(100);
            FileDownloadDialog task
                = new FileDownloadDialog(downloadDBList);
            TaskManager.executeTask(task, jTaskConfig);
            downloadDBList = checkMappingResources(annotationSpeciesCode);
            checkDownloadStatus();
            if(downloadDBList.isEmpty()) {
                idMappingTypeValues = IdMapping.getSourceTypes(GOLayout.GOLayoutDatabaseDir
                        +identifyLatestVersion(GOLayoutUtil.retrieveLocalFiles(
                        GOLayout.GOLayoutDatabaseDir), annotationSpeciesCode+
                        "_Derby", ".bridge")+".bridge");
                rAnnTypComboBox.setModel(new DefaultComboBoxModel(idMappingTypeValues.toArray()));
            }
            rAnnIdeComboBox.setSelectedItem("ID");
            setDefaultAttType("ID");            
        } else if (((JButton)evt.getSource()).getText().equals(this.annotationButtonLabel)) {
            String[] selectSpecies = getSpeciesCommonName(rAnnSpeComboBox.getSelectedItem().toString());
            //annotationSpeciesCode = speciesCode[1];
            if(!selectSpecies[0].equals("")) {
                List<String> localFileList = GOLayoutUtil.retrieveLocalFiles(
                    GOLayout.GOLayoutDatabaseDir);
                String localDerbyDB = GOLayout.GOLayoutDatabaseDir +
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_Derby", ".bridge") + ".bridge";
                String localGOslimDB = GOLayout.GOLayoutDatabaseDir+
                        identifyLatestVersion(localFileList,selectSpecies[1]+
                        "_GOslim", ".tab") + ".tab";
                final JTaskConfig jTaskConfig = new JTaskConfig();
                jTaskConfig.setOwner(cytoscape.Cytoscape.getDesktop());
                jTaskConfig.displayCloseButton(true);
                jTaskConfig.displayCancelButton(false);
                jTaskConfig.displayStatus(true);
                jTaskConfig.setAutoDispose(true);
                jTaskConfig.setMillisToPopup(100);
                AnnotationDialog task = new AnnotationDialog(localDerbyDB,
                        localGOslimDB, rAnnIdeComboBox.getSelectedItem().toString(),
                        rAnnTypComboBox.getSelectedItem().toString(),
                        idMappingTypeValues.get(findMatchType("Ensembl")));
                TaskManager.executeTask(task, jTaskConfig);
                this.annotationButtonLabel = "Re-annotate";
                rAnnMesButton.setText(this.annotationButtonLabel);
                rAnnMesButton.setForeground(Color.BLACK);
                rAnnMesLabel.setText("You can re-annotate this network and old annotation will be replaced.");
                rAnnMesLabel.setForeground(Color.BLACK);
                checkAnnotationStatus();
            } else {
                System.out.println("Retrive species error!");
            }
        } else if (((JButton)evt.getSource()).getText().equals("Help!")) {
            JOptionPane.showConfirmDialog(Cytoscape.getDesktop(),
                    "You need internet connection for downloading databases!",
                    "Warning", JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_rAnnMesButtonActionPerformed

    private void submitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_submitButtonActionPerformed
        // TODO add your handling code here:
        beforeSubmit();
        CellAlgorithm.attributeName = null;
        PartitionNetworkVisualStyleFactory.attributeName = null;

        if (null != CellAlgorithm.attributeName) {
            PartitionAlgorithm.layoutName = CellAlgorithm.LAYOUT_NAME;
        }
        System.out.println(PartitionNetworkVisualStyleFactory.attributeName);
//        CyLayoutAlgorithm layout = CyLayouts.getLayout("partition");
//        layout.doLayout(Cytoscape.getCurrentNetworkView(), taskMonitor);
        final JTaskConfig jTaskConfig = new JTaskConfig();
            jTaskConfig.setOwner(Cytoscape.getDesktop());
            jTaskConfig.displayCloseButton(false);
            jTaskConfig.displayCancelButton(false);
            jTaskConfig.displayStatus(true);
            jTaskConfig.setAutoDispose(true);
            jTaskConfig.setMillisToPopup(100); // always pop the task

            // Execute Task in New Thread; pop open JTask Dialog Box.
            TaskManager.executeTask(new RunLayout(), jTaskConfig);
    }//GEN-LAST:event_submitButtonActionPerformed

    private void lTepPreRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lTepPreRadioButtonActionPerformed
        // TODO add your handling code here:
        if(lTepPreRadioButton.isSelected()) {
            lTepPreComboBox.setEnabled(true);
            lTepCusTextField.setEnabled(false);
            lTepCusButton.setEnabled(false);
        } else {
            lTepPreComboBox.setEnabled(false);
            lTepCusTextField.setEnabled(true);
            lTepCusButton.setEnabled(true);
        }
    }//GEN-LAST:event_lTepPreRadioButtonActionPerformed

    private void lTepCusRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lTepCusRadioButtonActionPerformed
        // TODO add your handling code here:
        if(lTepCusRadioButton.isSelected()) {
            lTepPreComboBox.setEnabled(false);
            lTepCusTextField.setEnabled(true);
            lTepCusButton.setEnabled(true);
        } else {
            lTepPreComboBox.setEnabled(true);
            lTepCusTextField.setEnabled(false);
            lTepCusButton.setEnabled(false);
        }
    }//GEN-LAST:event_lTepCusRadioButtonActionPerformed

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                GOLayoutSettingDialog dialog = new GOLayoutSettingDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ButtonPanel;
    private javax.swing.JComboBox aAttLayComboBox;
    private javax.swing.JLabel aAttLayLabel;
    private javax.swing.JTextField aAttLayTextField;
    private javax.swing.JComboBox aAttNodComboBox;
    private javax.swing.JLabel aAttNodLabel;
    private javax.swing.JTextField aAttNodTextField;
    private javax.swing.JComboBox aAttParComboBox;
    private javax.swing.JLabel aAttParLabel;
    private javax.swing.JTextField aAttParTextField;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton lTepCusButton;
    private javax.swing.JRadioButton lTepCusRadioButton;
    private javax.swing.JTextField lTepCusTextField;
    private javax.swing.JPanel lTepPanel;
    private javax.swing.JComboBox lTepPreComboBox;
    private javax.swing.JRadioButton lTepPreRadioButton;
    private javax.swing.JComboBox rAnnIdeComboBox;
    private javax.swing.JLabel rAnnIdeLabel;
    private javax.swing.JButton rAnnMesButton;
    private javax.swing.JLabel rAnnMesLabel;
    private javax.swing.JPanel rAnnPanel;
    private javax.swing.JComboBox rAnnSpeComboBox;
    private javax.swing.JLabel rAnnSpeLabel;
    private javax.swing.JComboBox rAnnTypComboBox;
    private javax.swing.JLabel rAnnTypLabel;
    private javax.swing.JCheckBox sParCroCheckBox;
    private javax.swing.JLabel sParCroLabel;
    private javax.swing.JLabel sParFewLabel;
    private javax.swing.JTextField sParFewTextField;
    private javax.swing.JComboBox sParLevComboBox;
    private javax.swing.JLabel sParLevLabel;
    private javax.swing.JLabel sParMorLabel;
    private javax.swing.JTextField sParMorTextField;
    private javax.swing.JPanel sParPanel;
    private javax.swing.JComboBox sParPatComboBox;
    private javax.swing.JLabel sParPatLabel;
    private javax.swing.JLabel sParSpaLabel;
    private javax.swing.JTextField sParSpaTextField;
    private javax.swing.JButton submitButton;
    // End of variables declaration//GEN-END:variables

    public void actionPerformed(ActionEvent e) {
//        System.out.println(e.getClass());
    }

    private void beforeSubmit() {
        if (aAttParComboBox.getSelectedItem().equals("(none)")) {
            PartitionAlgorithm.attributeName = null;
        } else {
            PartitionAlgorithm.attributeName = aAttParComboBox.getSelectedItem().toString();
        }
        if (aAttLayComboBox.getSelectedItem().equals("(none)")) {
            CellAlgorithm.attributeName = null;
        } else {
            CellAlgorithm.attributeName = aAttLayComboBox.getSelectedItem().toString();
        }
        if (aAttNodComboBox.getSelectedItem().equals("(none)")) {
            PartitionNetworkVisualStyleFactory.attributeName = null;
        } else {
            PartitionNetworkVisualStyleFactory.attributeName = aAttNodComboBox.getSelectedItem().toString();
        }
        PartitionAlgorithm.NETWORK_LIMIT_MIN = new Integer(sParFewTextField.getText()).intValue();
        PartitionAlgorithm.NETWORK_LIMIT_MAX = new Integer(sParMorTextField.getText()).intValue();
        CellAlgorithm.distanceBetweenNodes = new Double(sParSpaTextField.getText()).doubleValue();
        CellAlgorithm.pruneEdges = sParCroCheckBox.isSelected();
    }

    private class RunLayout implements Task {
        private TaskMonitor taskMonitor;

        public RunLayout() {
        }

        /**
         * Executes Task.
         */
        //@Override
        public void run() {
                try {
                    taskMonitor.setStatus("Runing partition...");
                    CyLayoutAlgorithm layout = CyLayouts.getLayout("partition");
                    //layout.doLayout(Cytoscape.getCurrentNetworkView(), taskMonitor);
                    layout.doLayout(Cytoscape.getCurrentNetworkView());
                    taskMonitor.setPercentCompleted(100);
                } catch (Exception e) {
                        taskMonitor.setPercentCompleted(100);
                        taskMonitor.setStatus("Failed.\n");
                        e.printStackTrace();
                }
        }

        /**
         * Halts the Task: Not Currently Implemented.
         */
        //@Override
        public void halt() {

        }

        /**
         * Sets the Task Monitor.
         *
         * @param taskMonitor
         *            TaskMonitor Object.
         */
        //@Override
        public void setTaskMonitor(TaskMonitor taskMonitor) throws IllegalThreadStateException {
                this.taskMonitor = taskMonitor;
        }

        /**
         * Gets the Task Title.
         *
         * @return Task Title.
         */
        //@Override
        public String getTitle() {
                return "Runing partition...";
        }
    }
}
