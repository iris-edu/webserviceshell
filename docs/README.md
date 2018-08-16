# WebServiceShell (WSS)


## Documentation
- [User](WebServiceShell-2.4.x.md)
- [Example Service](ExampleService.md)
- [Working with Handler Programs](HandlerRequirements.md)
- [Setup Recommendations](Recommendations.md)

### Introduction

The Web Service Shell (WSS) is a web service that can be configured via
simple properties files to utilize external resources (either
command-line programs or Java classes) to fulfill web service requests.

### Goals

WSS was written with the goal of removing the barriers to creating a
modern web service (under certain constraints) for delivering data. It allows
command line, \*nix-based, programs to be executed from a pre-written
Java servlet. Moreover, WSS can be configured via properties
files to execute any command line program (or script) conforming to its
requirements.

WSS provides basic request parameter validation as well as HTTP
authentication (via the servlet container). It also provides usage
logging, operational logging, and various other mundane functions required
of an HTTP server.

The WSS can also be configured to use a Java class rather than a command
line program to fulfill the data retrieval. More details about using
Java classes with the WSS can be found in the [developer notes](docs/README.md)
