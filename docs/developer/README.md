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

When a WSS web application starts, it interacts with Jersey framework in a sequence of operations to get and set information. Once the startup sequence finishes, Jersey is able to pass each request to the appropriate endpoint along with the information that WSS needs to handle each request.

The following **WSS Startup Sequence** diagram highlights key elements of the startup sequence.

![example seq](WSS_startup_sequence.png)

#### Startup Item 1 - Initializing log4j

**AppContextListener** is a standalone class driven by the Tomcat/Jersey framework. It initializes log4j.

It depends on
- the system property **wssConfigDir** being set in setenv.sh in Tomcat or in Glassfish, with an admin command starting with `asadmin create-system-properties ...` respectively.
- the **context** path is determined by Tomcat war file name, or in Glassfish, with an admin command starting with `asadmin deploy --contextroot ...``

#### Startup Item 2 - Configuration

When the **MyApplication** object is created, it in turn, creates a **WssSingleton** object called **sw**. The sw object reads and stores the cfg files’ information. The sw object is also responsible for opening logging connections for either JMS or RabbitMQ logging.

#### Startup Item 3 - Registration

Once configuration information is processed, **MyApplication** registers the WSS class which contains static endpoints. Based on configuration information, dynamic endpoints are created and added also.

#### Startup Item 4 - Finishing the Startup Sequences

The Jersey framework calls `onStartup` on the **MyContainerLifecycleListener** object near the end of the startup sequence. The **WssSingleton** objec **sw** is registered as a context object, making it available for request handling.





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
- An **IrisProcessingResult** object must be returned, depending on procession results, here are the constructors, more can be added as needed ...

  - **processError** - create error return with our without detailed message
  - **processStream** - simple successful string return to http client
  - **processString** - successful streaming object returned with mediaType and optionally updated http headers

some image processing

and a graffle

![example seq](WSS_startup_sequence.png)


#### Other public API
To create regular DMC - FDSN error handling and usage logging output (for usage log file, JMS, or RabbitMQ), use these methods from edu.iris.wss.framework.Util

- **Util.logAndThrowException** - makes standard FDSN HTTP error text message and logs to file, JMS or RabbitMQ
- **Util.logUsageMessage** - logs to file, JMS or RabbitMQ
