/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genmapp.golayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.pathvisio.cytoscape.GpmlPlugin;

/**
 *
 * @author Chao
 */
public class GOLayoutUtil {

    public static boolean checkGPMLPlugin(){
        try {
            GpmlPlugin.getInstance();
            return true;
        } catch(NoClassDefFoundError e){
            return false;
        }
    }

    /**
     * @param strUrl
     * @return
     */
    public static List<String> readUrl(final String strUrl) {
        final List<String> ret = new ArrayList<String>();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            public void run() {
                try {
                    URL url = new URL(strUrl);
                    URLConnection yc = url.openConnection();
                    BufferedReader in = new BufferedReader(
                                new InputStreamReader(yc.getInputStream()));

                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        ret.add(inputLine);
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // TODO: refactor executor
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                // System.err.println("Failed to connect to " + strUrl);
                executor.shutdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param prefix
     * @return
     */
    public static List<String> retrieveLocalFiles(String filePath) {
        File dir = new File(filePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
        String[] children = dir.list();
        return Arrays.asList(children);
    }
}
