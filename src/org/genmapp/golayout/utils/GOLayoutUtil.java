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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

    /**
     * @param strUrl
     * @return
     */
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
     * @param filename
     * @return
     */
    public static List<String> readFile(final String filename) {
        final List<String> ret = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                ret.add(inputLine);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param txtList
     * @param MyFilePath
     * @return
     */
    public static boolean writeFile(List<String> txtList, String MyFilePath) {
        boolean tag = true;
        try {
            FileWriter writer = new FileWriter(MyFilePath);
            BufferedWriter bufWriter = new BufferedWriter(writer);
            for(String txtData:txtList){
                bufWriter.write(txtData);
                bufWriter.newLine();
            }
            bufWriter.close();
            writer.close();
        } catch (Exception e) {
            tag = false;
            e.printStackTrace();
        }
        return tag;
    }

    /**
     * @param filename
     * @return
     */
    public static List<String> readResource(final URL filename) {
        final List<String> ret = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(filename.openStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                ret.add(inputLine);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param filePath
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
