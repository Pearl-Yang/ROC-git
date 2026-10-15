#!/bin/sh
# 在 rocgit-backend 容器内运行
B=http://127.0.0.1:8080
echo "===== 1. login ====="
LOGIN=$(curl -s -X POST $B/api/v1/user/login -H 'Content-Type: application/json' --data-binary @/tmp/login.json)
echo "$LOGIN"
# 用 grep -o 提 token / uid
TOKEN=$(echo "$LOGIN" | grep -oE '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
UID=$(echo "$LOGIN" | grep -oE '"userId":[0-9]+' | head -1 | cut -d':' -f2)
H="Authorization: Bearer $TOKEN"
echo "uid=$UID token starts: $(echo $TOKEN | cut -c1-30)"

echo
echo "===== 2. create task (T-09 assign 提醒) ====="
echo '{"projectId":1,"stageId":2,"name":"T-09 smoke assign","assigneeId":1,"deadline":"2026-12-31 23:59:59"}' > /tmp/task.json
TASK=$(curl -s -X POST $B/api/v1/task/create -H 'Content-Type: application/json' -H "$H" --data-binary @/tmp/task.json)
echo "$TASK"

echo
echo "===== 3. list my messages (应能看到新一条 task_reminder) ====="
MSG=$(curl -s "$B/api/v1/message/list?pageNum=1&pageSize=20" -H "$H")
echo "$MSG"

echo
echo "===== 4. 直接查 message 表 ====="
docker exec rocgit-mysql mysql -uroot -proot roc_git -e "SELECT id, user_id, title, type, related_id, is_read FROM message WHERE type='task_reminder' ORDER BY id DESC LIMIT 5;" 2>&1 | grep -v "Warning\|World-writable"

echo
echo "===== 5. T-02 create share view_only ====="
echo '{"projectId":1,"permissionLevel":"view_only","expiresIn":"30d","needPassword":false,"password":""}' > /tmp/share.json
SHARE=$(curl -s -X POST $B/api/v1/share/create -H 'Content-Type: application/json' -H "$H" --data-binary @/tmp/share.json)
echo "$SHARE"
SHARE_TOKEN=$(echo "$SHARE" | grep -oE '"shareToken":"[^"]*"' | head -1 | cut -d'"' -f4)
SHARE_ID=$(echo "$SHARE" | grep -oE '"shareId":[0-9]+' | head -1 | cut -d':' -f2)
echo "shareId=$SHARE_ID shareToken=$SHARE_TOKEN"

echo
echo "===== 6. T-02 visitor view (view_only) ====="
V1=$(curl -s "$B/api/v1/share/visitor/$SHARE_TOKEN")
echo "$V1" | head -c 600
echo

echo
echo "===== 7. update share -> view_with_download ====="
echo '{"permissionLevel":"view_with_download"}' > /tmp/upd.json
curl -s -X PUT $B/api/v1/share/$SHARE_ID -H 'Content-Type: application/json' -H "$H" --data-binary @/tmp/upd.json
echo

echo
echo "===== 8. visitor view (view_with_download) 应有 files + url ====="
V2=$(curl -s "$B/api/v1/share/visitor/$SHARE_TOKEN")
echo "$V2" | head -c 800
echo

echo
echo "===== 9. update share password=1234 ====="
echo '{"needPassword":true,"password":"1234"}' > /tmp/upd2.json
curl -s -X PUT $B/api/v1/share/$SHARE_ID -H 'Content-Type: application/json' -H "$H" --data-binary @/tmp/upd2.json
echo

echo
echo "===== 10. visitor view no password -> 应 500/403 ====="
curl -s "$B/api/v1/share/visitor/$SHARE_TOKEN"
echo

echo
echo "===== 11. visitor view with password=1234 -> 应当成功 ====="
V3=$(curl -s "$B/api/v1/share/visitor/$SHARE_TOKEN?password=1234")
echo "$V3" | head -c 400
echo

echo
echo "===== 12. 等 65s 让 @Scheduled 跑（task 100 已 overdue 25h） ====="
echo "现在 message 表里 task_reminder 的总数："
docker exec rocgit-mysql mysql -uroot -proot roc_git -e "SELECT count(*) AS total, type FROM message WHERE type='task_reminder' GROUP BY type;" 2>&1 | grep -v "Warning\|World-writable"
echo "task_reminder 各类型（按 title 含 即将到期/已逾期/新任务）："
docker exec rocgit-mysql mysql -uroot -proot roc_git -e "SELECT id, title FROM message WHERE type='task_reminder' ORDER BY id DESC LIMIT 8;" 2>&1 | grep -v "Warning\|World-writable"

echo
echo "===== 13. 后端 log 看 reminder job 调度 ====="
docker logs rocgit-backend --tail 30 2>&1 | grep -E "TaskReminder|overdue|before_due" | tail -10

echo
echo "===== 14. task 100 的 reminder 表记录 ====="
docker exec rocgit-mysql mysql -uroot -proot roc_git -e "SELECT id, task_id, reminder_type, remind_at, is_sent, deleted FROM task_reminder WHERE task_id IN (100, (SELECT id FROM task WHERE title='T-09 smoke assign' ORDER BY id DESC LIMIT 1)) ORDER BY id DESC;" 2>&1 | grep -v "Warning\|World-writable"
