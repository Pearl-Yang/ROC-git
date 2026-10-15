#!/usr/bin/env python3
import json, sys, urllib.request, urllib.parse

B = "http://127.0.0.1:8080"

def req(method, path, data=None, token=None, params=None):
    url = B + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())

print("=== 1. login ===")
_, d = req("POST", "/api/v1/user/login", {"code":"oTest_001_leader","nickname":"liming"})
print(json.dumps(d, ensure_ascii=False, indent=2))
token = d.get("data", {}).get("token", "")

print("\n=== 2. create share ===")
_, d = req("POST", "/api/v1/share/create", {"projectId":1,"permissionLevel":"view_only","needPassword":False}, token=token)
print(json.dumps(d, ensure_ascii=False, indent=2))
st = d.get("data", {}).get("shareToken", "")
sid = d.get("data", {}).get("shareId", 0)

print("\n=== 3. visitor view (view_only) - no files ===")
code, d = req("GET", "/api/v1/share/visitor/" + st)
print(f"code={code}")
keys = list(d.get("data", {}).keys())
print(f"data keys: {keys}")
files = d.get("data", {}).get("files")
print(f"files field: {'ABSENT (correct for view_only)' if files is None else 'PRESENT ' + str(len(files))}")

print("\n=== 4. update -> view_with_download ===")
_, d = req("PUT", f"/api/v1/share/{sid}", {"permissionLevel":"view_with_download"}, token=token)
print(json.dumps(d, ensure_ascii=False))

print("\n=== 5. visitor view (view_with_download) - should have files with url ===")
_, d = req("GET", "/api/v1/share/visitor/" + st)
print(f"files count: {len(d.get('data',{}).get('files',[]))}")
f1 = d.get('data',{}).get('files',[{}])[0]
print(f"first file has url: {bool(f1.get('url'))}")

print("\n=== 6. update password=abcd ===")
_, d = req("PUT", f"/api/v1/share/{sid}", {"needPassword":True,"password":"abcd"}, token=token)
print(json.dumps(d, ensure_ascii=False))

print("\n=== 7. visitor view NO password -> 500 ===")
code, d = req("GET", "/api/v1/share/visitor/" + st)
print(f"code={code} msg={d.get('message','')}")

print("\n=== 8. visitor view with password=abcd -> 200 ===")
code, d = req("GET", "/api/v1/share/visitor/" + st, params={"password":"abcd"})
print(f"code={code} project={d.get('data',{}).get('project',{}).get('name','')}")

print("\n=== 9. T-09: message list - should have task_reminder ===")
_, d = req("GET", "/api/v1/message/list", params={"pageNum":1,"pageSize":20}, token=token)
msgs = [m for m in d.get("data",{}).get("list",[]) if m.get("type") == "task_reminder"]
print(f"task_reminder count: {len(msgs)}")
for m in msgs:
    print(f"  [{m.get('messageId')}] {m.get('title')} | {m.get('content','')[:50]}")
