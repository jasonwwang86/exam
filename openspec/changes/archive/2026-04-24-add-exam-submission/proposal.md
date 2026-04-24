## Why

当前仓库已经完成考生端登录、确认信息和在线答题能力，但考试执行流程仍停留在“过程记录”阶段，缺少提交试卷这一闭环。考生无法主动结束考试，系统也不能在截止时间到达时可靠地把答题会话落为最终提交状态，提交结果展示与重复提交保护也没有明确约束，因此需要在不扩展到成绩单、判分或监考大屏的前提下，补齐独立可验收的“提交试卷”模块。

## What Changes

- `exam-web` 在 `src/modules/candidate-portal/` 中扩展主动交卷确认交互、交卷中的防重复触发控制，以及手动交卷/自动交卷后的提交结果展示。
- `exam-service` 新增考生端提交试卷 REST API，并补充服务端自动交卷能力，确保考生在线或离线时都能在截止时间后进入最终提交状态。
- `exam-service` 以现有答题会话为核心持久化提交状态，记录最终提交时间与提交方式，阻止已提交会话继续保存答案或再次生成新的提交结果。
- `exam-service` 扩展考试列表与答题会话 DTO，使 `exam-web` 能够展示已提交、自动提交和不可重复提交的状态摘要，同时继续使用显式 DTO，不直接暴露持久化实体。
- `exam-service` 继续复用 `TraceNo`、请求/响应/异常日志和脱敏规则，为主动提交、自动提交、重复提交拦截补充可检索业务日志。
- 包含范围：主动提交、到时自动提交、提交状态记录、防重复提交，以及 `exam-web` 的提交确认与结果展示；影响 `exam-web` 与 `exam-service`。
- 不包含范围：在线答题能力本身、成绩单、判分、阅卷、监考大屏、动态大屏，以及任何超出“提交试卷闭环与状态落库”的考试结果扩展能力。

## Capabilities

### New Capabilities
- `exam-submission`: `exam-web` 与 `exam-service` 为考生提供主动交卷、截止自动交卷、提交结果展示、提交状态持久化和防重复提交能力。

### Modified Capabilities
- `online-answering-flow`: 在线答题页从“超时只读且不交卷”调整为支持交卷入口，并在截止时间到达时切换为自动交卷后的最终状态展示。
- `candidate-login-and-profile-confirmation`: 考试列表从仅展示答题进度摘要调整为同时展示已提交/自动提交状态摘要，并对已最终提交的考试禁止再次进入答题。

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: 新增 `POST /api/candidate/exams/{planId}/submission`；修改 `GET /api/candidate/exams`、`PUT /api/candidate/exams/{planId}/answer-session`、`PUT /api/candidate/exams/{planId}/questions/{paperQuestionId}/answer`
- Planned DTOs: 交卷结果响应、考试列表项提交状态扩展响应、答题会话提交状态扩展响应、保存答案拒绝后的会话状态响应
- Database impact: 扩展 `exam_answer_session` 增加最终提交时间等字段、补充适用于自动交卷扫描的索引，并将现有仅超时未提交的终态统一迁移为可追踪的最终提交状态
- Dependencies: 依赖既有 `online-answering-flow` 的答题会话与答案记录模型、`candidate-login-and-profile-confirmation` 的考生鉴权与考试列表入口，以及现有 `TraceNo` 与日志脱敏基础能力
