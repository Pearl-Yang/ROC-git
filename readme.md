# ROC-git · 课程小组文档协作管理平台

**软件系统分析与设计综合实践** 课程项目。uni-app + Spring Boot + MySQL 全栈，大学生以小组为单位协作完成课程作业，平台统一管理资料文件、版本、任务分工、研讨决策与审核归档。

本仓库已按"前后端分离 + Docker 一键编排"重新组织：

- `backend/`：Spring Boot + Knife4j 后端（已接入 MyBatis-Plus，可连 MySQL 8.0）
- `frontend/`：uni-app + Vue 3 前端（支持 H5 / 微信小程序 / APP 三端）
- `docker-compose.yml`：顶层编排，一键起 MySQL + 后端 + 前端 nginx
- `doc/`：课程报告 / 需求文档 / mermaid 工具
- `scripts/`：迁移与验证脚本
- `快速UI/`：原 HTML 备份（迁移参考，不参与构建）
- `SWAGGER_GUIDE.md`：Knife4j 使用与部署

## 一、一键启动（Docker 推荐）

前置：Docker Desktop 或 Docker Engine 24+。

```bash
# 在仓库根目录
docker compose up -d --build
# 等待 ~60s（首次构建含 Maven 依赖下载 + MySQL 启动 + schema 初始化）
docker compose ps
docker compose logs -f backend
```

启动后访问：

| 地址 | 内容 |
| --- | --- |
| http://127.0.0.1/ | uni-app H5 前端（docker 内 nginx 托管） |
| http://127.0.0.1/api/v1/health | 后端健康检查 |
| http://127.0.0.1/api/v1/health/db | 后端 + MySQL 连通性（含 user/group/project 表行数） |
| http://127.0.0.1:8080/doc.html | Knife4j 接口文档（中文 UI） |
| http://127.0.0.1:3306 | MySQL（root/root，库名 roc_git） |

关闭：

```bash
docker compose down            # 保留数据卷
docker compose down -v         # 同时清掉 MySQL 数据卷
```

## 一.5、上云部署（阿里云 / 腾讯云）

```bash
# 1) 修改 .env.cloud.example 里的占位符
cp .env.cloud.example .env.cloud
$EDITOR .env.cloud

# 2) 推镜像到阿里云 ACR
./scripts/push-images.sh -t 1.0.0          # 本地手推
# 或：git tag v1.0.0 && git push origin v1.0.0   （GitHub Actions 自动）

# 3) ECS 上 docker compose 一键起
docker compose -f docker-compose.cloud.yml --env-file .env.cloud up -d
```

详细步骤、镜像仓库、备案、SSL、监控、备份见 [doc/云端部署.md](doc/云端部署.md)。

## 二、目录结构

