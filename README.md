# Web Service Shell (WSS) Developer Notes

This document describes selected components of WSS and hints for implementing an endpoint.

- repository: ssh://dmscode/repos/webserviceshell
- 2016-03-09 - initial version
- 2018-07-17 - updates

## Key Components

Folder |  Description
--------------- | --------------------------
com.Ostermiller.util ``<<package>>`` | Code for processing seed data in CmdProcessor when "writeMiniSeed" code runs, i.e. from a format=mseed request. It is used to generate miniseed usage log information.
edazdarevic.commons.net ``<<package>>`` | Code for white list capabilities.
edu.iris.wss ``<<package>>`` |
 | ``Wss`` - Contains WSS builtin functions like version, wssversion, etc.
 | ``Info1`` - Example code, see its comments.
edu.iris.wss.endpoints ``<<package>>`` | Builtin WSS endpoint code and dependencies. These code is not runnable until configured in service.cfg and param.cfg.
 | ``CmdProcessor`` - Creates and runs external process i.e. "handlers"
 | ``ProxyResource`` - Reads data from the source defined by property **proxyURL** and streams the data out with the media type specified in a **format** request parameter.
 | ``IncomingHeaders`` - shows HTTP headers coming into WSS, it is not normally needed or configured.
edu.iris.wss.framework ``<<package>>`` | Provides core features of WSS
 | ``AppConfigurator`` - Reads and stores service.cfg properties.
 | ``MyApplication`` - First code to execute when application is started. It creates WssSingleton and dynamically binds configured endpoints.
 | ``ParamConfigurator`` - Reads and stores param.cfg properties.
 | ``ParameterTranslator`` - Quality checks parameters for each request.
 | ``RequestInfo`` - Stores application state information, it is needed for processing every request.
 | ``ServiceShellException`` - Used for error handling and generating standard FDSN error messages.
 | ``WssSingleton`` - It loads properties from cfg files and sets up logging.
 | ``StatsKeeper`` - Stores runtime information which is used for the builtin getStatus endpoint.
edu.iris.wss.provider ``<<package>>`` | Provides endpoint interface
 | ``IrisDynamicProvider`` - The code for receiving a request from the Jersey framework and calling application endpoint code. The method ``doIrisProcessing`` contains the WSS business logic for calling each configured endpoint. It implements the rules for managing the header and status information returned to an HTTP client.
  | ``IrisProcessor`` - Public interface for an endpoint, endpoint code must extend this class and implement getProcessingResults.
  | ``IrisProcessingResult`` - An object from this class defines the results of an endpoint's processing i.e. the results of calling IrisProcessor.getProcessingResults. The returned object contains HTTP status, a streaming data declaration, and optionally HTTP header overrides.
webapp/WEB-INF ``<<webapp>>`` |
 | ``web.xml`` - Web application configuration, defines MyApplication as the servlet entry point and specifies authentication configuration.

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

#### Other public API
To create regular DMC - FDSN error handling and usage logging output (for usage log file, JMS, or RabbitMQ), use these methods from edu.iris.wss.framework.Util

- **Util.logAndThrowException** - makes standard FDSN HTTP error text message and logs to file, JMS or RabbitMQ
- **Util.logUsageMessage** - logs to file, JMS or RabbitMQ
