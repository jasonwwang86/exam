## Why

当前系统已开始具备前后端联调能力，但后端仍缺少统一的请求/响应日志和关键逻辑日志，问题排查时无法快速定位一次请求从前端发起到后端处理完成的完整链路。需要引入统一的 TraceNo 与日志输出约束，确保可以按一次请求追踪完整处理流程，并让这套能力作为通用基础设施被后续模块复用。

## What Changes

- 为 `exam-service` 增加统一的请求进入、请求结束、异常处理与关键业务节点日志能力，作为后端通用日志基础设施。
- 为 `exam-service` 增加 TraceNo 机制：优先读取前端请求头中的 TraceNo，缺失或非法时自动生成 32 位无连字符 UUID，并在响应头和后端日志中统一输出。
- 为 `exam-service` 增加日志输出约束，包括日志级别、字段格式、脱敏规则、禁止输出内容和异常日志要求。
- 为 `exam-web` 增加基础 TraceNo 请求头透传能力，使浏览器发起的管理端请求能够携带 TraceNo 到后端。
- 明确本次包含范围：TraceNo 透传、请求/响应报文日志、关键逻辑日志、异常日志与日志输出约束。
- 明确本次不包含范围：链路追踪平台接入、分布式调用追踪、复杂审计日志平台。

## Capabilities

### New Capabilities
- `admin-trace-logging`: 以当前 `exam-web` 到 `exam-service` 链路为落地点，建立可被后续模块复用的 TraceNo、请求/响应日志、关键逻辑日志和统一日志输出约束。

### Modified Capabilities
- None.

## Impact

- Affected systems: `exam-web`、`exam-service`
- Frontend impact (`exam-web`):
  - 为管理端 Axios 请求增加 TraceNo 请求头生成与透传
  - 保持现有登录与权限流程不变，不新增业务页面
- Backend impact (`exam-service`):
  - 在 `cn.jack.exam.config` 下新增或扩展请求日志过滤器、TraceNo 上下文管理、统一日志配置，作为通用日志基础能力
  - 在当前已有认证与权限链路中补充关键逻辑日志，并为后续模块复用提供约束
  - 不新增数据库表结构，也不修改现有认证 REST 业务语义
- REST APIs:
  - `POST /api/admin/auth/login`
  - `GET /api/admin/auth/me`
  - `POST /api/admin/auth/logout`
  - `GET /api/admin/dashboard/summary`
- Request/response DTOs:
  - 不新增业务 DTO
  - 受影响接口需支持 `TraceNo` 请求头输入与响应头回写
- Assumptions:
  - TraceNo 使用 32 位无连字符 UUID 格式
  - TraceNo 首期仅要求贯穿浏览器请求头与单体后端处理链路，不要求跨外部系统传播
  - 请求/响应日志需遵守脱敏约束，不记录明文密码、完整 Token 或敏感鉴权头
