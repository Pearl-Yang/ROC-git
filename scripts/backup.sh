#!/usr/bin/env bash
# P3-4：MySQL 每日全量备份 + 上传文件归档
# 配合 cron：0 2 * * * /opt/rocgit/scripts/backup.sh

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/opt/rocgit/backup}"
DATA_DIR="${DATA_DIR:-/opt/rocgit/data}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-rocgit-mysql}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-root}"
MYSQL_DB="${MYSQL_DB:-roc_git}"
S3_BUCKET="${S3_BUCKET:-}"   # 留空则不上传 OSS，只本地留
RETAIN_DAYS="${RETAIN_DAYS:-7}"

DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p "$BACKUP_DIR"

# 1) MySQL 全量 dump
echo "[backup] MySQL dump..."
docker exec "$MYSQL_CONTAINER" mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASS" \
    --single-transaction --routines --triggers --events \
    "$MYSQL_DB" > "$BACKUP_DIR/db_${DATE}.sql"
gzip "$BACKUP_DIR/db_${DATE}.sql"

# 2) 上传文件归档
echo "[backup] files archive..."
tar czf "$BACKUP_DIR/files_${DATE}.tgz" -C "$DATA_DIR" files

# 3) 上传 OSS（可选）
if [[ -n "$S3_BUCKET" ]] && command -v aws >/dev/null 2>&1; then
    aws s3 cp "$BACKUP_DIR/db_${DATE}.sql.gz" "s3://${S3_BUCKET}/db/" || true
    aws s3 cp "$BACKUP_DIR/files_${DATE}.tgz" "s3://${S3_BUCKET}/files/" || true
fi

# 3.5) W2：若 ROCGIT_FILE_S3_ENDPOINT 配了，dump 时跳过 files（已迁移到 OSS），只上传 db
if [[ -n "${ROCGIT_FILE_S3_ENDPOINT:-}" ]]; then
    echo "[backup] 跳过本地 files 归档（OSS 已接管）"
    rm -f "$BACKUP_DIR/files_${DATE}.tgz"
fi

# 4) 清理过期
echo "[backup] cleanup (retain ${RETAIN_DAYS} days)..."
find "$BACKUP_DIR" -name "db_*.sql.gz" -mtime +${RETAIN_DAYS} -delete
find "$BACKUP_DIR" -name "files_*.tgz" -mtime +${RETAIN_DAYS} -delete

echo "[backup] done. Latest: $BACKUP_DIR/db_${DATE}.sql.gz + files_${DATE}.tgz"
