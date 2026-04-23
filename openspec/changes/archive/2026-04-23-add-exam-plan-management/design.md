## Context

当前仓库已经完成管理端登录、权限控制、统一管理台壳层、考生管理与试卷管理模块，但还没有把试卷、考试时间和考生范围编排为一次可执行考试的独立模块。考试计划位于路线图中的“考务数据建设”阶段，依赖现有考生主数据与试卷主数据，需要同时新增 `exam-web` 的考试计划页面与 `exam-service` 的 REST、DTO、Mapper 和数据库设计，并严格把范围收敛在“管理端考试计划编排与维护”，避免提前扩展到考生端登录、在线答题、交卷、监考大屏或动态大屏。

## Goals / Non-Goals

**Goals:**
- 在 `exam-web` 中新增落在 `src/modules/exam-plan-management/` 的考试计划模块，支持计划列表、计划创建、编辑、详情查看、状态筛选与状态切换。
- 在 `exam-web` 中提供计划配置流程，覆盖考试时间配置、试卷选择和考生范围设置，并继续接入统一管理台壳层，而不是为单一模块重复建设独立首页框架。
- 在 `exam-service` 中新增 `cn.jack.exam.controller.admin`、`service`、`dto`、`mapper`、`entity` 分层下的考试计划实现，提供清晰的 REST 契约。
- 为考试计划与考生范围关联建立可演进的 MySQL 表结构、MyBatis-Plus Mapper 与初始化/测试数据，支撑后续考生端考试流程复用。
- 复用现有权限、`TraceNo`、请求/响应/异常日志与脱敏规则，为考试计划创建、编辑、范围更新与状态变更增加可检索的关键业务日志。
- 明确本次 change 的 TDD 范围：`exam-service` 全量适用 TDD，`exam-web` 的接口封装、状态处理、表单校验、配置流程、权限/路由逻辑适用 TDD；纯样式与布局微调通过针对性验证完成。

**Non-Goals:**
- 不实现试卷管理、考生管理能力本身，也不重做管理端登录、角色权限和统一管理台壳层。
- 不实现考生端登录、可参加考试列表、在线答题、自动交卷、判分或成绩查询流程。
- 不实现监考大屏、动态大屏、考试中实时监控、统计看板或通知能力。
- 不实现按组织、标签、院系等动态规则自动圈选考生范围；本次只支持基于现有考生数据的显式选人。
- 不引入试卷版本冻结、考试发布审批、异步调度器或其他超出“计划配置与维护”的基础设施。

## Decisions

### Decision 1: `exam-web` 使用统一列表页承载考试计划入口，并以模块内分步配置流程完成时间、试卷和考生范围设置

`exam-web` 在 `src/modules/exam-plan-management/` 下拆分 `pages`、`components`、`services`、`types`。考试计划主页面继续放在统一管理台壳层内，以查询条件区、操作区和数据表格区承载考试计划列表；新建或编辑时，通过模块内分步配置流程维护计划基础信息、考试时间、试卷关联和考生范围，而不是跳出统一壳层或创建新的首页级框架。

这样可以与考生管理、试卷管理现有模式保持一致，同时让“考试计划配置流程”具有足够的结构化交互，不把复杂表单堆进单个弹框。备选方案是把配置流程拆成多个一级路由页面，但这会提升导航和草稿状态同步复杂度；直接在列表页放一个超大表单则会让考生范围选择和状态反馈难以维护。

### Decision 2: REST 契约按 `exam-plans` 主资源与 `exam-plans/{id}/examinees` 关联资源拆分，并复用既有试卷/考生查询能力作为选择来源

后端统一暴露以下受保护接口：
- `/api/admin/exam-plans`：分页查询、详情获取、创建、更新
- `/api/admin/exam-plans/{planId}/examinees`：覆盖考生范围、查询已选考生范围
- `/api/admin/exam-plans/{planId}/status`：状态更新

其中：
- 计划基础信息与时间、试卷关联通过显式 DTO 在 `POST /api/admin/exam-plans` 与 `PUT /api/admin/exam-plans/{id}` 中维护。
- 考生范围通过 `PUT /api/admin/exam-plans/{id}/examinees` 以“全量覆盖当前范围”的方式提交，避免把大批量 `examineeIds` 混进基础信息请求。
- 计划配置流程中的试卷候选与考生候选，不新增专门的选项接口，而是复用已有 `GET /api/admin/papers` 和 `GET /api/admin/examinees` 的受保护查询能力，由 `exam-web` 适配成选择器数据源。

该方案能保持资源边界清晰，也便于未来把已选考生范围做分页预览。备选方案是把 `examineeIds` 直接嵌入创建/编辑请求，但在大名单场景下会放大请求体与校验复杂度。

### Decision 3: `exam-service` 使用 `exam_plan` 与 `exam_plan_examinee` 两张核心表，考生范围以显式关联存储而不是动态规则

后端新增两张核心表：
- `exam_plan`：`id`、`name`、`paper_id`、`start_time`、`end_time`、`status`、`remark`、`deleted`、`created_at`、`updated_at`
- `exam_plan_examinee`：`id`、`exam_plan_id`、`examinee_id`、`created_at`

其中：
- `exam_plan.name` 为必填展示名；本次不要求唯一，但列表与详情必须返回 `id` 作为稳定标识。
- `paper_id` 必须关联到现有有效试卷，且当前 change 不引入试卷快照或版本冻结能力。
- `exam_plan_examinee` 通过 `(exam_plan_id, examinee_id)` 唯一约束防止同一考生被重复加入同一考试计划。
- 考生范围必须显式落表，不采用“状态筛选后动态命中”的规则，以确保后续考生端和统计口径有稳定的关联结果。

