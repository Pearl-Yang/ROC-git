#!/bin/sh
B=http://127.0.0.1:8080

# wget wrapper for json
post() {
  wget -q -O - --post-file="$2" --header="Content-Type: application/json" "$1"
}
get() {
  wget -q -O - "$1"
}
put() {
  wget -q -O - -O /dev/null --post-file="$2" --header="Content-Type: application/json" "$1"
}

echo "=== 1. login ==="
post "$B/api/v1/user/login" /tmp/login.json > /tmp/r1.json
cat /tmp/r1.json
TOKEN=$(cat /tmp/r1.json | grep -oE '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
H="Authorization: Bearer $TOKEN"
echo "TOKEN=${TOKEN:0:40}..."

echo ""
echo "=== 2. create share ==="
echo '{"projectId":1,"permissionLevel":"view_only","needPassword":false}' > /tmp/s.json
post "$B/api/v1/share/create" /tmp/s.json -H "$H" > /tmp/r2.json
cat /tmp/r2.json
ST=$(cat /tmp/r2.json | grep -oE '"shareToken":"[^"]*"' | head -1 | cut -d'"' -f4)
SID=$(cat /tmp/r2.json | grep -oE '"shareId":[0-9]+' | head -1 | cut -d':' -f2)
echo "ST=$ST SID=$SID"

echo ""
echo "=== 3. visitor view no pwd (should 200, view_only=no files) ==="
get "$B/api/v1/share/visitor/$ST" | head -c 400
echo

echo ""
echo "=== 4. update to needPassword=true, password=abcd ==="
echo '{"needPassword":true,"password":"abcd"}' > /tmp/u.json
# PUT 需要 header，用 wget 手动构造
wget -q -O - --post-file=/tmp/u.json --header="Content-Type: application/json" --header="$H" "$B/api/v1/share/$SID"
echo

echo ""
echo "=== 5. visitor view no password -> 500 (T-02 password protection) ==="
get "$B/api/v1/share/visitor/$ST"
echo

echo ""
echo "=== 6. visitor view with password=abcd -> 200 ==="
get "$B/api/v1/share/visitor/$ST?password=abcd"
echo
