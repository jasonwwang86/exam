## ADDED Requirements

### Requirement: `exam-web` 应提供由 `exam-service` 支撑的考生端登录入口与待考试状态页
系统 MUST 在 `exam-web` 中为考生提供独立于管理端壳层的登录入口，并由 `exam-service` 的考生端认证接口支撑。考生端登录页 MUST 使用独立路由和布局，成功登录后 MUST 进入待考试状态页或身份确认页，而不是进入管理端首页。

#### Scenario: 考生使用合法凭据成功登录
- **WHEN** 启用状态的考生在 `exam-web` 提交合法的 `examineeNo` 与 `idCardNo`
- **THEN** `exam-web` 必须调用 `POST /api/candidate/auth/login`
- **THEN** `exam-service` 必须返回考生 Bearer Token、Token 有效期、考生基础摘要以及当前会话确认状态
- **THEN** `exam-web` 必须将考生导航到待考试状态页或身份确认页

#### Scenario: 非法凭据或禁用考生登录失败
- **WHEN** 考生提交不存在的考生编号、错误的身份证号，或对应考生处于禁用状态
- **THEN** `exam-service` 必须拒绝登录请求
- **THEN** `exam-web` 必须停留在考生登录页并展示通用失败提示

#### Scenario: 未登录考生不能访问考生端受保护页面
- **WHEN** 未登录用户直接访问 `exam-web` 的考生端确认页或考试列表页
- **THEN** `exam-web` 必须将其重定向到考生登录页
- **THEN** `exam-service` 必须拒绝未携带有效考生 Token 的受保护请求

### Requirement: `exam-web` 与 `exam-service` 应支持考生身份信息展示与考前确认
系统 MUST 在考生成功登录后展示身份信息确认页，并通过显式 DTO 契约返回考生身份摘要。确认页 MUST 至少展示考生编号、姓名、身份证号脱敏摘要和当前可读提示。考生完成确认后，系统 MUST 将当前会话标记为已确认。

#### Scenario: 已登录考生查看身份信息确认页
- **WHEN** 已登录考生进入 `exam-web` 的身份确认页
- **THEN** `exam-web` 必须调用 `GET /api/candidate/profile`
- **THEN** `exam-service` 必须返回当前考生的身份摘要、确认状态与待考试说明
- **THEN** `exam-web` 不得展示管理端字段或无关考试执行控件

#### Scenario: 考生确认身份信息成功
- **WHEN** 已登录考生在身份确认页点击确认并提交
- **THEN** `exam-web` 必须调用 `POST /api/candidate/profile/confirm`
- **THEN** `exam-service` 必须将当前考生会话更新为已确认状态并返回更新后的会话摘要或新 Token
- **THEN** `exam-web` 必须允许考生进入可参加考试列表页

#### Scenario: 未确认考生不能查看可参加考试列表
- **WHEN** 已登录但尚未完成身份确认的考生请求 `GET /api/candidate/exams`
- **THEN** `exam-service` 必须拒绝该请求并返回未确认错误
- **THEN** `exam-web` 必须将考生引导回身份确认页

### Requirement: `exam-service` 应向已确认考生返回可参加考试列表
系统 MUST 为已完成身份确认的考生提供可参加考试列表能力。`GET /api/candidate/exams` MUST 只返回已分配给当前考生、考试计划状态为 `PUBLISHED` 且尚未结束的考试计划。返回结果 MUST 使用显式 DTO，至少包含考试计划 ID、考试名称、试卷名称、开始时间、结束时间、考试时长、展示状态和备注摘要。

#### Scenario: 已确认考生查询可参加考试列表
- **WHEN** 已确认考生在 `exam-web` 打开可参加考试列表页
- **THEN** `exam-web` 必须调用 `GET /api/candidate/exams`
- **THEN** `exam-service` 必须返回该考生当前可参加的考试列表，并按开始时间升序排序

#### Scenario: 已结束或未发布的考试不能出现在列表中
- **WHEN** 某场考试未发布、已取消、已关闭，或结束时间早于当前时间
- **THEN** `exam-service` 必须将该考试从当前考生的可参加考试列表中过滤掉
- **THEN** `exam-web` 不得将该考试展示为可参加记录

#### Scenario: 考生没有可参加考试时展示空态
- **WHEN** 已确认考生当前没有任何符合条件的考试计划
- **THEN** `exam-service` 必须返回空列表
- **THEN** `exam-web` 必须展示清晰的空态提示，并保持当前考生登录态

#### Scenario: 已确认考生可主动刷新考试列表
- **WHEN** 已确认考生在 `exam-web` 的可参加考试列表页点击刷新考试列表
- **THEN** `exam-web` 必须重新调用 `GET /api/candidate/exams`
- **THEN** `exam-web` 必须用最新返回结果覆盖当前页面状态和本地考试列表缓存
- **THEN** 本次能力仍不得扩展到在线答题、提交试卷或成绩单入口

#### Scenario: 已登录考生可主动退出登录
- **WHEN** 已登录考生在身份确认页或可参加考试列表页点击退出登录
- **THEN** `exam-web` 必须清除 `candidate_token`、`candidate_profile`、`candidate_exams` 本地状态
- **THEN** `exam-web` 必须将当前用户导航回考生登录页
- **THEN** `exam-web` 不得保留上一个考生的身份摘要或考试列表缓存

#### Scenario: 本次范围内不得暴露开始答题或成绩查看动作
- **WHEN** 已确认考生查看可参加考试列表
- **THEN** `exam-web` 只能展示考试信息与当前只读状态
- **THEN** `exam-web` 与 `exam-service` 不得在本次能力中提供在线答题、提交试卷或成绩单相关入口与字段

### Requirement: `exam-service` 应复用带有 TraceNo 关联的脱敏日志能力记录考生端流程
系统 MUST 在考生端登录、身份确认与考试列表接口中复用现有 `TraceNo`、请求日志、响应日志、异常日志和日志脱敏规则，并额外记录登录成功/失败、身份确认完成和考试列表查询的可检索业务日志。

#### Scenario: 考生端关键动作可通过业务日志追踪
- **WHEN** 考生完成登录、身份确认或考试列表查询
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联的业务日志
- **THEN** 日志必须包含考生标识摘要、动作类型和结果摘要

#### Scenario: 登录凭据与身份字段在日志中保持脱敏
- **WHEN** 考生端请求、响应或异常信息中包含身份证号、Token、Authorization 头或其他敏感字段
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守统一的日志脱敏规则
