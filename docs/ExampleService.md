# Example Service Implementation using WSS 2.x

WSS can be used to create REST-like web service for delivering data to
an HTTP client. This procedure highlights the steps needed to install a
WSS service given the following:

- A command-line script called **sample.py** - see listing below.
- That Tomcat is installed.
- A folder is available for storing files, in this example the fo/der is called /WSSHOME

#### This procedure's objective is to:

- create a web service called **mysrv/sample/1**, accessable at
    http://localhost:8080/mysrv/sample/1
- have the web service deliver random numbers using parameter
    **num\_values**
- deliver **xml**, **text**, or **zip** using parameter **format**
- returns error 3 and error message if the value of **num\_values** is
    not between 1 and 100

### Setup Tomcat Environment for WSS

Step | Action
---- | -----
if not installed, install Apache Tomcat | see [Installation instructions](Installation.md)
create WSS configuration folder: **/WSSHOME/config** | for this example: <br />```mkdir /WSSHOME/config```
specify the configure folder path | Add to, or edit file **/WSSHOME/tomcat/bin/setenv.sh** with this content: <br />``` JAVA_OPTS="-Xmx512m -DwssConfigDir='/WSSHOME/config'" ``` <br />as described in [Installation Instructions](Installation.md)

### Configure WSS
Step | Action
----- | -----
choose service name | for this example: **mysrv/sample/1**
deploy script | - The script must be executable <br /> - for this example, copy sample.py to **/WSSHOME/config/sample.py**
deploy WSS config files | - copy [WSS cfg Files](WebServiceShell-2.4.x.md#servicecfg) to folder **/WSSHOME/config** -- with the names, respectively: **mysrv.sample.1-service.cfg**, **mysrv.sample.1-param.cfg**, and **mysrv.sample.1-log4j.properties** <br /> - for naming rules see [WSS Operation and Configuration](WebServiceShell-2.4.x.md#wss-operation-and-configuration) <br /> - for parameter definitions, see [WSS Configuration Reference](WebServiceShell-2.4.x.md#wss-configuration-reference)
edit file **mysrv.sample.1-service.cfg** | - set parameter **appName**: appName=mysrv-sample-1 <br /> - set parameter **version**: version=1.0.0 <br /> - set parameter **query.handlerProgram**: query.handlerProgram=/WSSHOME/config/sample.py <br /> - set parameter **query.formatTypes**: query.formatTypes = \ <br /> xml: application/xml, \ <br /> text: text/plain, \ <br /> zip: application/zip <br /> - set parameter **query.formatDispositions**: query.formatDispositions= \ <br /> zip: attachment; filename="data.zip"
edit file **mysrv.sample.1-param.cfg** | - replace line **query.type=TEXT** with query.num\_values=NUMBER <br /> - remove lines with **query.minlongitude, query.maxlongitude, query.minlatitude,** and **query.maxlatitude** <br /> - add lines: query.aliases = \ <br /> num\_values: num
edit file **mysrv.sample.1-log4j.properties** | - set parameter **log4j.appender.ShellAppender.File**: log4j.appender.ShellAppender.File=${catalina.home}/logs/sample.log <br /> - set parameter **log4j.appender.UsageAppender.File**: log4j.appender.UsageAppender.File=${catalina.home}/logs/sample\_usage.log
download and rename WSS war file | - download the WSS war file from [releases page](https://github.com/iris-edu/webserviceshell/releases) <br /> - copy the WSS war to the desired service name i.e. **mysrv\#sample\#1.war** <br /> ```cp webserviceshell_2.2.2.war /WSSHOME/tomcat/mysrv#sample#1.war```

### Startup and deploy WSS
Step | Action
----- | -----
start tomcat | run <br /> ```/WSSHOME/tomcat/bin/startup.sh``` <br /> then check that tomcat started, <br /> ```tail -f tomcat/logs/catalina.out```
deploy WSS for **mysrv/sample/1** | ```mv /WSSHOME/tomcat/mysrv#sample#1.war tomcat/webapps``` <br /> note that catalina.out shows that the service started|\
check operation with browser or curl | example URLs for testing, <br />  # check for plain text output <br /> **http://localhost:8080/mysrv/sample/1/query?num_values=25&format=text** <br /> #check for error handling <br /> **http://localhost:8080/mysrv/sample/1/query?num_values=-25&format=text** <br /> check for xml output <br /> **http://localhost:8080/mysrv/sample/1/query?num_values=25&format=xml** <br /> check for zip output of multiple files <br /> **http://localhost:8080/mysrv/sample/1/query?num_values=25&format=zip** <br /> check for "num" alias <br /> **http://localhost:8080/mysrv/sample/1/query?num=25&format=zip**

[releases1](/releases)<br />
[releases2](/releases/)<br />
[releases3](../releases/)<br />
[releases4](../../releases/)<br />
[releases5](iris-edu/webserviceshell/releases)<br />
[releasesa](https://github.com/iris-edu/webserviceshell/releases)




### Handler File

#### sample.py
``` python
    #!/usr/bin/python
    '''
    A sample web handler for the IRIS Web Service Shell.

    The program produces a set of random values in a specified
    format type of either text, xml, or a zip of text files with
    up to 10 random values in each file. The number of values
    returned is defined by the num_values parameter.
    '''

    from argparse import ArgumentParser
    import xml.etree.cElementTree as ET
    from cStringIO import StringIO
    import random
    import sys
    import zipfile

    def parse_arguments():
        # define command line arguments
        parser = ArgumentParser(description='Command line parser for sample Web Service Shell handler.')
        parser.add_argument('--format', help="Type of data requested. Choose from 'text', 'xml', and 'zip'")
        parser.add_argument('--num_values', help="The number of random values that the program should return. Max of 10.")
        args = parser.parse_args()
        return args

    def get_text(num):
        '''Return a list of random numbers between 0 and 99.
           @param num integer: the number of random values
           to produce
        '''
        return ", ".join([str(random.randint(0, 100)) for x in xrange(num)])

    def get_xml(num):
        '''Return a XML structure of random numbers between 0 and 99.
           @param num integer: the number of random values
           to produce
        '''
        root = ET.Element("root")
        values = ET.SubElement(root, "values")
        for i in xrange(num):
            rand_val = str(random.randint(0, 100))
            ET.SubElement(values, "field_{0}".format(i), name="value_{0}".format(rand_val)).text = rand_val
        tree = ET.ElementTree(root)
        st = StringIO()
        tree.write(st)
        return st.getvalue() #return the XML file as a string

    def get_zip(num):
        '''Return a zip file archive containing files with
           up to 10 comma separated random numbers in each file
           between 0 and 99. Files will be created as needed depending
           on the number of values requested.
           @param num integer: the number of random values
           to produce
        '''
        stio = StringIO()
        idx = 0
        with zipfile.ZipFile(stio, mode='w', compression=zipfile.ZIP_STORED,allowZip64=True) as z:
            while num > 0:
                if num >= 10:
                    z.writestr('file_{0}'.format(idx), get_text(10))
                    num = num - 10
                else:
                    z.writestr('file_{0}'.format(idx), get_text(num))
                    num = 0
                idx = idx + 1
        return stio.getvalue()

    if __name__ == '__main__':
        args = parse_arguments()
        num_values = int(args.num_values)
        if (num_values < 1) or (num_values > 100):
            #Throw an exception if the requested number of values exceeds 100
            sys.stderr.write("The requested number of values must be between 1 and 100")
            sys.exit(3)
        #Handle the request for the requested format type
        if args.format.lower() == "text":
            sys.stdout.write(get_text(num_values))
        elif args.format.lower() == "xml":
            sys.stdout.write(get_xml(num_values))
        elif args.format.lower() == "zip":
            sys.stdout.write(get_zip(num_values))
```
