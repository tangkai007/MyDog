package org.huangpu.mydog.core.preserver;

import org.apache.commons.io.FileUtils;
import org.huangpu.mydog.core.Preserver;
import org.huangpu.mydog.core.outputitem.CopyOutputItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.file.FileURLConnection;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by shenli on 2017/4/22.
 */
public class ByCopyPreserver implements Preserver<CopyOutputItem> {

    private static final Logger LOG = LoggerFactory.getLogger(ByCopyPreserver.class);

    @Override
    public void persistent(CopyOutputItem outputItem) {
        String cpFilePath = outputItem.getCpFilePath();
        String outputPath = outputItem.getOutputPath();
        LOG.info("cpFilePath = {}" , cpFilePath);
        LOG.info("outputPath = {}" , outputPath);
        URL resourceFolder = outputItem.getResourceFolder();
        ClassLoader classLoader = outputItem.getClassLoader();

        Objects.requireNonNull(cpFilePath);
        Objects.requireNonNull(resourceFolder);

        try {
            URLConnection urlConnection = resourceFolder.openConnection();
            LOG.info("urlConnection={}", urlConnection);
            if (urlConnection instanceof JarURLConnection) {
                LOG.info("is JarURLConnection");
                JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
                JarFile jarFile = jarURLConnection.getJarFile();
                LOG.info("jarFile = " + jarFile);
                Enumeration<JarEntry> entrys = jarFile.entries();
                while(entrys.hasMoreElements()){
                    JarEntry entry = entrys.nextElement();
                    copyJarEntry2File(entry,cpFilePath,outputPath,classLoader);
                }
                jarFile.close();
            } else if (urlConnection instanceof FileURLConnection) {
                LOG.info("is FileURLConnection, cpFilePath={}", cpFilePath);
                File source = new File(resourceFolder.getPath());
                System.out.println("source = " + source);
                if (source.isDirectory()) {
                    FileUtils.copyDirectory(source, new File(outputPath));
                }
                else if (source.getPath().endsWith(".jar")) {
                    JarFile jarFile = new JarFile(source.getPath());
                    Enumeration<JarEntry> entrys = jarFile.entries();
                    while(entrys.hasMoreElements()){
                        JarEntry entry = entrys.nextElement();
                        copyJarEntry2File(entry,cpFilePath,outputPath,classLoader);
                    }
                }
                else{
                    LOG.error("Can not parse urlConnection : {}", urlConnection);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void copyJarEntry2File(JarEntry entry, String cpFilePath, String outputPath, ClassLoader classLoader){

        String entryName = entry.getName();
        if (entryName.startsWith(cpFilePath) && !entryName.endsWith("/")) {
            URL entryUrl = classLoader.getResource(entryName);
            File outFile = new File(outputPath + entryName.substring(cpFilePath.length()));

            try(InputStream  is = entryUrl.openConnection().getInputStream();
                OutputStream os = new FileOutputStream(outFile);
            ){
                byte[] buffer = new byte[1024];
                int readBytes;

                if(!outFile.exists()){
                    if(!outFile.getParentFile().exists()){
                        outFile.getParentFile().mkdirs();
                    }
                    outFile.createNewFile();
                }
                while ((readBytes = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readBytes);
                }
            } catch (IOException ioe){
                LOG.error("copyJarEntry2File failed ",ioe);
            }
        }
    }

}