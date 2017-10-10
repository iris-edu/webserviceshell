#!/usr/bin/python
#!/usr/bin/env python

'''
Created by Nick Falco - 2017-05-30

A test web handler for the IRIS Web Service Shell.

The program by default produces a set of random values (default=50) in a specified
format type of either text (default), xml, or a zip of text files with
up to 10 random values in each file. The number of values
returned is defined by the num_values parameter.

Various handler exit sequences can be manually triggered
via the command line handler.

Example error sequences:
------------------------
force a *runtime* error with a stack trace 3 levels deep
.../irisws/wsstest/1/query?force_error=runtime&runtime_error_level=3&nodata=404

force an exception with exit code 1 *before* writing data to std-out
.../irisws/wsstest/1/query?force_error=before&force_exit_code=1&nodata=404

force an exception with exit code 3 *after* writing data to std-out
.../irisws/wsstest/1/query?force_error=after&force_exit_code=3&nodata=404

sleep for 5 seconds and force an exception with exit code 1 *before* writing data to std-out
.../irisws/wsstest/1/query?force_sleep=5&force_error=before&force_exit_code=3&nodata=404

force an exception with exit code 1 *before* writing data to std-out and write a cutom error message to std-error
.../irisws/wsstest/1/query?force_error=before&force_exit_code=1&error_msg='my custom error message'&nodata=404
'''

from argparse import ArgumentParser
import xml.etree.cElementTree as ET
from cStringIO import StringIO
##from io import StringIO
import random
import sys
import time
import zipfile
import traceback


def parse_arguments():
    # define command line arguments
    parser = ArgumentParser(description='Command line parser for a test Web Service Shell handler.')
    parser.add_argument('--format', help="Type of data requested. Choose from 'text', 'xml', and 'zip'", default='text', type=str)
    parser.add_argument('--num_values', help="The number of random values that the program should return.", default=50, type=int)
    parser.add_argument('--force_error', help=("Optionally close the handler with an error at a certain state of the program execution."
                                             "Choose between 'before', 'runtime', 'after' indicating whether to exit "
                                             "with an error before, at runtime, or after writing to std-out."), type=str, default="")
    parser.add_argument('--error_msg', help="Optionally specify a custom error message to exit the handler with. (write to std-error)", type=str)
    parser.add_argument('--force_exit_code', help="Optionally force the exit status code to exit the handler with.", type=int)
    parser.add_argument('--force_sleep', help="Optionally force the handler to sleep for a certain number of seconds before exiting.", default=0, type=float)
    parser.add_argument('--runtime_error_level', help="Optionally specify the level of the stack trace when forcing a runtime error.", default=0, type=int)
    args = parser.parse_args()
    return args


class WSSTest(object):

    def __init__(self, args):
        self.args = args

    def exit_with_error(self, msg, exitcode=1):
        '''Terminates the process with a given msg and exit code.
           @param msg string: The error message to write to std error.
           @param exitcode integer: The exit code that the handler should exit with.
        '''
        # Force the handler to sleep for an allotted amount of time (Default = 0)
        time.sleep(int(self.args.force_sleep))
        # Force the handler to return a particular error message
        if self.args.error_msg:
            msg = self.args.error_msg
        # Force the handler to exit with a certain code.
        if self.args.force_exit_code:
            exitcode = int(self.args.force_exit_code)
        sys.stderr.write(msg)
        sys.exit(exitcode)

    def exit_normal(self):
        '''Terminates the process without a error message.
        '''
        # Force the handler to sleep for an allotted amount of time (Default = 0)
        time.sleep(int(self.args.force_sleep))
        # Force the handler to exit with a certain code.
        if self.args.force_exit_code:
            exitcode = int(self.args.force_exit_code)
            exit(exitcode)
        else:
            exit(0)

    def get_traceback(self):
        '''Returns the stack traceback string.
        '''
        _, _, tb = sys.exc_info()
        stio = StringIO()
        traceback.print_exc(file=stio)
        del tb
        tb_msg = stio.getvalue()
        return tb_msg

    def raise_runtime_error(self):
        '''Raises a runtime error with an optional stack trace level (default=0)
        '''
        try:
            # simulate a variable level excpetion traceback
            while self.args.runtime_error_level > 0:
                self.args.runtime_error_level -= 1
                self.raise_runtime_error()
            0 * (1/0)
        except Exception:
            tb = self.get_traceback()
            raise RuntimeError(tb)

    def get_text(self, num):
        '''Return a list of random numbers between 0 and 99.
           @param num integer: the number of random values
           to produce
        '''
        text_output = ", ".join([str(random.randint(0, 100)) for x in xrange(num)])

        if self.args.force_error.lower() == "runtime":
            self.raise_runtime_error()

        return text_output

    def get_xml(self, num):
        '''Return a XML structure of random numbers between 0 and 99.
           @param num integer: the number of random values
           to produce
        '''
        root = ET.Element("root")
        values = ET.SubElement(root, "values")
        for i in xrange(num):
            rand_val = str(random.randint(0, 100))
            ET.SubElement(values, "field_{0}".format(i), name="value_{0}".format(rand_val)).text = rand_val

        if self.args.force_error.lower() == "runtime":
            self.raise_runtime_error()

        tree = ET.ElementTree(root)
        st = StringIO()
        tree.write(st)
        return st.getvalue() # return the XML file as a string

    def get_zip(self, num):
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
                    z.writestr('file_{0}'.format(idx), self.get_text(10))
                    num = num - 10
                else:
                    z.writestr('file_{0}'.format(idx), self.get_text(num))
                    num = 0
                idx = idx + 1

            if self.args.force_error.lower() == "runtime":
                self.raise_runtime_error()

        return stio.getvalue()

    def process(self):
        num_values = int(args.num_values)
        if args.format.lower() == "text":
            sys.stdout.write(wsstest.get_text(num_values))
        elif args.format.lower() == "xml":
            sys.stdout.write(wsstest.get_xml(num_values))
        elif args.format.lower() == "zip":
            sys.stdout.write(wsstest.get_zip(num_values))


if __name__ == '__main__':
    args = parse_arguments()
    wsstest = WSSTest(args)

    DEFAULT_EXIT_CODE = 1 # the default exit code to raise when --force_exit_code is not defined

    try:
        if args.force_error.lower() == "before":
            wsstest.exit_with_error(msg="Forced error before writing to std-out.", exitcode=DEFAULT_EXIT_CODE)
            wsstest.process()
        elif args.force_error.lower() == "after":
            wsstest.process()
            wsstest.exit_with_error(msg="Forced error after writing to std-out.", exitcode=DEFAULT_EXIT_CODE)
        else:
            wsstest.process()
    except RuntimeError as err:
        wsstest.exit_with_error(err.message, DEFAULT_EXIT_CODE)

    wsstest.exit_normal()
