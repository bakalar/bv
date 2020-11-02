ACCESS_TOKEN=`./login.sh 2>/dev/null|json_pp|fgrep access_token|cut -d\" -f4`
curl -v -X GET -H "Authorization: Bearer ${ACCESS_TOKEN}" "http://localhost:8080/books/9781861978769/1/url"
