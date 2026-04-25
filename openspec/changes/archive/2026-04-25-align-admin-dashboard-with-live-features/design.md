## Context

当前 `exam-web` 的 `/dashboard` 页面虽然已经接入统一主页面壳层，但内容仍以静态占位为主：个人信息里存在部门、岗位等当前系统并未提供的数据，本月数据和趋势图也没有真实后端支撑。与此同时，仓库已经稳定接入了管理员当前用户信息、考生管理、题库管理、试卷管理和考试计划管理等真实模块，因此首页继续保留假数据会持续放大“页面展示”与“系统实际能力”之间的偏差。

这次变更同时影响 `exam-web` 和 `exam-service`，属于前后端边界明确但需要同步收敛契约的改造。前端目标是在 `exam-web/src/modules/dashboard` 中把首页改成真实工作台；后端目标是在 `cn.jack.exam` 包下补齐 dashboard 汇总查询能力，并通过稳定 REST 契约供前端消费。变更不涉及登录、权限模型、TraceNo 规则、数据库表结构和菜单生成逻辑，也不改变 `GET /api/admin/auth/me` 的字段结构。

现有限制如下：
- 当前登录态可直接提供的真实个人信息只有 `displayName`、`username`、`roles`、`permissions`、`menus`
- `GET /api/admin/dashboard/summary` 目前只返回占位响应 `{ "message": "ok" }`
- 考生、题目、试卷、考试计划相关实体均已有 `created_at` 字段，`exam_plan` 另有 `start_time`
- 当前系统没有“部门”“岗位”等管理员档案字段，因此首页不能继续以这些字段作为展示前提

## Goals / Non-Goals

**Goals:**
- 将 `/dashboard` 收敛为只包含 `个人信息`、`本月数据`、`常用功能` 的真实工作台页面。
- 在前端使用当前登录用户和 dashboard summary 接口渲染页面，不再展示没有数据来源的静态字段。
- 在后端为 `GET /api/admin/dashboard/summary` 提供稳定响应 DTO 和真实统计逻辑。
- 明确本月数据的统计口径，避免前后端分别猜测。
- 保持现有鉴权、菜单、路由和统一壳层行为不变。
- 按仓库约定执行 TDD：`exam-service` 强制测试先行，`exam-web` 的 dashboard 业务逻辑同样先写失败测试。

**Non-Goals:**
- 不新增趋势图、系统概览、部门岗位档案或新的管理员资料接口。
- 不修改 `GET /api/admin/auth/me` 返回结构，不为首页新增新的当前用户字段。
- 不新增 MySQL 表，不修改现有表结构，只复用既有业务表做聚合统计。
- 不扩展菜单权限模型，不改变当前可见菜单的生成方式。
- 不在本次变更中引入新的 dashboard 配置中心、缓存层或异步统计任务。

## Decisions

### Decision 1: 首页个人信息直接复用当前登录态，不额外请求新的个人资料接口

`个人信息` 区块直接来自 `GET /api/admin/auth/me` 已有字段，由前端在现有 `currentUser` 基础上渲染：
- 姓名：`displayName`
- 账号：`username`
- 角色：`roles`，按顿号或标签展示
- 可访问模块数：`menus.length`

这样做的原因是当前系统已经有稳定的当前用户读取接口，而且首页个人信息仅需展示身份摘要，不需要引入新的后端职责。备选方案是为 dashboard 再聚合返回一份用户资料，但这会造成用户信息来源重复，并让首页接口承担不必要的账号档案职责。

### Decision 2: 常用功能直接以当前账号可见菜单为准，不再维护固定入口列表

`常用功能` 区块直接消费当前登录账号的 `menus`，只渲染当前仓库已接入且当前账号有权限进入的入口；点击行为沿用现有路由跳转，不再在 `DashboardPage` 内维护固定功能数组。

这样做的原因是首页入口应与系统实际可用功能严格一致，而 `menus` 已经是权限筛选后的稳定结果。备选方案是继续维护前端硬编码入口再与权限逐个比对，但这会让首页和菜单配置产生双重维护点。

### Decision 3: 本月数据采用“本月新增/本月开考”四指标，统一由 summary 接口返回

`本月数据` 统一由 `GET /api/admin/dashboard/summary` 返回四个数字指标：
- `monthlyNewExamineeCount`：本月新增考生数，按 `examinee.created_at` 统计
- `monthlyNewQuestionCount`：本月新增题目数，按 `question.created_at` 统计
- `monthlyNewPaperCount`：本月新增试卷数，按 `paper.created_at` 统计
- `monthlyActiveExamPlanCount`：本月开考计划数，按 `exam_plan.start_time` 落在本月统计

这里采用“新增/开考”口径，而不是累计总量或复杂趋势，是因为当前页面目标是把静态假数据替换为最直接、最稳定、最容易校验的真实月度概览。备选方案是返回累计总量、同比环比或趋势数组，但这些能力会明显扩大 dashboard 的产品定义和实现复杂度。

### Decision 4: 后端以专用 DTO + service + mapper 聚合查询实现 summary，controller 只负责契约与鉴权边界

`exam-service` 中的落点划分如下：
- `cn.jack.exam.controller.admin.AdminDashboardController`：保留 `GET /api/admin/dashboard/summary` 入口和 `@RequirePermission("dashboard:read")`
- `cn.jack.exam.service.dashboard.AdminDashboardService`：负责定义统计口径和聚合 orchestration
- `cn.jack.exam.dto.admin.AdminDashboardSummaryResponse`：承载稳定响应结构
- `cn.jack.exam.mapper.*`：在现有业务模块 mapper 上补充按月份统计的方法，或引入专用 dashboard mapper 做聚合查询

