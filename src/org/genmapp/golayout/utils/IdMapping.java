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
package org.genmapp.golayout.utils;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.data.CyAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.genmapp.golayout.GOLayout;

/**
 *
 * @author Chao
 */
public class IdMapping {

    public IdMapping() {
    }

    public static List<String> getSourceTypes() {
        Map<String, Object> noargs = new HashMap<String, Object>();
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping", "get target id types", noargs);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<String> sourceIDTypes = new ArrayList<String>();
        if (null != result) {
            //System.out.println(result.getResult().toString());
            Set<String> idTypes = (Set<String>) result.getResult();
            for(String t : idTypes) {
                sourceIDTypes.add(t);
            }
        }
        return sourceIDTypes;
    }
    
    public static Set<String> guessIdType(String sampleId){
        Map<String, Object> args = new HashMap<String, Object>();
        Set<String> idTypes;
        args.put("sourceid", sampleId);
        try {
            CyCommandResult result = CyCommandManager.execute("idmapping", "guess id type", args);
            idTypes = (Set<String>) result.getResult();
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        return idTypes;
    }
    
//    public static boolean mapID(String derbyFilePath, String sourceIDName, String targetIDName) {
//        System.out.println("Call idmapping success!");
//        Map<String, Object> args = new HashMap<String, Object>();
//        args.put("classpath", "org.bridgedb.rdb.IDMapperRdb");
//        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
//        args.put("displayname", derbyFilePath);
//        //CyDataset d
//        connectFileDB(args);
//        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
//        List<String> nodeIds = new ArrayList<String>();
//        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
//            nodeIds.add(cn.getIdentifier());
//        }
////        mapAttribute(sourceIDName, targetIDName);
//        mapGeneralAttribute(nodeIds, targetIDName);
//        System.out.println("Successfully mapping ID");
//        args = new HashMap<String, Object>();
//        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
//        //disConnectFileDB(args);
//        return true;
//    }

    private void setGOAttribute(Map<String, Set<String>> idGOMap, String attributeName) {
        CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            String dnKey = cn.getIdentifier();
            Set<String> secKeys = idGOMap.get(dnKey);
            List<String> secKeyList = new ArrayList<String>();
            if(secKeys!=null) {
                if(secKeys.size()>1) {
                    secKeyList = new Vector(secKeys);
                } else {
                    secKeyList = Arrays.asList(secKeys.toArray()[0].toString().trim().split(","));
                }
                //Sort the resuls with reverse order.
                Collections.sort(secKeyList, Collections.reverseOrder());
            } else {
                secKeyList.add("unassigned");
            }
            nodeAttrs.setListAttribute(dnKey, attributeName, secKeyList);
       }
    }

    private void connectGOSlimSource(String GOSlimFilePath) {
        //Remove one '/' to fit MacOS
        if(GOSlimFilePath.charAt(0)=='/')
            GOSlimFilePath = GOSlimFilePath.substring(1, GOSlimFilePath.length());
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.file.IDMapperText");
        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
        args.put("displayname", GOSlimFilePath);
        connectFileDB(args);
    }

    private void disConnectGOSlimSource(String GOSlimFilePath) {
        //Remove one '/' to fit MacOS
        if(GOSlimFilePath.charAt(0)=='/')
            GOSlimFilePath = GOSlimFilePath.substring(1, GOSlimFilePath.length());
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.file.IDMapperText");
        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
        args.put("displayname", GOSlimFilePath);
        disConnectFileDB(args);
    }

