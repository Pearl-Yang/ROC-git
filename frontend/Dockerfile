# ROC-git 前端 Dockerfile
# 多阶段构建：Node 20 跑 npm run build:h5 → nginx 托管

# ============ 阶段 1：构建 ============
FROM node:20-alpine AS build
WORKDIR /app

# 构建期 API base：'/api' 走 nginx 反代到后端容器
ARG VITE_API_BASE=/api
ENV VITE_API_BASE=${VITE_API_BASE}

# 先复制依赖文件以最大化缓存
COPY package*.json ./
COPY .npmrc ./
RUN npm install --no-audit --no-fund

# 复制源码并构建
COPY . .
RUN npm run build:h5

# ============ 阶段 2：运行时 ============
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# uni-app H5 默认构建到 dist/build/h5
COPY --from=build /app/dist/build/h5/ ./

# 反代与单页 fallback
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]