```
软件设计实践/
├── backend/                           # Spring Boot 后端
│   ├── Dockerfile                     #   多阶段：maven 编译 → jre 17 运行
│   ├── docker/conf/my.cnf             #   MySQL 字符集 / 时区配置
│   ├── database/
│   │   ├── schema.sql                 #   MySQL 8.0 建库建表（22 张）
│   │   ├── seed.sql                   #   阶段模板 seed
│   │   └── README.md                  #   表清单 / 状态机 / 接续开发
│   ├── src/main/java/com/rocgit/knife4j/
│   │   ├── Application.java           #   启动类
│   │   ├── config/MybatisPlusConfig   #   MyBatis-Plus + MapperScan
│   │   ├── entity/                    #   User / Group / Project 等 MyBatis 实体
│   │   ├── mapper/                    #   BaseMapper 子接口
│   │   ├── controller/                #   146 个 REST 接口 + DbHealthController
│   │   ├── dto/                       #   业务 DTO
│   │   ├── service/                   #   业务 service（如有）
│   │   ├── common/                    #   Result 通用返回
│   │   └── util/                      #   JwtUtil 等
│   ├── src/main/resources/application.yml
│   ├── openapi.yaml                   #   OpenAPI 3.0 规范
│   ├── pom.xml
│   └── README.md / API_*.md
│
├── frontend/                          # uni-app + Vue 3 前端
│   ├── Dockerfile                     #   node 20 构建 → nginx 1.27 托管
│   ├── nginx.conf                     #   /api 反代到后端 + 单页 fallback
│   ├── .env.development               #   VITE_API_BASE=/api
│   ├── .env.production                #   VITE_API_BASE=/api
│   ├── package.json                   #   含 dev:h5 / build:h5 / dev:mp-weixin / build:mp-weixin / dev:app / build:app
│   ├── vite.config.js
│   ├── index.html
│   ├── .npmrc
│   ├── src/
│   │   ├── pages/{home,project,create,meeting,messages,profile,login}/*.vue
│   │   ├── components/NativeFragment.vue
│   │   ├── components/NativeDate.vue
│   │   ├── utils/api.js               #   真实接口请求层（BASE_URL 走环境变量）
│   │   ├── utils/bind-api.js          #   页面数据绑定
│   │   ├── utils/page-controller.js
│   │   ├── utils/avatar.js
│   │   ├── controllers/*.js
│   │   ├── data/*.js
│   │   ├── styles/
│   │   ├── App.vue
│   │   ├── main.js
│   │   ├── pages.json
│   │   └── manifest.json              #   微信 AppID、平台配置
│   └── scripts/                       #   迁移 / 校验脚本（如有）
│
├── docker-compose.yml                 # 顶层编排：mysql + backend + frontend-nginx
├── doc/                               # 报告 / mermaid / 课程材料
├── scripts/                           # 仓库级迁移与验证脚本
├── 快速UI/                            # 原 HTML 备份（迁移参考）
├── SWAGGER_GUIDE.md
├── 需求规格说明书.md
├── 报告参考模板.docx.md
└── readme.md                          # 本文件
```

## 三、后端（backend/）

### 1. 后端原生开发（不通过 Docker）

```bash
cd backend
# 1) 先本地起一个 MySQL（也可以是 docker run mysql）
# 2) 导入库
mysql -uroot -p < database/schema.sql
mysql -uroot -p < database/seed.sql
# 3) 配置环境变量（PowerShell 示例）
$env:DB_HOST="127.0.0.1"; $env:DB_USER="root"; $env:DB_PASSWORD="root"
# 4) 启动
mvn clean package -DskipTests
java -jar target/knife4j-spring-1.0.0.war
```

访问：
- Knife4j 文档：http://localhost:8080/doc.html
- DB 健康检查：http://localhost:8080/api/v1/health/db
- OpenAPI JSON：http://localhost:8080/v3/api-docs

### 1.1 纯本地（无 Docker）联调

如果你想完全脱离 Docker 直接联调后端与 MySQL（推荐用于开发期快速验证）：

```powershell
# 1) 启动一个本地 MySQL（也可以用现成的端口，如 3306 / 13306 / 13307 等）
docker run -d --name rocgit-mysql-test `
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=roc_git `
  -p 13307:3306 `
  -v "${PWD}\backend\database:/docker-entrypoint-initdb.d:ro" `
  mysql:8.0

# 2) 启动后端（指向本地 MySQL）
cd backend
$env:DB_HOST="127.0.0.1"; $env:DB_PORT="13307"
$env:DB_NAME="roc_git"; $env:DB_USER="root"; $env:DB_PASSWORD="root"
java -jar target/knife4j-spring-1.0.0.war --server.port=8090

# 3) 验证
curl http://127.0.0.1:8090/api/v1/health/db
```

### 2. 后端技术栈

- Spring Boot 2.7.18（JDK 17 编译运行）
- Knife4j 4.4.0（OpenAPI 3 中文增强 UI）
- MyBatis-Plus 3.5.5 + mysql-connector-j（**新增**：连接 Docker MySQL）
- JWT 0.11.5（HS256，JwtUtil）
- Lombok 1.18.30

### 3. 数据库

