@echo off
chcp 65001 >nul
set DB_HOST=127.0.0.1
set DB_PORT=13307
set DB_NAME=roc_git
set DB_USER=root
set DB_PASSWORD=root

cd /d "d:\ProjectSpace\2026年上学年\软件设计实践\backend"

if not exist cp.txt (
    mvn -o dependency:build-classpath -Dmdep.outputFile=cp.txt -q
)

set /p CP=<cp.txt
set FULL_CP=target\classes;%CP%

java -cp "%FULL_CP%" com.rocgit.knife4j.Application --server.port=8090
