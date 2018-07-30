
  Date: 10-Jan-2017
  Subj: Glassfish notes for this operation

The ports are adjusted to avoid conflicts with other services
  The settings are in file glassfish4/glassfish/domains/domain1/config/domain.xml
    http-listener-1  8102
    http-listener-2  8202
    admin-listener   8343

to see logs
  tail -f glassfish-8102-4.1.1/glassfish4/glassfish/domains/domain1/logs/server.log
  tail -f glassfish-8102-4.1.1/glassfish4/glassfish/domains/domain1/logs/intmag.log

to start
  glassfish4/glassfish/bin/asadmin start-domain

to check system properties, wssConfigDir must be correctly set
note: must use admin-listener port on all admin commands, port: 8343
      admin user name: admin
      admin pw: cube1iris

to get use a service
  http://cube1:8102/intmag/wssversion
  http://cube1:8102/intmag/v2/swagger

to stop
glassfish4/glassfish/bin/asadmin stop-domain

to replace web service, the server usually needs to be shutdown
  - shows up as errors related to stale file
  - it seems to be problem on cube1 where nfs mounts are used to provide the file system
    rm glassfish4/glassfish/domains/domain1/autodeploy/intmag.war
    glassfish4/glassfish/bin/asadmin stop-domain
    rm -rf glassfish4/glassfish/domains/domain1/applications/intmag/
    glassfish4/glassfish/bin/asadmin start-domain
    cp ~/transient/webserviceshell-2.3.war glassfish4/glassfish/domains/intmag.war
    mv glassfish4/glassfish/domains/intmag.war glassfish4/glassfish/domains/domain1/autodeploy/


to install custom log formatter
  cp custom-formatter-1.0-SNAPSHOT.jar glassfish4/glassfish/domains/domain1/lib/ext/
  edit file glassfish4/glassfish/domains/domain1/config/logging.properties 
  update GFFIleHandler.formatter to
  com.sun.enterprise.server.logging.GFFileHandler.formatter=edu.iris.logging.GlassfishCustomFormatter
  see log properties, may need to restart
  glassfish4/glassfish/bin/asadmin --port 8348 restart-domain domain1

to see log properties
  glassfish4/glassfish/bin/asadmin --port 8348 list-log-attributes

