
# testing set header

 
args="$@"

/bin/echo -n "HTTP_HEADERS_START"
echo "Content-Type : image/png"
echo "Content-Disposition : inline"
echo "Access-Control-Allow-Origin : http://host.example"
echo "Test-Header : value-for-test-hdr"
/bin/echo -n "HTTP_HEADERS_END"

echo ""
echo "set_header_CD1 stdout args: ${args}"
echo "set_header_CD1 date -u: " `date -u`
