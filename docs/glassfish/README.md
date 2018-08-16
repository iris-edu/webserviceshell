# Glassfish 4.1.1 Basic Operating Notes

- 2018-08-16 - update
- 2017-01-10

To **change ports**, look for the following, default listener in the settings file **glassfish4/glassfish/domains/domain1/config/domain.xml**

- http-listener-1  8102
- http-listener-2  8202
- admin-listener   8343

To **start** glassfish, use the following command.
```
glassfish4/glassfish/bin/asadmin start-domain
```
Note: the admin-listener port parameter **-- port 8348** may be needed. i.e.
```
glassfish4/glassfish/bin/asadmin --port 8348 start-domain
```

To **stop**
```
glassfish4/glassfish/bin/asadmin stop-domain
```

To specify the WSS configuration file location, the Java property, wssConfigDir must be set explicitly to the path for the cfg files. The cfg files may be located at any convenient path. **to set wssConfigDir**
```
glassfish4/glassfish/bin/asadmin create-system-properties wssConfigDir=$(pwd)/glassfish/domains/domain1/wss_config
```

Glassfish does not recognize the convention that Tomcat uses to create a context name based on "#" characters in the the war file name. If this does not matter, autodeploy may be used, see discussion below, however, to **deploy with a specific context, like fdsnws/event/1**, the context must be set explicitly with a deploy command:
```
glassfish4/glassfish/bin/asadmin deploy --contextroot fdsnws/event/1 /path/to/webserviceshell-2.4.war
```

To **see logs**, check the domain1/logs folder for the server.log and WSS logs
```
tail -f glassfish4/glassfish/domains/domain1/logs/server.log
tail -f glassfish4/glassfish/domains/domain1/logs/xyz_app.log
```

**Using autodeploy:** autodeploy works as expected on OS X (excepting not recognizing "#"), but may not work reliably if NFS mounts are used to provide the file system. **Errors related to stale file ...** indicate this problem. **If using autodeploy on an NFS file system**, the following steps for deploying a service called "intmag", may be needed:

```
# remove war file
rm glassfish4/glassfish/domains/domain1/autodeploy/intmag.war
# stop server
glassfish4/glassfish/bin/asadmin stop-domain
# remover respective applications folder
rm -rf glassfish4/glassfish/domains/domain1/applications/intmag/
# start server
glassfish4/glassfish/bin/asadmin start-domain
# make a copy of war file with appropriate name
cp ~/transient/webserviceshell-2.3.war glassfish4/glassfish/domains/intmag.war
# move it into autodeploy folder
mv glassfish4/glassfish/domains/intmag.war glassfish4/glassfish/domains/domain1/autodeploy/
```

**Optional logger installation:** For extended testing with glassfish, the **default log messages can be very verbose**, an optional custom logger is available called custom-formatter-1.0-SNAPSHOT, **to install a custom log formatter in glassfish**:

- Check log properties before and after changes to verify installation
  ```
  glassfish4/glassfish/bin/asadmin --port 8348 list-log-attributes
  ```
- put the formatter jar file in the lib/ext folder
  ```
  cp custom-formatter-1.0-SNAPSHOT.jar glassfish4/glassfish/domains/domain1/lib/ext/
  ```
- change the configuration
  - ** edit file** glassfish4/glassfish/domains/domain1/config/logging.properties
  - update property **GFFIleHandler.formatter**
  - the full setting is
  **com.sun.enterprise.server.logging.GFFileHandler.formatter=edu.iris.logging.GlassfishCustomFormatter**


- check log properties, **a restart may be needed**
  ```
  glassfish4/glassfish/bin/asadmin --port 8348 restart-domain domain1
  ```
