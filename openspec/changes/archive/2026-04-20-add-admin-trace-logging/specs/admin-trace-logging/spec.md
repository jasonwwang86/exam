## ADDED Requirements

### Requirement: `exam-web` 与 `exam-service` 使用统一的 `TraceNo` 链路标识
The system SHALL support end-to-end `TraceNo` propagation between `exam-web` and `exam-service`, and each `TraceNo` MUST use a 32-character UUID without hyphens. `exam-web` SHALL send a `TraceNo` request header for management APIs. `exam-service` SHALL preserve a valid inbound `TraceNo` as the request correlation id and SHALL return the same value in the response header. If the inbound `TraceNo` is missing or invalid, `exam-service` MUST generate a new valid `TraceNo` and use it for logging and response header output.

#### Scenario: Frontend provided TraceNo is preserved
- **WHEN** `exam-web` 以 `TraceNo` 请求头调用 `exam-service` 的管理端接口
- **THEN** `exam-service` 必须在该请求处理过程中的日志中输出相同的 `TraceNo`
- **THEN** `exam-service` 必须在响应头中返回相同的 `TraceNo`

#### Scenario: Backend generates TraceNo when header is missing
- **WHEN** 请求未携带 `TraceNo` 请求头到达 `exam-service`
- **THEN** `exam-service` 必须生成新的 TraceNo 作为本次请求链路标识
- **THEN** `exam-service` 必须在响应头和请求处理日志中输出该生成值

#### Scenario: Backend replaces invalid TraceNo format
- **WHEN** 请求携带的 `TraceNo` 不是 32 位无连字符 UUID 格式
- **THEN** `exam-service` 不得继续沿用该值
- **THEN** `exam-service` 必须生成新的合法 TraceNo 并用于响应头与日志输出

### Requirement: `exam-service` 输出统一的请求、响应与异常日志
`exam-service` SHALL emit standardized request-start, request-completion, and exception logs for HTTP interfaces. These logs MUST include at least `TraceNo`, HTTP method, request path, response status code, and processing duration. When a request or response body exists, the system SHALL log a controllable summary suitable for troubleshooting.

#### Scenario: Request completion log contains core fields
- **WHEN** 管理端接口请求被 `exam-service` 正常处理完成
- **THEN** 请求完成日志必须包含 `TraceNo`、HTTP 方法、请求路径、响应状态码和处理耗时

#### Scenario: Exception log keeps the same TraceNo
- **WHEN** 管理端接口处理过程中抛出异常并由 `exam-service` 返回错误响应
- **THEN** 异常日志必须包含与本次请求一致的 `TraceNo`
- **THEN** 异常日志必须包含异常类型和错误消息摘要

### Requirement: `exam-service` 为业务关键节点输出可检索日志
`exam-service` SHALL emit searchable logs for key business events, and those logs MUST remain queryable together with `TraceNo` and essential business context summaries. The current implementation SHALL at least cover login success, login failure, disabled-account rejection, authentication failure, permission denial, and logout success. Later modules SHALL reuse the same logging constraints.

#### Scenario: Login failure is traceable
- **WHEN** 管理员因账号不存在、密码错误或账号禁用而登录失败
- **THEN** `exam-service` 必须输出包含 `TraceNo`、用户名摘要和失败原因摘要的关键业务日志

#### Scenario: Permission denial is traceable
- **WHEN** 已登录管理员访问无权限的管理端接口
- **THEN** `exam-service` 必须输出包含 `TraceNo`、用户标识摘要和目标权限码或接口摘要的关键业务日志

### Requirement: 日志输出遵守统一的脱敏与内容约束
`exam-service` request, response, exception, and business logs MUST apply one consistent masking policy. The system MUST NOT write plaintext passwords, full Tokens, full `Authorization` headers, password hashes, or other sensitive authentication data to logs. When such fields are present, the system SHALL replace them with masked placeholders or omit the original values.

#### Scenario: Sensitive authentication fields are masked
- **WHEN** 管理端登录、鉴权或退出接口的请求或响应包含密码、Token 或鉴权头
- **THEN** `exam-service` 输出的日志中不得出现这些字段的明文值
- **THEN** 对应字段必须显示为脱敏占位或被省略

#### Scenario: Logging foundation remains reusable across modules
- **WHEN** 本次 change 被实现
- **THEN** TraceNo、请求/响应日志、异常日志和脱敏规则必须以通用基础能力方式落地
- **THEN** 后续模块必须能够在不修改核心约束的前提下复用同一套日志机制
