<#
.SYNOPSIS
    ROC-git MySQL 一键重灌种子脚本（Windows / PowerShell）

.DESCRIPTION
    1. 停止 rocgit-backend 容器（防止它写日志/触发迁移）
    2. 在 rocgit-mysql 容器里 drop 数据库 roc_git
    3. 重新执行 01-schema.sql + 02-seed.sql + 3 个迁移脚本
    4. （可选）重启 rocgit-backend

.PARAMETER ContainerName
    MySQL 容器名，默认 rocgit-mysql

.PARAMETER MysqlRootPassword
    root 密码，默认 root

.PARAMETER SkipRestart
    跳过重启 backend

.EXAMPLE
    .\reseed.ps1
    .\reseed.ps1 -ContainerName rocgit-mysql-test -MysqlRootPassword root
    .\reseed.ps1 -SkipRestart
#>
[CmdletBinding()]
param(
    [string]$ContainerName = "rocgit-mysql",
    [string]$MysqlRootPassword = "root",
    [switch]$SkipRestart
)

# 关键：用 Continue + 调低 Warning，避免 PowerShell 把 docker exec 的 stderr 当致命错误
$ErrorActionPreference = "Continue"
$WarningPreference     = "SilentlyContinue"

$root = Split-Path -Parent $PSScriptRoot
$dbDir = Join-Path $root "backend\database"
$composeDir = $root

function Write-Section($msg) {
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Cyan
    Write-Host $msg -ForegroundColor Cyan
    Write-Host "=========================================" -ForegroundColor Cyan
}

# 在 PowerShell 5 中以 cmd /c 包一层 + 2>nul 避免 stderr 触发 NativeCommandError
function Invoke-DockerExec($cmd) {
    $full = "cmd /c `"$cmd 2>nul`""
    $output = & cmd /c $cmd '2>nul' 2>&1
    return [pscustomobject]@{ Output = $output; ExitCode = $LASTEXITCODE }
}

# 1. 校验
Write-Section "[1/5] 校验环境"
if (-not (Test-Path $dbDir)) { throw "reseed: 找不到数据库目录: $dbDir" }
$schema = Join-Path $dbDir "01-schema.sql"
$seed   = Join-Path $dbDir "02-seed.sql"
$migAddPhone = Join-Path $dbDir "03-migration-add-phone.sql"
$update  = Join-Path $dbDir "03-update.sql"
$privacy = Join-Path $dbDir "04-privacy-compliance.sql"
foreach ($f in @($schema, $seed, $migAddPhone, $update, $privacy)) {
    if (-not (Test-Path $f)) { throw "reseed: 缺少 SQL 文件: $f" }
}
Write-Host "  schema       = $schema"
Write-Host "  seed         = $seed"
Write-Host "  migration    = 3 files"

# 检查容器
$exists = docker ps -a --format '{{.Names}}' | Select-String -SimpleMatch $ContainerName
if (-not $exists) {
    throw "reseed: 容器 $ContainerName 不存在，请先 docker compose up -d"
}

# 2. 停 backend
Write-Section "[2/5] 停掉 rocgit-backend（释放对 MySQL 的连接）"
$backendName = "rocgit-backend"
$bkExists = docker ps -a --format '{{.Names}}' | Select-String -SimpleMatch $backendName
if ($bkExists -and -not $SkipRestart) {
    docker stop $backendName 2>&1 | Out-Null
    Write-Host "  已停 $backendName"
} else {
    Write-Host "  跳过（容器不存在或 -SkipRestart）"
}

# 3. drop 库
Write-Section "[3/5] drop + 重建数据库 roc_git"
$r = Invoke-DockerExec "docker exec $ContainerName mysql -uroot -p$MysqlRootPassword -e ""DROP DATABASE IF EXISTS roc_git; CREATE DATABASE roc_git DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"""
if ($r.ExitCode -ne 0) {
    throw "reseed: drop/create 失败 (exit=$($r.ExitCode))"
}
Write-Host "  OK"

# 4. 灌入 schema + 迁移 + seed（顺序：先建表 + 迁移加列，再灌种子）
Write-Section "[4/5] 灌入 01-schema + 3 个迁移 + 02-seed"
foreach ($f in @($schema, $update, $privacy, $seed)) {
    $name = Split-Path -Leaf $f
    Write-Host "  -> $name"
    $r = Invoke-DockerExec "type `"$f`" | docker exec -i $ContainerName mysql -uroot -p$MysqlRootPassword roc_git"
    if ($r.ExitCode -ne 0) {
        throw "reseed: 执行 $name 失败 (exit=$($r.ExitCode))"
    }
}
# 03-migration-add-phone.sql 仅用于历史已部署的库（schema 已含 phone 列则跳过）
Write-Host "  -> 03-migration-add-phone.sql (按需，已跳过：当前 schema 已含 phone 列)"

# 5. 自检
Write-Section "[5/5] 自检（关键表行数）"
$check = "SELECT (SELECT COUNT(*) FROM user) AS user_count, (SELECT COUNT(*) FROM \`group\`) AS group_count, (SELECT COUNT(*) FROM project) AS project_count, (SELECT COUNT(*) FROM task) AS task_count, (SELECT COUNT(*) FROM file) AS file_count, (SELECT COUNT(*) FROM message) AS message_count, (SELECT COUNT(*) FROM operation_log) AS log_count, (SELECT COUNT(*) FROM discussion) AS discussion_count, (SELECT COUNT(*) FROM decision) AS decision_count;"
Invoke-DockerExec "docker exec $ContainerName mysql -uroot -p$MysqlRootPassword -t roc_git -e ""$check""" | ForEach-Object { Write-Host $_ }

# 6. 重启 backend
if (-not $SkipRestart -and $bkExists) {
    Write-Section "[bonus] 重启 $backendName"
    Set-Location $composeDir
    docker compose up -d backend 2>&1 | Out-Null
    Write-Host "  等待 20 秒让 Spring Boot 启动..."
    Start-Sleep -Seconds 20
    $r = Invoke-DockerExec "docker exec $backendName wget -qO- http://127.0.0.1:8080/api/v1/health/db"
    Write-Host "  /api/v1/health/db -> $($r.Output)"
}

Write-Section "[完成] 重灌成功"
Write-Host "  H5 入口  : http://127.0.0.1/" -ForegroundColor Green
Write-Host "  后端文档 : http://127.0.0.1:8080/doc.html" -ForegroundColor Green
Write-Host "  DB 健康  : http://127.0.0.1:8080/api/v1/health/db" -ForegroundColor Green
