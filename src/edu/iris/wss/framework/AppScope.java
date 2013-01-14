package edu.iris.wss.framework;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;

// This class configures the Application to know about the app-scoped class,
// SingletonWrapper classes through @Context annotations.

@Provider
public class AppScope extends SingletonTypeInjectableProvider<Context, SingletonWrapper> {
	public static final Logger logger = Logger.getLogger(AppScope.class);	
	
	public  AppScope() {
		super(SingletonWrapper.class, new SingletonWrapper());
	}
	
	// I'd love to have the servlet context injected for use in this class's constructor
	// but that's obv. not viable.  The @Context annotation allows the context to be injected
	// when it's available.  
	//
	// Therefore we call the configure method of the SingletonWrapper object in the method 
	// below when this setter is called.
	@Context
	public void setServleContext(ServletContext context) {
		this.getValue().configure(context);
	}
}