- 库名 `roc_git`，MySQL 8.0，InnoDB，utf8mb4
- 22 张表（user / group / group_member / project / stage / stage_template / task / file / file_version / file_review / discussion / decision / achievement / achievement_file / announcement / announcement_read / message / user_subscription / share / share_access_log / operation_log / archive）
- 状态机枚举：`file.status`、`project.status`、`task.status`、`user.role` 等（详见 `backend/database/README.md` 第四节）

## 四、前端（frontend/）

### 1. H5 本地开发

```bash
cd frontend
npm install
npm run dev:h5
# 默认 http://127.0.0.1:5173/
```

### 2. 微信小程序产物

```bash
cd frontend
npm run build:mp-weixin
# 产物在 frontend/dist/build/mp-weixin
# 用微信开发者工具「导入项目」→ 选该目录
```

在 `src/manifest.json` 填写真实 `mp-weixin.appid`（以 `wx` 开头），发布前需在微信公众平台配置 HTTPS 合法域名。

### 3. APP 产物（APK）

```bash
cd frontend
npm run build:app
# 产物在 frontend/dist/build/app-plus
# 用 HBuilderX「发行 → 原生 APP-云打包」或自行签名后产出 APK
```

### 4. 前端技术栈

- uni-app 3.0 + Vue 3.4 + Vite 5
- Tailwind 3.4 + 自研 PostCSS（保留原型 rem → px 转换）
- @dcloudio 全家桶（uni-app / uni-h5 / uni-mp-weixin）
- 真实接口请求层 `src/utils/api.js`（基于 uni.request，自动管理 JWT）

## 五、MuMu 模拟器调试

前置：MuMu 模拟器 12+（自带 adb），或单独安装 adb。

### 方式 A：H5 直接进 MuMu 浏览器（最省事）

```powershell
# 1) 启动 Docker 编排
docker compose up -d --build

# 2) adb reverse：让模拟器能访问宿主机的 80 / 8080
adb -s 127.0.0.1:7555 reverse tcp:80 tcp:80
adb -s 127.0.0.1:7555 reverse tcp:8080 tcp:8080
# MuMu 默认端口是 7555；多实例时也可能是 5555 / 5555+

# 3) 在 MuMu 内置浏览器打开
# http://127.0.0.1/
```

> MuMu 12+ 的 adb 端口可在「设置 → 关于 → ADB 调试端口」查看，或在 MuMu 安装目录下 `MuMuPlayer/api/adb.exe` 自带 adb。

### 方式 B：H5 dev server + adb reverse（带热更新）

```powershell
cd frontend
npm run dev:h5
# 另一个终端：
adb -s 127.0.0.1:7555 reverse tcp:5173 tcp:5173
adb -s 127.0.0.1:7555 reverse tcp:8080 tcp:8080
# MuMu 浏览器打开 http://127.0.0.1:5173/
```

### 方式 C：APK 安装到 MuMu

```powershell
cd frontend
npm run build:app
# 把 dist/build/app-plus 目录用 HBuilderX 云打包成 APK，或自行签名
adb -s 127.0.0.1:7555 install -r app.apk
adb -s 127.0.0.1:7555 shell am start -n 包名/.MainActivity
```

> APK 模式的 `BASE_URL` 默认仍是 `/api`（在 APK 内不通过 nginx 转发，会失败）。如要 APK 走真后端，请改 `frontend/.env.production` 为 `VITE_API_BASE=http://127.0.0.1:8080/api`（搭配 `adb reverse tcp:8080 tcp:8080`），重新 `npm run build:app`。

### 方式 D：微信小程序 + MuMu（当 H5 用）

```powershell
cd frontend
npm run build:mp-weixin
# 产物 frontend/dist/build/mp-weixin → 微信开发者工具「导入项目」
# 「真机调试」扫码，MuMu 模拟器里的微信扫码即可
```

## 六、验证清单

```powershell
# Docker 起来后
curl http://127.0.0.1/api/v1/health
curl http://127.0.0.1/api/v1/health/db

# 浏览器
# http://127.0.0.1/                   前端 H5
# http://127.0.0.1:8080/doc.html      接口文档
# http://127.0.0.1:3306                MySQL（root/root）
```

