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
package org.genmapp.golayout.partition;

import cytoscape.CyNetwork;
import cytoscape.CyNetworkTitleChange;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.actions.ApplyVisualStyleAction;
import cytoscape.actions.CreateNetworkViewAction;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import cytoscape.groups.CyGroup;
import cytoscape.groups.CyGroupManager;
import cytoscape.logger.CyLogger;
import cytoscape.util.CyNetworkNaming;
import cytoscape.util.swing.JTreeTable;
import cytoscape.view.ColumnTypes;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.NetworkTreeTableModel;
import cytoscape.view.TreeCellRenderer;
import giny.model.Node;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.genmapp.golayout.layout.PartitionNetworkVisualStyleFactory;
import org.genmapp.golayout.utils.GOLayoutStaticValues;
import org.genmapp.golayout.utils.GOLayoutUtil;



/**
 * GUI component for managing network list in current session.
 */
public class GOLayoutNetworkPanel extends JPanel implements PropertyChangeListener,
			TreeSelectionListener, SelectEventListener, ChangeListener {

	private static final int DEF_DEVIDER_LOCATION = 280;
	private static final int PANEL_PREFFERED_WIDTH = 250;

	private static final int DEF_ROW_HEIGHT = 20;

	// Make this panel as a source of events.
	private final SwingPropertyChangeSupport pcs;

	public final JTreeTable treeTable;
	private final GOLayoutNetworkTreeNode root;
	private JPanel functionPanel;
    private JComboBox functionComboBox;
    private ChangeFunctionListener changeFunctionListener;
	private JPanel networkTreePanel;
	private JPopupMenu popup;
	private PopupActionListener popupActionListener;
	private JMenuItem createViewItem;
	private JMenuItem destroyViewItem;
	private JMenuItem destroyNetworkItem;
	private JMenuItem editNetworkTitle;
    private JMenuItem destroyALLNetworkItem;
    private JMenu partitionNetworkMenu;
	private JMenu applyVisualStyleMenu;

	private boolean doNotEnterValueChanged = false;
	public final NetworkTreeTableModel networkTreeTableModel;

	private CyLogger logger;
    private PartitionAlgorithm partitionObject;

    public static Map<String, String> descGOMappingFile = new HashMap<String, String>();
	/**
	 * Constructor for the Network Panel.
	 *
	 * @param desktop
	 */
	public GOLayoutNetworkPanel(CyLogger cyLogger, PartitionAlgorithm partitionObject) {
		super();
		this.logger = cyLogger;
        this.partitionObject = partitionObject;

		root = new GOLayoutNetworkTreeNode("Network Root", "nroot");
		networkTreeTableModel = new NetworkTreeTableModel(root);

		treeTable = new JTreeTable(networkTreeTableModel);
		treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		initialize();

		/*
		 * Remove CTR-A for enabling select all function in the main window.
		 */
		for (KeyStroke listener : treeTable.getRegisteredKeyStrokes()) {
			if (listener.toString().equals("ctrl pressed A")) {
				final InputMap map = treeTable.getInputMap();
				map.remove(listener);
				treeTable.setInputMap(WHEN_FOCUSED, map);
				treeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
			}
		}

		pcs = Cytoscape.getSwingPropertyChangeSupport();

		// Make this a prop change listener for Cytoscape global events.
		Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);
		Cytoscape.getDesktop().getSwingPropertyChangeSupport()
				.addPropertyChangeListener(Cytoscape.getDesktop()
                .NETWORK_VIEW_FOCUSED, this);

		// For listening to adding/removing Visual Style events.
		Cytoscape.getVisualMappingManager().addChangeListener(this);
	}

	/**
	 * Initialize GUI components
	 */
	private void initialize() {
        setLayout(new BorderLayout());
		setPreferredSize(new Dimension(PANEL_PREFFERED_WIDTH, 700));
		setMinimumSize(new Dimension(PANEL_PREFFERED_WIDTH, PANEL_PREFFERED_WIDTH));

		networkTreePanel = new JPanel();
        networkTreePanel.setBorder(BorderFactory.createTitledBorder(null,"",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", 1, 12), Color.darkGray));
		networkTreePanel.setLayout(new BoxLayout(networkTreePanel, BoxLayout.Y_AXIS));

		treeTable.getTree().addTreeSelectionListener(this);
		treeTable.getTree().setRootVisible(false);
		ToolTipManager.sharedInstance().registerComponent(treeTable);
        TreeCellRenderer treeCellRenderer = new TreeCellRenderer();
//		ImageIcon collapseIcon = new ImageIcon(GOLayout.class.getResource("/images/GO-collapse.png"));
//		if (collapseIcon != null) {
//			treeCellRenderer.setOpenIcon(collapseIcon);
//		}
//        ImageIcon expandIcon = new ImageIcon(GOLayout.class.getResource("/images/GO-expand.png"));
//		if (expandIcon != null) {
//			treeCellRenderer.setClosedIcon(expandIcon);
//		}
//		treeTable.getTree().setCellRenderer(treeCellRenderer);
        treeTable.getTree().setCellRenderer(new TreeCellRenderer());
		resetTable();

        final JScrollPane scroll = new JScrollPane(treeTable);
		networkTreePanel.add(scroll);

		functionPanel = new JPanel();
        functionPanel.setBorder(BorderFactory.createTitledBorder(null,"Node color selection",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", 1, 12), Color.darkGray));
		functionPanel.setMinimumSize(new Dimension(PANEL_PREFFERED_WIDTH, 50));
        functionPanel.setMaximumSize(new Dimension(10000, 50));
		functionPanel.setPreferredSize(new Dimension(PANEL_PREFFERED_WIDTH, 50));
        String[] functionList = {"Show all", "---------"};
        functionComboBox = new JComboBox(functionList);
        functionPanel.setLayout(new BoxLayout(functionPanel, BoxLayout.Y_AXIS));
        functionPanel.add(functionComboBox);
        changeFunctionListener = new ChangeFunctionListener();
        functionComboBox.addActionListener(changeFunctionListener);
        functionComboBox.setEnabled(false);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(functionPanel);
        contentPanel.add(networkTreePanel);
        add(contentPanel);

		// this mouse listener listens for the right-click event and will show
		// the pop-up
		// window when that occurrs
		treeTable.addMouseListener(new PopupListener());

		// create and populate the popup window
		popup = new JPopupMenu();
		editNetworkTitle = new JMenuItem(PopupActionListener.EDIT_TITLE);
		createViewItem = new JMenuItem(PopupActionListener.CREATE_VIEW);
		destroyViewItem = new JMenuItem(PopupActionListener.DESTROY_VIEW);
		destroyNetworkItem = new JMenuItem(PopupActionListener.DESTROY_NETWORK);
        destroyALLNetworkItem = new JMenuItem(PopupActionListener.DESTROY_ALL);
        partitionNetworkMenu = new JMenu(PopupActionListener.PARTITION_NETWORK);
		applyVisualStyleMenu = new JMenu(PopupActionListener.APPLY_VISUAL_STYLE);

		// action listener which performs the tasks associated with the popup
		// listener
		popupActionListener = new PopupActionListener();
		editNetworkTitle.addActionListener(popupActionListener);
		createViewItem.addActionListener(popupActionListener);
		destroyViewItem.addActionListener(popupActionListener);
		destroyNetworkItem.addActionListener(popupActionListener);
        destroyALLNetworkItem.addActionListener(popupActionListener);
        partitionNetworkMenu.addActionListener(popupActionListener);
		applyVisualStyleMenu.addActionListener(popupActionListener);
		popup.add(editNetworkTitle);
		popup.add(createViewItem);
		popup.add(destroyViewItem);
		popup.add(destroyNetworkItem);
        popup.add(destroyALLNetworkItem);
        popup.add(partitionNetworkMenu);
		popup.addSeparator();
		popup.add(applyVisualStyleMenu);
	}

	private void resetTable() {
		treeTable.getColumn(ColumnTypes.NETWORK.getDisplayName())
                .setPreferredWidth(170);
		treeTable.getColumn(ColumnTypes.NODES.getDisplayName())
				.setPreferredWidth(45);
		treeTable.getColumn(ColumnTypes.EDGES.getDisplayName())
				.setPreferredWidth(45);
		treeTable.setRowHeight(DEF_ROW_HEIGHT);
	}

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public void setFuntionValues(List values) {
        List result = new ArrayList();
        result.add("Show all");
        result.add("---------");
        Collections.sort(values);
        
        if(GOLayoutUtil.isValidGOTerm(values)) {
            Set<Object> attributeValues = new HashSet(values);
            Map<String, String> goDescMappingFile = GOLayoutUtil.readMappingFile(this.getClass().getResource(GOLayoutStaticValues.GO_DescFile), attributeValues, 0);
            for(Object o:attributeValues){
                if(goDescMappingFile.containsKey(o))
                    descGOMappingFile.put(goDescMappingFile.get(o).toString(), o.toString());
            }
            List descList = new ArrayList(descGOMappingFile.keySet());
            Collections.sort(descList);
            result.addAll(descList);
        } else {
            result.addAll(values);
        }

        functionComboBox.setModel(new DefaultComboBoxModel(result.toArray()));
        functionComboBox.setEnabled(true);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public SwingPropertyChangeSupport getSwingPropertyChangeSupport() {
		return pcs;
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param e
	 *            DOCUMENT ME!
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (Cytoscape.NETWORK_CREATED.equals(e.getPropertyName())) {
			logger.debug("NetworkPanel: " + e.getPropertyName());
			addNetwork((String) e.getNewValue(), (String) e.getOldValue());
		} else if (Cytoscape.NETWORK_DESTROYED.equals(e.getPropertyName())) {
			logger.debug("NetworkPanel: " + e.getPropertyName());
            //We have to destroy all child network first.
			removeNetwork((String) e.getNewValue());
		} else if (CytoscapeDesktop.NETWORK_VIEW_FOCUSED.equals(e.getPropertyName())) {
			logger.debug("NetworkPanel: " + e.getPropertyName());
			if (e.getSource() != this)
				focusNetworkNode((String) e.getNewValue());
		} else if (Cytoscape.NETWORK_TITLE_MODIFIED.equals(e.getPropertyName())) {
			logger.debug("NetworkPanel: " + e.getPropertyName());
			CyNetworkTitleChange cyNetworkTitleChange = (CyNetworkTitleChange) e.getNewValue();
			String newID = cyNetworkTitleChange.getNetworkIdentifier();
			CyNetwork _network = Cytoscape.getNetwork(newID);
			// Network "0" is the default and does not appear in the panel
			if (_network != null && !_network.getIdentifier().equals("0"))
				updateTitle(_network);
		} else if (Cytoscape.CYTOSCAPE_INITIALIZED.equals(e.getPropertyName())) {
			logger.debug("NetworkPanel: " + e.getPropertyName());
			updateVSMenu();
		}
	}

	/**
	 * This method highlights a network in the NetworkPanel.
	 *
	 * @param e DOCUMENT ME!
	 */
	public void valueChanged(TreeSelectionEvent e) {
		// TODO: Every time user select a network name, this method will be called 3 times!
		if (doNotEnterValueChanged)
			return;

		final JTree mtree = treeTable.getTree();

		// sets the "current" network based on last node in the tree selected
		final GOLayoutNetworkTreeNode node = (GOLayoutNetworkTreeNode) mtree.getLastSelectedPathComponent();
		if ( node == null || node.getUserObject() == null )
			return;

        pcs.firePropertyChange(new PropertyChangeEvent(this, CytoscapeDesktop.NETWORK_VIEW_FOCUS,
	            null, (String) node.getNetworkID()));

        // creates a list of all selected networks
		final List<String> networkList = new LinkedList<String>();
		try {
			for ( int i = mtree.getMinSelectionRow(); i <= mtree.getMaxSelectionRow(); i++ ) {
				GOLayoutNetworkTreeNode n = (GOLayoutNetworkTreeNode) mtree.getPathForRow(i).getLastPathComponent();
				if ( n != null && n.getUserObject() != null && mtree.isRowSelected(i) )
					networkList.add( n.getNetworkID() );
			}
		} catch (Exception ex) {
			CyLogger.getLogger().warn("Exception handling network panel change: "+ex.getMessage());
			ex.printStackTrace();
		}

		if ( networkList.size() > 0 ) {
			Cytoscape.setSelectedNetworks(networkList);
			Cytoscape.setSelectedNetworkViews(networkList);
		}
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param event DOCUMENT ME!
	 */
	public void onSelectEvent(final SelectEvent event) {
        if (event.getTargetType() == SelectEvent.SINGLE_NODE || event.getTargetType() == SelectEvent.NODE_SET) {
			final Set<Node> selectedNodes = (Set<Node>)Cytoscape.getCurrentNetwork().getSelectedNodes();
			final List<String> selectedNestedNetworkIDs = new ArrayList<String>();
			for (final Node node : selectedNodes) {
				final CyNetwork nestedNetwork = (CyNetwork)node.getNestedNetwork();
				if (nestedNetwork != null)
					selectedNestedNetworkIDs.add(nestedNetwork.getIdentifier());
			}

			doNotEnterValueChanged = true;
			try {
				final TreePath[] treePaths = new TreePath[selectedNestedNetworkIDs.size() + 1];
				int index = 0;
				final String currentNetworkID = Cytoscape.getCurrentNetwork().getIdentifier();
				TreePath currentPath = null;
				final JTree tree = treeTable.getTree();
				for (int row = 0; row < tree.getRowCount(); ++row) {
					final TreePath path = tree.getPathForRow(row);
					final String ID = ((GOLayoutNetworkTreeNode)path.getLastPathComponent()).getNetworkID();
					if (ID.equals(currentNetworkID))
						currentPath = path;
					else if (selectedNestedNetworkIDs.contains(ID))
						treePaths[index++] = path;
				}

				Cytoscape.setSelectedNetworks(selectedNestedNetworkIDs);

				treePaths[index] = currentPath;
				tree.getSelectionModel().setSelectionPaths(treePaths);
				tree.scrollPathToVisible(currentPath);
			} finally {
				doNotEnterValueChanged = false;
			}
		}

		treeTable.getTree().updateUI();
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param network_id DOCUMENT ME!
	 * @param parent_id DOCUMENT ME!
	 */
	public void addNetwork(String network_id, String parent_id) {
		// first see if it exists
		if (getNetworkNode(network_id) == null) {
			//logger.info("NetworkPanel: addNetwork " + network_id);
			GOLayoutNetworkTreeNode dmtn = new GOLayoutNetworkTreeNode(Cytoscape.getNetwork(network_id).getTitle(),
			                                           network_id);
			Cytoscape.getNetwork(network_id).addSelectEventListener(this);

			if (parent_id != null && getNetworkNode(parent_id) != null) {
				getNetworkNode(parent_id).add(dmtn);
			} else {
				root.add(dmtn);
			}

			// apparently this doesn't fire valueChanged
			treeTable.getTree().collapsePath(new TreePath(new TreeNode[] { root }));

			treeTable.getTree().updateUI();
			TreePath path = new TreePath(dmtn.getPath());
			treeTable.getTree().expandPath(path);
			treeTable.getTree().scrollPathToVisible(path);
			treeTable.doLayout();

			// this is necessary because valueChanged is not fired above
			focusNetworkNode(network_id);
		}
	}

	/**
	 * Remove a network from the panel.
	 *
	 * @param network_id
	 */
	public void removeNetwork(final String network_id) {
		final GOLayoutNetworkTreeNode node = getNetworkNode(network_id);
		if (node == null) return;

		final Enumeration<GOLayoutNetworkTreeNode> children = node.children();
		GOLayoutNetworkTreeNode child = null;
		final List removed_children = new ArrayList();

		while (children.hasMoreElements()) {
			removed_children.add(children.nextElement());
		}

		for (Iterator i = removed_children.iterator(); i.hasNext();) {
			child = (GOLayoutNetworkTreeNode) i.next();
			child.removeFromParent();
			root.add(child);
		}

		Cytoscape.getNetwork(network_id).removeSelectEventListener(this);
		node.removeFromParent();
		treeTable.getTree().updateUI();
		treeTable.doLayout();
	}

	/**
	 * update a network title
	 */
	public void updateTitle(final CyNetwork network) {
		// updates the title in the network panel
		if (treeTable.getTree().getSelectionPath() != null) { // user has selected something
			networkTreeTableModel.setValueAt(network.getTitle(),
		                          treeTable.getTree().getSelectionPath().getLastPathComponent(), 0);
		} else { // no selection, means the title has been changed programmatically
			GOLayoutNetworkTreeNode node = getNetworkNode(network.getIdentifier());
			networkTreeTableModel.setValueAt(network.getTitle(), node, 0);
		}
		treeTable.getTree().updateUI();
		treeTable.doLayout();
		// updates the title in the networkViewMap
		Cytoscape.getDesktop().getNetworkViewManager().updateNetworkTitle(network);
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param network_id DOCUMENT ME!
	 */
	public void focusNetworkNode(String network_id) {
        //logger.info("NetworkPanel: focus network node");
		DefaultMutableTreeNode node = getNetworkNode(network_id);

		if (node != null) {
			// fires valueChanged if the network isn't already selected
			treeTable.getTree().getSelectionModel().setSelectionPath(new TreePath(node.getPath()));
			treeTable.getTree().scrollPathToVisible(new TreePath(node.getPath()));
		}
	}

	/**
	 *  DOCUMENT ME!
	 *
	 * @param network_id DOCUMENT ME!
	 *
	 * @return  DOCUMENT ME!
	 */
	public GOLayoutNetworkTreeNode getNetworkNode(String network_id) {
		Enumeration tree_node_enum = root.breadthFirstEnumeration();

		while (tree_node_enum.hasMoreElements()) {
			GOLayoutNetworkTreeNode node = (GOLayoutNetworkTreeNode) tree_node_enum.nextElement();

			if (node.getNetworkID().equals(network_id)) {
				return node;
			}
		}

		return null;
	}

    /**
	 * This class listens to mouse events from the TreeTable, if the mouse event
	 * is one that is canonically associated with a popup menu (ie, a right
	 * click) it will pop up the menu with option for destroying view, creating
	 * view, and destroying network (this is platform specific apparently)
	 */
	protected class ChangeFunctionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String selectedGOTerm = ((JComboBox)e.getSource()).getSelectedItem().toString();
            if(!selectedGOTerm.equals("---------")) {
                if(!descGOMappingFile.isEmpty()) {
                    if(!selectedGOTerm.equals("Show all"))
                        selectedGOTerm = descGOMappingFile.get(((JComboBox)e.getSource()).getSelectedItem()).toString();
                }
                PartitionNetworkVisualStyleFactory.highlightNodes(selectedGOTerm);
            }
        }
    }

	/**
	 * This class listens to mouse events from the TreeTable, if the mouse event
	 * is one that is canonically associated with a popup menu (ie, a right
	 * click) it will pop up the menu with option for destroying view, creating
	 * view, and destroying network (this is platform specific apparently)
	 */
	protected class PopupListener extends MouseAdapter {
		/*
		 * On windows, popup is triggered by "release", not "pressed"
		 */
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * if the mouse press is of the correct type, this function will maybe
		 * display the popup
		 */
		private void maybeShowPopup(MouseEvent e) {
			// check for the popup type
			if (e.isPopupTrigger()) {
				logger.debug("network context menu triggered");
				// get the selected rows
				final int[] nselected = treeTable.getSelectedRows();

				if (nselected != null && nselected.length != 0) {
					boolean enableViewRelatedMenu = false;
					final int selectedItemCount = nselected.length;
					CyNetwork cyNetwork = null;
					final JTree tree = treeTable.getTree();
					final TreePath treePath = tree
                            .getPathForRow(nselected[0]);
                    final String networkID = (String) ((GOLayoutNetworkTreeNode) treePath
                            .getLastPathComponent()).getNetworkID();

                    cyNetwork = Cytoscape.getNetwork(networkID);
                    if (Cytoscape.viewExists(networkID)) {
                        enableViewRelatedMenu = true;
                    }
                    
					logger.debug(selectedItemCount
							+ " networks selected, with view?: "
							+ enableViewRelatedMenu);
					/*
					 * Edit title command will be enabled only when ONE network
					 * is selected.
					 */
					if (selectedItemCount == 1) {
						editNetworkTitle.setEnabled(true);
						popupActionListener.setActiveNetwork(cyNetwork);
					} else {
						editNetworkTitle.setEnabled(false);
					}
					if (enableViewRelatedMenu) {
						// At least one selected network has a view.
						createViewItem.setEnabled(true);
						destroyViewItem.setEnabled(true);
//						viewMetanodesAsSubMenu.setEnabled(true);
						applyVisualStyleMenu.setEnabled(true);
					} else {
						// None of the selected networks has view.
						createViewItem.setEnabled(true);
						destroyViewItem.setEnabled(false);
//						viewMetanodesAsSubMenu.setEnabled(false);
						applyVisualStyleMenu.setEnabled(false);
					}
                    if(GOLayoutUtil.isValidGOTerm(partitionObject.nodeAttributeValues)) {
                        String goTerm = partitionObject.getGOTerm(cyNetwork.getTitle());
                        ArrayList<String> unbuildChildList = partitionObject.getUnbuildChildList(goTerm);
                        if(unbuildChildList.size()>0) {
                            partitionNetworkMenu.setEnabled(true);
                            partitionNetworkMenu.removeAll();
                            JMenuItem styleMenu = new JMenuItem("ALL");
                            //styleMenu.setAction(partitionObject.buildSubnetwork(goTerm, unbuildChildList));
                            styleMenu.addActionListener(popupActionListener);
                            partitionNetworkMenu.add(styleMenu);
                            partitionNetworkMenu.addSeparator();
                            for (String subnetworkName : unbuildChildList) {
                                styleMenu = new JMenuItem(subnetworkName);
                                styleMenu.addActionListener(popupActionListener);
                                //styleMenu.setAction(new ApplyVisualStyleAction(name));
                                partitionNetworkMenu.add(styleMenu);
                            }
                        } else {
                            partitionNetworkMenu.setEnabled(false);
                        }
                        ArrayList<String> existChildList = partitionObject.getExistChildList(goTerm);
                        if(existChildList.size()>0)
                            destroyALLNetworkItem.setEnabled(true);
                        else
                            destroyALLNetworkItem.setEnabled(false);
                    } else {
                        destroyALLNetworkItem.setEnabled(false);
                        partitionNetworkMenu.setEnabled(false);
                    }
                    System.out.println(cyNetwork.getTitle());
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	}

	public void stateChanged(ChangeEvent e) {
		updateVSMenu();
	}

	private void updateVSMenu() {
		applyVisualStyleMenu.removeAll();

		final Set<String> vsNames = new TreeSet<String>(Cytoscape
				.getVisualMappingManager().getCalculatorCatalog()
				.getVisualStyleNames());
		for (String name : vsNames) {
			final JMenuItem styleMenu = new JMenuItem(name);
			styleMenu.setAction(new ApplyVisualStyleAction(name));
			applyVisualStyleMenu.add(styleMenu);
		}
	}

	/**
	 * This class listens for actions from the popup menu, it is responsible for
	 * performing actions related to destroying and creating views, and
	 * destroying the network.
	 */
	class PopupActionListener implements ActionListener {
		public static final String DESTROY_VIEW = "Destroy View";
		public static final String CREATE_VIEW = "Create View";
		public static final String DESTROY_NETWORK = "Destroy Network";
		public static final String EDIT_NETWORK_TITLE = "Edit Network Title";
		public static final String VIEW_METANODES_AS = "View Metanodes As...";
		public static final String METANODES_COLLAPSED = "Collapsed";
		public static final String METANODES_EXPANDED = "Expanded";
		public static final String METANODES_NESTED = "Nested";
		public static final String APPLY_VISUAL_STYLE = "Apply Visual Style";
        public static final String DESTROY_ALL = "Destroy All Sub Network";
        public static final String PARTITION_NETWORK = "Partition This Network";
        public static final String EDIT_TITLE = "Edit Network Title";

        /**
		 * This is the network which originated the mouse-click event (more
		 * appropriately, the network associated with the ID associated with the
		 * row associated with the JTable that originated the popup event
		 */
		protected CyNetwork cyNetwork;

		/**
		 * Based on the action event, destroy or create a view, or destroy a
		 * network
		 */
		public void actionPerformed(ActionEvent ae) {
			final String label = ((JMenuItem) ae.getSource()).getText();
			logger.debug(label + " triggered");

			if (DESTROY_VIEW.equals(label)) {
				final List<CyNetwork> selected = Cytoscape
						.getSelectedNetworks();
				for (final CyNetwork network : selected) {
					final CyNetworkView targetView = Cytoscape
							.getNetworkView(network.getIdentifier());
					if (targetView != Cytoscape.getNullNetworkView()) {
						Cytoscape.destroyNetworkView(targetView);
					}
				}
			} else if (CREATE_VIEW.equals(label)) {
				final List<CyNetwork> selected = Cytoscape
						.getSelectedNetworks();

				for (CyNetwork network : selected) {
					if (!Cytoscape.viewExists(network.getIdentifier()))
						CreateNetworkViewAction
								.createViewFromCurrentNetwork(network);
				}
			} else if (DESTROY_NETWORK.equals(label)) {
                CyNetwork selectedNetwork = Cytoscape.getSelectedNetworks().get(0);
                if(GOLayoutUtil.isValidGOTerm(partitionObject.nodeAttributeValues)) {
                    String networkID = partitionObject.getGOTerm(selectedNetwork.getTitle());
                    if(!networkID.equals(""))
                        partitionObject.destroyAllSubNet(networkID,networkID);
                    else
                        partitionObject.destroyAllSubNet("root","root");
                    partitionObject.resetNetworkID(selectedNetwork);
                }
				Cytoscape.destroyNetwork(selectedNetwork);                
                partitionObject.updateOverview();
			} else if (EDIT_NETWORK_TITLE.equals(label)) {
				CyNetworkNaming.editNetworkTitle(cyNetwork);
				Cytoscape.getDesktop().getNetworkPanel().updateTitle(cyNetwork);
            } else if (DESTROY_ALL.equals(label)) {
				CyNetwork selectedNetwork = Cytoscape.getSelectedNetworks().get(0);
                String networkID = partitionObject.getGOTerm(selectedNetwork.getTitle());
                if(!networkID.equals(""))
                    partitionObject.destroyAllSubNet(networkID,networkID);
                else
                    partitionObject.destroyAllSubNet("root","root");
                partitionObject.updateOverview();
//            } else if (PARTITION_NETWORK.equals(label)) {
//				System.out.println("PARTITION_NETWORK");
			} else {
                System.out.println(label);
                if(!label.equals("")) {
                    CyNetwork selectedNetwork = Cytoscape.getSelectedNetworks().get(0);
                    String goTerm = partitionObject.getGOTerm(selectedNetwork.getTitle());
                    ArrayList<String> unbuildChildList = new ArrayList();
                    if(label.equals("ALL"))
                        unbuildChildList = partitionObject.getUnbuildChildList(goTerm);
                    else
                        unbuildChildList.add(label);
                    partitionObject.partitionSubnetwork(goTerm, unbuildChildList);
                    partitionObject.updateOverview();
                } else {
                    CyLogger.getLogger().warn(
						"Unexpected network panel popup option");
                }
			}
		}

		/**
		 * Right before the popup menu is displayed, this function is called so
		 * we know which network the user is clicking on to call for the popup
		 * menu
		 */
		public void setActiveNetwork(final CyNetwork cyNetwork) {
			this.cyNetwork = cyNetwork;
		}
	}
}
