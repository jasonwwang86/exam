## Context

当前仓库已经完成考生端登录、身份确认、在线答题和提交试卷，考生能够完成考试执行主流程，但结果输出仍停留在“已交卷”层面，缺少可查询的成绩单页面与稳定的成绩结果读模型。成绩单位于路线图的“结果输出模块”，依赖既有 `candidate-login-and-profile-confirmation`、`online-answering-flow`、`exam-submission` 以及外部的判分或成绩生成机制，需要同时扩展 `exam-web` 的考生端页面、`exam-service` 的成绩查询契约、结果模型与汇总查询逻辑。

本次变更跨越前端模块、候选端 REST、持久化读模型、列表行为调整与日志约束，并且必须明确把边界收敛在“成绩查询与展示”，因此需要先把技术决策和不包含项固定下来。

## Goals / Non-Goals

**Goals:**
- 在 `exam-web/src/modules/candidate-portal/` 中新增成绩单入口与成绩详情页，支持考生查看总分、成绩状态、试卷信息、提交摘要与逐题作答摘要。
- 在 `exam-service` 中通过 `cn.jack.exam.controller.candidate`、`service.candidate`、`dto.candidate` 提供 `GET /api/candidate/exams/{planId}/score-report` 契约，并扩展 `GET /api/candidate/exams` 的成绩摘要字段。
- 为结果输出建立统一的成绩结果模型，承载考试总分、各题得分、作答状态、提交时间和结果生成时间，供后续判分机制写入、本次查询能力读取。
- 明确列表与详情边界：考试列表负责展示成绩状态与进入入口，成绩详情页负责展示分数结果、试卷信息与作答摘要，不把交卷、判分或考试编排逻辑拉入本 change。
- 继续复用现有候选鉴权、`TraceNo`、请求/响应/异常日志和脱敏规则，并为成绩查询动作补充可检索业务日志。
- 明确 TDD 范围：`exam-service` 全量适用 TDD；`exam-web` 中接口封装、成绩状态判定、路由进入控制、数据转换与关键展示逻辑适用 TDD；纯样式和版式调整通过针对性页面验证完成。
- 在 proposal、design、tasks 中显式标注本模块只包含分数计算结果展示、成绩详情、试卷信息与作答摘要，不扩展到动态大屏、考试计划、在线答题或提交试卷。

**Non-Goals:**
- 不实现在线答题、保存答案、主动交卷、到时自动交卷或交卷结果页重构。
- 不实现判分规则、阅卷算法、成绩生成任务、结果发布工作流或成绩更正流程。
- 不扩展考试计划、管理端成绩管理、大屏展示、监考视图或任何运营分析看板。
- 不新增独立考生端壳层，继续复用现有 `candidate-portal` 路由分组与页面风格。
- 不在本次 change 中输出标准答案、解析、排名或横向统计报表。

## Decisions

### Decision 1: 继续扩展 `candidate-portal`，在现有考试列表中承接成绩单入口，而不是新建独立结果模块

前端继续以 `exam-web/src/modules/candidate-portal/` 作为考生端单一业务模块，在现有考试列表基础上增加成绩状态与“查看成绩”入口，并新增成绩详情页路由，例如 `/candidate/exams/:planId/report`。页面拆分延续现有结构：`pages` 负责列表页和成绩详情页，`services` 负责查询 API，`types` 对齐 DTO，`components` 放成绩总览卡、试卷信息区、提交摘要区和作答摘要列表，跨页面复用的标签或状态展示组件再考虑沉淀到 `shared/`。

这样做可以复用考生登录态、路由守卫、现有考试列表数据源和视觉体系，避免把“考试执行”和“结果输出”拆成两个平行模块后造成状态、路由和缓存散落。备选方案是新建 `score-report` 前端模块，但当前考生端能力还集中在一个路由分组中，独立模块收益不足。

### Decision 2: 使用独立的成绩结果读模型 `exam_result` / `exam_result_item`，由判分模块写入，本次只负责查询与汇总

