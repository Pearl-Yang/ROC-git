#!/usr/bin/env bash
# P3-4：版本回滚
# 用法：./scripts/rollback.sh v1.1

set -euo pipefail

VERSION="${1:-}"
if [[ -z "$VERSION" ]]; then
  echo "Usage: $0 <tag>" >&2
  echo "  e.g. $0 v1.1" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "[rollback] down current containers..."
docker compose down

echo "[rollback] checkout ${VERSION}..."
git fetch --tags
git checkout "$VERSION"

echo "[rollback] bring up ${VERSION} containers..."
docker compose up -d --build

echo "[rollback] done. Check health: curl http://127.0.0.1/api/v1/health"
