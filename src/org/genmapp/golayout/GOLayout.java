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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayouts;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingConstants;
import org.genmapp.golayout.tree.WorkspacesPanel;
import org.genmapp.golayout.utils.GOLayoutUtil;


public class GOLayout extends CytoscapePlugin{
    public static String GOLayoutBaseDir;
    public static boolean tagInternetConn;
    public static boolean tagGPMLPlugin;
    public static boolean tagCyComPlugin;
    public static boolean tagNodePlugin;
    public static List<String> derbyRemotelist = new ArrayList<String>();
    public static List<String> goslimRemotelist = new ArrayList<String>();
	
    /**
     * The constructor registers our layout algorithm. The CyLayouts mechanism
     * will worry about how to get it in the right menu, etc.
     */
    public GOLayout() {
        try {
            GOLayoutBaseDir = PluginManager.getPluginManager().getPluginManageDirectory().getCanonicalPath() + "/GOLayout/";
        } catch (IOException e) {
            GOLayoutBaseDir = "/GOLayout/";
            e.printStackTrace();
        }
        //Check internet connection
        GOLayout.tagInternetConn = GOLayoutUtil.checkConnection();
        if(GOLayout.tagInternetConn) {
            //Get the lastest db lists
            derbyRemotelist = GOLayoutUtil.readUrl(GOLayoutStaticValues.bridgedbDerbyDir);
            goslimRemotelist = GOLayoutUtil.readUrl(GOLayoutStaticValues.genmappcsDatabaseDir);
        }
        
        CyLayouts.addLayout(new GOLayoutAlgorithm(), "GO Layout");
        CyLayouts.addLayout(new PartitionAlgorithm(), null);
        CyLayouts.addLayout(new CellAlgorithm(), null);
        //CyLayouts.addLayout(new IdMapping(), "IdMapping");

        // JMenuItem item = new JMenuItem("Add GO-slim annotations");
        // JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
        // .getMenu("Layout");
        // item.addActionListener(new AddAnnotationCommandListener());
        //
        // layoutMenu.add(item);
        // create workspaces panel
        CytoPanel cytoPanel1 = Cytoscape.getDesktop().getCytoPanel(
                SwingConstants.WEST);
        WorkspacesPanel wsPanel = new WorkspacesPanel();
        cytoPanel1.add("GOLayout", wsPanel);
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

/**
 * This class direct a browser to the help manual web page.
 */
class GetHelpListener implements ActionListener {
	private String helpURL = "http://genmapp.org/GOLayout/GOLayout.html";

	public void actionPerformed(ActionEvent ae) {
		cytoscape.util.OpenBrowser.openURL(helpURL);
	}

}
