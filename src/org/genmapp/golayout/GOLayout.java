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

import cytoscape.CyNetwork;
import org.genmapp.golayout.partition.PartitionAlgorithm;
import org.genmapp.golayout.layout.PartitionNetworkVisualStyleFactory;
import org.genmapp.golayout.layout.CellAlgorithm;
import org.genmapp.golayout.utils.GOLayoutStaticValues;
import org.genmapp.golayout.setting.GOLayoutAlgorithm;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayouts;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.NetworkPanel;
import cytoscape.view.cytopanels.CytoPanel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
//import org.genmapp.golayout.tree.WorkspacesPanel;
import org.genmapp.golayout.partition.GOLayoutNetworkPanel;
import org.genmapp.golayout.setting.GOLayoutSettingDialog;
import org.genmapp.golayout.utils.GOLayoutUtil;


public class GOLayout extends CytoscapePlugin{
    public static String GOLayoutBaseDir;
    public static String GOLayoutDatabaseDir;
    public static boolean tagInternetConn;
    public static boolean tagGPMLPlugin;
    public static boolean tagCyComPlugin;
    public static boolean tagNodePlugin;
    public static List<String> derbyRemotelist = new ArrayList<String>();
    public static List<String> goslimRemotelist = new ArrayList<String>();
    public static List<String> speciesMappinglist = new ArrayList<String>();
    private static final String HELP = "GOLayout Help";
	
    /**
     * The constructor registers our layout algorithm. The CyLayouts mechanism
     * will worry about how to get it in the right menu, etc.
     */
    public GOLayout(){
        try {
            GOLayoutBaseDir = PluginManager.getPluginManager().getPluginManageDirectory().getCanonicalPath() + "/GOLayout/";
        } catch (IOException e) {
            GOLayoutBaseDir = "/GOLayout/";
            e.printStackTrace();
        }
        GOLayoutDatabaseDir=GOLayoutBaseDir+"/DB/";
        speciesMappinglist = GOLayoutUtil.readResource(this.getClass().getResource(GOLayoutStaticValues.bridgedbSpecieslist));
        //Check internet connection
        GOLayout.tagInternetConn = GOLayoutUtil.checkConnection();
        if(GOLayout.tagInternetConn) {
            //Get the lastest db lists
            derbyRemotelist = GOLayoutUtil.readUrl(GOLayoutStaticValues.bridgedbDerbyDir);
            //GOLayoutUtil.writeFile(derbyRemotelist, GOLayoutBaseDir+"derbyDBList.txt");
            goslimRemotelist = GOLayoutUtil.readUrl(GOLayoutStaticValues.genmappcsDatabaseDir);
            //GOLayoutUtil.writeFile(derbyRemotelist, GOLayoutBaseDir+"goslimDBList.txt");
        }
//        } else {
//            if(new File(GOLayoutBaseDir+"derbyDBList.txt").exists())
//                derbyRemotelist = GOLayoutUtil.readFile(GOLayoutBaseDir+"derbyDBList.txt");
//            if(new File(GOLayoutBaseDir+"goslimDBList.txt").exists())
//                goslimRemotelist = GOLayoutUtil.readFile(GOLayoutBaseDir+"goslimDBList.txt");
//        }
        
        CyLayouts.addLayout(new GOLayoutAlgorithm(), "GO Layout");
        CyLayouts.addLayout(new PartitionAlgorithm(), null);
        CyLayouts.addLayout(new CellAlgorithm(), null);
        //CyLayouts.addLayout(new IdMapping(), "IdMapping");
        // Add GOLayout menu item
        JMenuItem item = new JMenuItem("GOLayout");
        JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
                .getMenu("Plugins");
        item.addActionListener(new GOLayoutPluginActionListener(this));
        layoutMenu.add(item);
        // Add help menu item
        JMenuItem getHelp = new JMenuItem(HELP);
        getHelp.setToolTipText("Open online help for GOLayout");
        GetHelpListener getHelpListener = new GetHelpListener();
        getHelp.addActionListener(getHelpListener);
        Cytoscape.getDesktop().getCyMenus().getHelpMenu().add(getHelp);

        // create workspaces panel
        CytoPanel cytoPanel1 = Cytoscape.getDesktop().getCytoPanel(
                SwingConstants.WEST);
        //WorkspacesPanel wsPanel = new WorkspacesPanel();
        GOLayoutNetworkPanel wsPanel = new GOLayoutNetworkPanel(Cytoscape.getDesktop());
        cytoPanel1.add("GOLayout", null, wsPanel, "GOLayout Panel", 1);
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
                dialog.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
                        "Please load a network first!", "GOLayout",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}

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
	private String helpURL = "http://genmapp.org/GOLayout/GOLayout.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}
}