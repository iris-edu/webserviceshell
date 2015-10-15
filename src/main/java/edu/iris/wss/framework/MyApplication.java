/**
 * *****************************************************************************
 * Copyright (c) 2014 IRIS DMC supported by the National Science Foundation.
 *
 * This file is part of the Web Service Shell (WSS).
 *
 * The WSS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * The WSS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * A copy of the GNU Lesser General Public License is available at
 * <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package edu.iris.wss.framework;

import edu.iris.wss.utils.WebUtils;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

// Set up application for injection and loading to container

// Don't use annotation ApplicationPath here 
//@ApplicationPath("/")
public class MyApplication extends ResourceConfig {
    
  public static final Logger logger = Logger.getLogger(MyApplication.class);

  // first thing to run, setup logging, load configuration, etc.
  @Inject
  public MyApplication(ServiceLocator serviceLocator, @Context ServletContext servletContext) {
    // always setup log4j first
    WebUtils.myInitLog4j(servletContext);

    // get configuration information now
    SingletonWrapper sw = new SingletonWrapper();
    sw.configure(servletContext);

    // bind classes as needed to be available via a CONTEXT annotation
    DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);
    Injections.addBinding(
        Injections.newBinder(sw).to(SingletonWrapper.class), dc);
    dc.commit();

    // add in classes which have static endpoints via annotations
    register(edu.iris.wss.Wss.class);

    // add in endpoints
    addEndpoint("info1", edu.iris.wss.Info1.class, "getDyWssVersion");

    // add dynamic endpoints as defined in -service.cfg file
    Set<String> epNames = sw.appConfig.getEndpoints();
    for (String epName : epNames) {
        addEndpoint(epName, edu.iris.wss.framework.IrisDynamicExecutor.class,
          "doIrisStreaming");
    }
  }
  
  public MyApplication() {
    System.out.println("*****  MyApplication no-arg constructor");
  }

  private void addEndpoint(String epPath, Class epClass, String methodName) {
    final Resource.Builder resourceBuilder = Resource.builder();
    resourceBuilder.path(epPath);

    final ResourceMethod.Builder methodForGet = resourceBuilder.addMethod("GET");
    try {
        methodForGet.produces(MediaType.TEXT_PLAIN_TYPE)
              .handledBy(epClass, epClass.getMethod(methodName, null));

    } catch (NoSuchMethodException ex) {
        String msg = "MyApplication attempted endpoint: " + epPath + "  class: "
              + epClass.getName() + "  NoSuchMethodException: " + ex;
        System.out.println(msg);
        logger.error(msg);
    } catch (SecurityException ex) {
        String msg = "MyApplication attempted endpoint: " + epPath + "  class: "
              + epClass.getName() + "  SecurityException: " + ex;
        System.out.println(msg);
        logger.error(msg);
    }

    final Resource endpointRes = resourceBuilder.build();
    registerResources(endpointRes);
    
    String msg = "MyApplication added endpoint: " + endpointRes.getPath();
    System.out.println(msg);
    logger.info(msg);
  }
}

// example of alternate implemenation using ResourceConfig

//@ApplicationPath("/")
//public class MyApplicationAlt3 extends ResourceConfig {
//    
//  public static final Logger logger = Logger.getLogger(MyApplicationAlt3.class);
//  
//  public static class MyHK2Binder extends AbstractBinder {
//
//    @Override
//    protected void configure() {
//      System.out.println("***** MyApplicationAlt3 static MyHK2Binder configure start");
//      
//      //bindAsContract(AppScope.class).in(Singleton.class);
//      // singleton instance binding
//      AppScope appScope = new AppScope();
//      bind(appScope).to(AppScope.class);
//      bind(appScope.getSingletonWrapper()).to(SingletonWrapper.class);
//      System.out.println("***** MyApplicationAlt3 MyHK2Binder configure");
//    }
//  }
//  
//  public MyApplicationAlt3() {
//    System.out.println("***** MyApplicationAlt3 extends ResourceConfig construct");
//    register(Wss.class);
//    register(new MyHK2Binder());
//    System.out.println("***** MyApplicationAlt3 regular constructor end");
//  }
//}


