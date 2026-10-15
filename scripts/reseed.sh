#!/usr/bin/env bash
# ROC-git MySQL 一键重灌种子脚本（Linux / macOS / WSL）
#
# 用法：
#   ./scripts/reseed.sh
#   CONTAINER=rocgit-mysql PASS=root ./scripts/reseed.sh
#   CONTAINER=rocgit-mysql PASS=root SKIP_RESTART=1 ./scripts/reseed.sh
#
# 需要：本机已装 docker，并且 docker compose 可用

set -euo pipefail

CONTAINER="${CONTAINER:-rocgit-mysql}"
PASS="${PASS:-root}"
SKIP_RESTART="${SKIP_RESTART:-0}"
BACKEND_NAME="${BACKEND_NAME:-rocgit-backend}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_DIR="$ROOT/backend/database"

c_red()    { printf "\033[31m%s\033[0m\n" "$*"; }
c_green()  { printf "\033[32m%s\033[0m\n" "$*"; }
c_cyan()   { printf "\033[36m%s\033[0m\n" "$*"; }
section()  { printf "\n\033[36m=========================================\033[0m\n%s\n\033[36m=========================================\033[0m\n" "$*"; }

# 1) 校验
section "[1/5] 校验环境"
[[ -d "$DB_DIR" ]] || { c_red "找不到目录: $DB_DIR"; exit 1; }
for f in 01-schema.sql 02-seed.sql 03-migration-add-phone.sql 03-update.sql 04-privacy-compliance.sql; do
  [[ -f "$DB_DIR/$f" ]] || { c_red "缺少 SQL: $DB_DIR/$f"; exit 1; }
done
docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER" || {
  c_red "容器 $CONTAINER 不存在，请先 docker compose up -d"
  exit 1
}
echo "  DB 容器     = $CONTAINER"
echo "  SQL 目录    = $DB_DIR"

# 2) 停 backend
section "[2/5] 停掉 $BACKEND_NAME（释放对 MySQL 的连接）"
if docker ps -a --format '{{.Names}}' | grep -qx "$BACKEND_NAME"; then
  if [[ "$SKIP_RESTART" == "1" ]]; then
    echo "  SKIP_RESTART=1，跳过停容器"
  else
    docker stop "$BACKEND_NAME" >/dev/null
    echo "  已停 $BACKEND_NAME"
  fi
else
  echo "  容器 $BACKEND_NAME 不存在，跳过"
fi

# 3) drop 库
section "[3/5] drop + 重建数据库 roc_git"
docker exec "$CONTAINER" mysql -uroot -p"$PASS" \
  -e "DROP DATABASE IF EXISTS roc_git; CREATE DATABASE roc_git DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" \
  >/dev/null
echo "  OK"

# 4) 灌入 SQL
section "[4/5] 灌入 01-schema + 3 个迁移 + 02-seed（先建表加列，再灌种子）"
for f in 01-schema.sql 03-update.sql 04-privacy-compliance.sql 02-seed.sql; do
  echo "  -> $f"
  docker exec -i "$CONTAINER" mysql -uroot -p"$PASS" roc_git < "$DB_DIR/$f" >/dev/null
done
# 03-migration-add-phone.sql 仅用于已部署的旧库，reseed 不需要（schema 已含 phone 列）
echo "  -> 03-migration-add-phone.sql (跳过：当前 schema 已含 phone 列)"
echo "  全部 SQL 执行完毕"

# 5) 自检
section "[5/5] 自检（关键表行数）"
docker exec "$CONTAINER" mysql -uroot -p"$PASS" -t roc_git -e "
SELECT
  (SELECT COUNT(*) FROM user)              AS user_count,
  (SELECT COUNT(*) FROM \`group\`)         AS group_count,
  (SELECT COUNT(*) FROM project)           AS project_count,
  (SELECT COUNT(*) FROM task)              AS task_count,
  (SELECT COUNT(*) FROM file)              AS file_count,
  (SELECT COUNT(*) FROM message)           AS message_count,
  (SELECT COUNT(*) FROM operation_log)     AS log_count,
  (SELECT COUNT(*) FROM discussion)        AS discussion_count,
  (SELECT COUNT(*) FROM decision)          AS decision_count;
" 2>/dev/null

# 6) 重启 backend
if [[ "$SKIP_RESTART" != "1" ]] && docker ps -a --format '{{.Names}}' | grep -qx "$BACKEND_NAME"; then
  section "[bonus] 重启 $BACKEND_NAME"
  ( cd "$ROOT" && docker compose up -d backend >/dev/null )
  echo "  等待 15 秒让 Spring Boot 启动..."
  sleep 15
  HEALTH=$(docker exec "$BACKEND_NAME" wget -qO- http://127.0.0.1:8080/api/v1/health/db 2>/dev/null || true)
  echo "  /api/v1/health/db -> $HEALTH"
fi

section "[完成] 重灌成功"
c_green "  H5 入口  : http://127.0.0.1/"
c_green "  后端文档 : http://127.0.0.1:8080/doc.html"
c_green "  DB 健康  : http://127.0.0.1:8080/api/v1/health/db"
