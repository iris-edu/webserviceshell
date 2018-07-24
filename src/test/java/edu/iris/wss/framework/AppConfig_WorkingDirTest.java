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
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class AppConfig_WorkingDirTest {
    public AppConfig_WorkingDirTest() {
    }

    @Test
    public void test_readsJavaProperties() throws Exception {
        String endPath = "target/orother";
        File directory = new File(endPath);
        if(!directory.exists()){
            directory.mkdirs();
        }

        String currentDir = System.getProperty("user.dir");
        String targetPath = currentDir + File.separator + endPath;
//        System.out.println("** tp: " + targetPath);
//        System.out.println("** env: " + System.getenv());

        System.setProperty("something", "orother");
        AppConfigurator appCfg = new AppConfigurator();
        assert(targetPath.equals(appCfg.getValidatedWorkingDir(currentDir
              + "/target/${something}")));
    }

    @Test
    public void test_workingDirExists() throws Exception {
        String endPath = "target/workingdirtest";
        File directory = new File(endPath);
        if(directory.exists()){
            directory.delete();
        }

        String currentDir = System.getProperty("user.dir");
        String targetPath = currentDir + File.separator + endPath;

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.getValidatedWorkingDir(targetPath);
            fail();
        } catch (Exception ex) {
            // noop - success for dirctory not existing
        }
    }

    @Test
    public void test_notAbsolute() throws Exception {
        String endPath = "target/workingdirtest";

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.getValidatedWorkingDir(endPath);
            fail();
        } catch (Exception ex) {
            // noop - success for not absolute path
        }
    }
}
