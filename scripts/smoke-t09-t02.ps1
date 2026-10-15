# T-09 / T-02 冒烟测试脚本
# 依赖：start-backend-local.ps1 跑成功后，18081 端口在听

$ErrorActionPreference = "Continue"
$base = "http://127.0.0.1:18081"
$root = "D:\ProjectSpace\2026年上学年\软件设计实践"
$out  = "$root\data\smoke.log"

function Step($name) { Write-Host "`n=== $name ===" | Tee-Object -FilePath $out -Append }

# 1) 登录
Step "1. login"
$body = '{"code":"oTest_001_leader","nickname":"liming","avatarUrl":"https://api.dicebear.com/7.x/avataaars/svg?seed=li"}'
$r = Invoke-RestMethod -Uri "$base/api/v1/user/login" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 8
$token = $r.data.token
$h = @{ Authorization = "Bearer $token" }
"login uid=$($r.data.userId)" | Tee-Object -FilePath $out -Append

# 2) 创建带 deadline 的任务（用空格分隔日期以验证 JacksonConfig 修好）
Step "2. create task (T-09 立即触发 assign 提醒)"
$body2 = '{"projectId":1,"stageId":2,"name":"T-09 smoke assign","assigneeId":1,"deadline":"2026-12-31 23:59:59"}'
$r2 = Invoke-RestMethod -Uri "$base/api/v1/task/create" -Method Post -ContentType "application/json" -Body $body2 -Headers $h -TimeoutSec 8
"create taskId=$($r2.data.taskId)" | Tee-Object -FilePath $out -Append

# 3) 查 message 表（应有一条 assign 提醒）
Step "3. 查我自己的消息（应有 1 条 task_reminder）"
$r3 = Invoke-RestMethod -Uri "$base/api/v1/message/list?pageNum=1&pageSize=20" -Method Get -Headers $h -TimeoutSec 8
"msg total=$($r3.data.total)" | Tee-Object -FilePath $out -Append
$r3.data.list | ForEach-Object {
  "$($_.type) | $($_.title) | $($_.content)" | Tee-Object -FilePath $out -Append
}

# 4) 创建分享 (T-02)
Step "4. create share (T-02)"
$body4 = '{"projectId":1,"permissionLevel":"view_only","expiresIn":"30d","needPassword":false,"password":""}'
$r4 = Invoke-RestMethod -Uri "$base/api/v1/share/create" -Method Post -ContentType "application/json" -Body $body4 -Headers $h -TimeoutSec 8
$token2 = $r4.data.shareToken
"share shareId=$($r4.data.shareId) shareToken=$token2 shareUrl=$($r4.data.shareUrl)" | Tee-Object -FilePath $out -Append

# 5) 访客视图 (公开)
Step "5. visitor view (view_only)"
$r5 = Invoke-RestMethod -Uri "$base/api/v1/share/visitor/$token2" -Method Get -TimeoutSec 8
"view perm=$($r5.data.permissionLevel) project=$($r5.data.project.name) files=" + ($r5.data.files -ne $null) | Tee-Object -FilePath $out -Append

# 6) 改权限到 view_with_download
Step "6. update share to view_with_download"
$body6 = '{"permissionLevel":"view_with_download"}'
Invoke-RestMethod -Uri "$base/api/v1/share/$($r4.data.shareId)" -Method Put -ContentType "application/json" -Body $body6 -Headers $h -TimeoutSec 8 | Out-Null

Step "7. visitor view (view_with_download) — 应有 files 列表带 url"
$r7 = Invoke-RestMethod -Uri "$base/api/v1/share/visitor/$token2" -Method Get -TimeoutSec 8
"view files count=$($r7.data.files.Count) first.url=$($r7.data.files[0].url)" | Tee-Object -FilePath $out -Append

# 8) 改到密码保护
Step "8. update share password"
$body8 = '{"needPassword":true,"password":"1234"}'
Invoke-RestMethod -Uri "$base/api/v1/share/$($r4.data.shareId)" -Method Put -ContentType "application/json" -Body $body8 -Headers $h -TimeoutSec 8 | Out-Null

Step "9. visitor view wrong password — 应 403/错误"
try {
  $r9 = Invoke-RestMethod -Uri "$base/api/v1/share/visitor/$token2" -Method Get -TimeoutSec 8
  "view with no pwd: code=$($r9.code) msg=$($r9.message)" | Tee-Object -FilePath $out -Append
} catch {
  "no-pwd 403: $($_.Exception.Message)" | Tee-Object -FilePath $out -Append
}

Step "10. visitor view with right password"
$r10 = Invoke-RestMethod -Uri "$base/api/v1/share/visitor/$token2?password=1234" -Method Get -TimeoutSec 8
"view with pwd: project=$($r10.data.project.name)" | Tee-Object -FilePath $out -Append

Write-Host "`n完整结果在 $out"
