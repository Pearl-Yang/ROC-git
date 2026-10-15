#!/usr/bin/env bash
# W4：阿里云 DNS 校验的泛域名证书签发脚本
#
# 用法：
#   1) 阿里云控制台 → RAM 访问控制 → 创建用户，给 AliyunDNSFullAccess 权限，拿到 AccessKey
#   2) export ALIYUN_ACCESS_KEY_ID=<id>
#      export ALIYUN_ACCESS_KEY_SECRET=<secret>
#   3) ./scripts/ssl-issue.sh -d rocgit.example.com -d "*.rocgit.example.com"
#
# 产物：./ssl/cert.pem + ./ssl/key.pem，自动 reload nginx。

set -euo pipefail

DOMAINS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -d) DOMAINS+=("$2"); shift 2;;
    *)  echo "unknown arg: $1" >&2; exit 1;;
  esac
done

if [[ ${#DOMAINS[@]} -eq 0 ]]; then
  echo "用法：$0 -d <domain> [-d <domain> ...]" >&2
  exit 2
fi

if [[ -z "${ALIYUN_ACCESS_KEY_ID:-}" || -z "${ALIYUN_ACCESS_KEY_SECRET:-}" ]]; then
  echo "请先 export ALIYUN_ACCESS_KEY_ID / ALIYUN_ACCESS_KEY_SECRET" >&2
  exit 3
fi

if ! command -v certbot >/dev/null 2>&1; then
  echo "[ssl] 找不到 certbot，先装：apt install -y certbot python3-certbot-dns-aliyun" >&2
  exit 4
fi

# 写临时 credentials
mkdir -p ~/.secrets
cat > ~/.secrets/aliyun.ini <<EOF
dns_aliyun_access_key_id = ${ALIYUN_ACCESS_KEY_ID}
dns_aliyun_access_key_secret = ${ALIYUN_ACCESS_KEY_SECRET}
EOF
chmod 600 ~/.secrets/aliyun.ini

DOMAIN_ARGS=()
for d in "${DOMAINS[@]}"; do DOMAIN_ARGS+=(-d "$d"); done

certbot certonly --authenticator dns-aliyun --dns-aliyun-credentials ~/.secrets/aliyun.ini \
  --non-interactive --agree-tos -m admin@${DOMAINS[0]} \
  "${DOMAIN_ARGS[@]}"

# 复制到 ./ssl
mkdir -p ./ssl
FIRST="${DOMAINS[0]}"
if [[ "$FIRST" == \*.* ]]; then FIRST="${FIRST#\*.}"; fi
cp /etc/letsencrypt/live/${FIRST}/fullchain.pem ./ssl/cert.pem
cp /etc/letsencrypt/live/${FIRST}/privkey.pem ./ssl/key.pem
chmod 644 ./ssl/cert.pem
chmod 600 ./ssl/key.pem

# 续期 hook：自动复制到 ./ssl
HOOK="/etc/letsencrypt/renewal-hooks/deploy/rocgit-copy.sh"
sudo mkdir -p /etc/letsencrypt/renewal-hooks/deploy
sudo tee "$HOOK" >/dev/null <<EOF
#!/bin/bash
set -e
FIRST="${FIRST}"
cp /etc/letsencrypt/live/\${FIRST}/fullchain.pem ${PWD}/ssl/cert.pem
cp /etc/letsencrypt/live/\${FIRST}/privkey.pem  ${PWD}/ssl/key.pem
chmod 644 ${PWD}/ssl/cert.pem
chmod 600 ${PWD}/ssl/key.pem
docker compose -f ${PWD}/docker-compose.cloud.yml --env-file ${PWD}/.env.cloud exec frontend nginx -s reload || true
EOF
sudo chmod +x "$HOOK"

echo "[ssl] 证书已生成：./ssl/cert.pem + ./ssl/key.pem"
echo "[ssl] 自动续期 hook：$HOOK"
