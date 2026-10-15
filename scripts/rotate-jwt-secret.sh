#!/usr/bin/env bash
# P3-3：JWT secret 轮换脚本
# 用法：./scripts/rotate-jwt-secret.sh
#
# 步骤：
#   1. 生成新 secret（≥ 32 字节）
#   2. 更新 docker-compose / application.yml
#   3. 重启 backend
#   4. 老 token 全部失效，用户需重新登录（可接受，因为 secret 轮换本来就期望强制下线）

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_DIR/.env"
COMPOSE_FILE="$PROJECT_DIR/docker-compose.yml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo ".env not found at $ENV_FILE" >&2
  exit 1
fi

# 1) 生成新 secret
NEW_SECRET=$(openssl rand -base64 48 | tr -d '\n=' | cut -c1-48)
echo "[rotate] new JWT secret generated (length=${#NEW_SECRET})"

# 2) 写入 .env
if grep -q '^JWT_SECRET=' "$ENV_FILE"; then
  sed -i.bak "s|^JWT_SECRET=.*|JWT_SECRET=${NEW_SECRET}|" "$ENV_FILE"
else
  echo "JWT_SECRET=${NEW_SECRET}" >> "$ENV_FILE"
fi

# 3) 同步到 docker-compose environment
if grep -q 'JWT_SECRET' "$COMPOSE_FILE"; then
  sed -i.bak "s|JWT_SECRET:.*|JWT_SECRET: ${NEW_SECRET}|" "$COMPOSE_FILE"
else
  echo "  请手动在 $COMPOSE_FILE backend.environment 加 JWT_SECRET: ${NEW_SECRET}" >&2
fi

# 4) 重启 backend
cd "$PROJECT_DIR"
docker compose up -d --no-deps backend

echo "[rotate] backend 重启完毕，老 token 已失效。"
echo "[rotate] 用户下次请求会被 401 跳到登录页。"
