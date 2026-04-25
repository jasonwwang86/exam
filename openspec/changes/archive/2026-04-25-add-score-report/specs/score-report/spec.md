## ADDED Requirements

### Requirement: `exam-web` 应为已出分考试提供成绩单入口与成绩详情页
系统 MUST 在 `exam-web` 的 `src/modules/candidate-portal/` 中为当前考生提供成绩单查看入口，并在详情页中展示考试结果总览、试卷信息、提交摘要与作答摘要。成绩详情页 MUST 通过 `exam-service` 的显式 DTO 加载数据，不得直接依赖持久化实体，也不得在本次范围内扩展到标准答案解析、排名或动态大屏入口。

#### Scenario: 已出分考试可以进入成绩详情页
- **WHEN** 已确认考生在 `exam-web` 的考试列表中查看一场已经生成成绩结果的考试
- **THEN** `exam-web` 必须展示该考试的成绩状态摘要和成绩单入口
- **THEN** 考生进入详情页后，`exam-web` 必须调用 `GET /api/candidate/exams/{planId}/score-report`
- **THEN** 页面必须展示总分、试卷名称、提交时间和作答摘要，而不是交卷按钮或管理端入口

#### Scenario: 待出分考试不能进入成绩详情页
- **WHEN** 已确认考生查看一场已经交卷但成绩结果尚未生成的考试
- **THEN** `exam-web` 必须展示待出分或生成中的状态说明
- **THEN** `exam-web` 不得展示可进入成绩详情页的入口

### Requirement: `exam-service` 应返回成绩结果详情、试卷信息与作答摘要
系统 MUST 通过成绩结果模型向 `exam-web` 返回稳定的成绩单详情。`GET /api/candidate/exams/{planId}/score-report` MUST 至少返回考试名称、试卷名称、总分、成绩状态、提交时间、已答/未答统计、逐题得分明细和作答摘要；返回结果 MUST 使用显式 DTO，并保持逐题顺序与试卷题序一致。

#### Scenario: 成绩详情查询成功
- **WHEN** 当前考生请求一场已存在成绩结果且属于自己的考试成绩单
- **THEN** `exam-service` 必须返回该考试的成绩总览、试卷信息、提交摘要与逐题作答摘要
- **THEN** 逐题明细必须包含题号、题型名称、该题满分、得分、作答状态和我的作答摘要
- **THEN** `exam-service` 不得在本次范围内返回标准答案、解析或排名统计

#### Scenario: 结果项按试卷题序展示
- **WHEN** `exam-service` 返回某场考试的成绩详情
- **THEN** 成绩详情中的逐题摘要必须按试卷 `display_order` 或等效题序字段升序排列
- **THEN** `exam-web` 不得自行依赖原始答案记录重排题序

### Requirement: `exam-service` 应限制成绩查询到当前考生本人且结果已就绪的最终提交考试
系统 MUST 只允许当前登录考生查询自己的成绩结果，并且该考试 MUST 已处于 `SUBMITTED` 或 `AUTO_SUBMITTED` 等最终提交状态。若结果尚未生成、目标考试不属于当前考生，或不存在可展示结果，`exam-service` MUST 拒绝详情查询或返回明确的未就绪提示，不得泄露他人成绩信息。

#### Scenario: 非本人考试成绩查询被拒绝
- **WHEN** 当前考生请求一个不属于自己的考试计划成绩单，或试图通过猜测 `planId` 访问他人成绩
- **THEN** `exam-service` 必须拒绝该请求
- **THEN** 响应不得返回任何他人考试成绩、作答摘要或试卷详情

#### Scenario: 已交卷但未出分时详情查询被拒绝
- **WHEN** 当前考生请求一场已最终提交但成绩结果尚未生成的考试成绩单
- **THEN** `exam-service` 必须返回明确的待出分或结果未就绪提示
- **THEN** `exam-web` 必须据此保持只读状态说明，而不是展示空成绩详情

### Requirement: `exam-service` 应为成绩查询输出可追踪且脱敏的业务日志
系统 MUST 在考试列表成绩摘要查询、成绩详情查询、待出分拒绝和越权拒绝过程中继续复用统一 `TraceNo`、请求/响应/异常日志与脱敏规则，并输出可检索的成绩查询业务日志。日志中 MUST 不得记录明文答案、完整 Token、完整 Authorization 头、身份证号或完整答案内容。

#### Scenario: 成绩查询动作可通过业务日志追踪
- **WHEN** 考生查询成绩列表摘要或打开成绩详情页
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联或可关联的业务日志
- **THEN** 日志必须包含考生标识摘要、考试计划标识、结果状态和查询结果摘要

#### Scenario: 成绩查询中的敏感信息保持脱敏
- **WHEN** 成绩查询请求、响应或异常上下文中包含答案内容、Token、Authorization 头或身份证号
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守统一的日志脱敏规则
