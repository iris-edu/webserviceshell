#!/bin/bash

# testing set header, set some other header beside Content-Disposition to
# see that default still works

args="$@"

# http://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash

# Expecting key, value pairs, so process two arguments at a time,
# the first argument $1 is "key" and its value is $2
# process all arguments before continuing
# $# is count of positional arguments, arguments
# "shift" means decrement positional counter by 1
#
while [[ $# > 1 ]]
do
    key="$1"
    case $key in
        --format)
            format_arg="$2"
            shift
        ;;
        --overrideDisp)
            overrideDisp_arg="$2"
            shift
        ;;
        *)
            # unknown option
        ;;
    esac
    shift
done

# 
/bin/echo -n "HTTP_HEADERS_START"
if [[ ${format_arg} == "json" ]] ; then
    if [[ ${overrideDisp_arg} == "true" ]] ; then
         echo "Content-Disposition : override-json-settings"
    fi
fi
echo "dummy-header : dummy-value"
/bin/echo -n "HTTP_HEADERS_END"

echo ""
echo "set_header_CD3 stdout args: ${args}"
echo "set_header_CD3 date -u: " `date -u`
