/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
