
# testing set header, set some other header beside Content-Disposition to
# see that default still works

 
args="$@"

/bin/echo -n "HTTP_HEADERS_START"
echo "dummy-header : dummy-value"
/bin/echo -n "HTTP_HEADERS_END"

echo ""
echo "set_header_CD2 stdout args: ${args}"
echo "set_header_CD2 date -u: " `date -u`
