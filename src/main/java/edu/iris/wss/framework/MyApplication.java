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

import edu.iris.wss.Wss;
import edu.iris.wss.utils.WebUtils;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import org.apache.log4j.Logger;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;

// Set up application for injection and loading to container

// Don't use annotation ApplicationPath here because it is explicitely
// set in web.xml along with some security and other settings
//@ApplicationPath("/")
public class MyApplication extends Application {

  @Inject
  public MyApplication(ServiceLocator serviceLocator, @Context ServletContext servletContextConstr) {
    System.out.println("*****constr MyApplication Inject, serviceLocator: " + serviceLocator);
    WebUtils.myInitLog4j(servletContextConstr);

    SingletonWrapper sw = new SingletonWrapper();
    sw.configure(servletContextConstr);
    
    DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);
//    AppScope appScope = new AppScope();
//    Injections.addBinding(
//        Injections.newBinder(appScope).to(AppScope.class), dc);
//    Injections.addBinding(
//        Injections.newBinder(appScope.getSingletonWrapper()).to(SingletonWrapper.class), dc);
    
    Injections.addBinding(
        Injections.newBinder(sw).to(SingletonWrapper.class), dc);

    
    dc.commit();
//    System.out.println("*****constr MyApplication Inject, appScope: " + appScope);
    System.out.println("*****constr MyApplication servletContextConstr: " + servletContextConstr);
    System.out.println("*****constr MyApplication ctxPath: " + servletContextConstr.getContextPath());
  }
  
  @Override
  public Set<Class<?>> getClasses() {
    System.out.println("***** MyApplication getClasses start");
    final Set<Class<?>> classes = new HashSet<>();
    classes.add(Wss.class);
    System.out.println("***** MyApplicatiosn getClasses, classes size: " + classes.size());
    //new Throwable().printStackTrace();
    return classes;
  }
    
  public static final Logger logger = Logger.getLogger(MyApplication.class);
  
  public MyApplication() {
    System.out.println("*****regconstr MyApplication regular constructor");
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


