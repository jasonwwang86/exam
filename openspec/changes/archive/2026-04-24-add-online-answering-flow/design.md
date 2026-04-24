## Context

当前仓库已经完成考生端登录、身份确认和可参加考试列表能力，但考生端流程停留在“待考”阶段，无法真正进入试卷作答，也没有过程化答题记录。在线答题位于路线图中的“考生考试主流程”核心环节，依赖已完成的 `paper-management`、`exam-plan-management` 与 `candidate-login-and-profile-confirmation`，需要同时扩展 `exam-web` 的考生端页面交互和 `exam-service` 的会话、题目读取、答案保存与过程日志能力。

本次变更跨越前端模块、考生端接口、持久化设计与既有试卷快照模型，且存在时间边界、答题恢复、日志脱敏和数据库兼容性约束，因此需要单独设计文档先把边界和技术决策固定下来。

## Goals / Non-Goals

**Goals:**
- 在 `exam-web` 的 `src/modules/candidate-portal/` 中补齐在线答题页，支持从考试列表进入作答、展示题目、切题、查看答题状态和倒计时。
- 在 `exam-service` 中为考生端提供在线答题会话加载/恢复和单题答案保存能力，继续通过 `cn.jack.exam.controller.candidate`、`service.candidate`、`dto.candidate` 暴露清晰的 REST 契约。
- 新增可恢复的答题过程持久化模型，使系统能够记录考生在某场考试中的开始时间、截止时间、最后保存时间和逐题答案记录。
- 保证在线答题加载的数据基于稳定快照，不因题库后续修改直接改变考生已进入考试时看到的题目展示配置。
- 继续复用现有候选鉴权、`TraceNo`、请求/响应/异常日志和脱敏规则，并为在线答题关键动作补充可检索业务日志。
- 明确 TDD 范围：`exam-service` 全量适用 TDD；`exam-web` 中答题会话装配、倒计时、切题状态、接口封装和路由进入控制适用 TDD；纯样式与版式调整通过针对性页面验证完成。
- 在 proposal、design、tasks 中持续明确本次只覆盖题目加载、答题记录保存、倒计时和答题状态展示，不扩展到提交试卷、成绩单或监考大屏。

**Non-Goals:**
- 不实现主动交卷、到时自动交卷、判分、成绩单或监考类能力。
- 不重做考生端登录与确认信息主流程，仅在其基础上扩展进入答题与状态展示。
- 不引入分布式缓存、WebSocket 推送、多端实时同步或防作弊能力。
- 不在本次变更中补齐完整阅卷字段、客观题判分结果或考试结果落库。
- 不扩展管理端试卷管理 UI，只在必要处调整试卷题目快照存储结构以支撑考生端稳定读取。

## Decisions

### Decision 1: 继续扩展 `candidate-portal` 模块，而不是新建独立考生考试模块

前端仍以 `exam-web/src/modules/candidate-portal/` 作为考生端主模块，在现有 `pages`、`services`、`types` 基础上新增在线答题相关 `components`、`hooks` 和页面，例如 `/candidate/exams/:planId/answer`。考试列表页继续作为答题入口页，已确认考生只对可作答考试显示进入答题按钮；登录页、确认页、考试列表页和答题页保持同一考生端视觉语言，不接入管理端壳层。

这样做的理由是考生端目前已经形成独立模块和路由分组，在线答题天然属于该流程的后续步骤。若再新建平行模块，会让考生端会话状态、路由守卫和缓存散落到多个目录，反而增加实现复杂度。备选方案是新建 `online-answering` 模块并在 App 层协调，但当前收益不足。

### Decision 2: 使用“每考生每考试唯一答题会话 + 每题一条答案记录”的持久化模型

