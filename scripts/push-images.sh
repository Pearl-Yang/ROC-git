#!/usr/bin/env bash
# ROC-git 本地手推镜像到云端仓库
#
# 用法：
#   ./scripts/push-images.sh -t 1.0.0                 # 默认 aliyun
#   ./scripts/push-images.sh -t 1.0.0 -r tencent      # 切到 tencent
#   ./scripts/push-images.sh -t 1.0.0 -r aliyun -n myns   # 自定义命名空间
#   ./scripts/push-images.sh -t latest -s frontend    # 只推 frontend
#
# 前置：
#   docker login registry.cn-hangzhou.aliyuncs.com   （或 tencent registry）
#   docker buildx 已启用（Docker Desktop 默认开启）

set -euo pipefail

CLOUD="aliyun"
TAG="latest"
NAMESPACE="rocgit"
SERVICES=("backend" "frontend")
PLATFORM="linux/amd64"

usage() {
  cat <<EOF
用法: $0 -t <tag> [-r aliyun|tencent] [-n namespace] [-s backend|frontend|both] [-p platform]

参数:
  -t TAG          镜像 tag（必填）
  -r CLOUD        aliyun（默认） / tencent
  -n NAMESPACE    Registry 下的命名空间（默认 rocgit）
  -s SERVICE      backend / frontend / both（默认 both）
  -p PLATFORM     docker buildx 平台（默认 linux/amd64，云端 ARM 可改 linux/arm64）
  -h              帮助
EOF
}

while getopts ":t:r:n:s:p:h" opt; do
  case $opt in
    t) TAG="$OPTARG" ;;
    r) CLOUD="$OPTARG" ;;
    n) NAMESPACE="$OPTARG" ;;
    s) SERVICES=("$OPTARG") ;;
    p) PLATFORM="$OPTARG" ;;
    h) usage; exit 0 ;;
    \?) echo "未知参数 -$OPTARG" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$TAG" ]]; then
  echo "ERROR: 必须指定 -t <tag>" >&2
  usage
  exit 1
fi

case "$CLOUD" in
  aliyun)  REGISTRY="registry.cn-hangzhou.aliyuncs.com" ;;
  tencent) REGISTRY="ccr.ccs.tencentyun.com" ;;
  *)
    echo "ERROR: 未知云 $CLOUD（仅支持 aliyun / tencent）" >&2
    exit 1
    ;;
esac

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

c_cyan()  { printf "\033[36m%s\033[0m\n" "$*"; }
c_green() { printf "\033[32m%s\033[0m\n" "$*"; }
c_red()   { printf "\033[31m%s\033[0m\n" "$*"; }

c_cyan "=========================================="
c_cyan "  ROC-git 镜像推送"
c_cyan "=========================================="
echo "  Registry  = $REGISTRY"
echo "  Namespace = $NAMESPACE"
echo "  Tag       = $TAG"
echo "  Services  = ${SERVICES[*]}"
echo "  Platform  = $PLATFORM"
echo ""

# 1) 检查 login
c_cyan "[1/3] 检查 docker login 状态"
if ! docker info 2>/dev/null | grep -q "Username"; then
  echo "  未检测到已登录的 registry，请先："
  echo "    docker login $REGISTRY"
  exit 1
fi
echo "  OK"

# 2) buildx 构建
c_cyan "[2/3] docker buildx 构建"
for SVC in "${SERVICES[@]}"; do
  if [[ "$SVC" == "both" ]]; then SVC="backend"; fi
  if [[ ! -d "$ROOT/$SVC" ]]; then
    c_red "  缺少目录 $ROOT/$SVC，跳过"
    continue
  fi
  LOCAL_TAG="rocgit/${SVC}:${TAG}"
  REMOTE_TAG="${REGISTRY}/${NAMESPACE}/${SVC}:${TAG}"

  echo ""
  echo "  -> 构建 $SVC ..."
  if [[ "$SVC" == "frontend" ]]; then
    docker buildx build \
      --platform "$PLATFORM" \
      --build-arg VITE_API_BASE=/api \
      -t "$LOCAL_TAG" \
      -t "$REMOTE_TAG" \
      --load \
      "$ROOT/$SVC"
  else
    docker buildx build \
      --platform "$PLATFORM" \
      -t "$LOCAL_TAG" \
      -t "$REMOTE_TAG" \
      --load \
      "$ROOT/$SVC"
  fi
done

# 3) push
c_cyan "[3/3] docker push"
for SVC in "${SERVICES[@]}"; do
  if [[ "$SVC" == "both" ]]; then SVC="backend"; fi
  REMOTE_TAG="${REGISTRY}/${NAMESPACE}/${SVC}:${TAG}"
  echo ""
  echo "  -> 推送 $REMOTE_TAG"
  docker push "$REMOTE_TAG"
done

c_green ""
c_green "=========================================="
c_green "  推送完成"
c_green "=========================================="
echo ""
echo "  镜像地址："
for SVC in "${SERVICES[@]}"; do
  if [[ "$SVC" == "both" ]]; then SVC="backend"; fi
  echo "    ${REGISTRY}/${NAMESPACE}/${SVC}:${TAG}"
done
echo ""
echo "  在云端用："
echo "    docker pull ${REGISTRY}/${NAMESPACE}/backend:${TAG}"
echo "    docker pull ${REGISTRY}/${NAMESPACE}/frontend:${TAG}"