### 端到端真实数据验证（已通过）

```powershell
# 1) 健康检查
curl http://127.0.0.1:8080/api/v1/health/db
# 预期：{"projectCount":3,"userCount":5,"groupCount":2,"db":"ok"}

# 2) 登录获取 token（用 seed 里的演示用户）
curl -X POST http://127.0.0.1:8080/api/v1/user/login \
  -H "Content-Type: application/json" \
  -d '{"code":"oTest_001_leader","nickname":"李明","avatarUrl":"https://api.dicebear.com/7.x/avataaars/svg?seed=li"}'
# 预期：返回 {"token":"eyJ...","userId":1,"nickname":"李明",...}

# 3) 用 token 拉取真实数据
TOKEN=eyJhbGciOiJIUzI1NiJ9...
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/group/my-list
curl -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/v1/project/list?groupId=1"
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/task/my-tasks
curl -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/v1/file/list?projectId=1"
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/message/list
curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/group/1/members
```

预期返回都是真实数据（来自 MySQL 的 5 个用户、2 个小组、3 个项目、5 份文件、6 个任务、5 条消息等）。

### 三端构建产物

```powershell
cd frontend
npm run build:h5          # → dist/build/h5         (单页 H5，nginx 托管)
npm run build:mp-weixin   # → dist/build/mp-weixin  (微信开发者工具导入)
npm run build:app         # → dist/build/app        (HBuilderX 发行 → APK)
```

### 三端 BASE_URL 配置

| 平台 | 配置文件 | `VITE_API_BASE` |
| --- | --- | --- |
| H5 dev (HMR) | `frontend/.env.development` | `/api`（建议 `http://127.0.0.1:8080/api` 配合 `adb reverse`） |
| H5 prod (nginx) | `frontend/.env.production` | `/api` |
| 微信小程序开发 | `frontend/.env.mp-weixin` | `http://127.0.0.1:8080/api`（配合 `urlCheck: false`） |
| Android APK | `frontend/.env.app` | `http://10.0.2.2:8080/api`（模拟器访问宿主）/ 真机改宿主 IP |

## 七、常见问题

| 现象 | 处理 |
| --- | --- |
| `docker compose up` 后 backend 一直重启 | 看 `docker compose logs backend`；多半是 MySQL 还没初始化完，等 ~60s |
| MuMu 浏览器打不开 `127.0.0.1` | 改用宿主 IP（`ipconfig` 看 IPv4）；或确认 `adb reverse` 已成功 |
| `npm run dev:h5` 报 `npm.ps1 被策略阻止` | 改用 `npm.cmd` |
| Docker 拉镜像慢 | 配置镜像加速器（`~/.docker/daemon.json` 配 `registry-mirrors`） |
| Maven 拉依赖慢 | 在 `~/.m2/settings.xml` 配阿里云镜像 |
| 短信验证码收不到 / 报错 | 默认走 `LogSmsProvider`，验证码会写到后端控制台 + `backend/logs/sms-YYYY-MM-DD.log`；在 `application.yml` 设置 `sms.provider=tencent` 切真发（需填 `sms.tencent.secret-id/secret-key/app-id/template-id` 或环境变量 `TENCENT_SMS_*`）。credential 缺失时接口会返回 400 + 详细错误。 |
| 401 / token 过期 | 前端会自动跳登录页；如持续出现，确认 `frontend/.env.development` 的 `VITE_API_BASE` 指向正确的后端；多端可参考 `readme.md` 「三端 BASE_URL 配置」一节。 |
| 上传 413 / 失败 | 单文件大小由 `spring.servlet.multipart.max-file-size` 控制（默认 100MB）；可在 `application.yml` 调整。 |

## 八、参考

- uni-app 官方 CLI：https://uniapp.dcloud.net.cn/quickstart-cli
- Knife4j 官方文档：https://doc.xiaominfo.com/
- MyBatis-Plus 官方文档：https://baomidou.com/
- MuMu 模拟器：https://mumu.163.com/