后端新增两张核心表：
- `exam_answer_session`：记录 `exam_plan_id`、`examinee_id`、`paper_id`、`started_at`、`deadline_at`、`status`、`last_saved_at`、`created_at`、`updated_at`，并对 `(exam_plan_id, examinee_id)` 建唯一约束，确保同一考生在同一考试中只有一个有效答题会话。
- `exam_answer_record`：记录 `session_id`、`paper_question_id`、`question_id`、`answer_content`、`answer_status`、`last_saved_at`、`created_at`、`updated_at`，并对 `(session_id, paper_question_id)` 建唯一约束，用于逐题幂等覆盖保存。

`deadline_at` 的计算规则固定为 `min(exam_plan.end_time, started_at + paper.duration_minutes)`，首次进入时写入并在后续恢复时保持不变。这样可以同时满足“试卷时长”和“考试计划结束时间”两个约束，并避免页面刷新后重新计时。备选方案是只在前端临时计算倒计时或完全不落库会话起始时间，但那会让恢复、超时校验和过程追踪失去稳定依据。

### Decision 3: 扩展 `paper_question` 快照，在线答题始终读取快照而不是题库实时配置

当前 `paper_question` 已保存题干、题型和分值快照，但还没有足够支撑前端渲染选项或输入约束的配置快照。为避免题库题目在考试前后被修改导致考生看到的内容漂移，本次对 `paper_question` 增加 `answer_config_snapshot`（JSON）字段，并在迁移时把现有 `question.answer_config` 回填到历史有效试卷题目记录。

在线答题组装题目时，以 `paper_question` 的快照为主，再由 `service.candidate` 在返回 DTO 前做“候选配置裁剪”，去除标准答案、判分规则等不应返回给考生的字段。备选方案是直接读取 `question.answer_config` 实时值，这会破坏试卷作为稳定编排结果的边界，也会放大后续题库编辑对考试执行的影响。

### Decision 4: 候选端 API 采用“加载/恢复答题会话 + 单题保存答案”两类资源

本次 REST 契约保持最小集合：
- `GET /api/candidate/exams`：继续作为已确认考生的考试列表接口，但列表项扩展 `canEnterAnswering`、`answeringStatus`、`remainingSeconds` 等摘要字段。
- `PUT /api/candidate/exams/{planId}/answer-session`：创建或恢复当前考生在该考试下的答题会话，并返回完整答题页所需的会话和题目数据。
- `PUT /api/candidate/exams/{planId}/questions/{paperQuestionId}/answer`：按题保存结构化答案，返回当前题最新状态、更新时间、剩余时间和整体状态摘要。

这种设计把“进入/恢复答题”和“保存过程”清晰分开，既避免使用带副作用的 `GET` 创建会话，也不需要为“切题”增加额外服务端接口，因为切题是 `exam-web` 的本地 UI 行为。备选方案是拆成更多端点，例如单独的题目分页接口、导航状态接口或显式“开始考试”接口，但在本次范围内没有必要。

### Decision 5: 倒计时与超时校验采用前后端双重控制，超时后进入只读态而非自动交卷

`exam-service` 以 `deadlineAt` 和会话状态作为最终判定依据，所有会话恢复和答案保存请求都必须检查当前时间是否已超过截止时间；超时后，会话状态切换为 `TIME_EXPIRED`，保存接口直接拒绝写入。`exam-web` 以服务端返回的 `deadlineAt` 为基准做秒级本地倒计时展示，在倒计时结束或收到服务端超时响应后，将页面切换到只读提示态，保留题目与已保存答案浏览，但不再允许继续保存。

这样做可以覆盖浏览器时间不准、页面挂起恢复和多次刷新等情况，同时严格遵守“本次不实现自动交卷”的范围约束。备选方案是时间到自动提交或自动锁屏后跳转结果页，但这会直接扩展到交卷和成绩领域。

### Decision 6: 后端代码继续落在 `cn.jack.exam` 现有候选端分层中，日志遵守统一脱敏边界

