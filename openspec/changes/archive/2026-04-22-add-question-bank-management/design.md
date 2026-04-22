## Context

当前仓库已经完成管理端登录、权限控制、统一管理台壳层、首页与考生管理模块，但还没有可复用的试题主数据模块。题库管理位于路线图的“考务数据建设”阶段，依赖现有管理端认证与权限能力，需要同时新增 `exam-web` 的题库管理页面与 `exam-service` 的 REST、DTO、Mapper 和数据库设计，并严格把范围收敛在“试题基础属性管理”，避免提前扩展到试卷管理、考试计划、考生答题或题目分类。

## Goals / Non-Goals

**Goals:**
- 在 `exam-web` 中新增落在 `src/modules/question-bank/` 的题库管理模块，支持题目查询、录入、编辑、删除，以及题型管理。
- 在 `exam-web` 中为题目表单提供难度、分值和答案配置能力，并保持与 `exam-service` DTO 契约一致。
- 在 `exam-service` 中新增 `cn.jack.exam.controller.admin`、`service`、`dto`、`mapper`、`entity` 分层下的题库管理实现，提供清晰的 REST 契约。
- 为试题与题型建立可演进的 MySQL 表结构、MyBatis-Plus Mapper 与初始化/测试数据，支持后续试卷管理复用。
- 复用现有权限、`TraceNo`、请求/响应/异常日志与脱敏规则，为题目录入、编辑、删除和题型维护补充可检索的关键业务日志。
- 明确本次 change 的 TDD 范围：`exam-service` 全量适用 TDD，`exam-web` 的接口封装、查询状态、表单校验、权限/路由逻辑适用 TDD；纯样式与布局微调通过针对性验证完成。

**Non-Goals:**
- 不实现试卷管理、考试计划、考生答题、阅卷或成绩计算流程。
- 不实现题目分类、知识点标签、批量导入导出、随机抽题或组卷规则。
- 不重做管理端登录、角色权限基础设施或统一管理台壳层。
- 不引入消息队列、全文检索、对象存储等超出当前模块需要的新基础设施。

## Decisions

### Decision 1: `exam-web` 使用单一题库管理页面承载题目列表，题型管理作为模块内子工作流接入

`exam-web` 在 `src/modules/question-bank/` 下拆分 `pages`、`components`、`services`、`types`。题库管理主页面继续沿用统一管理台壳层，以查询区、工具栏和数据表格区承载题目列表；题目录入与编辑使用弹框或抽屉表单，题型管理通过同模块内的二级弹框或抽屉进入，而不是新建独立首页级框架。

这样可以复用现有 `AdminLayout` 与列表型业务页结构，减少路由和页面跳转复杂度，同时为后续试卷管理继续复用统一模块入口保留空间。备选方案是为题型管理单独创建一级路由页面，但在当前范围内会增加导航和状态同步成本，收益有限。

### Decision 2: REST 契约按 `questions` 与 `question-types` 两类资源拆分，并全部通过显式 DTO 交互

后端统一暴露以下受保护资源接口：
- `/api/admin/questions`：分页查询、详情获取、创建、更新、删除
- `/api/admin/question-types`：列表查询、创建、更新、删除

`dto/question` 下分别定义题目查询条件、分页项、详情响应、创建/更新请求，以及题型列表项、题型创建/更新请求等 DTO，不直接暴露实体。权限控制沿用现有菜单/API 权限模型，为题库管理至少补充菜单权限、题目查询/维护权限和题型管理权限。

这一方案与现有 `examinees` 模块的资源化接口风格一致，前后端职责也更清晰。备选方案是将题型维护并入题目接口，但会让接口语义和权限粒度变得混乱。

### Decision 3: `exam-service` 使用独立题型表与题目表，题目答案配置采用按题型模式校验的 JSON 结构

后端新增两张核心表：
- `question_type`：`id`、`name`、`answer_mode`、`sort`、`remark`、`deleted`、`created_at`、`updated_at`
- `question`：`id`、`stem`、`question_type_id`、`difficulty`、`score`、`answer_config`、`deleted`、`created_at`、`updated_at`

其中：
- `question_type.name` 需要唯一约束，删除采用逻辑删除；若仍被有效题目引用，则删除请求必须被拒绝。
- `question.difficulty` 使用稳定枚举值 `EASY`、`MEDIUM`、`HARD`。
- `question.score` 使用 `DECIMAL(6,2)`，兼容整数和小数分值。
- `question.answer_config` 使用 JSON 文本保存题目答案配置，并依据题型的 `answer_mode` 做结构化校验。