## 九、v1.1 改造：三端可运行、数据全走 MySQL

> 实施依据：`~/.cursor/plans/完善前后端数据库_0be21107.plan.md`

### 1. 改造范围

- **后端**：16 个 Controller 由 mock 改写为调 Service，16 个 Service 接口 + 实现全部接入 MyBatis-Plus；新增 19 个 Entity / Mapper；新增 `JwtInterceptor` + `WebConfig` + `CorsConfig` + `MybatisPlusMetaConfig` + `UserContext`。
- **数据库**：`backend/database/01-schema.sql` + `02-seed.sql`（含 `INSERT IGNORE`），所有 22 张表数据来自 seed，启动时不再重跑。
- **前端**：7 个 Vue 页面全部用 `<script setup>` 重写为响应式 + 真实 API 调用；新增 `src/utils/user-store.js`；`api.js` 改用 `import.meta.env.VITE_API_BASE`。
- **多端**：`pages.json` 启用 4 tab `tabBar`（首页/项目/消息/我的），移除各页 `disableScroll: true`；`manifest.json` `mp-weixin.setting.urlCheck: false`；H5 dev 走 Vite proxy `/api -> 127.0.0.1:8080`。

### 2. 关键技术决策

| 项 | 方案 |
| --- | --- |
| JDK | 17（Knife4j 4.4 + springdoc 4.x 强制） |
| MyBatis-Plus | 3.5.5 + `mybatis-plus-jsqlparser`（分页必需） |
| 鉴权 | JWT（jjwt 0.11.5）+ `HandlerInterceptor` 注入 `userId` |
| 三端 API 地址 | H5 `/api` (nginx 反代)；小程序 `urlCheck=false` 直连 `127.0.0.1:8080`；APK `10.0.2.2:8080` / 真机宿主 IP |
| 数据初始化 | Docker 启动时挂载 `01-schema.sql` + `02-seed.sql`；应用重启不再 DDL |

### 3. 验证清单

| 检查项 | 命令 / 路径 | 预期 |
| --- | --- | --- |
| 后端编译 | `cd backend && mvn -B clean package -DskipTests` | `BUILD SUCCESS`，生成 `target/knife4j-spring-1.0.0.war` |
| DB 健康 | `curl http://127.0.0.1:8080/api/v1/health/db` | `{ "userCount":..., "groupCount":..., "projectCount":..., "db":"ok" }` |
| 登录 | `curl -X POST http://127.0.0.1:8080/api/v1/user/login -H 'Content-Type: application/json' -d '{"nickname":"林知行","code":"wx_user_dev_demo"}'` | `{ "token":"eyJ...", "userId":..., "nickname":"林知行" }` |
| 个人中心 | `curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/user/profile` | 真实数据 |
| 小组列表 | `curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/group/my-list` | 至少 1 条 |
| 项目列表 | `curl -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/v1/project/list?groupId=1"` | 3 条（社区调研/统计学/校园文化） |
| 任务列表 | `curl -H "Authorization: Bearer $TOKEN" "http://127.0.0.1:8080/api/v1/task/list?projectId=1"` | 5+ 条 |
| H5 构建 | `cd frontend && npm run build:h5` | `dist/build/h5/index.html` 生成 |
| 小程序构建 | `cd frontend && npm run build:mp-weixin` | `dist/build/mp-weixin/` 生成 |
| 入口页跳转 | 浏览器打开 `http://127.0.0.1/` | 自动跳登录页；登录后跳首页且加载 3 个项目卡片 |

### 4. 常见问题 v1.1 新增

