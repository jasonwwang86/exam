## ADDED Requirements

### Requirement: `exam-web` 应提供主动交卷确认与提交结果展示
系统 MUST 在 `exam-web` 的 `src/modules/candidate-portal/` 在线答题流程中提供显式交卷入口，并在真正提交前展示确认交互。主动交卷或自动交卷完成后，`exam-web` MUST 展示提交结果视图，至少包含考试名称、试卷名称、提交方式、提交时间和已答/总题数摘要；本次能力 MUST 不展示成绩、判分结果、成绩单入口或监考信息。

#### Scenario: 考生确认后主动交卷成功
- **WHEN** 处于进行中答题会话的考生在 `exam-web` 点击交卷并确认提交
- **THEN** `exam-web` 必须调用 `POST /api/candidate/exams/{planId}/submission`
- **THEN** `exam-service` 必须返回当前考试的最终提交结果摘要
- **THEN** `exam-web` 必须切换到提交结果视图并停止展示可编辑答题控件

#### Scenario: 考生取消交卷确认
- **WHEN** 考生打开交卷确认交互后选择取消
- **THEN** `exam-web` 不得调用交卷接口
- **THEN** `exam-web` 必须保持当前答题页面和未提交状态

#### Scenario: 已交卷考生刷新后仍看到提交结果
- **WHEN** 会话已经处于 `SUBMITTED` 或 `AUTO_SUBMITTED` 终态的考生刷新页面或重新进入同一场考试
- **THEN** `exam-service` 必须返回可恢复的最终提交状态摘要
- **THEN** `exam-web` 必须继续展示提交结果视图，而不是恢复为可编辑答题页

### Requirement: `exam-service` 应持久化最终提交状态并防止重复提交
系统 MUST 以当前考生当前考试的唯一答题会话作为最终提交状态载体，并持久化至少一种最终提交状态和最终提交时间。`exam-service` MUST 保证同一考生同一考试只会产生一次有效终态迁移；重复或并发交卷请求不得生成第二次提交，也不得覆盖第一次终态的提交方式与时间。

#### Scenario: 首次主动交卷写入最终状态
- **WHEN** 已确认考生在截止时间前对处于进行中的答题会话发起交卷
- **THEN** `exam-service` 必须将该会话更新为 `SUBMITTED` 终态并记录最终提交时间
- **THEN** 后续考试列表、答题会话查询和提交结果展示都必须反映该最终状态

#### Scenario: 重复交卷请求不会产生第二次提交
- **WHEN** 同一会话已经完成交卷后再次收到交卷请求，或多个交卷请求并发命中同一会话
- **THEN** `exam-service` 必须返回既有最终提交结果或明确的已提交提示
- **THEN** `exam-service` 不得再写入第二次最终状态迁移或新的最终提交时间

#### Scenario: 已最终提交的会话不能继续保存答案
- **WHEN** 已处于 `SUBMITTED` 或 `AUTO_SUBMITTED` 终态的会话再次调用保存答案接口
- **THEN** `exam-service` 必须拒绝该写入请求
- **THEN** `exam-service` 不得改变原有答案记录和最终提交状态

### Requirement: `exam-service` 应在截止时间到达时自动交卷
系统 MUST 在答题截止时间到达时把尚未最终提交的答题会话自动收口为最终提交状态。自动交卷 MUST 在考生在线和离线两种场景下都成立，并且 MUST 与主动交卷共用同一套终态约束和防重复提交流程。

#### Scenario: 倒计时归零时在线考生看到自动交卷结果
- **WHEN** 考生仍停留在 `exam-web` 在线答题页且剩余时间减至零
- **THEN** `exam-web` 必须向 `exam-service` 请求当前考试的最终提交结果
- **THEN** `exam-service` 必须将该会话收口为 `AUTO_SUBMITTED` 或返回既有自动交卷结果
- **THEN** `exam-web` 必须展示自动交卷结果，而不是仅停留在超时只读提示

#### Scenario: 离线考生仍由服务端自动交卷
- **WHEN** 考生关闭页面、断网或浏览器冻结后，某个进行中的答题会话到达截止时间
- **THEN** `exam-service` 必须通过定时扫描或等效后端机制把该会话更新为 `AUTO_SUBMITTED`
- **THEN** 后续考试列表或答题会话查询必须观察到已自动交卷的最终状态，而不是永久停留在超时未提交状态

#### Scenario: 截止后的交卷请求以自动交卷结果为准
- **WHEN** 考生在截止时间之后才发起交卷请求，而该会话尚未完成最终状态迁移
- **THEN** `exam-service` 必须将该会话落为 `AUTO_SUBMITTED`
- **THEN** `exam-service` 不得把这次请求记录为新的主动交卷结果

### Requirement: `exam-service` 应记录可追踪且脱敏的交卷日志
系统 MUST 在主动交卷、自动交卷、重复交卷命中和已交卷后保存拒绝过程中继续复用统一 `TraceNo`、请求/响应/异常日志与脱敏规则，并输出可检索的交卷业务日志。日志中 MUST 不得记录明文答案、完整 Token、完整 Authorization 头或身份证号。

#### Scenario: 交卷关键动作可通过业务日志追踪
- **WHEN** 考生主动交卷成功、系统完成自动交卷或重复交卷保护生效
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联或可关联的业务日志
- **THEN** 日志必须包含考生标识摘要、考试计划标识、会话标识、交卷方式和结果摘要

#### Scenario: 交卷相关敏感信息保持脱敏
- **WHEN** 交卷请求、响应或异常上下文中包含答案内容、Token、Authorization 头或身份证号
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守统一的日志脱敏规则
