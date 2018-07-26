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

import edu.iris.wss.Wss;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.MediaType;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

// using web.xml and @ApplicationPath("/") for now, this annotation
// seems to be required to get glassfish to manage
@ApplicationPath("/")
public class MyApplication extends ResourceConfig {

  public static final Logger logger = Logger.getLogger(MyApplication.class);
  public static final String CLASS_NAME = MyApplication.class.getSimpleName();

  public static final String WSS_SINGLETON_KEYWORD = "wss_singleton_lookup";

  private WssSingleton sw;

  public void SetupWSS(String configBase) throws Exception {

    // get configuration information next
    sw = new WssSingleton();
    Map<String, edu.iris.wss.framework.WssSingleton> swprop = new HashMap();
    swprop.put(WSS_SINGLETON_KEYWORD, sw);
    sw.configure(configBase);

    // store reference to WssSinglton so that it can be bound
    // for injection with each request and its reference to rabbitmq can be
    // used at shutdown time
    setProperties(swprop);

    MyContainerLifecycleListener mCLL = new MyContainerLifecycleListener();
    this.registerInstances(mCLL);

    // add in classes which have static endpoints defined with annotations
    register(edu.iris.wss.Wss.class);

    // register Multipart for use in IrisDynamicProvider
    packages("org.glassfish.jersey.examples.multipart");
    register(MultiPartFeature.class);

    // add dynamic endpoints as defined in -service.cfg file
    Set<String> epNames = sw.appConfig.getEndpoints();
    for (String epName : epNames) {
        if (Wss.STATIC_ENDPOINTS.contains(epName)) {
            // don't try to make a static endpoint dynamic
            continue;
        }

        // Update here is a new type of processing is added
        String methodName = "doUndefinedMethod";
        if (sw.appConfig.getIrisEndpointClass(epName) instanceof
              edu.iris.wss.provider.IrisProcessor) {
            methodName = "doIrisProcessing";
        }

        Collection<MediaType> mediaTypes = sw.appConfig.getMediaTypes(epName);

        // Note: HEAD seems to be allowed by default
        addEndpoint(epName, edu.iris.wss.provider.IrisDynamicProvider.class,
              methodName, "GET", mediaTypes);

        if (sw.appConfig.isPostEnabled(epName)) {
            addEndpoint(epName, edu.iris.wss.provider.IrisDynamicProvider.class,
                  methodName, "POST", mediaTypes);

        }
    }
  }

  public MyApplication() {

    String configBase = AppContextListener.globalConfigBase;

    try {
        SetupWSS(configBase);
    } catch(Exception ex) {
        String msg = "--------------- ************** " + CLASS_NAME
              + " ERROR setting up endpoints, configBase: " + configBase
              + " exception: " + ex;
        System.out.println(msg);
        logger.error(msg);
    }
  }

  /**
   * This method encapsulates key details for dynamically anding an
   * endpoint.
   *
   * @param epName - name of last part of URI path
   * @param epClass - the class containing the method to respond to a request
   * @param epMethodName - the method that handles this request
   * @param httpMethod - usually GET or POST
   */
  private void addEndpoint(String epName, Class epClass, String epMethodName,
        String httpMethod, Collection<MediaType> mediaTypes) {
    final Resource.Builder resourceBuilder = Resource.builder();
    resourceBuilder.path(epName);

    final ResourceMethod.Builder rmBuilder = resourceBuilder.addMethod(httpMethod);
    try {
        rmBuilder.produces(mediaTypes)
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
          + "  httpMethod: " + httpMethod + "  epMethodName: " + epMethodName
          + "  mediaTypes: " + mediaTypes;
    System.out.println(msg);
    logger.info(msg);
  }
}