后端引入两张结果模型表：
- `exam_result`：记录 `exam_plan_id`、`examinee_id`、`session_id`、`paper_id`、`score_status`、`total_score`、`objective_score`、`subjective_score`、`answered_count`、`unanswered_count`、`submitted_at`、`generated_at`、`published_at` 等聚合字段，并对 `(exam_plan_id, examinee_id)` 建唯一约束。
- `exam_result_item`：记录 `result_id`、`paper_question_id`、`question_id`、`question_no`、`question_stem_snapshot`、`question_type_name_snapshot`、`item_score`、`awarded_score`、`answer_status`、`answer_summary`、`judge_status` 等逐题展示字段，并对 `(result_id, paper_question_id)` 建唯一约束。

这样可以把“成绩生成”与“成绩展示”通过稳定结果模型解耦：判分机制负责写入结果，本次成绩单模块只做读取、鉴权与展示，不需要在查询接口中临时拼装复杂判分逻辑。备选方案是每次查询时从 `exam_answer_record`、`paper_question` 和判分中间表实时汇总，但那会让查询接口与判分细节强耦合，也不利于后续成绩发布或重算。

### Decision 3: REST 契约保持最小集合，列表摘要和详情查询分层承载

本次 REST 契约保持两类接口：
- `GET /api/candidate/exams`：继续作为考生端考试列表接口，但列表项扩展 `scoreStatus`、`reportAvailable`、`totalScore`、`resultGeneratedAt` 等成绩摘要字段，并允许展示已交卷/已出分考试。
- `GET /api/candidate/exams/{planId}/score-report`：返回成绩详情页所需数据，至少包含考试与试卷信息、成绩总览、提交摘要、作答摘要和逐题得分明细。

列表接口只负责展示入口和状态，详情接口负责完整成绩单内容，避免让考试列表承担过多详情渲染职责，也避免拆出额外的“成绩汇总列表接口”造成重复查询。备选方案是再新增 `/api/candidate/score-reports` 列表资源，但当前考生端已经以考试列表为主导航，单独列表会带来重复入口。

### Decision 4: 成绩查询只面向“当前考生自己的最终提交且结果已生成”的考试开放

`exam-service` 在查询成绩单时必须同时校验：
- 当前用户属于目标考试计划的被分配考生；
- 当前考试存在 `SUBMITTED` 或 `AUTO_SUBMITTED` 的最终提交会话；
- 成绩结果模型中存在可展示的结果记录，且状态满足已生成或已发布；
- 若结果尚未生成，仅在考试列表返回“待出分/生成中”摘要，不返回详情内容。

这样可以把访问控制、数据就绪状态和展示边界分清楚，避免考生通过猜测 `planId` 访问他人成绩，也避免在结果未生成时返回半成品详情。备选方案是对详情接口返回空壳数据并由前端自行判定，但那会让前后端状态语义变得模糊。

### Decision 5: 作答摘要以“我的作答 + 得分结果 + 题目快照摘要”为主，不展示标准答案、解析或运营统计

成绩详情页的逐题区域使用 `exam_result_item` 与既有试卷快照数据构建，展示范围控制为：
- 题号、题型名称、题干快照摘要；
- 我的作答摘要或“未作答”标记；
- 该题满分、得分、判定状态；
- 必要的提交时间/阅卷完成时间摘要。

明确不返回标准答案、解析、同场对比排名、全班统计或试题正确率，这些能力都不属于本次“成绩单”范围。这样既满足“成绩详情与作答摘要”的目标，也避免把成绩单演化为解析页或大屏统计页。备选方案是直接返回完整题目解析，但这会越界到后续能力。

### Decision 6: 后端代码继续落在 `cn.jack.exam` 的候选端分层中，并把查询汇总逻辑集中在 service 层

建议放置方式如下：
- `cn.jack.exam.controller.candidate`：新增或扩展成绩查询 Controller，暴露考试列表和成绩详情查询接口。
- `cn.jack.exam.service.candidate`：新增成绩单查询服务，负责权限校验、结果状态判定、列表摘要装配与详情汇总。
- `cn.jack.exam.dto.candidate`：新增成绩摘要字段、成绩单详情响应、总览区 DTO、试卷信息 DTO、提交摘要 DTO、作答摘要项 DTO。
- `cn.jack.exam.entity`：新增 `ExamResult`、`ExamResultItem`，必要时扩展现有会话实体的查询关联字段。
- `cn.jack.exam.mapper` 与 `resources/mapper`：新增成绩结果表 Mapper/XML，并扩展考试列表 SQL 以拼接成绩摘要字段。

