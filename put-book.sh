ACCESS_TOKEN=`./login.sh 2>/dev/null|json_pp|fgrep access_token|cut -d\" -f4`
curl -v -X PUT --upload-file 1.pdf -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "Content-Type: application/pdf" "http://localhost:8080/books/9781861978769"
