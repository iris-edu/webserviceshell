/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template CONFIG_FILE, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author mike
 */
public class FileCreaterHelper {

    private static File doFilePrep(String fileName)
          throws FileNotFoundException, IOException {

        File trialFile = new File(fileName);
        if (trialFile.exists()) {
            trialFile.delete();
        }

        File trialPath = new File(trialFile.getParent());
        if(!trialPath.exists()){
            trialPath.mkdirs();
        }

        return trialFile;
    }

    /**
     * Create a file in the regular WebServiceShell folder as designated by
     * Java system property Util.WSS_OS_CONFIG_DIR, i.e. Util.WSS_OS_CONFIG_DIR
     * must be set before calling this method
     *
     * @param fileName - Treated as leading part of final file name, it may be
     *                   complete filename if nameSuffix is empty string
     * @param nameSuffix - may be empty string
     * @param contents - contents for file
     * @param isExecutable - set true to make the file executable
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String createFileInWssFolder(String fileName, String nameSuffix,
          String contents, boolean isExecutable)
          throws FileNotFoundException, IOException {

        String wssName = Util.createCfgFileName(fileName, nameSuffix);
        File newFile = doFilePrep(wssName);

        try (OutputStream os = new FileOutputStream(newFile)) {
            os.write(contents.getBytes());
            newFile.setExecutable(isExecutable);
        }

        return newFile.getAbsolutePath();
    }
}
