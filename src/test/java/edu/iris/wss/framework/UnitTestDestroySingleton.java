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

import edu.iris.wss.provider.IrisSingleton;
import java.util.Properties;
import org.apache.log4j.Logger;

public class UnitTestDestroySingleton implements IrisSingleton {
	public static final Logger logger = Logger.getLogger(UnitTestDestroySingleton.class);

    private boolean isDestroyedCalled = false;
    private Properties appinitProp = null;


	public void init() {
		logger.info("UnitTestDestroySingleton from test code init called");
	}

    public UnitTestDestroySingleton() {
        isDestroyedCalled = false;
    }

    @Override
    public void destroy() {
        isDestroyedCalled = true;
    }

    public boolean getIsDestroyedCalled() {
        return isDestroyedCalled;
    }

    @Override
    public void setAppinit(Properties prop) {
        appinitProp = prop;
    }

    public Properties getAppinitProperties() {
        return appinitProp;
    }
}