后端建议放置方式如下：
- `cn.jack.exam.controller.candidate`：扩展或新增候选在线答题 Controller，负责答题会话与保存接口。
- `cn.jack.exam.service.candidate`：新增答题会话装配、答题保存、会话超时判定、候选配置裁剪逻辑。
- `cn.jack.exam.dto.candidate`：新增答题会话响应、题目导航项、题目详情、保存答案请求/响应、考试列表摘要扩展 DTO。
- `cn.jack.exam.entity`：新增 `ExamAnswerSession`、`ExamAnswerRecord`，并扩展 `PaperQuestion`。
- `cn.jack.exam.mapper` 与 `resources/mapper`：新增答题表 Mapper/XML，并扩展考试列表与试卷题目查询 SQL。
- `config`：复用现有考生 Token 鉴权链路，不新增独立认证体系。

日志方面，在线答题会话创建/恢复、答案保存成功/失败、超时拒绝必须输出业务日志，并始终带 `TraceNo`。日志只记录考生 ID 摘要、考试计划 ID、题目标识、动作结果和会话状态，不输出明文答案、完整 Token、完整 Authorization 头或身份证号。备选方案是仅依赖通用 HTTP 日志，但那不足以支撑答题过程问题排查。

### Decision 7: TDD 先覆盖后端会话边界和前端关键交互，再实现页面与 DTO 细节

实现顺序遵循仓库约束：
- `exam-service` 先写失败测试，覆盖会话首次创建、会话恢复、截止时间计算、未分配/未开始/已结束拦截、单题答案覆盖保存、清空答案回到未答、超时拒绝与日志脱敏。
- `exam-web` 先写失败测试，覆盖考试列表进入答题入口、答题页恢复、题目切换、保存后状态变更、倒计时归零只读和异常提示。
- `exam-web` 的样式、题号布局和响应式排版不强制 TDD，但必须通过针对性页面验证确认桌面和移动宽度下可用。

## Risks / Trade-offs

- [为 `paper_question` 增加快照会引入数据冗余] → 通过只保存在线答题必须的展示配置，并在历史数据迁移时一次性回填来控制复杂度。
- [答题过程仅按题保存，浏览器中未触发保存的临时输入可能丢失] → 通过把“切题”和显式保存设为标准保存触发点，并在设计中明确保存边界。
- [未实现自动交卷可能让超时后的会话停留在只读态] → 这是本次有意收敛的边界，超时后只负责停止继续保存，不承担交卷和判分职责。
- [同一考生多标签页并发作答可能引起最后一次保存覆盖] → 先接受“后写覆盖前写”的单会话模型，并通过 `lastSavedAt` 帮助定位；更强并发冲突控制留待后续 change。
- [考试列表增加进入答题入口会改变既有“只读列表”行为] → 通过修改 `candidate-login-and-profile-confirmation` capability 显式记录该变化，并继续限制不扩展到交卷和成绩入口。

## Migration Plan

1. 在 `exam-service` 的 MySQL DDL 与测试 schema 中新增 `exam_answer_session`、`exam_answer_record`，扩展 `paper_question.answer_config_snapshot` 字段与索引。
2. 为现有有效 `paper_question` 执行一次性回填，把当前 `question.answer_config` 复制到新快照字段，确保历史试卷也能被在线答题读取。
3. 先完成后端失败测试、实体、Mapper、Service、Controller 和 DTO，再开放可联调的候选端在线答题接口。
4. 在 `exam-web` 的 `candidate-portal` 模块中补答题入口、答题页和状态组件，接入新接口并完成前端测试。
5. 联调时重点验证：进入答题条件、题目顺序与快照内容、已答/未答状态切换、刷新恢复、倒计时一致性、超时只读、TraceNo 透传和日志脱敏。
6. 回滚策略以“下线答题入口并回退应用版本”为主；数据库层若需紧急回滚，可保留新增答题表不再写入，避免破坏已记录的过程数据。

## Open Questions

当前没有阻塞 proposal 落地的开放问题。本次默认答案保存使用结构化 JSON，题目展示配置从 `answer_config_snapshot` 裁剪后返回，超时后进入只读不可写状态但不自动交卷；如后续需要自动交卷、过程防作弊、断网本地缓存或成绩生成，应通过新的 OpenSpec change 单独扩展。