优先倾向于在现有 `ExamineeMapper`、`QuestionMapper`、`PaperMapper`、`ExamPlanMapper` 上增加按月份统计方法，因为这些统计仍属于各自资源的聚合读取，没有必要为四个简单 count 新建一层跨资源 dashboard repository。若实现中发现 SQL 组织过于分散，再退回专用 mapper。

备选方案是继续让 controller 直接拼 `Map`，或直接在前端并行调用多个列表接口估算数据。前者会让契约不可演进，后者会让前端承担统计职责并增加请求次数。

### Decision 5: summary 接口保持最小稳定 JSON，不混入个人信息和菜单

后端 `summary` 响应只返回 dashboard 月度指标，不混入当前用户信息和常用功能数据。前端页面数据组合方式为：
- 个人信息、常用功能：来自已有 `currentUser`
- 本月数据：来自 `/api/admin/dashboard/summary`

这样做的原因是前后端边界更清晰，接口职责单一，也避免 dashboard summary 成为“首页大杂烩接口”。备选方案是把个人信息、menus 和 summary 一起打包返回，但这会与 `auth/me` 产生重复契约，并提高后续变更成本。

### Decision 6: 前端 dashboard 模块新增独立 service/type，并显式处理加载失败

`exam-web` 的代码落点为：
- `exam-web/src/modules/dashboard/pages/DashboardPage.tsx`
- `exam-web/src/modules/dashboard/pages/DashboardPage.module.css`
- 新增 `exam-web/src/modules/dashboard/services/dashboardApi.ts`
- 新增 `exam-web/src/modules/dashboard/types.ts`

页面初始化时拉取 summary；加载中展示统一加载态，失败时在 `本月数据` 区块内展示明确异常提示，避免再次回退为硬编码数字。个人信息和常用功能即使 summary 失败也仍然可以展示，因为它们来自当前登录态。

这样做可以把 dashboard 接口契约和页面渲染分层清楚。备选方案是把请求逻辑直接写进页面组件，但这会重复当前仓库里其他模块已经在做的 service/type 分层。

### Decision 7: 测试按前后端分层执行，严格遵守 TDD

本次变更同时适用 `exam-service` 和 `exam-web` 的 TDD 约束：

- `exam-service`：
  - controller 测试：验证 `GET /api/admin/dashboard/summary` 在有权限、无 token、无权限三类边界下的 HTTP 状态码和响应结构
  - service 测试：直接验证四个指标的统计口径，尤其是“本月”和“非本月”的边界过滤
  - mapper/custom SQL 测试：若增加自定义统计 SQL，则验证月份条件、软删除过滤和计划开考时间过滤
  - config：本次不引入新的 config 行为，不单独扩展

- `exam-web`：
  - 页面/服务测试：先写失败测试，验证 dashboard 使用真实 summary 数据、使用 current user 渲染个人信息和菜单、以及 summary 失败时的展示行为
  - 纯样式和排版调整不做脆弱快照测试，以语义查询和针对性验证为主

## Risks / Trade-offs

- [Risk] 统计口径如果前后端未明确一致，页面上的“本月数据”会再次变成语义模糊数字。 → Mitigation：在 DTO 字段名和 spec 中固定“本月新增/本月开考”定义，不返回泛化的 `count1/count2`。
- [Risk] 若直接以系统当前时间计算月份边界，测试容易受时间漂移影响。 → Mitigation：在 service 中集中处理月份起止时间，并在测试里构造明确的本月/跨月数据样本。
- [Risk] 将统计方法分别落到多个 mapper，可能让 dashboard 逻辑看起来分散。 → Mitigation：由 `AdminDashboardService` 统一收口，controller 和前端只依赖单一 summary DTO。
- [Risk] summary 失败时如果整页报错，会影响原本能显示的个人信息和常用功能。 → Mitigation：前端把 summary 失败限定在“本月数据”区块内处理，页面其余区块照常展示。

## Migration Plan

1. 先在 `exam-service` 为 controller/service/mapper 写失败测试，固定 summary 契约和统计口径。
2. 实现后端 DTO、service 和 mapper 统计逻辑，使 `/api/admin/dashboard/summary` 返回真实月度数据。
3. 再在 `exam-web` 为 dashboard 页面和服务写失败测试，固定个人信息、summary 数据和常用功能的展示行为。
4. 实现前端 service/type 和 `DashboardPage` 改造，移除静态趋势图、系统概览与硬编码入口。
5. 运行前后端测试，确认 dashboard 契约、权限边界和前端展示都与设计一致。

回滚策略：
- 若前端页面改造出现问题，可仅回滚 `exam-web` dashboard 模块改动，首页仍可恢复到旧静态版本。
- 若后端 summary 聚合实现有误，可回滚 `exam-service` 的 dashboard summary 变更；由于不涉及表结构变更，不存在数据迁移回滚成本。

## Open Questions

当前没有阻塞本次变更推进的开放问题。默认采用以下结论继续执行：
- 个人信息不展示部门、岗位等当前无来源字段
- 常用功能严格以当前账号 `menus` 为准
- 本月数据只做四个真实指标，不扩展趋势图或累计总量
