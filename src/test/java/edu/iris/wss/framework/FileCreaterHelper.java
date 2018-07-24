/*******************************************************************************
 * Copyright (c) 2018 IRIS DMC supported by the National Science Foundation.
 *
 * This file is part of the Web Service Shell (WSS).
 *
 * The WSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The WSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * A copy of the GNU Lesser General Public License is available at
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/

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
