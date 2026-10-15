$env:DB_HOST = '127.0.0.1'
$env:DB_PORT = '13307'
$env:DB_NAME = 'roc_git'
$env:DB_USER = 'root'
$env:DB_PASSWORD = 'root'

Set-Location "d:\ProjectSpace\2026年上学年\软件设计实践\backend"

mvn -o dependency:build-classpath -Dmdep.outputFile=cp.txt -q 2>&1 | Out-Null
$cp = (Get-Content cp.txt -Raw).Trim()
$fullCp = "target/classes;$cp"

$logDir = "C:\Users\m1346\.cursor\projects\d-ProjectSpace-2026/terminals"
$proc = Start-Process -FilePath "java.exe" `
  -ArgumentList @("-cp", $fullCp, "com.rocgit.knife4j.Application", "--server.port=8090") `
  -RedirectStandardOutput "$logDir\backend2.log" `
  -RedirectStandardError "$logDir\backend2.err" `
  -PassThru -WindowStyle Hidden

Write-Host "Started PID $($proc.Id)"
