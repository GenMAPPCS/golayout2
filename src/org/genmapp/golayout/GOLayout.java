/*******************************************************************************
 * Copyright 2010 Alexander Pico
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

import org.genmapp.golayout.partition.PartitionAlgorithm;
import org.genmapp.golayout.layout.PartitionNetworkVisualStyleFactory;
import org.genmapp.golayout.layout.CellAlgorithm;
import org.genmapp.golayout.utils.GOLayoutStaticValues;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayouts;
import cytoscape.logger.CyLogger;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import org.genmapp.golayout.partition.GOLayoutNetworkPanel;
import org.genmapp.golayout.setting.GOLayoutSettingDialog;
import org.genmapp.golayout.utils.GOLayoutUtil;
//import org.genmapp.golayout.utils.GpmlTest;
import org.genmapp.golayout.utils.IdMapping;


public class GOLayout extends CytoscapePlugin{
    public static final String pluginName = "Mosaic";
    private CyLogger logger;
    public static String GOLayoutBaseDir;
    public static String GOLayoutDatabaseDir;
    public static String GOLayoutTemplateDir;
    public static boolean tagInternetConn;
    public static boolean tagGPMLPlugin;
    public static boolean tagNodePlugin;
    public static List<String> derbyRemotelist = new ArrayList<String>();
    public static List<String> goslimRemotelist = new ArrayList<String>();
    public static List<String> speciesMappinglist = new ArrayList<String>();
    private static final String HELP = pluginName + " Help";
    private PartitionAlgorithm partitionObject;
    public static GOLayoutNetworkPanel wsPanel;
	
    /**
     * The constructor registers our layout algorithm. The CyLayouts mechanism
     * will worry about how to get it in the right menu, etc.
     */
    public GOLayout(){
        logger = CyLogger.getLogger(GOLayout.class);
		logger.setDebug(true);
        try {
            GOLayoutBaseDir = PluginManager.getPluginManager().getPluginManageDirectory()
                    .getCanonicalPath() + File.separator+ GOLayout.pluginName + File.separator;
        } catch (IOException e) {
            GOLayoutBaseDir = File.separator+ GOLayout.pluginName + File.separator;
            e.printStackTrace();
        }
        GOLayoutUtil.checkFolder(GOLayoutBaseDir);
        GOLayoutDatabaseDir=GOLayoutBaseDir+"/DB/";
        GOLayoutTemplateDir=GOLayoutBaseDir+"/Temp/";
        GOLayoutUtil.checkFolder(GOLayoutDatabaseDir);
        GOLayoutUtil.checkFolder(GOLayoutTemplateDir);
        speciesMappinglist = GOLayoutUtil.readResource(this.getClass()
                .getResource(GOLayoutStaticValues.bridgedbSpecieslist));
        //Check internet connection
        GOLayout.tagInternetConn = GOLayoutUtil.checkConnection();
        if(GOLayout.tagInternetConn) {
            //Get the lastest db lists
            derbyRemotelist = GOLayoutUtil.readUrl(GOLayoutStaticValues.bridgedbDerbyDir);
            //GOLayoutUtil.writeFile(derbyRemotelist, GOLayoutBaseDir+"derbyDBList.txt");
            goslimRemotelist = GOLayoutUtil.readUrl(GOLayoutStaticValues.genmappcsDatabaseDir);
            //GOLayoutUtil.writeFile(derbyRemotelist, GOLayoutBaseDir+"goslimDBList.txt");
            List<String> supportedSpeList = GOLayoutUtil.readUrl(GOLayoutStaticValues.genmappcsDatabaseDir+GOLayoutStaticValues.supportedSpecieslist);
            if(supportedSpeList.size()>0) {
                GOLayoutUtil.writeFile(supportedSpeList, GOLayoutBaseDir+GOLayoutStaticValues.supportedSpecieslist);
                GOLayoutStaticValues.speciesList = GOLayoutUtil.parseSpeciesList(supportedSpeList);
            }
        } else {
            if(new File(GOLayoutBaseDir+GOLayoutStaticValues.supportedSpecieslist).exists()) {
                List<String> supportedSpeList = GOLayoutUtil.readFile(GOLayoutBaseDir+GOLayoutStaticValues.supportedSpecieslist);
                if(supportedSpeList.size()>0) {
                    GOLayoutStaticValues.speciesList = GOLayoutUtil.parseSpeciesList(supportedSpeList);
                }
            }            
        }
        GOLayout.tagGPMLPlugin = GOLayoutUtil.checkGPMLPlugin();
        partitionObject = new PartitionAlgorithm();
        //CyLayouts.addLayout(new GOLayoutAlgorithm(), "GO Layout");
        CyLayouts.addLayout(partitionObject, null);
        CyLayouts.addLayout(new CellAlgorithm(), null);
        //CyLayouts.addLayout(new GpmlTest(), "GPML test");
        // Add GOLayout menu item
        JMenuItem item = new JMenuItem(pluginName);
        JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
                .getMenu("Plugins");
        item.addActionListener(new GOLayoutPluginActionListener(this));
        layoutMenu.add(item);
//        //for gpml test
//        item = new JMenuItem("GPML test");
//        layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
//                .getMenu("Plugins");
//        item.addActionListener(new GpmlActionListener(this));
//        layoutMenu.add(item);
        // Add help menu item
        JMenuItem getHelp = new JMenuItem(HELP);
        getHelp.setToolTipText("Open online help for " + pluginName);
        GetHelpListener getHelpListener = new GetHelpListener();
        getHelp.addActionListener(getHelpListener);
        Cytoscape.getDesktop().getCyMenus().getHelpMenu().add(getHelp);

        // create workspaces panel
        CytoPanel cytoPanel1 = Cytoscape.getDesktop().getCytoPanel(
                SwingConstants.WEST);
        //WorkspacesPanel wsPanel = new WorkspacesPanel();
        //GOLayoutNetworkPanel wsPanel = new GOLayoutNetworkPanel(Cytoscape.getDesktop());
        wsPanel = new GOLayoutNetworkPanel(logger, partitionObject);
        cytoPanel1.add(pluginName, null, wsPanel, pluginName+" Panel", 1);
//        cytoPanel1.add("GenMAPP-CS", new ImageIcon(getClass().getResource(
//                "images/genmappcs.png")), wsPanel, "Workspaces Panel", 0);
        cytoPanel1.setSelectedIndex(0);
//        // cytoPanel.remove(1);
//
//        // set properties
//        // set view thresholds to handle "overview" xGMMLs
//        CytoscapeInit.getProperties().setProperty("viewThreshold", "100000");
//        CytoscapeInit.getProperties().setProperty("secondaryViewThreshold",
//                        "120000");
//        // set default node width/height lock to avoid dependency issues
//        Cytoscape.getVisualMappingManager().getVisualStyle().getDependency()
//                        .set(VisualPropertyDependency.Definition.NODE_SIZE_LOCKED,
//                                        false);
//        // cycommands
//        //new WorkspacesCommandHandler();
    }

    public static void createVisualStyle(CyNetworkView view) {
        PartitionNetworkVisualStyleFactory.createVisualStyle(view);
    }    
}

