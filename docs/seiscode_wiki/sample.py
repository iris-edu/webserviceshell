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