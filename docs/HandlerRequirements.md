# Working with Handler Programs

### Requirements of a *Handler* Program

There are few requirements on a handler program. Essentially a handler
should:

- return data or error message within the time allotted by the WSS
    (configured via the endpoint `handlerTimeout` parameter),
- write data to `stdout` and write error messages to `stderr`
- read arguments from command line, additionally, if WSS is configured
    for POST, then read POST data from stdin
- exit with respective exit status codes described below.

### Concept of Operation

WSS manages command-line programs in two distinct phases:

1. Phase 1

  In this phase, WSS uses the CmdProcessor to create a process for the
handler program, then invoke the program with all the its attendant
arguments. The handler:
  - may write HTTP header information to `stdout`
    - The CmdProcessor saves header information.
  - is expected to write data to `stdout`
    - The CmdProcessor considers this a success condition and enters Phase 2
  by returning 'OK HTTP 200' to the client, and returning control
to the Tomcat framework.
  - exit with, or without, a \*NIX exit status code
    - The CmdProcessor returns a respective error code and error
message to the client.
  - do nothing
    - The CmdProcessor will timeout the handler process, kill the
handler, and return a respective error message to the client

2. Phase 2

 Once a handler starts writing data to stdout, the Tomcat framework
starts streaming the data to a client.
  - If the handler exits, log messages are written out and the process
ends.
  - If the handler does not exit, but stops writing data for a time
that exceeds the timeout period, the handler process is killed, log
messages are written out, and WSS attempts to write this 256 byte error
string to the client:
```
    000000##ERROR#######ERROR##STREAMERROR##STREAMERROR#STREAMERROR\n
    This data stream was interrupted and is likely incomplete.     \n
    #STREAMERROR##STREAMERROR##STREAMERROR##STREAMERROR#STREAMERROR\n
    #STREAMERROR##STREAMERROR##STREAMERROR##STREAMERROR#STREAMERROR\n
```
  Because the HTTP protocol does not allow a return code to the client at
this point (it had to be sent prior to data streaming) it is suggested
that clients check for this error string to help detect interrupted data
retrievals.

### Query Parameters and Command-line Arguments

WSS invokes a handler and provides command line arguments to the handler
corresponding to respective query (parameter name, value) pairs. Only
parameters configured in `param.cfg` are allowed. Any other query
parameters will cause an error response from WSS. Each query (parameter
name, value) pair e.g. `&quality=B` will be translated into a command
line form of `--quality B`.

The double hyphen '--' command line standard is always used.

Query parameter syntax is **not** translated, i.e. each parameter on the
URL is translated into a respective command line form, e.g.
`&network=IU` on the URL becomes `--network IU` on the command line.

**Only** parameters configured via `param.cfg` are passed to the
handler, but WSS may add the following parameters

1.  `--username USER` is added by WSS when a user has been successfully
    authenticated.
2.  `--STDIN` is added by WSS if a client request uses *HTTP POST*
    rather than *HTTP GET*. A handler should use this parameter to
    indicate it needs to read `stdin` to get the post data.

For the URL query parameters, WSS will accept by default:

1.  `&nodata=404 or &nodata=204`, setting 404 will instruct WSS to
    explicitly return an error message and HTTP 404 Not Found when there
    is no data, rather than the HTTP default of 204 No Content and no
    message to the client.
2.  `&format=formatType` may be used to select a formatType, however,
    only BINARY is available unless the formatTypes parameter is defined
    for additional types.

### Exit Status Codes

WSS translates the following exit status codes from a handler into the
respective HTTP Status for the WSS client. Additionally for errors, a
handler should write a short, user oriented error message to `stderr`.

Exit Status | HTTP STATUS | Description
---- | ---- |----
0 | 200 | Successfully processed request, data returned via stdout
1 | 500 | General error. An error description may be provided on stderr
2 | 204 | No data. Request was successful but results in no data
3 | 400 | Invalid or unsupported argument/parameter
4 | 413 | Too much data requested

Note: For exit status code 2, query parameter `nodata` can be used to
have WSS return a 404 to the WSS client rather than 204.

### Timing out and Network Interruptions

Timeouts can occur at any point after the handler program is invoked.
WSS will terminate the handler program if no data or exit status code is
received within the configured timeout period (i.e. handlerTimeout).
Once data flow starts, WSS returns an HTTP 200 OK status to the client,
but the client will continue to receive data as long as the connection
is maintained. Because HTTP protocol requires an HTTP status to be
returned to a client before data starts downloading, it is possible for
the the connection to drop or the handler to fail after the HTTP 200 OK
is returned to the client, but before all the data is sent. Therefore,
the client should check that all the expected data was received, see the
"STREAMERROR" message described above in the Phase 2 operations above.

#### Handling Network Interruptions

It is somewhat common for the network connection from the HTTP client to
the WSS to be interrupted. This can occur due to the network connection
being dropped, or the client closing the connection while a transfer is
ongoing. One common example is if the client is a browser, the user
requests a SEED file, and then dismisses the File Save dialog via
'Cancel'.

A handler program should gracefully handle a network disconnection,
clean up and exit. These disconnections may be detected via an IO
Exception when the `stdout` connection to which the handler had been
writing goes away. Defensive programming is always preferred, but, in
general, nothing untoward happens with regard to WSS if the handler
behaves badly. If the handler stops sending data for a long enough time,
WSS will terminate it. If the interruption is upstream from WSS, i.e the
client appears to disconnect, WSS will attempt to terminate its handler.
This prevents zombie handler processes.

### Error Text

If the handler program writes text to `stderr` during Phase 1 **and** an
error code is received by the WSS, WSS will include the error text in
the error response sent to the client.

### Output Formats

Each WSS endpoint should be configured for all of the media types that a
handler program produces. WSS uses the `format` parameter to

- a) indicate to the HTTP client what media type the data is represented
    in [i.e. HTTP header item `Content-Type`] and
- b) to suggested a file name [i.e. HTTP header item
    `Content-Disposition`] if the data is downloaded to a file.

The data representation (i.e. media type) can be specified for a request
by using the `&format=formatType` query parameter. The possible options
should be defined with the `formatTypes` parameter in the `service.cfg`
file, a typical list might be:

formatType  |  Media type
---- | ---- |----
xml         | application/xml
mseed       | application/vnd.fdsn.mseed
text        | text/plain
texttree    | text/plain
json        | application/json

By default without configuration, WSS defines one formatType, "binary"
corresponding to media type "application/octet-stream".

Note:

- The first item on the list is the default format used when `&format`
    is not specified.
- A conflict between the formatType specified (i.e. returned media
    type), and the handler's streamed output will not cause any harm,
    but will confuse clients.

### Environment Variables Set by the WSS-

The WSS CmdProcessor sets certain environment variables when starting a
handler program. This allows the handler program to know about the HTTP
request and the version of the WSS used with any eye towards logging;
e.g. if a handler program wished to perform its own 'per request'
logging, this information would be vital. Below is a table with the
environment variables.

Variable name         | Value
---- | ---- |----
REQUESTURL            | The URL of the incoming request
USERAGENT             | User agent string supplied in the HTTP header for this request
IPADDRESS             | IP Address of the request's client
APPNAME               | Application name supplied via the `appName` parameter from the service configuration
VERSION               | Application version supplied via the `version` parameter from the service configuration
CLIENTNAME            | not currently in use
HOSTNAME              | host name of WSS server
AUTHENTICATEDUSERNAME | Authenticated user name, only present if a user was authenticated
