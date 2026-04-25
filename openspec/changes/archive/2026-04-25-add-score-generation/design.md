## Context

当前仓库已经完成 `candidate-login-and-profile-confirmation`、`online-answering-flow`、`exam-submission` 和 `add-score-report` 所需的结果读模型，考生能够登录、答题、交卷并在结果存在时查看成绩单。但 `exam-service` 目前只会把答题会话落为 `SUBMITTED` / `AUTO_SUBMITTED`，不会把 `exam_answer_session`、`exam_answer_record` 转换为 `exam_result`、`exam_result_item`，导致考生交卷后长期停留在“待出分”。

这次变更的职责不是重做成绩单，也不是引入人工阅卷，而是在既有读模型上补齐“自动判分与成绩生成”这一段闭环。该 change 主要影响 `exam-service`，`exam-web` 继续复用 `src/modules/candidate-portal/` 既有考试列表和成绩详情页，不新增独立前端模块。由于本次变更涉及状态流转、结果快照生成、幂等落库、日志约束和与交卷流程的衔接，适合单独产出设计文档。

## Goals / Non-Goals

**Goals:**
- 在 `exam-service` 中新增独立的成绩生成服务，把已最终提交的答题会话转换成稳定的 `exam_result` 与 `exam_result_item` 快照。
- 在主动交卷与自动交卷链路中同步尝试触发成绩生成，使现有考试列表与成绩单能力能够基于已生成结果直接工作。
- 为当前题库模型定义自动判分规则，至少覆盖 `SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE`、`TEXT` 四类答案模式的服务端判定方式，并明确不引入模糊匹配、人工复核或部分给分。
- 保持成绩生成幂等：同一考生同一考试重复触发时，只维护一份最新结果快照，不生成重复结果记录。
- 复用统一 `TraceNo`、请求/响应/异常日志与脱敏规则，为成绩生成成功、重复触发、失败和重试输出可检索业务日志。
- 明确 TDD 范围：本次以后端 `exam-service` 为主，强制先写失败测试再实现；`exam-web` 无新增业务逻辑页面，主要做联调与现有页面验证。

**Non-Goals:**
- 不新增或重构 `exam-web` 的在线答题 UI、交卷结果页或成绩详情页。
- 不实现人工阅卷、成绩发布审批、成绩更正、复议、排名统计、标准答案解析或任何大屏能力。
- 不把成绩生成设计成独立消息队列、工作流引擎或分布式任务平台。
- 不修改题库管理的答案配置模型；继续复用已有 `answer_config` / `answer_config_snapshot` 结构。
- 不解决当前 `exam-web` 在线答题页对 `MULTIPLE_CHOICE`、`TRUE_FALSE` 输入控件不足的问题；本次只定义服务端判分规则和结果生成方式。

## Decisions

### Decision 1: 采用“交卷后同步尝试生成 + 独立事务落库 + 失败保留待出分”的策略

`exam-service` 在主动交卷成功和自动交卷落终态后，同步调用成绩生成服务，但成绩生成使用独立事务完成，并以“尽力生成”为原则：如果成绩生成成功，考生随即可以看到成绩；如果生成失败，交卷终态仍然保留，考试列表继续显示“待出分”，后续可通过同一服务重试生成。

这样做的原因是：交卷是考试执行事实，成绩生成是结果处理事实，二者不应被强绑成“同事务同生共死”。如果把交卷和判分放在一个事务里，成绩生成异常会反向导致交卷回滚，不符合考生已提交这一事实，也会让自动交卷调度变得脆弱。备选方案是完全异步任务化或引入消息队列，但对当前单体仓库过重。

### Decision 2: 成绩生成服务集中放在 `cn.jack.exam.service.candidate`，并以 `exam_result` / `exam_result_item` 作为唯一结果落点

本次新增一个面向候选端考试流程的成绩生成服务，例如 `CandidateScoreGenerationService`，放在 `cn.jack.exam.service.candidate` 下，负责：
- 校验会话是否属于最终提交态；
- 读取 `exam_answer_session`、`exam_answer_record`、`paper_question` 快照与必要的 `question` / `question_type` 信息；
- 生成结果聚合与逐题结果；
- 幂等写入 `exam_result`、`exam_result_item`；
- 输出生成日志与失败日志。

不在 Controller 层新增专门的人工操作接口，也不把判分逻辑散落到 `CandidateAnsweringService` 或 `CandidateScoreReportService` 中。这样能保持职责清晰：交卷服务负责状态迁移，成绩生成服务负责结果产出，成绩单服务负责结果读取。备选方案是把判分逻辑直接塞进 `CandidateAnsweringService.submitExam`，但那会让单个服务承担过多职责。

### Decision 3: 结果快照使用“父记录 upsert + 子记录整体替换”的幂等策略

对同一 `(exam_plan_id, examinee_id)`，`exam_result` 保持唯一记录；当成绩生成再次触发时：
- 若父记录不存在，则插入；
- 若父记录已存在，则更新父记录聚合字段；
- 对应的 `exam_result_item` 采用“先删后插”或等效全量替换策略，确保逐题结果与最新生成快照完全一致。

这样比逐题比对更新更简单、可验证，也更符合成绩单是“快照”而不是“过程增量”的定位。备选方案是逐题 upsert，但会增加复杂度，并让删除题目、顺序变化或摘要重算更难保证一致性。

### Decision 4: 判分规则采用确定性、无部分给分的最小闭环

