/**
 * *****************************************************************************
 * Copyright (c) 2015 IRIS DMC supported by the National Science Foundation.
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

import java.util.Set;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

// Set up application for injection and loading to container

// Don't use annotation ApplicationPath here 
//@ApplicationPath("/")
public class MyApplication extends ResourceConfig {
    
  public static final Logger logger = Logger.getLogger(MyApplication.class);
  public static final String CLASS_NAME = MyApplication.class.getSimpleName();

  // first thing to run, setup logging, load configuration, etc.
  @Inject
  public MyApplication(ServiceLocator serviceLocator,
        @Context ServletContext servletContext) throws Exception {
    // always setup log4j first
    String configBase = Util.getWssFileNameBase(servletContext.getContextPath());
    Util.myNewInitLog4j(configBase);

    // get configuration information now
    SingletonWrapper sw = new SingletonWrapper();
    sw.configure(configBase);

    // bind classes as needed to be available via a CONTEXT annotation
    DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);
    Injections.addBinding(
        Injections.newBinder(sw).to(SingletonWrapper.class), dc);
    dc.commit();

    // add in classes which have static endpoints via annotations
    register(edu.iris.wss.Wss.class);

    // add in endpoints  TBD remove soon
    addEndpoint("info1", edu.iris.wss.Info1.class, "getDyWssVersion", "GET");

    // add dynamic endpoints as defined in -service.cfg file
    Set<String> epNames = sw.appConfig.getEndpoints();
    for (String epName : epNames) {
        
        String methodName = "doIrisStreaming";
        if (sw.appConfig.getIrisEndpointClass(epName) instanceof
              edu.iris.wss.provider.IrisProcessor) {
            methodName = "doIrisProcessing";
        }

        // Note: HEAD seems to be allowed by default
        addEndpoint(epName, edu.iris.wss.provider.IrisDynamicProvider.class,
          methodName, "GET");
        
        if (sw.appConfig.isPostEnabled(epName)) {
            addEndpoint(epName, edu.iris.wss.provider.IrisDynamicProvider.class,
                  methodName, "POST");

            // temporary test
            addEndpoint(epName + "postecho",
                  edu.iris.wss.provider.IrisDynamicProvider.class,
                  "echoPostString", "POST");
        }
    }
  }
  
  public MyApplication() {
    System.out.println("*****  " + CLASS_NAME + " no-arg constructor");
  }

  /**
   * 
   * @param epName - name of last part of URI path
   * @param epClass - the class containing the method to respond to a request
   * @param epMethodName - the method that handles this request
   * @param httpMethod - usually GET or POST
   */
  private void addEndpoint(String epName, Class epClass, String epMethodName,
        String httpMethod) {
    final Resource.Builder resourceBuilder = Resource.builder();
    resourceBuilder.path(epName);

    final ResourceMethod.Builder rmBuilder = resourceBuilder.addMethod(httpMethod);
    try {
        rmBuilder.produces(MediaType.TEXT_PLAIN_TYPE)
              .handledBy(epClass, epClass.getMethod(epMethodName, null));

    } catch (NoSuchMethodException ex) {
        String msg = CLASS_NAME + " attempted endpoint: " + epName + "  class: "
              + epClass.getName() + "  NoSuchMethodException: " + ex;
        System.out.println(msg);
        logger.error(msg);
    } catch (SecurityException ex) {
        String msg = CLASS_NAME + " attempted endpoint: " + epName + "  class: "
              + epClass.getName() + "  SecurityException: " + ex;
        System.out.println(msg);
        logger.error(msg);
    }

    final Resource endpointRes = resourceBuilder.build();
    registerResources(endpointRes);
    
    String msg = CLASS_NAME + " added endpoint: " + endpointRes.getPath()
          + "  httpMethod: " + httpMethod;
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


