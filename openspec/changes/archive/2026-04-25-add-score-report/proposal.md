## Why

当前仓库已经覆盖考生登录、确认信息、在线答题与提交试卷，但考生在交卷后仍缺少查看考试结果的正式入口，系统也没有稳定的成绩结果读模型来承载总分、各题得分与作答摘要。现在需要在不扩展到在线答题、提交试卷流程、动态大屏或考试计划的前提下，补齐“成绩查询与成绩详情展示”这一段独立可验收的结果输出能力。

## What Changes

- `exam-web` 在 `src/modules/candidate-portal/` 下新增成绩单入口、成绩详情页和作答摘要展示，并继续沿用现有考生端路由与样式体系。
- `exam-service` 新增考生成绩查询 REST API，提供成绩总览、成绩详情、试卷信息与作答摘要查询能力，继续使用显式 DTO，不直接暴露持久化实体。
- `exam-service` 引入面向查询的成绩结果模型与汇总逻辑，统一承载总分、各题得分、已答/未答统计和提交摘要；该模型由前置判分或成绩生成机制写入，本次不负责判分生成过程本身。
- `exam-service` 扩展考生考试列表摘要，使已交卷且存在成绩结果的考试可以展示成绩状态与成绩单入口；未发布、无权限或尚未具备结果的记录继续遵守现有边界控制。
- 包含范围：分数计算结果展示、成绩详情、试卷信息与作答摘要；影响 `exam-web` 与 `exam-service`。
- 不包含范围：在线答题、提交试卷、判分规则实现、成绩生成任务编排、动态大屏、监考大屏、考试计划，以及任何超出“成绩查询与展示”的运营可视化能力。

## Capabilities

### New Capabilities
- `score-report`: `exam-web` 与 `exam-service` 为考生提供成绩单查询、成绩详情展示、试卷信息展示与作答摘要查看能力。

### Modified Capabilities
- `candidate-login-and-profile-confirmation`: 已确认考生的考试列表从只覆盖待参加/进行中的考试，扩展为同时展示已交卷与已出分考试的结果状态摘要，并在满足条件时提供成绩单入口。

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned frontend structure: 继续落在 `exam-web/src/modules/candidate-portal/pages|components|services|hooks|types`，必要的通用展示组件落在 `shared/`
- Planned REST endpoints: `GET /api/candidate/exams`, `GET /api/candidate/exams/{planId}/score-report`
- Planned DTOs: 考试列表项成绩摘要扩展响应、成绩单详情响应、成绩总览摘要、试卷信息摘要、作答摘要项、题目得分明细项
- Database impact: 新增 `exam_result`、`exam_result_item` 表及查询索引，用于承载成绩结果模型；若前置判分模块先落地等价结果模型，则本 change 复用既有表并仅补充查询字段/索引
- Dependencies: 复用既有考生端登录与确认信息模块、在线答题/提交试卷模块输出的 `exam_answer_session`、`exam_answer_record` 与提交终态，以及前置判分或成绩生成机制写入的成绩结果数据；继续复用 `TraceNo`、请求/响应/异常日志与日志脱敏基础能力
