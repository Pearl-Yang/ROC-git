# T-09 / T-02 冒烟测试启动脚本
# 用法：.\scripts\start-backend-local.ps1
# 依赖：Docker Desktop 在跑，rocgit-mysql 容器已起（port 3306 暴露给 host）

$ErrorActionPreference = "Continue"
$root = "D:\ProjectSpace\2026年上学年\软件设计实践"
$war  = "$root\backend\target\knife4j-spring-1.0.0.war"
$log  = "$root\data\backend-local.log"
$err  = "$root\data\backend-local.err.log"

if (-not (Test-Path $war)) {
  Write-Host "war 不存在，先 mvn package"; exit 1
}

# 关掉旧 java
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 1

# 优先尝试 host.docker.internal (Docker Desktop)，回退 127.0.0.1
$dbHost = if (Test-Path "$root\.use-host-docker-internal") { "host.docker.internal" } else { "127.0.0.1" }

Write-Host "[start-backend-local] war=$war  port=18081  DB=$dbHost"
Start-Process -FilePath "java" -ArgumentList `
  "-Dfile.encoding=UTF-8", `
  "-jar", $war, `
  "--server.port=18081", `
  "--DB_HOST=$dbHost", `
  "--DB_PORT=3306", `
  "--DB_USER=root", `
  "--DB_PASSWORD=root", `
  "--ROCGIT_FILE_ROOT=$root\data\files", `
  "--ROCGIT_FILE_BASE_URL=/files" `
  -RedirectStandardOutput $log `
  -RedirectStandardError $err `
  -PassThru | ForEach-Object { "started pid=$($_.Id)" }

Write-Host "等待 30s 让 Spring Boot 起来..."
Start-Sleep -Seconds 30
$ok = (& netstat.exe -ano 2>&1 | Select-String ":18081.*LISTEN" | Measure-Object).Count
if ($ok -gt 0) { Write-Host "OK: 18081 LISTENING" } else { Write-Host "FAIL: 18081 未监听，看 $err" }