汇总逻辑放在 service 层而不是 Controller 或 Mapper XML 里，可以把“状态判定、字段裁剪、空值处理、结果未就绪分支”集中管理，保持 REST 契约清晰。备选方案是把大部分拼装逻辑放在 SQL 中一次查完，但那会让条件判断和可维护性变差。

### Decision 7: 继续复用统一 `TraceNo` 与日志脱敏能力，并把测试边界写入实现路径

所有成绩查询接口继续复用现有 `TraceNo` 透传、请求日志、响应日志、异常日志与脱敏规则。除此之外，`exam-service` 需要为考试列表成绩摘要加载、成绩单详情查询、成绩未就绪拒绝和越权访问拒绝输出关键业务日志，日志中仅记录考生 ID 摘要、考试计划 ID、结果状态和查询结果摘要，不输出明文答案、完整 Token、完整 Authorization 头、身份证号或完整题目答案内容。

测试策略遵守仓库约束：
- `exam-service` 先写失败测试，再实现 Controller、Service、Mapper。Service 层直接测试必须覆盖结果状态判定、本人权限校验、已交卷未出分分支、逐题汇总、空结果处理和幂等查询。
- `exam-service` 的 Controller 测试聚焦 REST 契约、鉴权失败、参数校验、HTTP 状态码、响应结构与 `TraceNo`。
- `exam-service` 的 Mapper/custom SQL 测试聚焦成绩汇总查询条件、软删除过滤、结果项排序和唯一性约束。
- `exam-service` 的 config 测试只在本 change 触及日志或异常映射时补充。
- `exam-web` 先写失败测试，覆盖成绩状态入口展示、未出分禁入、详情页加载、异常态与数据转换；纯样式和版式不强制 TDD，但必须做针对性页面验证。

## Risks / Trade-offs

- [新增结果读模型会引入与判分模块的协同成本] → 通过在设计中明确 `exam_result` / `exam_result_item` 作为统一写入目标，把接口边界前置固定。
- [考试列表从“可参加考试”扩展为“候选端考试列表”会改变既有过滤逻辑] → 通过修改现有 capability 明确这一行为变化，并把显示状态和结果入口规则放在 service 层集中控制。
- [逐题作答摘要如果直接返回完整答案 JSON，可能带来敏感信息或展示复杂度问题] → 通过结果模型保存面向展示的 `answer_summary`，避免前端自行解析原始答案结构。
- [结果未生成时前端可能出现空页面或误导性入口] → 列表只返回待出分状态，不开放详情入口；详情接口对未就绪结果直接返回明确错误。
- [后续若新增排名、解析或复查能力，当前 DTO 可能继续膨胀] → 本次 DTO 只覆盖总分、试卷信息、提交摘要和作答摘要，其他扩展通过新的 OpenSpec change 进入。

## Migration Plan

1. 在 `exam-service` 的 MySQL DDL、测试 schema 与初始化数据中新增 `exam_result`、`exam_result_item` 表与必要索引，并约定与 `exam_answer_session` 的关联方式。
2. 若前置判分模块尚未落地统一结果表，则先提供最小兼容的数据迁移/写入约定，确保后续判分模块可以写入这两张表；本次不实现判分逻辑。
3. 先完成 `exam-service` 的失败测试、Mapper、DTO、Service、Controller 和考试列表扩展，再开放可联调的成绩查询接口。
4. 在 `exam-web/src/modules/candidate-portal/` 中补充成绩状态展示、成绩详情页和必要组件，接入新 DTO 与查询接口。
5. 联调时使用测试配置验证：已交卷未出分、已出分可查看、越权访问拒绝、试卷信息与逐题摘要一致、`TraceNo` 透传和日志脱敏。
6. 回滚策略以“下线成绩入口并回退应用版本”为主；数据库层保留新增结果表但停止读取，不影响既有交卷和答题数据。

## Open Questions

当前没有阻塞本次 proposal 落地的开放问题。本次默认由前置判分或成绩生成机制写入 `exam_result` / `exam_result_item`，成绩单只负责查询和展示；如果后续需要结果发布审批、排名、解析或成绩复查，应通过新的 OpenSpec change 单独扩展。
