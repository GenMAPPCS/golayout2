/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.genmapp.golayout.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    
    public static boolean checkConnection() {
        try {
            URL url = new URL("http://www.google.com/");
            URLConnection urlConnection = url.openConnection();

            InputStream inputStream = urlConnection.getInputStream();
            Reader reader = new InputStreamReader(inputStream);

            StringBuilder contents = new StringBuilder();
            CharBuffer buf = CharBuffer.allocate(1024);

            while (true) {
                    reader.read(buf);
                    if (!buf.hasRemaining())
                            break;

                    contents = contents.append(buf);
            }
            inputStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * @param strUrl
     * @return
     */
//    public static List<String> readUrl(final String strUrl) {
//        final List<String> ret = new ArrayList<String>();
//
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        executor.execute(new Runnable() {
//            public void run() {
//                try {
//                    URL url = new URL(strUrl);
//                    URLConnection yc = url.openConnection();
//                    BufferedReader in = new BufferedReader(
//                                new InputStreamReader(yc.getInputStream()));
//
//                    String inputLine;
//                    while ((inputLine = in.readLine()) != null)
//                        ret.add(inputLine);
//                    in.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        // TODO: refactor executor
//        try {
//            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
//                // System.err.println("Failed to connect to " + strUrl);
//                executor.shutdown();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ret;
//    }

    public static List<String> readUrl(final String strUrl) {
        final List<String> ret = new ArrayList<String>();
        try {
            URL url = new URL(strUrl);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            InputStream in = c.getInputStream();
            if (in != null) {
                StringBuilder sb = new StringBuilder();
                String line;
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(in, "UTF-8"));
                    while ((line = reader.readLine()) != null) {
                        ret.add(line);
                    }
                } finally {
                    in.close();
                }
            } else {
                System.out.println("No databases found at " + strUrl);
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
