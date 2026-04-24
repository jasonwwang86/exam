# online-answering-flow Specification

## Purpose
定义 `exam-web` 与 `exam-service` 在本次范围内支撑考生在线答题、单题过程保存、会话恢复与倒计时只读控制的约束，明确不扩展到交卷、成绩单或监考大屏。

## Requirements
### Requirement: `exam-web` 应允许已确认考生进入并恢复在线答题会话
系统 MUST 仅允许已登录且已完成身份确认、被分配到指定考试计划且当前处于可作答时间窗口内的考生进入在线答题页。`exam-web` 必须通过 `exam-service` 的答题会话接口创建或恢复当前考生在某场考试中的唯一答题会话，并在进入后展示当前考试摘要、剩余时间与题目导航。

#### Scenario: 考生首次进入可作答考试时创建答题会话
- **WHEN** 已确认考生在 `exam-web` 的考试列表页点击某场已开始且未结束考试的进入答题入口
- **THEN** `exam-web` 必须调用 `PUT /api/candidate/exams/{planId}/answer-session`
- **THEN** `exam-service` 必须为当前考生和该考试计划创建唯一答题会话或返回当前有效会话
- **THEN** `exam-web` 必须导航到在线答题页并展示返回的题目、倒计时和答题状态

#### Scenario: 考生刷新页面后恢复既有答题会话
- **WHEN** 已存在答题会话的考生刷新在线答题页或重新进入同一场考试
- **THEN** `exam-web` 必须再次调用 `PUT /api/candidate/exams/{planId}/answer-session`
- **THEN** `exam-service` 必须返回该考生当前有效会话的剩余时间、已保存答案和题目状态，而不是重复创建第二个会话

#### Scenario: 不满足进入条件的考生被拒绝进入答题会话
- **WHEN** 未确认考生、未分配考生、考试未开始、考试已结束、考试已关闭或考生不再可用时请求在线答题会话
- **THEN** `exam-service` 必须拒绝创建或返回答题会话
- **THEN** `exam-web` 必须阻止进入在线答题页并展示可读错误提示

### Requirement: `exam-service` 应返回适用于考生作答的题目快照与答题状态
系统 MUST 为在线答题页返回稳定、可恢复的题目快照和导航状态。答题会话响应 MUST 至少包含考试计划摘要、试卷摘要、`startedAt`、`deadlineAt`、`remainingSeconds`、会话状态，以及按试卷顺序返回的题目列表。每道题 MUST 包含 `paperQuestionId`、题号、题干快照、题型、作答模式、候选作答配置、分值、当前已保存答案和答题状态。`exam-service` MUST 不向考生暴露标准答案、判分规则或成绩结果字段。

#### Scenario: 在线答题页按试卷顺序加载题目
- **WHEN** 合法考生成功打开某场考试的在线答题页
- **THEN** `exam-service` 必须返回该考试关联试卷下全部有效题目，并按 `paper_question.display_order` 升序排列
- **THEN** `exam-web` 必须基于返回结果渲染题号导航、当前题目内容和已保存答案

#### Scenario: 题目响应仅暴露考生可见配置
- **WHEN** `exam-service` 组装在线答题题目响应
- **THEN** 响应中必须只包含考生作答所需的展示配置，例如选项、输入约束和分值
- **THEN** 响应中不得包含标准答案、阅卷结果、分数明细或其他超出本次范围的结果字段

#### Scenario: 答题状态可由服务端稳定恢复
- **WHEN** 某个答题会话已经保存过部分题目的答案
- **THEN** `exam-service` 必须在答题会话响应中返回每道题当前的已答/未答状态与已保存答案
- **THEN** `exam-web` 在页面刷新或重新进入后必须恢复这些状态

### Requirement: `exam-web` 与 `exam-service` 应保存考生答题过程记录
系统 MUST 支持按题保存考生答题记录。`exam-web` 在考生切题、显式保存或其他约定的保存触发点发生时，必须调用 `PUT /api/candidate/exams/{planId}/questions/{paperQuestionId}/answer` 提交当前题目的结构化答案。`exam-service` MUST 以会话维度对答案进行幂等覆盖保存，并在响应中返回更新后的题目状态、最后保存时间和当前剩余时间。

