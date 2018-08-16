# WebServiceShell (WSS) Developer Notes

This document provides a developer overview of WSS components and key operational sequences.

- 2018-08-15 - latest updates
- 2018-07-17 - add sequence diagrams
- 2016-03-09 - initial version

## Introduction

WSS uses Java and Jersey to provide RESTful (GET, POST, and HEAD) web services middleware for streaming data. WSS does not accessed data directly, but leverages backend applications to request data and then return the resulting data to an HTTP client request.

WSS is designed to stream any kind of data, plus it has an optional capability to provided usage log information specifically related to seismological SEED (Standard for the Exchange of Earthquake Data) data.

## Package Overview

Folder |  Description
---- | ----
com.Ostermiller.util ``<<package>>`` | Code used to interpret miniseed data and generate summary information. This code is run in the CmdProcessor when a **format=mseed** request is made. It is used to generate miniseed usage log information.
edazdarevic.commons.net ``<<package>>`` | Code for white list capabilities with CIDR notation.
edu.iris.wss ``<<package>>`` |
<i></i>             | ``Wss`` - Contains WSS builtin functions to service request for version, wssversion, etc.
edu.iris.wss.endpoints ``<<package>>`` | This code provides the implementation for CmdProcessor and ProxyResource. This code is dynamically loaded based on configuration settings, it is not loaded by default and must be configured in service.cfg and param.cfg.
<i></i>         | ``CmdProcessor`` - Creates and runs external process i.e. "handlers"
<i></i>         | ``ProxyResource`` - Reads data from the source defined by property **proxyURL** and streams the data out with the media type specified in a **format** request parameter.
<i></i>         | ``IncomingHeaders`` - shows HTTP headers coming into WSS, it is not normally needed or configured.
edu.iris.wss.framework ``<<package>>`` | Provides core features of WSS
<i></i>         | ``AppConfigurator`` - Reads and stores service.cfg properties.
<i></i>         | ``AppContextListener`` - First code to execute when application is started. It configures log4j.
<i></i>         | ``MyApplication`` - Main code to execute when application is started. It creates WssSingleton and dynamically binds configured endpoints. Is specified in web.xml.
<i></i>         | ``ParamConfigurator`` - Reads and stores param.cfg properties.
<i></i>         | ``ParameterTranslator`` - Performs parameter existence and type checks for each request.
<i></i>         | ``RequestInfo`` - Stores application state information, it is needed for processing every request.
<i></i>         | ``ServiceShellException`` - Used for error handling and generating standard FDSN error messages.
<i></i>         | ``WssSingleton`` - It loads properties from cfg files and sets up logging.
<i></i>         | ``StatsKeeper`` - Stores runtime information which is used for the builtin getStatus endpoint.
edu.iris.wss.provider ``<<package>>`` | Provides endpoint interface
<i></i>         | ``IrisDynamicProvider`` - The code for receiving a request from the Jersey framework and calling application endpoint code. The method ``doIrisProcessing`` contains the WSS business logic for calling each configured endpoint. It implements the rules for managing the header and status information returned to an HTTP client.
<i></i>         | ``IrisProcessor`` - Public interface for an endpoint, endpoint code must extend this class and implement getProcessingResults.
<i></i>         | ``IrisProcessingResult`` - An object from this class defines the results of an endpoint's processing i.e. the results of calling IrisProcessor.getProcessingResults. The returned object contains HTTP status, a streaming data declaration, and optionally HTTP header overrides.
edu.iris.wss.utils ``<<package>>`` |
<i></i>         | ``LoggerUtils`` - Code for creating sending usage and wfstats logging.
<i></i>         | ``WebUtils`` - Code for processing request information.
webapp/WEB-INF ``<<webapp>>`` |
<i></i>         | ``web.xml`` - Web application configuration, defines MyApplication as the servlet entry point and specifies authentication configuration.

## Operating Sequences

When a war file is loaded into a running container, or the container itself is started, respective WSS code is called as defined by the Jersey framework. The following sequences, describe, at a high level, the two main operating sequences, startup, and handling requests.

