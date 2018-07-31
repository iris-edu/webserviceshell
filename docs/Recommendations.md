# Recommendations

### Web Service Shell

The WSS was written towards the goal of removing the barriers to
creating a modern web service (under certain constraints). It allows
command line, \*nix-based, programs to be *wrapped* in a pre-written
Java servlet. Moreover, the WSS can be configured via simple properties
files to *wrap* any command line programming conforming to its
requirements.

The WSS provides basic parameter validation as well as HTTP
authentication (via the servlet container). It also provides usage
logging, standard logging, and various other mundane functions required
of an HTTP server.

The WSS can also be configured to use a Java class rather than a command
line program to fulfill the data retrieval. More details about using
Java classes with the WSS can be found in the [release-specific
documentation](https://seiscode.iris.washington.edu/projects/webserviceshell/wiki/)

Additionally, more information about FDSN web services can be found
here: [FDSN WS
Specifications](http://www.fdsn.org/webservices/FDSN-WS-Specifications-1.1.pdf)

### Servlet Container

We recommend using Apache Tomcat, a standard servlet container in use
world-wide. Version 8.0.33 is the latest as of the writing of this
document. It is relatively simple to use, drop-dead trivial to install
and although it is not bug free, it is simple to maintain and operate.
Authentication setup is non-trivial, but this document will walk through
the setup step by step. Other servlet containers can also be used,
including Oracle's Glassfish (on which the WSS has been tested) and
others. Authentication methods will vary between these different
containers. We will only address HTTP authentication for Tomcat below.

### HTTP Authentication Methods (if authentication will be needed)

Use of HTTP Digest authentication is recommended. HTTP Basic
authentication can be used, but user credentials are passed
(essentially) as clear text in the HTTP header. HTTP Digest
authentication is more complicated, but is handled by all modern HTTP
servlet (or application) containers. Tomcat supports both methods fully.

When using HTTP digest authentication, the server must have access to
the user credentials hashed (or digested) locally on the server. There
are a multitude of hashing algorithms in use, but many have limited
support from various HTTP clients. The hashing algorithm we recommend is
MD5 as it is supported by all modern web clients and servers. The
hashing algorithm chosen comes into play when creating digested user
credential entries that will be stored on the server.

### Storage *Realms* (if authentication will be needed)

The server needs to have a storage location for user credentials. Most
modern servlet containers support a variety of *realms* for doing so.
The container can be configured to use either a file-based system, a
database driven system or a handful of other potential systems, e.g.
LDAP, and access schemes, e.g. JNDI, that are outside the scope of this
document.

The simplest realm to use and configure is the *User Database Realm*, in
which user credentials are stored, line by line, in a file that is
required to be present in a particular location of every servlet
container. Clearly, if multiple servers are in use, keeping these
hand-managed files synchronized can be burdensome.

In the *JDBC Database Realm*, the users' credentials are stored in
tables inside the database. This is relatively simple to configure from
the either the Tomcat side or the database side. Essentially, only two
tables are required, but can be reduced to one via the use of *Views* in
the database.

We recommend using the *UserDatabase Realm* if you plan on running your
services in a single container and the *JDBC Database Realm* if you are
using multiple containers **and** have access to a database, e.g. MySQL,
PostgreSQL, Oracle.
