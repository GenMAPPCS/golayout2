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

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Chao
 */
public class IdMapping {

    public IdMapping() {
    }

    public static List<String> getSourceTypes(String derbyFilePath) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.rdb.IDMapperRdb");
        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
        args.put("displayname", "DerbyDatabase");
        connectFileDB(args);
        String type = null;
        Map<String, Object> noargs = new HashMap<String, Object>();
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping",
                    "get target id types", noargs);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<String> sourceIDTypes = new ArrayList<String>();
        if (null != result) {
            Set<String> idTypes = (Set<String>) result.getResult();
            for(String t : idTypes) {
                sourceIDTypes.add(t);
            }
        }
        return sourceIDTypes;
    }
    
    public static boolean mapID(String derbyFilePath, String sourceIDName, String targetIDName) {
        System.out.println("Call idmapping success!");
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.rdb.IDMapperRdb");
        args.put("connstring", "idmapper-pgdb:"+derbyFilePath);
        args.put("displayname", "DerbyDatabase");
        //CyDataset d
        connectFileDB(args);
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        List<String> nodeIds = new ArrayList<String>();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            nodeIds.add(cn.getIdentifier());
        }
        mapAttribute(sourceIDName, targetIDName);
        return true;
    }

    public static boolean mapAnnotation(String GOSlimFilePath, String idName) {
        System.out.println("Call idmapping success!");
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("classpath", "org.bridgedb.file.IDMapperText");
        args.put("connstring", "idmapper-text:dssep=	,transitivity=false@file:/"+GOSlimFilePath);
        args.put("displayname", "fileGOslim");
        //CyDataset d
        connectFileDB(args);
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        List<String> nodeIds = new ArrayList<String>();
        for (CyNode cn : (List<CyNode>) currentNetwork.nodesList()) {
            nodeIds.add(cn.getIdentifier());
        }
        mapAttribute(idName, GOLayoutStaticValues.BP_ATTNAME);
        mapAttribute(idName, GOLayoutStaticValues.CC_ATTNAME);
        mapAttribute(idName, GOLayoutStaticValues.MF_ATTNAME);
        return true;
    }

    public static Set<String> guessIdType(String sampleId){
        Set<String> idTypes;
        Map<String, Object> args = new HashMap<String, Object>();
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

    /**
     *
     */
    private static CyCommandResult mapAttribute(String pkt, String skt) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("sourceattr", pkt);
        args.put("targettype", skt);
        CyCommandResult result = null;
        try {
            result = CyCommandManager.execute("idmapping",
                    "attribute based mapping", args);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    /**
     *
     */
    private static void connectFileDB(Map<String, Object> args) {
        System.out.println("-----run connectFileDB-----");
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
            };
            //System.out.println(count);
        } catch (CyCommandException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuntimeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