后端代码放置建议如下：
- `cn.jack.exam.controller.admin`：`AdminExamPlanController`
- `cn.jack.exam.service.examplan`：计划查询与维护、考生范围维护、状态管理
- `cn.jack.exam.dto.examplan`：查询、创建/编辑、详情、状态更新、范围更新 DTO
- `cn.jack.exam.entity`：`ExamPlan`、`ExamPlanExaminee`
- `cn.jack.exam.mapper` 与 `resources/mapper`：MyBatis-Plus Mapper 与 SQL
- `resources/db/mysql` 与测试库 schema/data：表结构、索引、权限种子与测试数据

备选方案是按院系、标签等规则表达“考生范围”，但当前仓库尚未建设这些维度，过早抽象只会扩大本次边界。

### Decision 4: 考试时间采用 `startTime`/`endTime` 显式窗口，并校验窗口覆盖所选试卷时长

考试计划时间配置采用显式的开始时间与结束时间：
- `startTime` 必须早于 `endTime`
- `endTime - startTime` 必须大于或等于所选试卷的 `durationMinutes`
- 创建与更新时都必须重新校验所选试卷与时间窗口的兼容性

这样既能满足“安排考试”的最小闭环，也能为后续考生端进入考试提供稳定时间窗口。备选方案是只保存开考时间并默认结束时间等于开始时间加试卷时长，但这会削弱管理端对考试时间窗的显式控制，不利于后续迟到/缓冲策略演进。

### Decision 5: 考试状态采用手工维护的有限状态机，当前只覆盖计划编排阶段而不引入执行态自动流转

本次状态枚举限定为：
- `DRAFT`：草稿，默认创建态
- `PUBLISHED`：已安排，表示已完成可用配置
- `CLOSED`：已结束，手工关闭
- `CANCELLED`：已取消，手工取消

状态流转规则如下：
- `DRAFT -> PUBLISHED` 或 `DRAFT -> CANCELLED`
- `PUBLISHED -> CLOSED` 或 `PUBLISHED -> CANCELLED`
- `CLOSED`、`CANCELLED` 为终态，不再允许编辑基础配置或重新开启

其中 `PUBLISHED` 前必须满足：试卷已关联、时间窗口合法、考生范围至少包含一名有效考生。当前 change 不根据系统时钟自动把计划转为“进行中”或“已结束”，避免与尚未实现的考生端执行流程耦合。备选方案是按时间自动派生状态，但这会让“计划状态”与“执行状态”混在一起。

### Decision 6: 复用现有 TraceNo 与日志脱敏基础能力，并把 TDD 约束写入实现路径

所有考试计划接口继续复用现有 `TraceNo` 透传、请求日志、响应日志、异常日志和日志脱敏规则。除此之外，`exam-service` 需要为计划创建、编辑、考生范围覆盖和状态变更输出关键业务日志，日志中仅记录计划 ID、试卷 ID、考生数量、状态变化摘要和脱敏后的操作结果，不输出敏感个人字段集合。

实现策略上：
- `exam-service` 必须先写失败测试，再写实体、DTO、Mapper、Service、Controller 与迁移。
- `exam-web` 对接口封装、表单校验、配置流程状态和权限可见性先写失败测试，再补实现。
- `exam-web` 的样式接入、布局对齐和纯展示文案不强制 TDD，但必须做针对性页面验证。

## Risks / Trade-offs

- [考试计划只关联试卷 ID，不冻结试卷版本] → 本次明确不引入试卷快照；若后续需要“发布后试卷不可变”，通过新的 OpenSpec change 增补版本或快照机制。
- [考生范围采用显式关联，批量选人可能带来较大请求体] → 通过单独的范围更新接口承载名单，并在 UI 中采用筛选加批量选择；超大规模异步导入不纳入本次范围。
- [状态采用手工维护，可能与实际考试时间出现语义偏差] → 在列表与详情中同时展示开始/结束时间和当前状态，并把自动执行态留给后续考试执行模块统一定义。
- [计划创建与考生范围更新拆成两类接口，前端流程需要更多状态编排] → 通过模块内分步配置流程统一封装保存动作，避免把接口复杂度暴露给管理员。
- [禁用或删除考生后，既有考试计划中的范围可能出现失效成员] → 在范围更新与发布校验时过滤无效考生，并在详情查询中返回有效人数与失效提示口径。

## Migration Plan

1. 在 `exam-service` 新增数据库迁移脚本，创建考试计划表、考试计划考生范围关联表、索引与菜单/API 权限种子数据，并同步更新测试用 schema/data。
2. 先完成 `exam-service` 的失败测试、DTO、Mapper、Service、Controller、状态流转校验和范围覆盖逻辑，再提供可联调的 REST 接口。
3. 在 `exam-web` 接入考试计划管理模块、菜单与路由，并基于接口完成列表页、配置流程、考生范围选择、状态切换和详情展示。
4. 联调阶段重点验证：时间窗口校验、试卷时长兼容性、考生范围为空时禁止发布、状态流转限制、无权限场景、`TraceNo` 透传与日志脱敏。
5. 回滚策略以“禁用菜单/权限入口并回退应用版本”为主；若尚未被后续考试执行模块依赖，可通过独立迁移脚本下线考试计划相关表与权限数据。

## Open Questions

当前没有阻塞 proposal 落地的开放问题。本次默认考生范围以显式考生 ID 集合维护，默认只允许选择当前有效考生；如后续需要按标签、院系、批次等规则动态圈选，或需要试卷版本冻结、自动状态流转、考试通知能力，应通过新的 OpenSpec change 增补，而不是在本次 change 中扩边界。
