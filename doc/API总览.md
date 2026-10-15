# API 总览（v1.2 · 按 Controller 分组）

> 完整描述见 Knife4j：`http://127.0.0.1:8080/doc.html` 或 `http://127.0.0.1/doc.html`（docker 编排时）。
> 鉴权：除 `*` 公开接口外，其它都需要 `Authorization: Bearer <token>` 头；token 由 `/v1/user/login` 取得。

## 02. 用户 (`/api/v1/user`) — `UserController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/login` | 公开 | 微信 code 登录，首次自动注册（W3 真接 jscode2session） |
| POST | `/sms/send-code` | 公开 | 发送短信验证码 |
| POST | `/sms/login-by-phone` | 公开 | 手机号+验证码登录 |
| GET | `/profile` | 需登录 | 个人信息 |
| PUT | `/profile` | 需登录 | 更新昵称/头像 |
| GET | `/login-history` | 需登录 | 登录历史（最近 30 条） |
| POST | `/logout` | 需登录 | 退出（前端清 token 即可） |
| POST | `/refresh-token` | 需登录 | 真用 `JwtUtil` 重签 token |
| GET | `/contribution-stats` | 需登录 | 贡献度统计 |
| POST | `/bind-wechat` | 需登录 | 绑定微信 openid（去重） |
| GET | `/openid-status` | 需登录 | 查询 openid 绑定状态（W3） |

## 03. 小组 (`/api/v1/group`) — `GroupController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/my-list` | 需登录 | 当前用户所在的所有小组 |
| GET | `/{id}` | 需登录 | 小组详情 |
| GET | `/{id}/stats` | 需登录 | 小组统计 |
| GET | `/{id}/members` | 需登录 | 成员列表（分页） |
| GET | `/{id}/member-stats` | 需登录 | 成员贡献排行 |
| GET | `/{id}/projects` | 需登录 | 小组下所有项目（同名 `/v1/project/list?groupId=`） |
| POST | `/create` | 需登录 | 创建小组 |
| POST | `/join` | 需登录 | 加入小组 |
| POST | `/validate-invite-code` | 公开 | 校验邀请码 |
| PUT | `/{id}/member/{userId}/remark` | 需登录 | 修改成员备注 |
| POST | `/{id}/transfer-leader` | 需登录 | 转让组长 |
| POST | `/{id}/invite-code` | 需登录 | 重新生成邀请码 |

## 04. 项目 (`/api/v1/project`) — `ProjectController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/list?groupId=` | 需登录 | 项目列表 |
| GET | `/{id}` | 需登录 | 项目详情 |
| GET | `/{id}/progress` | 需登录 | 项目进度 |
| GET | `/{id}/activities` | 需登录 | 项目动态 |
| GET | `/{id}/task-stats` | 需登录 | 任务统计 |
| GET | `/{id}/stage` | 需登录 | 阶段列表 |
| GET | `/{id}/contribution-stats` | 需登录 | 成员贡献度 |
| POST | `/create` | 需登录 | 创建项目 |
| POST | `/{id}/copy` | 需登录 | 复制项目 |
| POST | `/{id}/complete` | 需登录 | 结项 |
| PUT | `/{id}` | 需登录 | 更新项目 |
| DELETE | `/{id}` | 需登录 | 删除项目 |
| GET | `/template/list` | 需登录 | 项目模板列表 |
| POST | `/{id}/stage` | 需登录 | 新增阶段 |
| PUT | `/{id}/stage/{stageId}` | 需登录 | 更新阶段 |
| DELETE | `/{id}/stage/{stageId}` | 需登录 | 删除阶段 |
| GET | `/{id}/stage/{stageId}` | 需登录 | 阶段详情 |

## 05. 任务 (`/api/v1/task`) — `TaskController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/list?projectId=` | 需登录 | 任务列表 |
| GET | `/my-tasks` | 需登录 | 我的任务 |
| GET | `/{id}` | 需登录 | 任务详情 |
| GET | `/due-soon?days=7` | 需登录 | 临期任务 |
| POST | `/create` | 需登录 | 创建任务 |
| POST | `/{id}/complete` | 需登录 | 完成任务 |
| POST | `/{id}/progress` | 需登录 | 更新进度 |
| PUT | `/{id}` | 需登录 | 更新任务 |
| DELETE | `/{id}` | 需登录 | 删除任务 |
| POST | `/{id}/transfer` | 需登录 | 转交任务 |

