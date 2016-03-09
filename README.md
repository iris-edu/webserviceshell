# Web Service Shell (WSS) Developer Highlights

This document describes selected components of WSS and hints for implementing an endpoint.

- repository: ssh://dmscode/repos/webserviceshell
- 2016-03-09 - initial version

## Selected Components

Folder |  Description
--------------- | --------------------------
edu.iris.wss ``<<package>>`` |
 | ``Wss`` - Contains WSS builtin functions like version, wssversion, etc.
edu.iris.wss.endpoints ``<<package>>`` | Contains builtin WSS endpoint code.
 | ``CmdProcessor`` - creates and runs external process
 | ``ProxyResource`` - copies contents of URL reference to HTTP client
 | ``IncomingHeaders`` - for internal use - shows HTTP headers coming into WSS
edu.iris.wss.framework ``<<package>>`` | Code for internal operation of WSS
 | ``MyApplication`` - The "main" code for starting and configuring web application, starts logging, drives loading of configuration files, dynamically binds functionality together.
 | ``WssSingleton`` - The code for loading a singleton java class, if a singleton class is configured.
edu.iris.wss.provider ``<<package>>`` | Provides endpoint interface
 | ``IrisDynamicProvider`` - The driving code for calling and operating endpoint code. It is also the primary interface to the Jersey framework for handling each request. The method ``doIrisProcessing`` contains the WSS business logic for calling an endpoint and managing header and status returned to an HTTP client.
  | ``IrisProcessor`` - Public interface for an endpoint
  | ``IrisProcessingResult`` - An object containing status, output data declaration, and optionally HTTP header overrides that must be returned from IrisProcessor.getProcessingResults method.
com.Ostermiller.util ``<<package>>`` | Provides code for processing seed data in CmdProcessor when "writeMiniSeed" is performed. It is used to generate miniseed usage log information.
webapp.WEB-INF ``<<webapp>>`` | Provides interface to Java servlet application environment.
 | ``web.xml`` - Provides link for container to WSS code, controls security access to function wssstatus, enables setup for authentication, and link to AppContextListener, which closes JMS connection when needed.

## Implementing an endpoint
To create a WSS endpoint, create a Java class which extends edu.iris.wss.provider.IrisProcessor.

See edu.iris.wss.endpoints.CmdProcessor for a more complex example and ProxyResource for a more simple example.

``` java
public class CmdProcessor extends IrisProcessor {
```

- implement method
``` java
 public abstract IrisProcessingResult getProcessingResults(
               RequestInfo ri, String wssMediaType)
```
- An **IrisProcessingResult** object must be returned, depending on procession results, here are the constructors, more can be added as needed ...

  - **processError** - create error return with our without detailed message
  - **processStream** - simple successful string return to http client
  - **processString** - successful streaming object returned with mediaType and optionally updated http headers

#### Other public API
To create regular DMC - FDSN error handling and usage logging output (for usage log file, JMS, or RabbitMQ), use these methods from edu.iris.wss.framework.Util

- **Util.logAndThrowException** - makes standard FDSN HTTP error text message and logs to file, JMS or RabbitMQ
- **Util.logUsageMessage** - logs to file, JMS or RabbitMQ
