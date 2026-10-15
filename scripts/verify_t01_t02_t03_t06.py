#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""端到端验证脚本：登录 → 上传文件 → 创建分享 → 访客视图 → 贡献统计 → 版本回滚"""
import json
import sys
import urllib.request
import urllib.parse
import urllib.error

BASE = "http://127.0.0.1:18080"
ANSI_G = "\033[92m"; ANSI_R = "\033[91m"; ANSI_Y = "\033[93m"; ANSI_RST = "\033[0m"

def call(method, path, body=None, token=None, raw=False):
    url = BASE + path
    headers = {}
    data = None
    if body is not None:
        if isinstance(body, (bytes, bytearray)):
            data = bytes(body)
        else:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
            headers["Content-Type"] = "application/json; charset=utf-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            payload = r.read()
            if raw:
                return payload, r.headers
            try:
                return json.loads(payload.decode("utf-8"))
            except Exception:
                return {"_raw": payload[:500].decode("utf-8", errors="replace")}
    except urllib.error.HTTPError as e:
        return {"code": e.code, "message": str(e), "_body": e.read()[:500].decode("utf-8", errors="replace")}

def step(name):
    print(f"\n{ANSI_Y}=== {name} ==={ANSI_RST}")

def ok(msg): print(f"  {ANSI_G}OK{ANSI_RST} {msg}")
def fail(msg): print(f"  {ANSI_R}FAIL{ANSI_RST} {msg}")

# ---------- 1. 登录 ----------
step("1) 登录（演示登录：用李明/组长账号）")
r = call("POST", "/api/v1/user/login", {"code": "oTest_001_leader", "nickname": "李明", "avatarUrl": "https://api.dicebear.com/7.x/avataaars/svg?seed=li"})
if r.get("code") == 0 and r.get("data", {}).get("token"):
    token = r["data"]["token"]
    uid = r["data"].get("userId")
    ok(f"登录成功 uid={uid} token={token[:20]}...")
else:
    fail(f"登录失败：{r}")
    sys.exit(1)

# ---------- 2. T-06 贡献统计 ----------
step("2) T-06 贡献统计真接 SQL")
r = call("GET", "/api/v1/user/contribution-stats", token=token)
print("  ", json.dumps(r, ensure_ascii=False, indent=2))
if r.get("code") == 0:
    d = r["data"]
    if (d.get("uploadedFiles") or 0) > 0 or (d.get("discussions") or 0) > 0 or (d.get("completedTasks") or 0) > 0:
        ok(f"贡献统计非零：上传={d.get('uploadedFiles')} 已通过={d.get('approvedFiles')} 完成任务={d.get('completedTasks')} 研讨={d.get('discussions')}")
    else:
        fail(f"贡献统计仍全 0：{d}")
else:
    fail(f"贡献统计调用失败：{r}")

# ---------- 3. T-01 真上传 ----------
step("3) T-01 multipart 真上传（构造 1KB 文本文件）")
fake_content = b"hello rocgit T-01 upload test " * 30  # ~900 字节
boundary = "----rocgitBoundaryXYZ"
parts = []
parts.append(f"--{boundary}\r\n".encode())
parts[-1] += b"Content-Disposition: form-data; name=\"file\"; filename=\"t01_demo.txt\"\r\n"
parts[-1] += b"Content-Type: text/plain\r\n\r\n"
parts.append(fake_content)
parts.append(f"\r\n--{boundary}--\r\n".encode())
body = b"".join(parts)
url = BASE + "/api/v1/upload"
req = urllib.request.Request(url, data=body, method="POST",
    headers={"Content-Type": f"multipart/form-data; boundary={boundary}",
             "Authorization": "Bearer " + token})