当前仓库已有四类答案模式，本次服务端判分规则明确为：
- `SINGLE_CHOICE`：`selectedOption` 与 `correctOption` 完全相等则得满分，否则 0 分；
- `MULTIPLE_CHOICE`：`selectedOptions` 归一化去重、排序后，与 `correctOptions` 完全相等则得满分，否则 0 分；
- `TRUE_FALSE`：布尔值与 `correctAnswer` 完全相等则得满分，否则 0 分；
- `TEXT`：`textAnswer` 经过去首尾空白、连续空白折叠和大小写归一化后，与任一 `acceptedAnswers` 归一化结果完全相等则得满分，否则 0 分。

本次不实现部分给分、模糊匹配、同义词扩展、分词比对或人工复核。对 `TEXT` 题仍然按现有题库配置自动判分，这里的“文本题”在本次语义上是“可配置参考答案并自动比较的文本输入题”，不是开放式人工阅卷题。备选方案是直接把 `TEXT` 题都标为待人工阅卷，但这会把本 change 扩展到人工流程。

### Decision 5: 判分输入以服务端已保存的快照和答案 JSON 为准，不依赖前端回传临时状态

成绩生成时只读取数据库中已持久化的数据：
- `exam_answer_record.answer_content`
- `paper_question.answer_config_snapshot`
- `paper_question.item_score`
- 题号、题干、题型等试卷快照字段

不读取前端缓存，也不重新请求 `exam-web`。这样可以保证成绩生成是服务端可重放、可验证的确定性过程，也避免题库题目后续编辑影响历史判分。备选方案是实时读取 `question.answer_config`，但这会破坏试卷快照边界。

### Decision 6: `score_status` 在本次成功生成时直接落为可展示状态，不引入发布工作流

由于当前仓库没有成绩发布审批流，本次成功生成的结果直接写成当前可展示状态，例如沿用 `PUBLISHED`，同时写入 `generated_at` 和 `published_at`。失败时不写入半成品结果，继续由列表显示“待出分”。

这样可以最大程度复用现有 `add-score-report` 已完成的查询逻辑，避免为了“生成完成但未发布”再修改成绩单和考试列表判断。备选方案是增加 `GENERATED`、`PENDING_PUBLISH` 等状态，但这会引入新的发布语义和前端适配。

### Decision 7: 成绩生成失败恢复以“同步首试 + 调度补偿重试”完成，不新增外部操作入口

为了避免瞬时异常让已交卷考试永久停留在待出分，本次在 `exam-service` 增加补偿扫描机制，周期性查找已最终提交但尚无结果的会话，重试调用同一套成绩生成服务。这样不需要新增管理端“手工重算成绩”入口，也不需要考生主动触发重试。

备选方案是新增后台接口供管理员手动触发重算，但当前管理端还没有成绩管理模块，这会明显越界。

### Decision 8: 测试策略以后端分层 TDD 为中心，前端只做回归验证

本次适用 `exam-service` 强制 TDD。建议测试分层如下：
- controller：验证交卷后结果可观察性相关 REST 契约不回退，必要时验证无新增接口或既有响应结构稳定；
- service：直接覆盖判分规则、状态迁移后触发生成、重复触发幂等、结果替换、失败补偿、日志分支；
- mapper/custom SQL：验证结果唯一性、结果项替换、查询排序与软删除过滤；
- config：验证成绩生成日志仍受 `TraceNo` 与脱敏规则约束。

`exam-web` 不新增强制 TDD 业务代码；保留现有考试列表与成绩详情测试，并在联调中验证交卷后从“待出分”进入“已出分”的可见行为。

## Risks / Trade-offs

- [当前 `exam-web` 在线答题页没有为 `MULTIPLE_CHOICE`、`TRUE_FALSE` 提供专门输入控件] → 本次明确不扩展答题 UI；服务端仍定义 canonical 判分规则，并把端到端验证重点放在当前已支持的 `SINGLE_CHOICE`、`TEXT`
- [同步尝试生成会增加交卷请求耗时] → 采用轻量、确定性判分规则，并把生成逻辑保持在本地数据库内；失败时不阻塞交卷事实
- [补偿重试可能重复触发同一会话生成] → 使用 `exam_result` 唯一键和全量替换策略保证幂等
- [文本题归一化比较过于严格，可能造成业务上“看起来应该对”的答案被判错] → 在本次设计中明确这是最小闭环，不做模糊判定；如需增强，后续通过新 change 扩展
- [当前成绩结果模型没有显式失败状态] → 本次失败时不写结果快照，由“无结果=待出分”承担外部表现，避免扩展成绩状态语义

## Migration Plan

1. 在 `exam-service` 中补充成绩生成服务、判分规则组件和必要的 Mapper 访问逻辑，默认复用现有 `exam_result`、`exam_result_item` 表。
2. 在主动交卷和自动交卷链路中接入“同步尝试生成”调用，并增加针对已最终提交未生成结果会话的补偿扫描。
3. 若实现中发现 `exam_result`、`exam_result_item` 缺少必要索引或状态字段，仅做兼容性补充，不重做结果模型。
4. 通过 H2 测试配置完成后端测试与前后端联调，验证交卷后结果生成、列表出分和成绩详情一致。
5. 若上线后发现成绩生成异常，可先保留交卷终态、暂停补偿扫描或临时关闭生成调用；由于成绩单本身依赖结果表读取，回滚时不会破坏既有答题和交卷过程数据。

## Open Questions

当前没有阻塞 proposal 落地的开放问题。本次默认：
- 成绩成功生成后直接进入当前可展示状态；
- `TEXT` 题按 `acceptedAnswers` 做精确归一化匹配；
- 不新增管理端重算入口；
- 不扩展 `exam-web` 的题型输入控件。
