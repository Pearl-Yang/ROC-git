#!/usr/bin/env bash
# W2：本地文件 → OSS / COS / S3 迁移脚本
#
# 用法：
#   1) 在 ECS 上配置 .env.cloud 里的 ROCGIT_FILE_S3_* 凭证
#   2) export $(grep -v '^#' .env.cloud | xargs)  # 注入到当前 shell
#   3) ./scripts/migrate-local-to-oss.sh
#
# 行为：
#   - 扫 ${DATA_DIR:-/opt/rocgit/data}/files/yyyyMM/
#   - 并发上传到 OSS（每并发 4 个）
#   - 写一份 local-to-oss.map.csv（行格式：local_relpath,oss_key,size,etag）
#   - 完成后建议：
#       ROCGIT_FILE_PROVIDER=s3
#       docker compose -f docker-compose.cloud.yml --env-file .env.cloud up -d
#   - 老的本地文件不要立即删，先保留 7 天观察
#
# 依赖：aws cli（aliyun oss / 腾讯云 cos 都兼容 S3 协议）
#   pip install awscli  或  apt install -y awscli
#   aws configure  填入阿里云 RAM 子账号 AccessKey

set -euo pipefail

DATA_DIR="${DATA_DIR:-/opt/rocgit/data}"
OSS_ENDPOINT="${ROCGIT_FILE_S3_ENDPOINT:-}"
OSS_BUCKET="${ROCGIT_FILE_S3_BUCKET:-}"
OSS_ACCESS_KEY="${ROCGIT_FILE_S3_ACCESS_KEY:-}"
OSS_SECRET_KEY="${ROCGIT_FILE_S3_SECRET_KEY:-}"
OSS_REGION="${ROCGIT_FILE_S3_REGION:-oss-cn-shanghai}"
OSS_PATH_PREFIX="${ROCGIT_FILE_S3_PATH_PREFIX:-rocgit/}"
MAP_FILE="${MAP_FILE:-/opt/rocgit/backup/local-to-oss.map.csv}"
CONCURRENCY="${CONCURRENCY:-4}"

if [[ -z "$OSS_ENDPOINT" || -z "$OSS_BUCKET" || -z "$OSS_ACCESS_KEY" || -z "$OSS_SECRET_KEY" ]]; then
  echo "[migrate] 缺少 OSS 凭证，请 export ROCGIT_FILE_S3_* 后再跑" >&2
  exit 2
fi

if ! command -v aws >/dev/null 2>&1; then
  echo "[migrate] 找不到 aws cli，先安装：pip install awscli" >&2
  exit 3
fi

export AWS_ACCESS_KEY_ID="$OSS_ACCESS_KEY"
export AWS_SECRET_ACCESS_KEY="$OSS_SECRET_KEY"
export AWS_DEFAULT_REGION="$OSS_REGION"

FILES_ROOT="$DATA_DIR/files"
if [[ ! -d "$FILES_ROOT" ]]; then
  echo "[migrate] 本地文件目录不存在：$FILES_ROOT" >&2
  exit 4
fi

mkdir -p "$(dirname "$MAP_FILE")"
echo "local_relpath,oss_key,size,etag" > "$MAP_FILE"

TOTAL=0
UPLOADED=0
SKIPPED=0
FAILED=0

upload_one() {
  local f="$1"
  local rel="${f#$FILES_ROOT/}"
  local key="${OSS_PATH_PREFIX%/}/${rel}"
  local size
  size=$(stat -c %s "$f" 2>/dev/null || stat -f %z "$f")
  # 已存在则跳过（基于 size/etag 简单判断）
  if grep -q "^${rel}," "$MAP_FILE" 2>/dev/null; then
    SKIPPED=$((SKIPPED+1))
    return
  fi
  if aws --endpoint-url "$OSS_ENDPOINT" s3 cp "$f" "s3://${OSS_BUCKET}/${key}" \
        --only-show-errors 2>/dev/null; then
    echo "${rel},${key},${size},ok" >> "$MAP_FILE"
    UPLOADED=$((UPLOADED+1))
  else
    echo "${rel},${key},${size},FAIL" >> "$MAP_FILE"
    FAILED=$((FAILED+1))
    echo "[migrate] 上传失败：$f" >&2
  fi
}

export -f upload_one
export FILES_ROOT OSS_PATH_PREFIX OSS_BUCKET OSS_ENDPOINT MAP_FILE

# 收集所有待迁移文件
mapfile -t ALL_FILES < <(find "$FILES_ROOT" -type f)
TOTAL=${#ALL_FILES[@]}
echo "[migrate] 共发现 $TOTAL 个文件，并发 $CONCURRENCY 准备上传到 $OSS_BUCKET ..."

# 用 xargs 简单并发
printf '%s\n' "${ALL_FILES[@]}" | xargs -I{} -P "$CONCURRENCY" bash -c 'upload_one "$@"' _ {}

echo "[migrate] 完成：上传 $UPLOADED 成功 / 跳过 $SKIPPED / 失败 $FAILED / 总计 $TOTAL"
echo "[migrate] 映射文件：$MAP_FILE"
echo "[migrate] 接下来请修改 .env.cloud：ROCGIT_FILE_PROVIDER=s3，然后 docker compose up -d"