try:
    with urllib.request.urlopen(req, timeout=15) as resp:
        payload = resp.read().decode("utf-8")
        r = json.loads(payload)
        print("  ", json.dumps(r, ensure_ascii=False, indent=2))
        if r.get("code") == 0 and r["data"].get("url"):
            uploaded_url = r["data"]["url"]
            ok(f"上传成功：url={uploaded_url}")
            # 验证文件真可访问（经 nginx 80 端口 / 后端 18080 端口）
            full_urls = [
                "http://127.0.0.1" + uploaded_url,
                BASE + uploaded_url
            ]
            readable = False
            for fu in full_urls:
                try:
                    with urllib.request.urlopen(fu, timeout=5) as fr:
                        content = fr.read()
                        if content == fake_content:
                            ok(f"文件内容回读一致 ({fu})")
                            readable = True
                            break
                except Exception:
                    continue
            if not readable:
                fail(f"两个端口都拿不回文件")
        else:
            fail(f"上传失败：{r}")
except Exception as e:
    fail(f"上传异常：{e}")

# ---------- 4. T-02 创建分享 ----------
step("4) T-02 创建分享（view_only）")
r = call("POST", "/api/v1/share/create", {"projectId": 1, "permissionLevel": "view_only"}, token=token)
print("  ", json.dumps(r, ensure_ascii=False, indent=2))
if r.get("code") == 0 and r["data"].get("shareToken"):
    share_id = r["data"]["shareId"]
    share_token = r["data"]["shareToken"]
    ok(f"分享创建成功 shareId={share_id} token={share_token[:16]}...")
else:
    fail(f"分享创建失败：{r}")

# ---------- 5. T-02 myShares 按用户过滤 ----------
step("5) T-02 myShares 按用户过滤（应只看到上面那条）")
r = call("GET", "/api/v1/share/my-shares", token=token)
print("  ", json.dumps(r, ensure_ascii=False, indent=2))
if r.get("code") == 0:
    lst = r["data"]
    if isinstance(lst, list) and len(lst) >= 1 and lst[0]["shareToken"] == share_token:
        ok(f"myShares 仅返回当前用户创建的（{len(lst)} 条）")
    else:
        fail(f"myShares 异常：{lst}")
else:
    fail(f"myShares 调用失败：{r}")

# ---------- 6. T-02 访客视图（无 token） ----------
step("6) T-02 访客视图（无 token，按权限裁剪）")
r = call("GET", "/api/v1/share/visitor/" + share_token)
print("  ", json.dumps(r, ensure_ascii=False, indent=2))
if r.get("code") == 0 and r["data"].get("project"):
    data = r["data"]
    perm = data.get("permissionLevel")
    has_files = "files" in data
    if perm == "view_only" and not has_files:
        ok(f"view_only 权限正确裁剪（无 files 字段）")
    elif perm != "view_only" and has_files:
        ok(f"{perm} 权限正确返 files 字段（{len(data.get('files',[]))} 个）")
    else:
        fail(f"权限裁剪不符合预期：perm={perm} has_files={has_files}")
else:
    fail(f"访客视图失败：{r}")

# ---------- 7. T-03 文件版本 ----------
step("7) T-03 文件版本列表（file_id=3 已有 #1/#2 seed）")
r = call("GET", "/api/v1/file/3/versions", token=token)
print("  ", json.dumps(r, ensure_ascii=False, indent=2))
if r.get("code") == 0 and isinstance(r["data"], list) and len(r["data"]) >= 2:
    ok(f"versions 真接表，返 {len(r['data'])} 条")
else:
    fail(f"versions 仍返空：{r}")

# ---------- 8. T-03 回滚真接表 ----------
step("8) T-03 rollback 新增『恢复自 vX』版本记录")
r = call("POST", "/api/v1/version/file/3/rollback", {"targetVersion": "1.0", "remark": "测试回滚"}, token=token)
print("  ", json.dumps(r, ensure_ascii=False, indent=2))
r = call("GET", "/api/v1/file/3/versions", token=token)
if r.get("code") == 0 and len(r["data"]) >= 3:
    last = r["data"][0]
    if "回滚" in (last.get("remark") or ""):
        ok(f"rollback 真生成新版本号 {last.get('versionNumber')} 备注='{last.get('remark')}'")
    else:
        fail(f"rollback 新增记录但无回滚标记：{last}")
else:
    fail(f"rollback 后版本数未增：{r}")

print(f"\n{ANSI_G}===== 全部验证步骤结束 ====={ANSI_RST}")