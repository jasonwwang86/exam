## ADDED Requirements

### Requirement: `exam-service` 应为已最终提交的考试生成可展示的成绩结果快照
系统 MUST 在 `exam-service` 中为处于 `SUBMITTED` 或 `AUTO_SUBMITTED` 终态的答题会话生成稳定的成绩结果快照，并将结果落入 `exam_result` 与 `exam_result_item`。生成成功后，既有 `exam-web` 考试列表与成绩详情查询能力 MUST 能基于这些结果直接展示分数与作答摘要；本次能力 MUST 不新增独立成绩生成页面、人工操作入口或新的考生端前端模块。

#### Scenario: 主动交卷后立即生成成绩结果
- **WHEN** 考生通过既有交卷流程完成某场考试的主动提交，且 `exam-service` 已将会话落为 `SUBMITTED`
- **THEN** `exam-service` 必须同步尝试为该会话生成 `exam_result` 与 `exam_result_item`
- **THEN** 生成成功后，既有考试列表查询必须能够读取到总分、出分状态和成绩单入口所需摘要

#### Scenario: 自动交卷后立即生成成绩结果
- **WHEN** `exam-service` 因截止时间到达将某场考试会话自动落为 `AUTO_SUBMITTED`
- **THEN** `exam-service` 必须使用同一套成绩生成逻辑为该会话生成结果快照
- **THEN** 结果中的提交方式与提交时间必须与最终会话状态一致

#### Scenario: 成绩生成失败时不写入半成品结果
- **WHEN** 某个已最终提交会话在成绩生成过程中发生判分异常、数据不一致或持久化失败
- **THEN** `exam-service` 不得留下只写了一部分题目或聚合字段不完整的 `exam_result` / `exam_result_item`
- **THEN** 对外表现必须保持为“已交卷但待出分”，而不是返回损坏的成绩详情

### Requirement: `exam-service` 应按当前题型配置执行确定性自动判分
系统 MUST 基于 `paper_question.answer_config_snapshot`、题目快照分值和 `exam_answer_record.answer_content` 执行自动判分。判分 MUST 使用确定性规则，且本次 MUST 不实现人工复核、模糊匹配、标准答案解析或部分给分。

#### Scenario: 单选题按唯一选项精确判分
- **WHEN** 某道 `SINGLE_CHOICE` 题存在 `selectedOption` 答案内容
- **THEN** `exam-service` 必须仅在 `selectedOption` 与 `correctOption` 完全相等时判为正确并给满分
- **THEN** 若不相等或未作答，则该题得分必须为 0 分

#### Scenario: 多选题按选项集合完全相等判分
- **WHEN** 某道 `MULTIPLE_CHOICE` 题存在 `selectedOptions` 答案内容
- **THEN** `exam-service` 必须在去重和顺序归一化后，仅当所选集合与 `correctOptions` 完全一致时判为正确并给满分
- **THEN** 本次不得因“部分选对”而给部分分

#### Scenario: 判断题按布尔值精确判分
- **WHEN** 某道 `TRUE_FALSE` 题存在布尔型答案内容
- **THEN** `exam-service` 必须仅在该布尔值与 `correctAnswer` 完全一致时给满分
- **THEN** 其余情况必须判为错误并记 0 分

#### Scenario: 文本题按参考答案归一化精确匹配判分
- **WHEN** 某道 `TEXT` 题存在 `textAnswer` 且题库配置了 `acceptedAnswers`
- **THEN** `exam-service` 必须在去除首尾空白、折叠连续空白并做大小写归一化后，将 `textAnswer` 与任一 `acceptedAnswers` 进行精确匹配
- **THEN** 只有匹配成功时该题才可得满分，否则记 0 分

### Requirement: `exam-service` 应生成幂等且完整替换的成绩结果快照
系统 MUST 以 `(exam_plan_id, examinee_id)` 作为成绩结果唯一归属，同一考生同一考试重复触发成绩生成时，`exam-service` MUST 更新既有结果快照而不是新增重复记录。逐题结果 MUST 与本次最新生成内容完全一致，并保持与试卷题序一致。

#### Scenario: 重复触发成绩生成不会产生第二份结果
- **WHEN** 同一考生同一考试因重复交卷保护、补偿扫描或手动重入逻辑再次触发成绩生成
- **THEN** `exam-service` 必须复用既有 `exam_result` 唯一记录而不是新增第二条成绩结果
- **THEN** 对应 `exam_result_item` 必须被完整替换为本次最新生成的逐题结果快照

#### Scenario: 逐题结果按试卷题序持久化
- **WHEN** `exam-service` 为某场考试生成 `exam_result_item`
- **THEN** 每道结果项必须保留题号、题干快照、题型快照、满分、得分、作答状态、作答摘要和判定状态
- **THEN** 结果项顺序必须与试卷题序一致，供既有成绩详情查询稳定复用

#### Scenario: 已提交未出分会话可被补偿扫描重试
- **WHEN** 某个会话已经处于最终提交态但此前成绩生成失败，导致仍不存在对应结果快照
- **THEN** `exam-service` 必须能够通过补偿扫描或等效后端机制再次触发同一套成绩生成流程
- **THEN** 重试成功后既有考生端成绩查询能力必须观察到已生成结果

### Requirement: `exam-service` 应为成绩生成输出可追踪且脱敏的业务日志
系统 MUST 在成绩生成成功、重复触发命中、生成失败和补偿重试过程中复用统一 `TraceNo`、请求/响应/异常日志与脱敏规则，并输出可检索的成绩生成业务日志。日志中 MUST 不得记录明文答案、完整 Token、完整 Authorization 头、身份证号或标准答案正文。

#### Scenario: 成绩生成关键动作可通过业务日志追踪
- **WHEN** `exam-service` 完成一次成绩生成、命中重复结果更新或执行补偿重试
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联或可关联的业务日志
- **THEN** 日志必须包含考生标识摘要、考试计划标识、会话标识、生成结果摘要和结果项数量

#### Scenario: 判分相关敏感内容在日志中保持脱敏
- **WHEN** 成绩生成请求上下文、异常上下文或业务日志中包含答案内容、标准答案配置或身份敏感信息
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守统一的日志脱敏规则