本次支持的 `answer_mode` 固定为 `SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE`、`TEXT` 四类。这样既能满足题型管理，又能把答案配置边界限制在“基础属性管理”范围内。备选方案是为每种题型建立完全不同的表结构，但这会在当前阶段引入更高的数据建模和查询复杂度。

后端代码放置建议如下：
- `cn.jack.exam.controller.admin`：`AdminQuestionController`、`AdminQuestionTypeController`
- `cn.jack.exam.service.question`：题目查询与维护、题型维护、答案配置校验
- `cn.jack.exam.dto.question`：题目与题型 DTO
- `cn.jack.exam.entity`：`Question`、`QuestionType`
- `cn.jack.exam.mapper` 与 `resources/mapper`：MyBatis-Plus Mapper 与 SQL
- `resources/db/mysql` 与测试库 schema/data：表结构、索引、权限种子与测试数据

### Decision 4: 题目录入与编辑时按照题型模式校验答案配置，前后端共用同一套字段语义

`exam-web` 题目表单根据所选题型的 `answerMode` 动态展示答案配置区域；`exam-service` 在创建与更新接口中做最终校验，至少保证：
- `SINGLE_CHOICE`：必须提供候选项集合和唯一正确答案
- `MULTIPLE_CHOICE`：必须提供候选项集合和至少一个正确答案
- `TRUE_FALSE`：必须提供布尔型标准答案
- `TEXT`：必须提供至少一个参考答案文本

这种“前端引导 + 后端兜底”的方案可以防止仅依赖 UI 约束造成的脏数据进入库表，也避免题库模块在当前阶段扩展为复杂富文本或专用题型引擎。备选方案是仅依赖前端表单校验，但这会削弱 `exam-service` 作为边界层的可靠性。

### Decision 5: 复用现有 TraceNo 与日志脱敏能力，并把题库业务日志限定在通用基础能力之上

所有题库管理接口继续复用现有 `TraceNo` 透传、请求日志、响应日志、异常日志和 `LogSanitizer` 脱敏规则。除此之外，`exam-service` 需要为题目录入、编辑、删除、题型创建、题型更新、题型删除输出关键业务日志，日志中仅记录题目 ID、题型 ID、操作结果和脱敏后的字段摘要，不输出完整答案配置正文或其他敏感内容。

这样既遵守已有日志约束，也能为后续联调、排障和权限审计提供稳定链路。备选方案是只依赖通用 HTTP 日志，但那不足以支撑题型删除被引用拒绝、答案配置校验失败等业务问题定位。

## Risks / Trade-offs

- [题型可维护但答案模式固定，管理员可能期待完全自定义题型行为] → 在规格中明确题型管理仅维护基础元数据，答案行为只支持四种内建模式。
- [答案配置使用 JSON 会降低数据库层面的强约束] → 通过 `exam-service` DTO 校验、测试覆盖和统一序列化结构保证数据质量。
- [题型删除存在引用约束，可能影响运营调整题型] → 删除前明确校验是否被题目引用；如需停用能力，可后续通过新 change 增补状态管理，而不是在本次扩范围。
- [前后端同时新增动态表单，接口契约容易漂移] → 先以 OpenSpec 定义字段与模式，再通过 `exam-service` 与 `exam-web` 的测试覆盖创建、编辑和查询结构。
- [范围容易被题目分类、导入导出等近邻需求侵蚀] → 在 proposal、specs、tasks 中显式声明不包含范围，并在联调验收时做范围检查。

## Migration Plan

1. 在 `exam-service` 新增数据库迁移脚本，创建题型表、题目表、索引与菜单/API 权限种子数据，并同步更新测试用 schema/data。
2. 先完成 `exam-service` 的失败测试、DTO、Mapper、Service、Controller 与答案配置校验逻辑，再提供可联调的 REST 接口。
3. 在 `exam-web` 接入题库管理模块、菜单与路由，并基于接口完成题目列表、题目录入/编辑和题型管理交互。
4. 联调阶段重点验证查询过滤、答案配置校验、题型删除引用保护、权限受限账号行为、`TraceNo` 透传和日志脱敏。
5. 回滚策略以“禁用菜单/权限入口并回退应用版本”为主；若后续模块尚未依赖题库数据，可通过独立迁移脚本下线题型表、题目表与权限数据。

## Open Questions

当前没有阻塞 proposal 落地的开放问题。本次默认题目基础字段为题干、题型、难度、分值和答案配置；如后续需要题目分类、解析、附件、批量导入导出或知识点标签，应通过新的 OpenSpec change 增补，而不是在本次 change 中扩边界。