| 现象 | 处理 |
| --- | --- |
| `mvn package` 报 Java 版本错误 | 确认本地 JDK 是 17（`java -version`）；Dockerfile 已用 `maven:3.9-eclipse-temurin-17` |
| 登录后端 401 | 看 `docker compose logs backend`；多半是 seed 没跑（`02-seed.sql` 在 `backend/database/`） |
| H5 dev 跨域 / 401 | Vite dev proxy 已配 `/api -> 127.0.0.1:8080`；访问 `http://127.0.0.1:5173/` |
| 微信开发者工具「不在以下 request 合法域名列表中」 | `manifest.json` 已设 `urlCheck: false`；同时勾选工具内「不校验合法域名」 |
| APK 在真机访问不到 8080 | 真机改宿主 IP（`ipconfig`），并把 `frontend/.env.production` 改成 `VITE_API_BASE=http://<宿主IP>:8080/api` 重新 build |
| 数据库每次启动都被重建 | 已把 `application.yml` 的 `spring.sql.init.mode` 改为 `never`，避免重跑 seed |

## 十、v1.2 · 生产交付加固

> 实施依据：`~/.cursor/plans/roc-git_生产交付计划_ad5f68bf.plan.md`

### 1. 变更日志

- **P0 前端 demo→真接口**
  - `pages/files/files.vue`：`myGroups` → `projectList` → `fileList` 串联，全量文件由后端返，按 `status` 切 `hiddenFiles`；审核/驳回走 `batchAudit` / `reject`。
  - `pages/view/view.vue`：去掉手写 `uni.request`，改 `api.shareVisitorView` + `api.shareRecordVisit`；修复 `f.fileId` 字段不一致。
  - `pages/project/project.vue`：`members` 走 `api.groupMembers`（按后端 `{list}` 解析）；`tasks` 走 `api.taskList`；`versionHistory` 走 `api.fileVersions`；项目操作走 `projectComplete / projectCopy / projectDelete`。
  - `pages/meeting/meeting.vue`：删 `demoDecisions / demoParticipants / demoUpdates`；决策走 `api.decisionList`、动态走 `api.projectActivities`、成员走 `api.groupMembers`、新建调 `api.discussionCreate`。
  - `pages/home/home.vue`：`pendingFiles` 取 `messageList`，`recentFiles` 聚合各项目 `fileList`，`activityFeed` 聚合 `projectActivities`；`groupMembers` 解析 `.list` 修正。
  - `components/Icon.vue`：补 `upload-cloud / file-search / plus-circle / file-up / file-check / file-spreadsheet / file-type / check-circle-2 / git-compare / scan / archive / maximize-2 / building-2 / clipboard-list` 等 emoji 兜底。
  - `utils/api.js`：新增 `discussionCreate / decisionCreate`。

- **P1 后端补完**
  - `MessageService.subscribe / unsubscribe` 真写 `subscription_message` 表（去重、续期 30 天）；`subscriptions` 真查表。
  - `UserService.refreshToken` 用 `JwtUtil.sign` 真重签；`bindWechat` 走 `user.openid` 绑定 + 去重（占用时把旧账号 openid 改成 `unbound_xxx`）。
  - `FileController.publicDownload` 强制要求 `shareToken`，并校验 `permissionLevel=view_with_download` 才允许拿真文件。
  - 新增 `config/CorsConfig` 显式声明（开发 `*`，生产 P3-1 收紧）。
  - `TencentSmsProvider`：credential 缺失时改为抛 `BusinessException(BAD_REQUEST)`，前端能看到具体缺哪个 secret。

- **P2 文档 + CI**
  - 新增 `doc/架构图.md`（C4 L1/L2 + 登录/上传/审核/分享/访客 序列图 + ER 概览）。
  - 新增 `doc/API总览.md`（按 17 个 Controller 分组、鉴权要求、错误码规范）。
  - 新增 `doc/部署手册.md`（Docker 一键、外部 Tomcat、阿里云镜像、npm 镜像、三端 baseURL、环境变量、数据卷、备份、回滚）。
  - 新增 `.github/workflows/backend.yml`（`mvn verify` on push/PR，缓存 `.m2`）。
  - 新增 `.github/workflows/frontend.yml`（`npm ci + build:h5 + build:mp-weixin`，缓存 `~/.npm`）。