    public static void connectDerbyFileSource(String derbyFilePath) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.rdb.IDMapperRdb");
        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
        args.put("displayname", derbyFilePath);
        connectFileDB(args);
    }

    public static void disConnectDerbyFileSource(String derbyFilePath) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.rdb.IDMapperRdb");
        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
        args.put("displayname", derbyFilePath);
        disConnectFileDB(args);
    }

    /**
     *
     */
    private Map<String, Set<String>> mapAttribute(List<String> sourceID,
            String sourceType, String targetType) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("sourceid", sourceID);
        args.put("sourcetype", sourceType);
        args.put("targettype", targetType);
        //System.out.println(sourceType+"\t"+targetType);
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping", "general mapping", args);
            //System.out.println("message:"+result.getMessages().get(0));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Map<String, Set<String>> secondaryKeyMap = new HashMap<String, Set<String>>();
        if (null != result) {
            secondaryKeyMap= (Map<String, Set<String>>) result.getResult();
        }
        return secondaryKeyMap;
    }

    private List<String> convertSetMapValueToList(Map<String, Set<String>> stringSetMap) {
        Set<String> sumValue = new HashSet();
        List<String> results = new ArrayList<String>();
        for(Object keyID : stringSetMap.keySet().toArray()) {
            //System.out.println(keyID);
            Set<String> valueSet = stringSetMap.get(keyID);
            for(String value:valueSet) {
                if(value.trim().length()>0) {
                    String[] multValues = value.split(",");
                    for(String eachValue:multValues)
                        sumValue.add(eachValue.trim());
                    
                }
            }
            //System.out.println(sumValue.toString());
        }
        if(sumValue.size()<1)
            results.add("unassigned");
        else if(sumValue.size()>1)
            results = new Vector(sumValue);
        else
            results = Arrays.asList(sumValue.toArray()[0].toString().trim().split(","));
        //Sort the resuls with reverse order.
        Collections.sort(results, Collections.reverseOrder());
        return results;
    }

    private Map<String, Set<String>> checkEmptyValue(Map<String, Set<String>> originalMap) {
        Map<String, Set<String>> result = originalMap;
        Object[] geneList = result.keySet().toArray();
        for(Object o:geneList) {
            Set<String> GOList = result.get(o);
            String temp = GOList.toString().trim();
            if(temp.substring(1, temp.length()-1).trim().length()<1) {
                Set<String> valueList = new HashSet<String>();
                valueList.add("unassigned");
                result.put(o.toString(), valueList);
            } else {
                result.put(o.toString(), GOList);
            }
        }
        return result;
    }

    private boolean isEnsemblID(String idType){
        if(idType.trim().toLowerCase().equals("gramene arabidopsis")) {
            return true;
        } else if(idType.toLowerCase().indexOf("ensembl")!=-1) {
            return true;
        } else
            return false;
    }
    
    public boolean mapID(String derbyFilePath, String GOSlimFilePath,
            String sourceIDName, String sourceType, String targetType) {
        Map<String, Set<String>> idGOMap = new HashMap<String, Set<String>>();
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
        List<String> nodeIds = new ArrayList<String>();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            nodeIds.add(cn.getIdentifier());
        }
        if(sourceIDName == null ? "ID" == null : sourceIDName.equals("ID")) {
            if(!isEnsemblID(sourceType)) {
                System.out.println("IdMapping:mapID: ID & Non-Ensembl");
                //Finished at 2011-09-21, for non-"Ensembl" & ID. ticket 3 for idmapping
                //connectDerbyFileSource(derbyFilePath);
                System.out.println("IdMapping:mapID: connectDerbyFileSource :"+ sourceType+" : "+ targetType);
                Map<String, Set<String>> idEnMap = mapAttribute(nodeIds, sourceType, targetType);
                System.out.println("IdMapping:mapID: mapAttribute Ensembl");
                setGOAttribute(idEnMap, GOLayout.pluginName+"_Ensembl");
                System.out.println("IdMapping:mapID: setGOAttribute");
                //disConnectDerbyFileSource(derbyFilePath);
                System.out.println("IdMapping:mapID: disConnectDerbyFileSource");
                connectGOSlimSource(GOSlimFilePath);
                for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
                    String keyID = cn.getIdentifier();
                    if(idEnMap.containsKey(keyID)) {
                        List<String> keyEnsemblSet = new ArrayList<String>(idEnMap.get(keyID));
                        if(keyEnsemblSet.size()>0) {
                            idGOMap = mapAttribute(keyEnsemblSet, "Ensembl", GOLayoutStaticValues.BP_ATTNAME);
                            nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.BP_ATTNAME, convertSetMapValueToList(idGOMap));
                            idGOMap = mapAttribute(keyEnsemblSet, "Ensembl", GOLayoutStaticValues.CC_ATTNAME);
                            nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.CC_ATTNAME, convertSetMapValueToList(idGOMap));
                            idGOMap = mapAttribute(keyEnsemblSet, "Ensembl", GOLayoutStaticValues.MF_ATTNAME);
                            nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.MF_ATTNAME, convertSetMapValueToList(idGOMap));
                        } else {
                            List<String> valueList = new ArrayList<String>();
                            valueList.add("unassigned");
                            nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.BP_ATTNAME, valueList);
                            nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.CC_ATTNAME, valueList);
                            nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.MF_ATTNAME, valueList);
                        }
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        valueList.add("unassigned");
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.BP_ATTNAME, valueList);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.CC_ATTNAME, valueList);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.MF_ATTNAME, valueList);
                    }
                }
                disConnectGOSlimSource(GOSlimFilePath);                
            } else {
                System.out.println("IdMapping:mapID: ID & Ensembl");
                //Finished, for "Ensembl" & ID.
                connectGOSlimSource(GOSlimFilePath);
                idGOMap = mapAttribute(nodeIds, "Ensembl", GOLayoutStaticValues.BP_ATTNAME);
                idGOMap=checkEmptyValue(idGOMap);
                setGOAttribute(idGOMap, GOLayoutStaticValues.BP_ATTNAME);
                idGOMap = mapAttribute(nodeIds, "Ensembl", GOLayoutStaticValues.CC_ATTNAME);
                idGOMap=checkEmptyValue(idGOMap);
                setGOAttribute(idGOMap, GOLayoutStaticValues.CC_ATTNAME);
                idGOMap = mapAttribute(nodeIds, "Ensembl", GOLayoutStaticValues.MF_ATTNAME);
                idGOMap=checkEmptyValue(idGOMap);
                setGOAttribute(idGOMap, GOLayoutStaticValues.MF_ATTNAME);
                disConnectGOSlimSource(GOSlimFilePath);
            }
        } else {
            if(!isEnsemblID(sourceType)) {
                System.out.println("IdMapping:mapID: Non-ID & Non-Ensembl");
                //unfinished, for "Ensembl" & non-ID. ticket 3 for idmapping
                Map<String, List<String>> idEnMap = new HashMap();
                //connectDerbyFileSource(derbyFilePath);
                for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
                    String keyID = cn.getIdentifier();
                    List<String> attList;
                    if (nodeAttrs.getType(sourceIDName) == CyAttributes.TYPE_SIMPLE_LIST) {
                        attList = nodeAttrs.getListAttribute(keyID, sourceIDName);
                    } else {
                        attList = new ArrayList<String>();
                        attList.add(nodeAttrs.getAttribute(keyID, sourceIDName).toString());
                    }
                    idGOMap = mapAttribute(attList, sourceType, targetType);
                    idEnMap.put(keyID, convertSetMapValueToList(idGOMap));                    
                }
                //disConnectDerbyFileSource(derbyFilePath);
                connectGOSlimSource(GOSlimFilePath);
                for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
                    String keyID = cn.getIdentifier();
                    List<String> attList = idEnMap.get(keyID);
                    if(attList.size()>0) {
                        idGOMap = mapAttribute(attList, "Ensembl", GOLayoutStaticValues.BP_ATTNAME);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.BP_ATTNAME, convertSetMapValueToList(idGOMap));
                        idGOMap = mapAttribute(attList, "Ensembl", GOLayoutStaticValues.CC_ATTNAME);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.CC_ATTNAME, convertSetMapValueToList(idGOMap));
                        idGOMap = mapAttribute(attList, "Ensembl", GOLayoutStaticValues.MF_ATTNAME);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.MF_ATTNAME, convertSetMapValueToList(idGOMap));
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        valueList.add("unassigned");
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.BP_ATTNAME, valueList);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.CC_ATTNAME, valueList);
                        nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.MF_ATTNAME, valueList);
                    }
                }
                disConnectGOSlimSource(GOSlimFilePath);
            } else {
                System.out.println("IdMapping:mapID: Non-ID & Ensembl");
                //Finished at 2011-09-21, for non-"Ensembl" & non-ID. ticket 3 for idmapping
                connectGOSlimSource(GOSlimFilePath);
                for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
                    String keyID = cn.getIdentifier();
                    List<String> ensemblList;
                    if (nodeAttrs.getType(sourceIDName) == CyAttributes.TYPE_SIMPLE_LIST) {
                        ensemblList = nodeAttrs.getListAttribute(keyID, sourceIDName);                        
                    } else {
                        ensemblList = new ArrayList<String>();
                        ensemblList.add(nodeAttrs.getAttribute(keyID, sourceIDName).toString());
                    }
                    idGOMap = mapAttribute(ensemblList, "Ensembl", GOLayoutStaticValues.BP_ATTNAME);
                    nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.BP_ATTNAME, convertSetMapValueToList(idGOMap));
                    idGOMap = mapAttribute(ensemblList, "Ensembl", GOLayoutStaticValues.CC_ATTNAME);
                    nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.CC_ATTNAME, convertSetMapValueToList(idGOMap));
                    idGOMap = mapAttribute(ensemblList, "Ensembl", GOLayoutStaticValues.MF_ATTNAME);
                    nodeAttrs.setListAttribute(keyID.toString(), GOLayoutStaticValues.MF_ATTNAME, convertSetMapValueToList(idGOMap));
                }
                disConnectGOSlimSource(GOSlimFilePath);
            }
        }
        

