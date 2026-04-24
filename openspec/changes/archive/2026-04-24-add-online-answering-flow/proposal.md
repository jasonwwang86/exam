## Why

当前仓库的考生端流程只覆盖登录、确认信息和可参加考试列表，考生还不能真正进入试卷完成在线答题，系统也无法持续记录答题过程。现在需要在不扩展到提交试卷、成绩单或监考大屏的前提下，补齐“进入答题、加载题目、保存作答、展示倒计时与答题状态”这一段独立可验收的考试执行能力。

## What Changes

- `exam-web` 在现有 `src/modules/candidate-portal/` 基础上扩展在线答题页、题号导航、答题状态面板和倒计时展示，并从可参加考试列表提供进入答题入口。
- `exam-service` 新增考生端在线答题 REST API，覆盖答题会话创建/恢复、题目加载与单题答案保存，继续使用显式 DTO，不直接暴露持久化实体。
- `exam-service` 新增答题过程持久化模型，用于记录考生在某场考试中的答题会话、剩余作答时限和各题答案快照，并补充必要的 MyBatis-Plus Mapper 与 MySQL 表结构。
- `exam-service` 扩展试卷题目快照数据，使在线答题读取的题目展示配置不依赖题库实时变更；继续复用 `TraceNo`、请求/响应/异常日志和脱敏能力。
- 包含范围：题目加载、答题记录保存、倒计时、切题与答题状态展示；影响 `exam-web` 与 `exam-service`。
- 不包含范围：提交试卷、到时自动交卷、成绩单、判分、监考大屏、动态大屏，以及任何超出“在线答题过程记录与展示”的考试结果能力。

## Capabilities

### New Capabilities
- `online-answering-flow`: `exam-web` 与 `exam-service` 为考生提供进入答题、加载题目、保存作答、倒计时和答题状态展示能力。

### Modified Capabilities
- `candidate-login-and-profile-confirmation`: 已确认考生的考试列表从只读展示调整为可对符合条件的考试提供进入答题入口与答题进度摘要，但仍不扩展到提交试卷或成绩查看。

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `GET /api/candidate/exams`, `PUT /api/candidate/exams/{planId}/answer-session`, `PUT /api/candidate/exams/{planId}/questions/{paperQuestionId}/answer`
- Planned DTOs: 考试列表项扩展响应、答题会话响应、题目导航摘要、答题题目详情、保存答案请求、保存答案响应
- Database impact: 新增 `exam_answer_session`、`exam_answer_record` 表及索引；扩展 `paper_question` 增加题目展示配置快照字段并为既有数据补齐回填
- Dependencies: 复用既有试卷管理提供的 `paper`、`paper_question` 数据，考试计划提供的 `exam_plan`、`exam_plan_examinee` 数据，以及考生端登录与确认信息模块提供的 Token、鉴权、`TraceNo` 和日志脱敏基础能力
