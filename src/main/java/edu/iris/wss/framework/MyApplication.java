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

// Don't use annotation ApplicationPath here because it is explicitely
// set in web.xml along with some security and other settings
//@ApplicationPath("/")
public class MyApplication extends ResourceConfig {
    
  public static final Logger logger = Logger.getLogger(MyApplication.class);

  @Inject
  public MyApplication(ServiceLocator serviceLocator, @Context ServletContext servletContext) {
    System.out.println("*****constr MyApplicationRC Inject, serviceLocator: " + serviceLocator);

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
    
    register(edu.iris.wss.Wss.class);
    
    final Resource.Builder resourceBuilder = Resource.builder();
    resourceBuilder.path("helloworld");

    final ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod("GET");
    methodBuilder.produces(MediaType.TEXT_PLAIN_TYPE)
            .handledBy(new Inflector<ContainerRequestContext, String>() {

        @Override
        public String apply(ContainerRequestContext containerRequestContext) {
            return "Hello hk2 World!";
        }
    });

    final Resource resource = resourceBuilder.build();
    registerResources(resource);

    // --------------
    addEndpoint("info1", edu.iris.wss.Info1.class, "getDyWssVersion");
    addEndpoint("dyquery", edu.iris.wss.Wss.class, "query");
    addEndpoint("info2", edu.iris.wss.Info2.class, "doIrisStreaming");
    addEndpoint("v2/query", edu.iris.wss.Wss.class, "query");
    addEndpoint("v3/query", edu.iris.wss.endpoints.CmdProcessorIrisEP.class, "doIrisStreaming");

    // -------------------
    System.out.println("*****constr MyApplicationRC servletContextConstr: " + servletContext);
    System.out.println("*****constr MyApplicationRC ctxPath: " + servletContext.getContextPath());
  }
  
  public MyApplication() {
    System.out.println("*****regconstr MyApplicationRC regular constructor");
  }

  private void addEndpoint(String epPath, Class epClass, String methodName) {
    final Resource.Builder resourceBuilder = Resource.builder();
    resourceBuilder.path(epPath);

    final ResourceMethod.Builder methodForGet = resourceBuilder.addMethod("GET");
    try {
        methodForGet.produces(MediaType.TEXT_PLAIN_TYPE)
              .handledBy(epClass, epClass.getMethod(methodName, null));

    } catch (NoSuchMethodException ex) {
        System.out.println("*****constr MyApplicationRC endpoint: " + epPath
              + "  class: " + epClass.getName() + "  NoSuchMethodException: " + ex);
    } catch (SecurityException ex) {
        System.out.println("*****constr MyApplicationRC endpoint: " + epPath
              + "  class: " + epClass.getName() + "  SecurityException: " + ex);
    }

    final Resource endpointRes = resourceBuilder.build();
    registerResources(endpointRes);
    
    
    System.out.println("*****constr MyApplicationRC Inject, res_dwv: " + endpointRes);
    System.out.println("*****constr MyApplicationRC Inject, res_dwv name: " + endpointRes.getName());
    System.out.println("*****constr MyApplicationRC Inject, res_dwv path: " + endpointRes.getPath());
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