## 06. 文件预览 (`/api/v1/preview`) — `PreviewController`（T-12）

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/{fileId}` | 需登录 | 预览元信息（按 Content-Type 分发 image / pdf / text / drawio / convert / none） |
| GET | `/{fileId}/pdf` | 需登录 | 已转码 PDF 流（仅 converted=true 时有内容） |

## 07. 文件 (`/api/v1/file`) — `FileController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/upload` (multipart) | 需登录 | 上传文件（自动版本+1） |
| GET | `/list?projectId=&stageId=&status=&pageNum=&pageSize=` | 需登录 | 文件列表 |
| GET | `/{id}` | 需登录 | 文件详情 |
| DELETE | `/{id}` | 需登录 | 删除文件 |
| PUT | `/{id}/rename` | 需登录 | 重命名 |
| PUT | `/{id}/move` | 需登录 | 移动到阶段 |
| POST | `/batch-delete` | 需登录 | 批量删除 |
| POST | `/batch-move` | 需登录 | 批量移动 |
| POST | `/batch-audit` | 需登录 | 批量审核 |
| POST | `/{id}/reject` | 需登录 | 驳回 |
| POST | `/{id}/resubmit` | 需登录 | 重新提交 |
| GET | `/{id}/preview` | 需登录 | 预览元信息（走 PreviewService 分发） |
| GET | `/{id}/download` | 需登录（也允许匿名） | 下载（local 流 / W2 OSS 302 签名 URL） |
| GET | `/public/download?fileId=&shareToken=` | 需 shareToken | 公开下载（必须 shareToken） |
| POST | `/{id}/lock` | 需登录 | 申请文件锁（W3 · T-10） |
| POST | `/{id}/unlock` | 需登录 | 释放文件锁（W3 · T-10） |
| GET | `/{id}/lock` | 需登录 | 查询文件锁状态（W3 · T-10） |
| GET | `/{id}/versions` | 需登录 | 版本列表 |
| GET | `/{id}/audit-logs` | 需登录 | 审核日志 |
| GET | `/{id}/download-records` | 需登录 | 下载记录 |
| GET | `/type-stats?projectId=` | 需登录 | 类型统计 |
| POST | `/export-list` | 需登录 | 导出文件清单 |

## 07. 分享 (`/api/v1/share`) — `ShareController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/create` | 需登录 | 创建分享 |
| PUT | `/{id}` | 需登录 | 更新分享 |
| DELETE | `/{id}` | 需登录 | 删除分享 |
| GET | `/my-shares` | 需登录 | 我的分享 |
| GET | `/{id}/stats` | 需登录 | 分享统计 |
| GET | `/{id}/access-logs` | 需登录 | 访问日志 |
| POST | `/{id}/visit` | 公开 | 记录访问 |
| GET | `/visitor/{token}?password=` | 公开 | 访客视图 |

## 08. 研讨 (`/api/v1/discussion`) — `DiscussionController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/list?projectId=` | 需登录 | 研讨列表 |
| GET | `/{id}` | 需登录 | 研讨详情 |
| POST | `/create` | 需登录 | 新建研讨 |
| PUT | `/{id}` | 需登录 | 更新研讨 |
| DELETE | `/{id}` | 需登录 | 删除研讨 |

## 09. 决策 (`/api/v1/decision`) — `DecisionController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/list?projectId=` | 需登录 | 决策列表 |
| GET | `/{id}` | 需登录 | 决策详情 |
| POST | `/create` | 需登录 | 新建决策 |
| PUT | `/{id}` | 需登录 | 更新决策 |
| DELETE | `/{id}` | 需登录 | 删除决策 |

## 10. 消息 (`/api/v1/message`) — `MessageController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/unread-count` | 需登录 | 未读数 |
| GET | `/list` | 需登录 | 消息列表 |
| POST | `/read-all` | 需登录 | 全部已读 |
| GET | `/announcement-list` | 需登录 | 公告列表 |
| POST | `/subscribe` | 需登录 | 订阅（P1-1 真写 subscription_message） |
| POST | `/unsubscribe` | 需登录 | 取消订阅 |
| GET | `/subscriptions` | 需登录 | 我的订阅列表 |
| PUT | `/subscription` | 需登录 | 更新订阅 |

## 11. 任务提醒 (`/api/v1/task-reminder`) — `TaskReminderController`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/my` | 需登录 | 我的提醒 |
| GET | `/unsent-count` | 需登录 | 未发送数 |
| POST | `/scan` | 需登录 | 触发扫描 |

## 12. 公告 (`/api/v1/announcement`) — `AnnouncementController`
## 13. 成果 (`/api/v1/achievement`) — `AchievementController`
## 14. 日志 (`/api/v1/log`) — `LogController`
## 15. 版本对比 (`/api/v1/version`) — `VersionController`
## 16. 流程图 (`/api/v1/config`) — `FlowchartController`
## 17. 健康检查 (`/api/v1/health`, `/api/v1/db`) — `HealthController`, `DbHealthController`

## 错误码规范

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 / 40101 | 未登录 / 凭证失效 |
| 403 | 无权访问 |
| 404 | 资源不存在 |
| 409 / 40901 | 冲突（如审核状态非法） |
| 423 | 资源被锁（文件锁） |
| 500 | 服务器内部错误 |
| 1001 | token 解析失败 |

## 跨域（CORS）

开发环境放行 `*`（H5 通过 nginx 同源，MP/APP 不受浏览器 CORS 限制）。
生产（P3-1）将 `CorsConfig.allowedOriginPatterns` 替换为具体域名。