//        Map<String, Object> args = new HashMap<String, Object>();
//        args.put("classpath", "org.bridgedb.rdb.IDMapperRdb");
//        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
//        args.put("displayname", derbyFilePath);
//        //CyDataset d
//        connectFileDB(args);
//        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
//        List<String> nodeIds = new ArrayList<String>();
//        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
//            nodeIds.add(cn.getIdentifier());
//        }
//
//        mapGeneralAttribute(nodeIds, targetIDName);
//        System.out.println("Successfully mapping ID");
//        args = new HashMap<String, Object>();
//        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
        //disConnectFileDB(args);
        return true;
    }
    
    public static boolean mapAnnotation(String GOSlimFilePath, String idName) {
        System.out.println("Call annotation mapping success!");
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.file.IDMapperText");
        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
        args.put("displayname", "fileGOslim");
        //CyDataset d
        connectFileDB(args);

        /*Using general mapping*/
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        List<String> nodeIds = new ArrayList<String>();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            nodeIds.add(cn.getIdentifier());
        }
        mapGeneralAttribute(nodeIds, GOLayoutStaticValues.BP_ATTNAME);
        mapGeneralAttribute(nodeIds, GOLayoutStaticValues.CC_ATTNAME);
        mapGeneralAttribute(nodeIds, GOLayoutStaticValues.MF_ATTNAME);
        /**/
        /*Using attribute based mapping*
        mapAttribute(idName, GOLayoutStaticValues.BP_ATTNAME);
        mapAttribute(idName, GOLayoutStaticValues.CC_ATTNAME);
        mapAttribute(idName, GOLayoutStaticValues.MF_ATTNAME);
        /**/

        args = new HashMap<String, Object>();
        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
        //disConnectFileDB(args);
        return true;
    }

