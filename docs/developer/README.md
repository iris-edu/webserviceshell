# Web Service Shell (WSS) Developer Notes

This document describes selected components of WSS and hints for implementing an endpoint.

- repository: ssh://dmscode/repos/webserviceshell
- 2016-03-09 - initial version
- 2018-07-17 - updates

## Key Components

Folder |  Description
---- | ----
com.Ostermiller.util ``<<package>>`` | Code used to interpret miniseed data and generate summary information. This code is run in the CmdProcessor when a **format=mseed** request is made. It is used to generate miniseed usage log information.
edazdarevic.commons.net ``<<package>>`` | Code for white list capabilities.
edu.iris.wss ``<<package>>`` |
<i></i>             | ``Wss`` - Contains WSS builtin functions like version, wssversion, etc.
edu.iris.wss.endpoints ``<<package>>`` | Builtin WSS endpoint code and dependencies. These code is not runnable until configured in service.cfg and param.cfg.
<i></i>         | ``CmdProcessor`` - Creates and runs external process i.e. "handlers"
<i></i>         | ``ProxyResource`` - Reads data from the source defined by property **proxyURL** and streams the data out with the media type specified in a **format** request parameter.
<i></i>         | ``IncomingHeaders`` - shows HTTP headers coming into WSS, it is not normally needed or configured.
edu.iris.wss.framework ``<<package>>`` | Provides core features of WSS
<i></i>         | ``AppConfigurator`` - Reads and stores service.cfg properties.
<i></i>         | ``MyApplication`` - First code to execute when application is started. It creates WssSingleton and dynamically binds configured endpoints.
<i></i>         | ``ParamConfigurator`` - Reads and stores param.cfg properties.
<i></i>         | ``ParameterTranslator`` - Performs parameter existance and type checks for each request.
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

When a WSS web application starts, it interacts with Jersey framework in a sequence of operations to get and set information. The interaction with Jersey must accomplish the registration of the desired functionality and make available application state information needed when request are handled. Once the startup sequence finishes, Jersey is able to pass each request to the appropriate endpoint along with the information that WSS needs to handle each request.

#### WSS startup

The following **WSS Startup Sequence** diagram highlights key steps in the startup sequence. The code implementing Startup Items 1,3, and 4 are subject to needing changed when new versions of Tomcat or Glassfish are introduced.

![WSS_startup](WSS_startup_sequence.png)

#### Startup Item 1 - Initializing log4j

**AppContextListener** is a standalone class driven by the Tomcat/Jersey framework. It initializes log4j. Key startup information is written to stdout as logging initialization is performed, so checking catalina.out or Glassfish logs will help determine configuration problems. Setting up log4j depends on:
- the system property **wssConfigDir** being set in setenv.sh in Tomcat or in Glassfish, with an admin command starting with `asadmin create-system-properties ...` respectively.
- the **context** path is determined by Tomcat war file name, or in Glassfish, with an admin command starting with `asadmin deploy --contextroot ...``

#### Startup Item 2 - Configuration

When the **MyApplication** object is created, it in turn, creates a **WssSingleton** object called **sw**. The sw object reads and stores the cfg files’ information. The sw object is also responsible for opening usage logging connections for either JMS or RabbitMQ logging. Configuration information is logged in the log4j logs and should be viewed when a new configuration is used.

#### Startup Item 3 - Registration

Once configuration information is processed, **MyApplication** registers the WSS class which contains static endpoints. Based on configuration information, dynamic endpoints are created and added also.

#### Startup Item 4 - Finishing the Startup Sequences

The Jersey framework calls `onStartup` on the **MyContainerLifecycleListener** object near the end of the startup sequence. The **WssSingleton** object **sw** is registered as a context object, making it available for request handling.


The **WSS Request Sequence** diagram highlights key steps in the response to a client request.

#### WSS Request

After the web application has successfully started, Tomcat (or Glassfish) can respond to client request. For dynamically defined classes, the entry point from Jersey is to call an **IrisDynamicProvider** object, which in turn calls one object which has implemented the **IrisProcessor** interface. IrisDynamicProvider, and the support objects it uses provides most of the core behavior of WSS. As the first implementation of IrisProcessor, the **CmdProcessor** has some behaviors that are unique to it as compared to other IrisProcessors. For example, the timeout behavior is unique to CmdProcessor and other IrisProcessors do not do this unless explicitly coded to do so.

![WSS_request](WSS_request_sequence.png)

#### Request Item 1 - Context information

Each request can take advantage of the WssSingleton object, sw, established at startup. For example all the parameters and their types, media types, etc. as defined in configuration, per endpoint, are available to the IrisProcessor.

#### Request Item 2 - Setup for Jersey call

Processor specific error checking, data structure setup, etc. should be done here. The entity returned in **IrisProcessingResult** may contain simple objects, but if a StreamingOutput object is used, it will not be executed until later. Do the definition of the HTTP protocol, the HTTP return code and any header information must be defined here, even though data is not being delivered

#### Request Item 3 - Streaming data

**IrisDynamicProvider** must return control to the Jersey frame work with the appropriate Response object. Jersey returns the HTTP information to the client, and if a StreamingOutput entity is defined, calls the write method of the respective object, i.e. code within an IrisProcessor. At this point, the client can only read the streaming data until the stream is closed or an I/O error occurs.

## General WSS Support Considerations

- The entry point classes, **AppContextListener** and **MyApplication** are explicitly maintained in the web.xml for ease of support and avoiding possible differences in startup sequences between Tomcat and Glassfish.
- Glassfish setup and operation information are in the docs/glassfish folder.
- Changes to **** will affect all dynamic endpoints.



## Implementing an endpoint
To create a WSS endpoint, create a Java class which extends edu.iris.wss.provider.IrisProcessor.

See edu.iris.wss.endpoints.CmdProcessor for a more complex example and ProxyResource for a more simple example.

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
- An **IrisProcessingResult** object must be returned, depending on procession results, here are the constructors.

  - **processError** - create error return with our without detailed message
  - **processStream** - simple successful string return to http client
  - **processString** - successful streaming object returned with mediaType and optionally updated http headers

#### Other public API
To create regular DMC - FDSN error handling and usage logging output (for usage log file, JMS, or RabbitMQ), use these methods from edu.iris.wss.framework.Util

- **Util.logAndThrowException** - makes standard FDSN HTTP error text message and logs to file, JMS or RabbitMQ
- **Util.logUsageMessage** - logs to file, JMS or RabbitMQ