#### Scenario: 考生保存单题答案成功
- **WHEN** 已进入在线答题页的考生提交某道题的合法答案内容
- **THEN** `exam-web` 必须调用 `PUT /api/candidate/exams/{planId}/questions/{paperQuestionId}/answer`
- **THEN** `exam-service` 必须为当前答题会话创建或更新该题的答题记录
- **THEN** `exam-service` 必须返回该题最新答题状态与最后保存时间

#### Scenario: 考生覆盖已保存答案
- **WHEN** 考生对同一题再次修改并保存答案
- **THEN** `exam-service` 必须覆盖该会话下该题此前的答案记录，而不是新增重复记录
- **THEN** `exam-web` 必须以最新保存结果更新当前题目和题号导航状态

#### Scenario: 清空答案后题目状态恢复未答
- **WHEN** 考生将某题答案清空并再次保存
- **THEN** `exam-service` 必须保留该题的过程记录但将其答题状态更新为未答
- **THEN** `exam-web` 必须在题号导航和状态汇总中反映该题未答

#### Scenario: 考生不能修改其他会话或其他考试的答案
- **WHEN** 考生尝试使用不属于自己或不属于当前考试计划的 `paperQuestionId` 保存答案
- **THEN** `exam-service` 必须拒绝该请求
- **THEN** 其他考生或其他考试的答题记录不得被影响

### Requirement: `exam-web` 应展示倒计时、切题能力和答题状态总览
系统 MUST 在在线答题页展示由服务端截止时间驱动的倒计时，并支持考生在题目间切换查看与作答。`exam-web` MUST 为每道题展示至少一种可见状态标识，例如未答、已答或当前题。倒计时结束后，在线答题页 MUST 进入只读超时态，不再允许继续保存答案，但本次能力 MUST 不自动提交试卷，也 MUST 不展示成绩结果。

#### Scenario: 在线答题页展示实时倒计时
- **WHEN** 在线答题会话响应返回 `deadlineAt` 与 `remainingSeconds`
- **THEN** `exam-web` 必须基于该截止时间展示秒级递减的剩余作答时间
- **THEN** 同一答题会话在页面刷新后必须继续沿用相同的截止时间而不是重新计时

#### Scenario: 考生切题时保持当前作答上下文
- **WHEN** 考生在题号导航、上一题或下一题之间切换当前题目
- **THEN** `exam-web` 必须保留已保存的作答结果并切换展示对应题目内容
- **THEN** `exam-web` 必须同步更新当前题标识与整体答题状态展示

#### Scenario: 作答时间耗尽后进入只读超时态
- **WHEN** 在线答题会话的剩余时间减至零或服务端判定当前会话已超时
- **THEN** `exam-service` 必须将该会话视为超时不可写
- **THEN** `exam-web` 必须停止继续保存答案并展示答题时间已结束的只读提示
- **THEN** 本次能力不得因此自动提交试卷或显示成绩单

### Requirement: `exam-service` 应复用带有 TraceNo 关联的脱敏日志能力记录在线答题过程
系统 MUST 在在线答题会话创建/恢复、题目加载、答案保存和超时拒绝过程中复用现有 `TraceNo`、请求日志、响应日志、异常日志和脱敏规则，并输出可检索的在线答题业务日志。日志中 MUST 不得记录明文答案内容、完整 Token、完整 Authorization 头或身份证号。

#### Scenario: 在线答题关键动作可通过业务日志追踪
- **WHEN** 考生进入答题页、恢复答题会话、保存答案或因超时被拒绝保存
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联的业务日志
- **THEN** 日志必须包含考生标识摘要、考试计划标识、题目标识、动作类型和结果摘要

#### Scenario: 答案与身份敏感信息在日志中保持脱敏
- **WHEN** 在线答题请求、响应或异常上下文中包含答案内容、Token、Authorization 头或身份证号
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守统一的日志脱敏规则