//    public static boolean mapAnnotation(String GOSlimFilePath, String idName) {
//        System.out.println("Call annotation mapping success!");
//        Map<String, Object> args = new HashMap<String, Object>();
//        args.put("classpath", "org.bridgedb.file.IDMapperText");
//        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
//        args.put("displayname", "fileGOslim");
//        //CyDataset d
//        connectFileDB(args);
//
//        /*Using general mapping*/
//        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
//        List<String> nodeIds = new ArrayList<String>();
//        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
//            nodeIds.add(cn.getIdentifier());
//        }
//        mapGeneralAttribute(nodeIds, GOLayoutStaticValues.BP_ATTNAME);
//        mapGeneralAttribute(nodeIds, GOLayoutStaticValues.CC_ATTNAME);
//        mapGeneralAttribute(nodeIds, GOLayoutStaticValues.MF_ATTNAME);
//        /**/
//        /*Using attribute based mapping*
//        mapAttribute(idName, GOLayoutStaticValues.BP_ATTNAME);
//        mapAttribute(idName, GOLayoutStaticValues.CC_ATTNAME);
//        mapAttribute(idName, GOLayoutStaticValues.MF_ATTNAME);
//        /**/
//
//        args = new HashMap<String, Object>();
//        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
//        //disConnectFileDB(args);
//        return true;
//    }

    /**
     *
     */
    private static CyCommandResult mapGeneralAttribute(List<String> pkt, String skt) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("sourceid", pkt);
        args.put("sourcetype", "Ensembl Yeast");
        args.put("targettype", skt);
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute(
                    "idmapping", "general mapping", args);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Map<String, Set<String>> secondaryKeyMap = new HashMap<String, Set<String>>();
        if (null != result) {
            secondaryKeyMap= (Map<String, Set<String>>) result.getResult();
//            Map<String, Set<String>> keyMappings =
//                    (Map<String, Set<String>>) result.getResult();
//            for (String primaryKey : keyMappings.keySet()) {
//                secondaryKeyMap.put(primaryKey, keyMappings.get(primaryKey));
//            }
        }
        CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            String dnKey = cn.getIdentifier();
            Set<String> secKeys = secondaryKeyMap.get(dnKey);
            List<String> secKeyList = new ArrayList<String>();
            if(secKeys!=null) {
                //TODO: always get a String rather than a List when pull out
                //the GO terms in Line 177 of PartitionAlgotithem.java
                if(secKeys.size()>1) {
                    secKeyList = new Vector(secKeys);
                } else {
                    secKeyList = Arrays.asList(secKeys.toArray()[0].toString().trim().split(","));
                }
            } else {
                secKeyList.add("unassigned");
            }
