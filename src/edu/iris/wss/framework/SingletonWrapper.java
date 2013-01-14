package edu.iris.wss.framework;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import edu.iris.wss.IrisStreamingOutput.IrisSingleton;
import edu.iris.wss.utils.WebUtils;

public class SingletonWrapper {
//	public ServletContext context;

	public AppConfigurator appConfig = new AppConfigurator();
	public ParamConfigurator paramConfig = new ParamConfigurator();
	public StatsKeeper statsKeeper = new StatsKeeper();
	public IrisSingleton singleton = null;
	
	public static final Logger logger = Logger.getLogger(SingletonWrapper.class);	

	// Will be loaded in application scope via the AppScope class.  
	// This is essentially, a singleton.  But nothing happens until the configure method is 
	// called from the AppScope class which can only occur once the AppScope class has
	// the servlet context.
	
	public SingletonWrapper()  {}
	
	public void configure(ServletContext context) {
		
		String appName = null;
		if (context != null) {
			appName = WebUtils.getWebAppName(context);
		}
		
    	try {
    		appConfig.loadConfigFile(appName);
    	} catch (Exception e) {
    		logger.fatal("Invalid application config file: " + e.getMessage());
    		return;
    	}
    	
    	try {
    		paramConfig.loadConfigFile(appName);
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