// Handles the top-level menu selection event from Cytoscape
class GOLayoutPluginActionListener implements ActionListener {
    GOLayout plugin = null;

    public GOLayoutPluginActionListener(GOLayout plugin_) {
        plugin = plugin_;
    }

    public void actionPerformed(ActionEvent evt_) {
        try {
            if(!GOLayoutUtil.checkCyThesaurus()) {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "Please install CyThesaurus plugin first!", GOLayout.pluginName,
                        JOptionPane.WARNING_MESSAGE);
            } else {

                if(Cytoscape.getNetworkSet().size()>0) {
                    NewDialogTask task = new NewDialogTask();

                    final JTaskConfig jTaskConfig = new JTaskConfig();
                    jTaskConfig.setOwner(Cytoscape.getDesktop());
                    jTaskConfig.displayCloseButton(false);
                    jTaskConfig.displayCancelButton(false);
                    jTaskConfig.displayStatus(true);
                    jTaskConfig.setAutoDispose(true);
                    jTaskConfig.setMillisToPopup(100); // always pop the task

                    // Execute Task in New Thread; pop open JTask Dialog Box.
                    TaskManager.executeTask(task, jTaskConfig);

                    final GOLayoutSettingDialog dialog = task.dialog();
                    dialog.addWindowListener(new WindowAdapter(){
                        public void windowClosed(WindowEvent e){
                            IdMapping.disConnectDerbyFileSource(GOLayout.GOLayoutDatabaseDir
                        +dialog.identifyLatestVersion(GOLayoutUtil.retrieveLocalFiles(
                        GOLayout.GOLayoutDatabaseDir), dialog.annotationSpeciesCode+
                        "_Derby", ".bridge")+".bridge");
                        }
                    });
                    dialog.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                            "Please load a network first!", GOLayout.pluginName,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}

//class GpmlActionListener implements ActionListener {
//    GOLayout plugin = null;
//
//    public GpmlActionListener(GOLayout plugin_) {
//        plugin = plugin_;
//    }
//
//    public void actionPerformed(ActionEvent evt_) {
//        try {
//            if(Cytoscape.getNetworkSet().size()>0) {
//                new GpmlTest();
//            } else {
//                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
//                        "Please load a network first!", GOLayout.pluginName,
//                        JOptionPane.WARNING_MESSAGE);
//            }
//        } catch (Exception e) {
//            System.out.println("Exception: " + e);
//        }
//    }
//}

class NewDialogTask implements Task {
    private TaskMonitor taskMonitor;
    private GOLayoutSettingDialog dialog;

    public NewDialogTask() {
    }

    /**
     * Executes Task.
     */
    //@Override
    public void run() {
        try {
            taskMonitor.setStatus("Initializing...");
            dialog = new GOLayoutSettingDialog(Cytoscape.getDesktop(), true);
            dialog.setLocationRelativeTo(Cytoscape.getDesktop());
            dialog.setResizable(true);
            taskMonitor.setPercentCompleted(100);
        } catch (Exception e) {
            taskMonitor.setPercentCompleted(100);
            taskMonitor.setStatus("Failed.\n");
            e.printStackTrace();
        }
    }

    public GOLayoutSettingDialog dialog() {
        return dialog;
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
        return "Initializing...";
    }
}

/**
 * This class direct a browser to the help manual web page.
 */
class GetHelpListener implements ActionListener {
	//private String helpURL = "http://genmapp.org/GOLayout/GOLayout.html";
    private String helpURL = "http://pinscher.ucsf.edu/meta/trunk/mosaic/index.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}
}