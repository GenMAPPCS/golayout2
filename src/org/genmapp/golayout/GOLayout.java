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
import cytoscape.CyNode;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPanel;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.CyAttributes;
import cytoscape.layout.AbstractLayout;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.layout.TunableListener;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.plugin.PluginManager;
import cytoscape.view.CyNetworkView;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JLabel;
import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.bio.BioDataSource;
import org.genmapp.golayout.utils.FileDownload;
import org.pathvisio.cytoscape.GpmlPlugin;

public class GOLayout extends CytoscapePlugin{
    public static String GOLayoutBaseDir;
	
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
        CyLayouts.addLayout(new GOLayoutAlgorithm(), "GO Layout");
        CyLayouts.addLayout(new PartitionAlgorithm(), null);
        CyLayouts.addLayout(new CellAlgorithm(), null);
        CyLayouts.addLayout(new IdMapping(), "IdMapping");

        // JMenuItem item = new JMenuItem("Add GO-slim annotations");
        // JMenu layoutMenu = Cytoscape.getDesktop().getCyMenus().getMenuBar()
        // .getMenu("Layout");
        // item.addActionListener(new AddAnnotationCommandListener());
        //
        // layoutMenu.add(item);
    }

    public static void createVisualStyle(CyNetworkView view) {
        PartitionNetworkVisualStyleFactory.createVisualStyle(view);
    }

    public class GOLayoutAlgorithm extends AbstractLayout implements
                    TunableListener {
        protected static final String LAYOUT_NAME = "0-golayout";
        private static final String HELP = "GOLayout Help";
        private LayoutProperties layoutProperties = null;

        public String annotationAtt = "ID";
        public String annotationCode = null;
		public String annotationSpeciesCode = "";
        private List<String> dsValues = new ArrayList<String>();
        private List<String> speciesValues = new ArrayList<String>();
        private List<String> aAttAnnValues = new ArrayList<String>();
		private IDMapper _mapper = null;
		private Set<DataSource> dataSources = null;
        private Tunable gAttParTunable;
        private Tunable gAttParPerTunable;
        private Tunable gAttLayTunable;
        private Tunable gAttLayPerTunable;
        private Tunable gAttNodTunable;
        private Tunable gAttNodPerTunable;
        private Tunable aAnnSwiTunable;
        private Tunable aSpeAnnTunable;
        private Tunable aAttAnnTunable;
        private Tunable dsTunable;
        private Tunable lPreTemTunable;
        private Tunable lLoaTemTunable;
        private Tunable pParLevTunable;

        /**
         * Creates a new CellularLayoutAlgorithm object.
         */
        public GOLayoutAlgorithm() {
            super();

            // hardcode species until web service support query of supported
            // species
            speciesValues = Arrays.asList(GOLayoutStaticValues.speciesList);
            
            dsValues.add("Ensembl");
            aAttAnnValues.add("ID");
            // dynamically populate list of datasource names
            //populateDataSourceList();

            layoutProperties = new LayoutProperties(getName());
            //Panel of "Global Settings"
            layoutProperties.add(new Tunable("global", "Global Settings",
                    Tunable.GROUP, new Integer(6)));
            gAttParTunable = new Tunable("attributePartition",
                    "The attribute to use for partitioning",
                    Tunable.NODEATTRIBUTE, GOLayoutStaticValues.BP_ATTNAME,
                    getInitialAttributeList(), (Object) null, 0);
            gAttParTunable.addTunableValueListener(this);
            layoutProperties.add(gAttParTunable);
            gAttParPerTunable = new Tunable("partitionAnnotationPer",
                    " ", Tunable.BUTTON, "0/0");
            gAttParPerTunable.setImmutable(true);
            layoutProperties.add(gAttParPerTunable);
            gAttLayTunable = new Tunable("attributeLayout",
                    "The attribute to use for the layout",
                    Tunable.NODEATTRIBUTE, GOLayoutStaticValues.CC_ATTNAME,
                    getInitialAttributeList(), (Object) null, 0);
            gAttLayTunable.addTunableValueListener(this);
            layoutProperties.add(gAttLayTunable);
            gAttLayPerTunable = new Tunable("layoutAnnotationPer",
                    " ", Tunable.BUTTON, "0/0");
            gAttLayPerTunable.setImmutable(true);
            layoutProperties.add(gAttLayPerTunable);
            gAttNodTunable = new Tunable("attributeNodeColor",
                    "The attribute to use for node color",
                    Tunable.NODEATTRIBUTE, GOLayoutStaticValues.MF_ATTNAME,
                    getInitialAttributeList(), (Object) null, 0);
            gAttNodTunable.addTunableValueListener(this);
            layoutProperties.add(gAttNodTunable);
            gAttNodPerTunable = new Tunable("colorAnnotationPer",
                    " ", Tunable.BUTTON, "0/0");
            gAttNodPerTunable.setImmutable(true);
            layoutProperties.add(gAttNodPerTunable);
            //Panel of "Annotation Settings"
            layoutProperties.add(new Tunable("annotation",
                    "Annotation Settings (optional)",
                    Tunable.GROUP, new Integer(4)));
            aAnnSwiTunable = new Tunable("annotationSwitch",
                    "Annotate current network", Tunable.BOOLEAN, true);
            aAnnSwiTunable.addTunableValueListener(this);
            layoutProperties.add(aAnnSwiTunable);
            aSpeAnnTunable = new Tunable("speciesAnnotation",
                    "Species of identifier", Tunable.LIST, new Integer(0),
                    (Object) speciesValues.toArray(), null, 0);
            aSpeAnnTunable.addTunableValueListener(this);
            layoutProperties.add(aSpeAnnTunable);
            aAttAnnTunable = new Tunable("attributeAnnotation",
                    "The identifier to use for annotation retrieval",
                    Tunable.NODEATTRIBUTE, annotationAtt,
                    aAttAnnValues, null, 0);
            layoutProperties.add(aAttAnnTunable);
            dsTunable = new Tunable("dsAnnotation",
                    "Type of identifier, e.g., Entrez Gene", Tunable.LIST,
                    new Integer(0), (Object) dsValues.toArray(), null, 0);
            layoutProperties.add(dsTunable);
            //Panel of "Partition Settings"
            layoutProperties.add(new Tunable("partition", "Partition Settings",
                    Tunable.GROUP, new Integer(3)));
            layoutProperties.add(new Tunable("partitionMin",
                    "Don't show subnetworks with fewer nodes than",
                    Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MIN));
            layoutProperties.add(new Tunable("partitionMax",
                    "Don't show subnetworks with more nodes than",
                    Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MAX));
            pParLevTunable = new Tunable("partitionLevel",
                    "The deepest level of GO term for partition", Tunable.LIST,
                    new Integer(0), (Object) dsValues.toArray(), null, 0);
            layoutProperties.add(pParLevTunable);
            //Panel of "Layout Settings"
            layoutProperties.add(new Tunable("layout", "Layout Settings",
                    Tunable.GROUP, new Integer(2)));
            lPreTemTunable = new Tunable("presetTemplate",
                    "Choose one templete from",
                    Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MIN);
            layoutProperties.add(lPreTemTunable);
            lLoaTemTunable = new Tunable("uploadTemplate",
                    "Or upload your templete",
                    Tunable.INTEGER, PartitionAlgorithm.NETWORK_LIMIT_MAX);
            layoutProperties.add(lLoaTemTunable);
            if (GOLayoutUtil.checkGPMLPlugin() == false) {
                lPreTemTunable.setImmutable(true);
                lLoaTemTunable.setImmutable(true);
            }
            //Panel of "Floorplan Settings"
            layoutProperties.add(new Tunable("floorplan", "Floorplan Settings",
                    Tunable.GROUP, new Integer(2)));
            layoutProperties.add(new Tunable("nodeSpacing",
                    "Spacing between nodes", Tunable.DOUBLE,
                    CellAlgorithm.distanceBetweenNodes));
            layoutProperties.add(new Tunable("pruneEdges",
                    "Prune cross-region edges?", Tunable.BOOLEAN, false));

            /*
             * We've now set all of our tunables, so we can read the property
             * file now and adjust as appropriate
             */
            layoutProperties.initializeProperties();

            /*
             * Finally, update everything. We need to do this to update any of
             * our values based on what we read from the property file
             */
            updateSettings(true);

            // Add help menu item
            JMenuItem getHelp = new JMenuItem(HELP);
            getHelp.setToolTipText("Open online help for GOLayout");
            GetHelpListener getHelpListener = new GetHelpListener();
            getHelp.addActionListener(getHelpListener);
            Cytoscape.getDesktop().getCyMenus().getHelpMenu().add(getHelp);

        }

//                public List<String> getAttributeList(String goAttribute){
//                    List tempList = getInitialAttributeList();
//                    if(tempList.indexOf(goAttribute)==-1)
//                        tempList.add(goAttribute);
//                    return tempList;
//                }

        public void populateDataSourceList(String annotationSpecies) {
            try {
                Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
            } catch (ClassNotFoundException e) {
                System.out.println("Can't register org.bridgedb.rdb.IDMapperRdb");
                e.printStackTrace();
            }

            BioDataSource.init();
            // now we connect to the driver and create a IDMapper instance.
            // TODO: Update to use multiple species
            try {
                _mapper = BridgeDb.connect("idmapper-bridgerest:http://webservice.bridgedb.org/"+annotationSpecies);
                //System.out.println(_mapper.toString());
            } catch (IDMapperException e) {
                e.printStackTrace();
            }
            try {
                dataSources = _mapper.getCapabilities().getSupportedSrcDataSources();
                //System.out.println(dataSources.size());
            } catch (IDMapperException e) {
                e.printStackTrace();
            }
            dsValues.clear();
            if (dataSources.size() > 0) {
                Iterator it = dataSources.iterator();
                while (it.hasNext()) {
                    dsValues.add(((DataSource) it.next()).getFullName());
                }
            }
//            System.out.println(dsValues.size());
        }

        private void checkAttributes(Tunable globalTunable, String goAttribute){
            List CurrentNetworkAtts = Arrays.asList(Cytoscape.getNodeAttributes().getAttributeNames());
            List layoutAttrs = (List)globalTunable.getLowerBound();
            if(!CurrentNetworkAtts.contains(goAttribute)){
                layoutAttrs.add(goAttribute);
                globalTunable.setLowerBound(layoutAttrs);
            }
            globalTunable.setValue(goAttribute);
        }

        private int checkAnnotationRate(String goAttribute) {
            int count = 0;
            CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
            CyAttributes currentAttrs = Cytoscape.getNodeAttributes();
            for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
                if (currentAttrs.hasAttribute(cn.getIdentifier(), goAttribute)) {
                	byte type = currentAttrs.getType(goAttribute);
                	if (type == CyAttributes.TYPE_SIMPLE_LIST) {
                		List list = currentAttrs.getListAttribute(cn.getIdentifier(), goAttribute);
                		if (list.size() > 1){
                			count++;
                		} else if (list.size() == 1){
                			if (list.get(0) != null)
                				if (!list.get(0).equals(""))
                					count++;
                		}
                	} else if (type == CyAttributes.TYPE_STRING) {    
                		if (!currentAttrs.getStringAttribute(cn.getIdentifier(), goAttribute).equals("null"))
                			count++;
                	} else {
                		//we don't have to be as careful with other attribute types
                		if (!currentAttrs.getAttribute(cn.getIdentifier(), goAttribute).equals(null))
                			count++;
                	}
                }

            }
            return count;
        }

        private boolean isGOAttr(Tunable globalTunable) {
            if(globalTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)||
                    globalTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)||
                    globalTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)) {
                return true;
            } else {
                return false;
            }
        }

        private void checkAnnotationStatus() {
            List CurrentNetworkAtts = Arrays.asList(Cytoscape.getNodeAttributes().getAttributeNames());
            int numberOfNodes = Cytoscape.getCurrentNetwork().nodesList().size();
//                    System.out.println(((gAttParTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.BP_ATTNAME)))+" "
//                            +(!(gAttParTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME))));
//                    System.out.println(((gAttLayTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.CC_ATTNAME)))+" "
//                            +(!(gAttLayTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME))));
//                    System.out.println(((gAttNodTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.MF_ATTNAME)))+" "
//                            +(!(gAttNodTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME))));
//                    System.out.println((((gAttParTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.BP_ATTNAME)) ||
//                            !(gAttParTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)))&&
//                            ((gAttLayTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.CC_ATTNAME)) ||
//                            !(gAttLayTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)))&&
//                            ((gAttNodTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.MF_ATTNAME)) ||
//                            !(gAttNodTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)))));
//                    System.out.println();

//                    if((gAttParTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.BP_ATTNAME))||
//                            (gAttParTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.CC_ATTNAME))||
//                            (gAttParTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.MF_ATTNAME))||
//                            (gAttLayTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.BP_ATTNAME))||
//                            (gAttLayTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.CC_ATTNAME))||
//                            (gAttLayTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.MF_ATTNAME))||
//                            (gAttNodTunable.getValue().equals(GOLayoutStaticValues.BP_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.BP_ATTNAME))||
//                            (gAttNodTunable.getValue().equals(GOLayoutStaticValues.CC_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.CC_ATTNAME))||
//                            (gAttNodTunable.getValue().equals(GOLayoutStaticValues.MF_ATTNAME)&&!CurrentNetworkAtts.contains(GOLayoutStaticValues.MF_ATTNAME))) {
//                        dsTunable.setImmutable(false);
//                        aSpeAnnTunable.setImmutable(false);
//                        aAttAnnTunable.setImmutable(false);
//                        aAnnSwiTunable.setValue(true);
//                    } else {
//                        dsTunable.setImmutable(true);
//                        aSpeAnnTunable.setImmutable(true);
//                        aAttAnnTunable.setImmutable(true);
//                        aAnnSwiTunable.setValue(false);
//                    }
            //If user didn't choose GO attribute for partition, disable 'The deepest level of GO term for partition'
            if(isGOAttr(gAttParTunable)) {
                pParLevTunable.setImmutable(false);
            } else {
                pParLevTunable.setImmutable(true);
            }
            if((isGOAttr(gAttParTunable)&&(!CurrentNetworkAtts.contains(gAttParTunable.getValue().toString())||checkAnnotationRate(gAttParTunable.getValue().toString())==0))||
                    (isGOAttr(gAttLayTunable)&&(!CurrentNetworkAtts.contains(gAttLayTunable.getValue().toString())||checkAnnotationRate(gAttLayTunable.getValue().toString())==0))||
                    (isGOAttr(gAttNodTunable)&&(!CurrentNetworkAtts.contains(gAttNodTunable.getValue().toString())||checkAnnotationRate(gAttNodTunable.getValue().toString())==0))) {
                //Any of three global settings is GO attribute and annotation rate equls 0. 
                //Force user to fetch the annotations, and user can not turn off the annotation panel.
                dsTunable.setImmutable(false);
                aSpeAnnTunable.setImmutable(false);
                aAttAnnTunable.setImmutable(false);
                aAnnSwiTunable.setValue(true);
                aAnnSwiTunable.setImmutable(true);
            } else if(!(isGOAttr(gAttParTunable)||isGOAttr(gAttLayTunable)||isGOAttr(gAttNodTunable))) {
                //None of three global settings is GO attribute, user can not turn on the annotattion panel.
                dsTunable.setImmutable(true);
                aSpeAnnTunable.setImmutable(true);
                aAttAnnTunable.setImmutable(true);
                aAnnSwiTunable.setValue(false);
                aAnnSwiTunable.setImmutable(true);
            } else {
                aAnnSwiTunable.setImmutable(false);
            }

            gAttParPerTunable.setValue(checkAnnotationRate(gAttParTunable.getValue().toString())+"/"+numberOfNodes);
            gAttLayPerTunable.setValue(checkAnnotationRate(gAttLayTunable.getValue().toString())+"/"+numberOfNodes);
            gAttNodPerTunable.setValue(checkAnnotationRate(gAttNodTunable.getValue().toString())+"/"+numberOfNodes);
//                    if(CurrentNetworkAtts.contains(GOLayoutStaticValues.BP_ATTNAME)&&CurrentNetworkAtts.contains(GOLayoutStaticValues.CC_ATTNAME)&&CurrentNetworkAtts.contains(GOLayoutStaticValues.MF_ATTNAME)) {
//                        dsTunable.setImmutable(true);
//                        aSpeAnnTunable.setImmutable(true);
//                        aAttAnnTunable.setImmutable(true);
//                    }
//                    System.out.println(.contains("GOslim-BiologicalProcess"));
//                    System.out.println(gAttLayTunable.getValue());
        }

        private String[] getSpeciesCommonName(String speName) {
            String[] result = {"", ""};
            List<String> lines = GOLayoutUtil.readUrl(GOLayoutStaticValues.bridgedbSpecieslist);
            for (String line : lines) {
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

        public void tunableChanged(Tunable t) {
            // TODO Auto-generated method stub
            checkAnnotationStatus();
            if (t.getName().equals("speciesAnnotation")) {
                //Regenerate list of ID types when user select another species.
                String[] speciesCode = getSpeciesCommonName(speciesValues.get(new Integer(t.getValue().toString()).intValue()));
                annotationSpeciesCode = speciesCode[1];
                populateDataSourceList(annotationSpeciesCode);
                dsTunable.setLowerBound((Object) dsValues.toArray());
            } else if (t.getName().equals("annotationSwitch")) {
                if(((Boolean) t.getValue()).booleanValue()==true) {
                    dsTunable.setImmutable(false);
                    aSpeAnnTunable.setImmutable(false);
                    aAttAnnTunable.setImmutable(false);
                } else {
                    dsTunable.setImmutable(true);
                    aSpeAnnTunable.setImmutable(true);
                    aAttAnnTunable.setImmutable(true);
                }
            }
            updateSettings();
        }

        /**
         * External interface to update our settings
         */
        public void updateSettings() {
                updateSettings(true);
        }

        /**
         * Signals that we want to update our internal settings
         *
         * @param force
         *            force the settings to be updated, if true
         */
        public void updateSettings(boolean force) {
            layoutProperties.updateValues();
            Tunable t = layoutProperties.get("attributePartition");
            if ((t != null) && (t.valueChanged() || force)) {
                String newValue = (String) t.getValue();
                if (newValue.equals("(none)")) {
                    PartitionAlgorithm.attributeName = null;
                } else {
                    PartitionAlgorithm.attributeName = newValue;
                }
            }

            t = layoutProperties.get("attributeLayout");
            if ((t != null) && (t.valueChanged() || force)) {
                String newValue = (String) t.getValue();
                if (newValue.equals("(none)")) {
                    CellAlgorithm.attributeName = null;
                } else {
                    CellAlgorithm.attributeName = newValue;
                }
            }

            t = layoutProperties.get("attributeNodeColor");
            if ((t != null) && (t.valueChanged() || force)) {
                String newValue = (String) t.getValue();
                if (newValue.equals("(none)")) {
                    PartitionNetworkVisualStyleFactory.attributeName = null;
                } else {
                    PartitionNetworkVisualStyleFactory.attributeName = newValue;
                }
            }

//			t = layoutProperties.get("attributeAnnotation");
//			if ((t != null) && (t.valueChanged() || force)) {
//				String newValue = (String) t.getValue();
//				annotationAtt = newValue;
//			}

//			t = layoutProperties.get("speciesAnnotation");
//			if ((t != null) && (t.valueChanged() || force)) {
//				String newValue = speciesValues.get((Integer) t.getValue());
//				annotationSpecies = newValue;
//			}
//
//			t = layoutProperties.get("dsAnnotation");
//			if ((t != null) && (t.valueChanged() || force)) {
//				String newValue = dsValues.get((Integer) t.getValue());
//				annotationCode = newValue;
//			}

            t = layoutProperties.get("partitionMin");
            if ((t != null) && (t.valueChanged() || force))
                PartitionAlgorithm.NETWORK_LIMIT_MIN = ((Integer) t.getValue())
                                    .intValue();

            t = layoutProperties.get("partitionMax");
            if ((t != null) && (t.valueChanged() || force))
                PartitionAlgorithm.NETWORK_LIMIT_MAX = ((Integer) t.getValue())
                                    .intValue();

            t = layoutProperties.get("nodeSpacing");
            if ((t != null) && (t.valueChanged() || force))
                CellAlgorithm.distanceBetweenNodes = ((Double) t.getValue())
                                    .doubleValue();

            t = layoutProperties.get("pruneEdges");
            if ((t != null) && (t.valueChanged() || force))
                CellAlgorithm.pruneEdges = ((Boolean) t.getValue())
                                    .booleanValue();

            // update UI options based on selection
            //checkAnnotationStatus();
        }

        /**
         * Reverts our settings back to the original.
         */
        public void revertSettings() {
            layoutProperties.revertProperties();
        }

        public LayoutProperties getSettings() {
            return layoutProperties;
        }

        /**
         * Returns the short-hand name of this algorithm NOTE: is related to the
         * menu item order
         *
         * @return short-hand name
         */
        public String getName() {
            return LAYOUT_NAME;
        }

        /**
         * Returns the user-visible name of this layout
         *
         * @return user visible name
         */
        public String toString() {
            return "GO Layout";
        }

        /**
         * Gets the Task Title.
         *
         * @return human readable task title.
         */
        public String getTitle() {
            return new String("GO Layout");
        }

        /**
         * Return true if we support performing our layout on a limited set of
         * nodes
         *
         * @return true if we support selected-only layout
         */
        public boolean supportsSelectedOnly() {
            return false;
        }

        /**
         * Returns the types of node attributes supported by this algorithm.
         *
         * @return the list of supported attribute types, or null if node
         *         attributes are not supported
         */
        public byte[] supportsNodeAttributes() {
            return null;
        }

        /**
         * Returns the types of edge attributes supported by this algorithm.
         *
         * @return the list of supported attribute types, or null if edge
         *         attributes are not supported
         */
        public byte[] supportsEdgeAttributes() {
            return null;
        }

        /**
         * Returns a JPanel to be used as part of the Settings dialog for this
         * layout algorithm.
         *
         */
        public JPanel getSettingsPanel() {
            //updates ui based on current network attributes
            checkAnnotationStatus();
            checkAttributes(gAttParTunable, GOLayoutStaticValues.BP_ATTNAME);
            checkAttributes(gAttLayTunable, GOLayoutStaticValues.CC_ATTNAME);
            checkAttributes(gAttNodTunable, GOLayoutStaticValues.MF_ATTNAME);

            //Guess species current network for annotation
            String[] defaultSpecies = getSpeciesCommonName(CytoscapeInit.getProperties().getProperty("defaultSpeciesName"));
            if(!defaultSpecies[0].equals("")) {
                annotationSpeciesCode = defaultSpecies[1];
                aSpeAnnTunable.setValue(speciesValues.indexOf(defaultSpecies[0]));
                populateDataSourceList(annotationSpeciesCode);
                dsTunable.setLowerBound((Object) dsValues.toArray());
            }
            //Guess id of current network for annotation
//            String defaultID = Cytoscape.getCurrentNetwork().getNode(0).getIdentifier();//Cytoscape.getNodeAttributes().//Cytoscape.getCurrentNetwork().;
            //populateDataSourceList(defaultSpecies[1]);

            dsTunable.setImmutable(true);
            aSpeAnnTunable.setImmutable(true);
            aAttAnnTunable.setImmutable(true);
            aAnnSwiTunable.setValue(false);
            
            JPanel panel = new JPanel(new GridLayout(0, 1));
            panel.add(layoutProperties.getTunablePanel());
            return panel;
        }

        /**
         *
         * Add "(none)" to the list
         *
         * @returns List of our "special" weights
         */
        public List<String> getInitialAttributeList() {
            ArrayList<String> attList = new ArrayList<String>();
            attList.add("(none)");
            // also add special GOSlim attribute names to trigger
            // auto-annotation
//			if (null != attName) {
//				attList.add(attName);
//			}

            return attList;
        }

        /**
         * The layout protocol...
         */
        public void construct() {
                // Check to see if annotations are present
//			String[] attNames = Cytoscape.getNodeAttributes()
//					.getAttributeNames();
//			boolean ccPresent = false;
//			boolean bpPresent = false;
//			boolean mfPresent = false;
//			for (int i = 0; i < attNames.length; i++) {
//				if (attNames[i] == CC_ATTNAME) {
//					ccPresent = true;
//				} else if (attNames[i] == BP_ATTNAME) {
//					bpPresent = true;
//				} else if (attNames[i] == MF_ATTNAME) {
//					mfPresent = true;
//				}
//			}
                // Fetch annotations if they are needed
//			if (CC_ATTNAME == CellAlgorithm.attributeName && !ccPresent) {
//				setupBridgeDB(CC_CODE, CC_ATTNAME, annotationAtt,
//						annotationCode, annotationSpecies);
//			}
//			if (BP_ATTNAME == PartitionAlgorithm.attributeName && !bpPresent) {
//				setupBridgeDB(BP_CODE, BP_ATTNAME, annotationAtt,
//						annotationCode, annotationSpecies);
//			}
//			if (MF_ATTNAME == PartitionNetworkVisualStyleFactory.attributeName
//					&& !mfPresent) {
//				setupBridgeDB(MF_CODE, MF_ATTNAME, annotationAtt,
//						annotationCode, annotationSpecies);
//			}
                //System.out.println(GpmlPlugin.getInstance());
            //Need annotation or not?
            if(((Boolean) aAnnSwiTunable.getValue()).booleanValue()) {
                //Check the selected ID. If it is Ensembl, skip this step
                if(false) {
                    //Check local file for mapping
                }
                //Check local file for annotation
                if(false) {
                    //Testing FileDownload function
                    new FileDownload("Sc_GOslim_20110601.tab");
                }
                IdMapping.mapAnnotation(GOLayout.GOLayoutBaseDir+"Sc_GOslim_20110601.tab", "ID");
            }            
//			if (null != CellAlgorithm.attributeName) {
//				PartitionAlgorithm.layoutName = CellAlgorithm.LAYOUT_NAME;
//			}
//			CyLayoutAlgorithm layout = CyLayouts.getLayout("partition");
//			layout.doLayout(Cytoscape.getCurrentNetworkView(), taskMonitor);
        }

        /**
         * This method connects to the BridgeDb web service and manages all the
         * transactions through to setting node attributes.
         *
         * @param code
         * @param attname
         * @param annAtt
         * @param annCode
         * @param annSpecies
         */
//		public void setupBridgeDB(String code, String attname, String annAtt,
//				String annCode, String annSpecies) {
//
//			DataSource annDs = DataSource.getByFullName(annCode);
//
//			try {
//				Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest");
//			} catch (ClassNotFoundException e) {
//				System.out
//						.println("Can't register org.bridgedb.rdb.IDMapperRdb");
//				e.printStackTrace();
//			}
//
//			BioDataSource.init();
//			// now we connect to the driver and create a IDMapper instance.
//			// TODO: Update to use multiple species
//			try {
//				_mapper = BridgeDb
//						.connect("idmapper-bridgerest:http://webservice.bridgedb.org/"
//								+ annSpecies);
//			} catch (IDMapperException e) {
//				e.printStackTrace();
//			}
//			System.out.println("ID Mapper = " + _mapper);
//			System.out.println("ID Mapper capabilites = "
//					+ _mapper.getCapabilities());
//
//			CyAttributes attrs = Cytoscape.getNodeAttributes();
//			Xref inXref = null;
//
//			Iterator<Node> it = Cytoscape.getCurrentNetwork().nodesIterator();
//			while (it.hasNext()) {
//				Node n = it.next();
//				String inputID = (String) Cytoscape.getNodeAttributes()
//						.getAttribute(n.getIdentifier(), annAtt);
//				if (null == inputID) {
//					continue;
//				}
//				if ("Sym" == annCode && "Human" == annSpecies){
//					inputID = inputID.toUpperCase();
//				}
//
//				inXref = new Xref(inputID, annDs);
//
//				Set<Xref> outXrefs = null;
//				try {
//					outXrefs = _mapper.mapID(inXref, DataSource
//							.getBySystemCode(code));
//				} catch (IDMapperException e) {
//					System.out
//							.println("Got ID mapper exception trying to get mappings");
//				}
//				System.out.println("Got mappings: " + outXrefs);
//				if (outXrefs != null) {
//					List<String> att = new ArrayList<String>();
//					for (Xref xref : outXrefs) {
//						att.add(xref.getId());
//					}
//					Cytoscape.getNodeAttributes().setListAttribute(
//							n.getIdentifier(), attname, att);
//				}
//
//			}
//		}
//
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