When a WSS web application starts, it interacts with the Jersey framework in a sequence of operations to discover its name (i.e. context), find respective configuration files, and register endpoints. At the end of the startup sequence, static and dynamic endpoints must be registered, along with respective parameter information. Once the startup sequence finishes, Jersey passes each request to the appropriate endpoint along with parameter information to validate names and value types.

### WSS startup

The following **WSS Startup Sequence** diagram highlights key steps in the startup sequence. The code implementing **Startup Items 1,3, and 4 are subject to startup errors when new versions of Tomcat or Glassfish are introduced**.

![WSS_startup](WSS_startup_sequence.png)

#### Startup Item 1 - Initializing log4j

**AppContextListener** is a standalone class driven by the Tomcat/Jersey framework. It initializes log4j. Additionally, key startup information is written to stdout as logging initialization is performed, so checking catalina.out or Glassfish logs will help determine configuration problems. Setting up log4j depends on:
- the system property **wssConfigDir** being set in setenv.sh in Tomcat or in [Glassfish](../glassfish/README.md), with an admin command starting with `asadmin create-system-properties ...` respectively.
- the **context** path is determined by Tomcat war file name, or in Glassfish, with an admin command starting with `asadmin deploy --contextroot ...``
- the log4j properties file has the correct *service.base*-log4j.properties file name and that respective logger File names and paths are set correctly.

#### Startup Item 2 - Configuration

When the **MyApplication** object is created, it in turn, creates a **WssSingleton** object called **sw**. The sw object reads and stores the cfg files’ information. The sw object is also responsible for opening usage logging connections for either JMS or RabbitMQ logging. Configuration information is logged in the log4j logs and should be viewed when a new configuration is used.

#### Startup Item 3 - Registration

Once configuration information is processed, **MyApplication** registers the Wss.class, which contains static endpoints. Then, based on configuration information, dynamic endpoints are created and added also.

#### Startup Item 4 - Finishing the Startup Sequences

The Jersey framework calls `onStartup` on the **MyContainerLifecycleListener** object near the end of the startup sequence. The **WssSingleton** object **sw** is registered as a context object, making it available for request handling.

### WSS Request

The **WSS Request Sequence** diagram highlights key steps in the response to a client request. After the web application has successfully started, Tomcat (or Glassfish) can respond to client request with configured endpoints.

For dynamically defined classes, Jersey calls an **IrisDynamicProvider** object, which in turn calls the respective, **IrisProcessor** object. IrisDynamicProvider provides most of the core behavior of WSS. One item of note, different IrisProcessor endpoints may have slightly different behavior, depending on respective implementations. For instance **CmdProcessor** uses a timer to stop halted requests, while ProxyResource does not.

![WSS_request](WSS_request_sequence.png)

#### Request Item 1 - Context information

The Jersey framework delivers the WssSingleton object by context. Each endpoint uses this object to get respective parameter names and types, media types, etc. as defined by configuration.

#### Request Item 2 - Setup for Jersey call

Processor specific error checking and the definition of an output entity must be done here. The entity returned in **IrisProcessingResult** may contain simple objects like Strings, but if a StreamingOutput object is declared, it will not be executed until later in the sequence. Based on HTTP protocol interaction, the HTTP return code and any header information must be defined here, even though data may not be delivered at this time.

#### Request Item 3 - Streaming data

**IrisDynamicProvider** must return control and a Response object which contains the HTTP return code and declared output entity. Jersey returns the HTTP information to the client.

#### Request Item 4 - Streaming data

If a StreamingOutput entity is declared, Jersey calls the write method of the respective object, i.e. executes the code within an IrisProcessor. At this point, the client can only read the streaming data until the stream is closed or an I/O error occurs.

## General WSS Support Considerations

- The entry point classes, **AppContextListener** and **MyApplication** are explicitly maintained in the web.xml for ease of support and avoiding possible differences in startup sequences between Tomcat and Glassfish.
- Glassfish setup and operation information are in the [docs/glassfish](../glassfish/README.md) folder.
- Changes to **IrisDynamicProvider** will affect all dynamic endpoints, while changes to any IrisProcessor only affects that endpoint.
- In the current WSS design, endpoint configuration properties only apply to an endpoint if that endpoint uses a respective property. One exception is property `allowedIPs`, which applies to each endpoint respectively.
- When mis-configurations occurs, the WSS implementation substitutes "dummy" content in to prevent hard-to-trace null pointer exceptions at run time. When the string "dummy" is observed embedded in log messages or object names, almost certainly something is wrong with the specification of:
  -  **wssConfigDir** - check catalina.out
  - inaccessible paths - i.e. no read access, mis-named path
  - incorrectly named cfg files - check name against war file name
  - mis-named configuration elements - check respective log files, check log4j.properties for location of log files

## Creating a Release

When all the updates for a given version are committed, and there are no outstanding changes in the code tree, verify that unit test pass.

```
mvn clean package
```

**Set the desired version number** in files, these strings should be identical:
- **version** element in **pom.xml**
- **wssVersion** value in src/main/java/edu/iris/wss/framework/**AppConfigurator.java**

**Add, commit, and push** the version updated with reference to a respective issue and comment.
```
git add pom.xml
git add src/main/java/edu/iris/wss/framework/AppConfigurator.java
git commit -m "issue #1234, yada yada"
git push
```

**Tag** with the same version string (e.g. x.y.z) placed in the pom and AppConfigurator files. It may be helpful to precede the comment with "tag" for future reference.
```
git tag -a vx.y.z -m "tag - possibly same as last commit i.e. issue #1234, yada yada"
git push origin vx.y.z
```

**Deploy internally** to Nexus repo, note that messages starting with "Uploaded ..." are occurring.
```
mvn clean Deploy
```
**Deploy to github** as needed for public release, copy jar and war file to releases area.

## Implementing an endpoint

To create a WSS endpoint, create a Java class which extends edu.iris.wss.provider.IrisProcessor. Write the desired code for getProcessingResults method and IrisProcessingResult object. When possible, the IrisProcessingResult should also be used to return error results.

The following code creates a JSON object, "jo", which is then returned to a client when Jersey runs the write method in the StreamingOutput object. See edu.iris.wss.endpoints.CmdProcessor for a complex example and ProxyResource for a simple example.

- extend IrisProcessor

``` java
public class MyNewEndpoint extends IrisProcessor {
```

- implement method IrisProcessingResult

``` java
 public IrisProcessingResult getProcessingResults(
               RequestInfo ri, String wssMediaType) {

      JSONObject jo = new JSONObject();
      jo.put("testproperty", "test values to be written out to the client");

      String jsonStr = jo.toJSONString();

      StreamingOutput so = new StreamingOutput() {
          @Override
          public void write(OutputStream output) {
              try {
                  output.write(jsonStr.getBytes());
              } catch (IOException ex) {
                  throw new RuntimeException(THIS_CLASS_NAME + MediaType.MULTIPART_FORM_DATA
                        +" test code"
                        + " failed to do streaming output, ex: " + ex);
              }
          }
      };

      // create status OK object
      IrisProcessingResult ipr = IrisProcessingResult.processStream(so, MediaType.APPLICATION_JSON);

      return ipr;
 }
```

An **IrisProcessingResult** object must be returned, depending on procession results, here are the constructors.
  - **processError** - create error return with our without detailed message
  - **processStream** - simple successful string return to http client
  - **processString** - successful streaming object returned with mediaType and optionally updated http headers

When an exception is needed,
- **Util.logAndThrowException** - makes standard FDSN HTTP error text message and logs to file, JMS or RabbitMQ

For usage logging output (for log4j, JMS, or RabbitMQ), use methods from edu.iris.wss.framework.Util,
- **Util.logUsageMessage** - logs to file, JMS or RabbitMQ
