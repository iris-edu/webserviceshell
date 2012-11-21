package edu.iris.wss.framework;


import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.log4j.Logger;

import edu.iris.wss.IrisStreamingOutput.IrisSingleton;

public class SingletonWrapper {
    @Context	ServletContext context;
    
	public AppConfigurator appConfig = new AppConfigurator();
	public ParamConfigurator paramConfig = new ParamConfigurator();
	public IrisSingleton singleton = null;
	
	public static final Logger logger = Logger.getLogger(SingletonWrapper.class);	
	
	// Will be loaded in application scope via the AppScope class.  Essentially, a singleton
	public SingletonWrapper()  {	
		
    	try {
    		appConfig.loadConfigFile();
    	} catch (Exception e) {
    		logger.fatal("Invalid application config file: " + e.getMessage());
    		return;
    	}
    	
    	try {
    		paramConfig.loadConfigFile();
    	} catch (Exception e) {
    		logger.fatal("Invalid parameter config file: " + e.getMessage());
    	}
    	
    	if (appConfig.getSingletonClassName() != null) {
    		try {
        		Class<?> singletonClass;
    			singletonClass = Class.forName(appConfig.getSingletonClassName());
    			singleton = (IrisSingleton) singletonClass.newInstance();
    		} catch (ClassNotFoundException e) {
    			String err = "Could not find class with name: " + appConfig.getSingletonClassName();
    			logger.fatal(err);
    			throw new RuntimeException(err);
    		} catch (InstantiationException e) {
    			logger.fatal("Could not instantiate class: " + appConfig.getSingletonClassName());
    		} catch (IllegalAccessException e) {
    			logger.fatal("Illegal access while instantiating class: " + appConfig.getSingletonClassName());
    		}
    	}

	}
	
	public SingletonWrapper(AppConfigurator ac, ParamConfigurator pc) {
		this.appConfig = ac;
		this.paramConfig = pc;
	}
}