- **P3 生产硬化（计划已列出，本仓库可逐项落地）**
  - HTTPS / 域名 / mp-weixin 合法域名 / Let's Encrypt 自动续期
  - actuator + Prometheus + Grafana + 告警
  - JWT 轮换脚本 + rate-limit（nginx + Bucket4j）+ 上传校验
  - 备份 mysqldump → OSS + `./data/files` 切 S3/OSS + 灰度 + 回滚脚本
  - 用户协议 / 隐私政策 + 注销账号（软删 30 天回收）+ 数据导出 + `login_history.ip` 30 天脱敏

### 2. 测试

`backend/src/test` 当前包含 6 个测试类共 45+ 用例：

| 类 | 用例数 |
| --- | --- |
| `I18nUtilTest` | 5 |
| `ResultCodeTest` | 3 |
| `ContributionStatsServiceTest` | 2 |
| `FileServiceTest` | 7 |
| `UserServiceTest` | 10 |
| `ShareServiceTest` | 5 |
| `WebConfigTest` | 2 |
| `JacksonConfigTest` | 5 |
| `ProjectServiceTest` | 6 |
| `PreviewServiceTest`（T-12） | 7 |

跑全部：
```bash
cd backend && mvn verify
```

## 十一、v1.3 · 试运营补完（W1~W4）

> 实施依据：`~/.cursor/plans/roc-git_试运营后续工作_65e6731c.plan.md`

### 1. W1 · 在线预览内容渲染

- 后端 `PreviewController`（`/api/v1/preview/{id}`）按 Content-Type 分发 image / pdf / text / drawio / convert / none。
- text 类直读前 256KB；docx/xlsx/pptx 走 `libreoffice --headless` 转 PDF 缓存到 `/data/files/preview/{fileId}.pdf`，异步触发、超时 90s、最多 3 次重试。
- 前端 `FilePreviewSheet` 组件（pdf.js 懒加载 + image / text / drawio / convert / 下载 兜底），接入 `pages/files/files.vue`。
- 暴露 `/preview/**` 静态资源映射（无需 token）。

### 2. W2 · 对象存储 OSS / COS / S3 真接

- 抽 `FileStorageProvider` 接口；`LocalFileStorageProvider`（默认）+ `S3FileStorageService`（AWS SDK v2）。
- `FileController.download` 在 provider=s3 时 302 跳签名 URL（5 分钟），节省后端带宽。
- `scripts/migrate-local-to-oss.sh` 并发迁移 + `local-to-oss.map.csv` 映射 + `scripts/backup.sh` 跳过本地 files。
- `application.yml` 新增 `rocgit.file.provider` / `rocgit.file.s3.*`；`.env.cloud.example` 配齐。

### 3. W3 · 文件锁 / 微信小程序

- `FileController` 新增 `POST/GET /api/v1/file/{id}/lock[/unlock]` 三件套；`FileService.acquireLock/releaseLock/lockStatus` 走真表 `file.lock_user_id / lock_expire_at`。
- `FileLockExpireTask` 每分钟清过期锁。
- `WechatMpService`（`@ConditionalOnProperty(wechat.mp.app-id)`）调 `jscode2session`；`UserService.login` 在 `wx_real_` 前缀或配 appid 时真接。
- `UserController.openidStatus` 返回是否已绑定 openid + openidHint（避免泄露完整 openid）。
- 前端 `api.js` 加 `fileLockAcquire / fileLockRelease / fileLockStatus / openidStatus`。

### 4. W4 · 云端上线

- `docker-compose.cloud.yml` 注入 `ROCGIT_FILE_PROVIDER / ROCGIT_FILE_S3_* / WECHAT_MP_*` 环境变量。
- 新增 `scripts/ssl-issue.sh`（certbot dns-aliyun 自动签发 + 续期 hook）。
- 新增 `.github/workflows/deploy-cloud.yml`（一键 SSH 拉新镜像 + smoke 验证）。
- `doc/云端部署.md` 补"OSS 切换"和"微信登录"两节。
- 关键端口：80/443（公网）、22（SSH）、3000/9090/9093（监控）。
