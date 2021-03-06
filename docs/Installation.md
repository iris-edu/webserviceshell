# Installation Instructions

These instructions cover the installation of Tomcat and the WebServiceShell(WSS)
application for running a data delivery web service. The conventions shown here
follow the [FDSN Web Service](http://www.fdsn.org/webservices/FDSN-WS-Specifications-1.1.pdf),
however, WSS can be adapted to most data delivery needs. Each service requires
that a "handler" is available to process the request. More information about
developing and using handlers can be found in [**handler requirements**](HandlerRequirements.md) and [**handler example and service configuration**](ExampleService.md).

These instructions are for Unix-like operating systems with a working
Java installation (1.8 is the current version). These instructions use
the term *WSSHOME* to refer to your installation folder. Please replace
*WSSHOME* with your path name where you are installing Tomcat.

This example uses fdsnws-dataselect for a specific example of a web
service deployed using WSS, but the installation instructions can apply
to any web service.

1. Create an OS user account, for example 'tomcat', to run the
web application container. This is highly recommended for long-term,
production environments.

2. Download an Apache Tomcat installer file and save it in *WSSHOME*:
See the [Apache Tomcat Downloads](http://tomcat.apache.org/download-80.cgi)
page for the latest version, the version 8.0.33 is used for this document.

3. Download the latest WebServiceShell WAR file (webserviceshell-VERSION.war)
and save in *WSSHOME*. See the
[releases page](https://github.com/iris-edu/webserviceshell/releases)
page for the latest version.

4. Untar/unpack Tomcat in installation directory:
    ``` bash
    tar -xzf apache-tomcat-8.0.33.tar.gz
    ```
    It should not be necessary to follow any further installation
    instructions for Tomcat, it should be able to run from where is was
    untarred/unpacked.

5. Create a link for the version-ed Tomcat directory:
    ``` bash
    ln -s apache-tomcat-8.0.33 tomcat
    ```

6. Create and edit **WSSHOME/tomcat/bin/setenv.sh** to contain the
following line.
    ``` bash
    JAVA_OPTS="-Xmx512m -DwssConfigDir='/WSSHOME/config'"
    ```
    These JAVA_OPTS
      - increases maximum memory to 512 MB (adjust as needed) and
      - defines the folder "'/WSSHOME/config" as the location for WSS
      configuration files.


7. Add an admin user to the Tomcat password file for the manager
application. Edit **/WSSHOME/tomcat/conf/tomcat-users.xml** and add
the <user> line within the <tomcat-users> tag:
    ``` XML
    <tomcat-users>
      ...
      <user username="admin" password="password" roles="tomcat,manager-gui"/>
    </tomcat-users>
    ```

8. Configure the manager webapp (included with Tomcat) to use the
password file for authentication. Edit
**/WSSHOME/tomcat/webapps/manager/META-INF/context.xml** and add the
<Realm> section below within the <Context> tag:
    ``` XML
    <Context>
      ...
      <Realm className="org.apache.catalina.realm.LockOutRealm">
         <!-- This Realm uses the UserDatabase configured in the global JNDI
              resources under the key "UserDatabase".  Any edits
              that are performed against this UserDatabase are immediately
              available for use by the Realm.  -->
         <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
                resourceName="UserDatabase"/>
      </Realm>
    </Context>
   ```

9. Copy, with a rename, the downloaded Web Service Shell WAR file
(version 2.2.2 in this example) into Tomcat deployment directory (e.g.,
`tomcat/webapps`). The new name should reflect the desired service name,
in this case, the desired service name is to be fdsnws/dataselect/1, so,
inserting '#' where '/' is desired in the service name, the new war
file name is "fdsnws#dataselect#1.war":
    ``` bash
    cp webserviceshell_2.2.2.war WSSHOME/tomcat/webapps/fdsnws#dataselect#1.war
    ```
    In this example the web application will be deployed at *http://hostname:port/fdsnws/dataselect/1/*

10. Create a directory to store service configuration files:
    ``` bash
    mkdir /WSSHOME/config
    ```

11. Download appropriate template WSS configuration files for each
service you plan to implement. Download from [**Support files for FDSN
services**](WebServiceShell-2.4.x.md#servicecfg) and copy to
the **/WSSHOME/config** folder.

    Rename the files, the leading part of the configuration file name must
    match the name of the war file with "." being inserted in place of '#', in
    this example, the files should be:
    - Service configuration: **fdsnws.dataselect.1-service.cfg**
    - Client interface parameter definitions: **fsnws.dataselect.1-param.cfg**
    - Logging configuration: **fdsnws.dataselect.1-log4j.properties**


12. Edit the Service configuration file (e.g. fdsnws.dataselect.1-service.cfg):
    1. Change properties **appName** and **version** as needed for the
    intended name and version
    2. Change the **rootServiceDoc** to a URL (can be a file or web page) that
    contains HTML describing this service.
    3. Change the **loggingMethod** to LOG4J unless JMS or RabbitMQ is to
    be used for usage logging.
    4. Set **query.handlerProgram** to the path of the program that will  
    handle requests and return data in expected format.
    5. Set **query.handlerWorkingDirectory** to a directory from which to run
    the handler. This can be `/tmp`
    6. Check remaining options, the defaults contained in the examples should
    be fine in most cases.


13. Edit the Logging configuration file (e.g.
fdsnws.dataselect.1-log4j.properties) and set the desired full path and
filename for the respective log files. Set the properties
    ```
    log4j.appender.ShellAppender.File=/WSSHOME/dataselect.log
    log4j.appender.UsageAppender.File=/WSSHOME/dataselect_usage.log
    ```
    An alternative location for service logs is to keep them with Tomcat
logs i.e. *${catalina.home}/logs/"*
    ```
    log4j.appender.ShellAppender.File=/${catalina.home}/logs/dataselect.log
    log4j.appender.UsageAppender.File=/${catalina.home}/logs/dataselect_usage.log
    ```

14. Start Tomcat:
    ``` bash
    /WSSHOME/tomcat/bin/startup.sh
    ```
    At this point the Tomcat container and FDSN web service applications are
    installed and running, if you did not change the default Tomcat port
    the service is accessible on port 8080.

15. Verify Tomcat is running by browsing
[**http://localhost:8080/**](http://localhost:8080/) (adjust host in URL
as necessary).

16. Verify that the installed service is running by browsing
[**http://localhost:8080/fdsnws/dataselect/1**](http://localhost:8080/fdsnws/dataselect/1)
(adjust host and service path in URL as necessary).

17. To stop Tomcat you may execute:
    ``` bash
    /WSSHOME/tomcat/bin/shutdown.sh
    ```
#### Further configuration and considerations:


- WSS expects an HTML page (as specified in property **rootServiceDoc**) to be
served from the root of the web service, possibly directing users to further
documentation. Here is a sample of [**FDSN dataselect documentation**](https://seiscode.iris.washington.edu/attachments/1127/dataselect_root.html).

- Consider installation of a [**system init script**](https://seiscode.iris.washington.edu//projects/webserviceshell/wiki/Tomcat_setup#Step-6-Create-startup-script) to start the Tomcat container on system
 boot and perform clean shutdowns.
- Consider adding a [**Tomcat authentication mechanisms**](https://seiscode.iris.washington.edu//projects/webserviceshell/wiki/Authentication) to authenticate users making request via the /queryauth service method (for access to restricted data).

- Configure an Apache web server to proxy requests made on port 80 to
 a Tomcat instances on port 8080. Apache HTTPD is known to work,
 nginx will probably work. This is recommended as a security measure and it
 also allows more flexible configuration options such as having multiple Tomcats
 installed but reachable via the same port (80).

    In Apache HTTPD, the following are examples of ProxyPass directives for an
    httpd server on the same host as the Tomcat servers (both forward and reverse
    proxy options are needed):
    ```
    ProxyPass               /fdsnws/dataselect/1 http://localhost:8080/fdsnws/dataselect/1
    ProxyPassReverse        /fdsnws/dataselect/1 http://localhost:8080/fdsnws/dataselect/1
    ```
