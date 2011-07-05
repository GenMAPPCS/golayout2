/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
                    "get source id types", noargs);
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