//            System.out.println(secKeyList.get(0));
//            List<String> secKeyList = new ArrayList<String>();
//            if(secKeys!=null) {
//                for (String sk : secKeys) {
//                    //System.out.println(sk);
//                    if(sk != null)
//                        secKeyList.add(sk);
//                    else
//                        secKeyList.add("");
//                }
//            } else {
//                secKeyList.add("");
//            }
            nodeAttrs.setListAttribute(dnKey, skt, secKeyList);
       }
        return result;
    }

    private static CyCommandResult mapAttribute(String pkt, String skt) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("sourceattr", pkt);
        args.put("targettype", skt);
        CyCommandResult result = null;
        try {
            long start=System.currentTimeMillis();
            result = CyCommandManager.execute(
                    "idmapping", "attribute based mapping", args);
            long pause=System.currentTimeMillis();
            System.out.println("Running time:"+(pause-start)/1000/60+"min "+(pause-start)/1000%60+"sec");
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (String msg : result.getMessages()) {
			System.out.println(msg);
		}
        return result;
    }

    /**
     *
     */
    private static void connectFileDB(Map<String, Object> args) {
        System.out.println("-----run connectFileDB-----");
        //System.out.println(args.toString());
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping",
                    "register resource", args);
            List<String> results = result.getMessages();
            if (results.size() > 0) {
                for (String re : results) {
                    if (re.contains("Success")) {
                        System.out.println("Success");
                    } else {
                        System.out.println("no Success");
                    }
                }
            } else {
                System.out.println("Failed to connect!");
            }
            //System.out.println(count);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private static void disConnectFileDB(Map<String, Object> args) {
        System.out.println("-----run disConnectFileDB-----");
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping",
                    "unregister resource", args);
            List<String> results = result.getMessages();
            if (results.size() > 0) {
                for (String re : results) {
                    System.out.println(re);
                }
            } else {
                System.out.println("Failed to unregister db!");
            };
            //System.out.println(count);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
