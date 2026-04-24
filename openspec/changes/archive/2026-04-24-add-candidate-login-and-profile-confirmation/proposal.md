## Why

考试计划与考生主数据已经具备独立建模基础，但仓库还缺少一个可独立验收的考生端入口，导致考生无法完成登录、确认身份信息并查看自己可参加的考试。现在需要先落地一个边界清晰的考生端登录与确认信息 change，在不扩展到在线答题、交卷或成绩单的前提下，让考生能够进入待考试状态。

## What Changes

- `exam-web` 新增落在 `src/modules/candidate-portal/` 的考生端模块，提供考生登录页、身份信息确认页和可参加考试列表页，并与管理端壳层隔离，避免复用管理端首页框架。
- `exam-web` 在考前确认页与可参加考试列表页补充会话内操作入口：支持已确认考生手动刷新考试列表缓存，并支持考生主动退出登录后清理本地缓存并返回考生登录页。
- `exam-service` 新增考生端认证、身份信息查询、考前确认与可参加考试查询 REST API，使用显式 DTO 返回考生身份摘要、确认状态与考试列表数据，不直接暴露持久化实体。
- `exam-service` 复用既有 `examinee`、`exam_plan`、`exam_plan_examinee` 与试卷主数据，基于考生编号和身份证号完成登录校验，并基于考试计划状态与考生范围返回可参加考试列表。
- 包含范围：考生登录、身份信息展示、考前确认信息页、可参加考试列表，以及前述页面中的“刷新考试列表”“退出登录”会话操作；影响 `exam-web`，并继续复用既有 `exam-service` 接口。
- 不包含范围：在线答题、开始考试按钮后的答题流程、提交试卷、成绩单、监考大屏，以及任何超出“登录与确认信息”边界的考试执行能力。

## Capabilities

### New Capabilities
- `candidate-login-and-profile-confirmation`: `exam-web` 与 `exam-service` 为考生提供登录、身份信息确认和可参加考试列表能力。

### Modified Capabilities
- 无。

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `POST /api/candidate/auth/login`, `GET /api/candidate/profile`, `POST /api/candidate/profile/confirm`, `GET /api/candidate/exams`
- Planned DTOs: 考生登录请求、考生登录响应、考生身份信息响应、考前确认响应、可参加考试列表项响应
- Database impact: 默认复用既有 `examinee`、`exam_plan`、`exam_plan_examinee`、`paper` 表及其索引，不新增核心业务表；需要在既有查询层补充考生端组合查询与鉴权配置
- Dependencies: 复用既有考生管理提供的考生主数据、考试计划提供的考试范围与状态数据，以及通用 `TraceNo`、请求/响应/异常日志和日志脱敏基础能力
