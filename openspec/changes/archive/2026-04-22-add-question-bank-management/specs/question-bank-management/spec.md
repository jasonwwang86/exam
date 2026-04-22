## ADDED Requirements

### Requirement: `exam-web` 应提供由 `exam-service` 支撑的题库管理模块入口
系统 MUST 在 `exam-web` 中为有权限的管理员提供题库管理能力，并由受保护的 `exam-service` 接口支撑。题库管理页面 MUST 在统一管理台壳层内，以查询条件区、操作区和数据表格区呈现题目列表，并提供题目录入、编辑、删除和题型管理入口；页面不得直接暴露持久化实体。

#### Scenario: 管理员按条件查询题目
- **WHEN** 具备题目查询权限的管理员在 `exam-web` 打开题库管理页面并提交关键字、题型或难度筛选条件
- **THEN** `exam-web` 必须调用 `GET /api/admin/questions`
- **THEN** `exam-service` 必须返回管理端展示所需的分页结果，至少包含题目 ID、题干摘要、题型名称、难度、分值和更新时间

#### Scenario: 管理员通过统一入口发起题目录入或题型管理
- **WHEN** 具备题库维护权限的管理员在题库管理页面点击顶部操作入口
- **THEN** `exam-web` 必须允许管理员发起 `新增题目` 或进入 `题型管理`
- **THEN** 题目录入与编辑必须通过模块内表单承载，题型管理必须通过同模块内的受保护工作流承载

#### Scenario: 无权限管理员不能进入题库模块
- **WHEN** 已登录管理员缺少题库菜单权限或相关 API 权限
- **THEN** `exam-web` 必须隐藏或阻止进入题库管理入口及相关操作按钮
- **THEN** `exam-service` 必须拒绝对应的受保护题库接口请求

### Requirement: `exam-service` 应支持题目录入与编辑时的基础属性配置
系统 MUST 允许有权限的管理员通过 `exam-web` 与 `exam-service` 之间的显式 DTO 契约维护题目。题目基础属性契约 MUST 至少包含 `stem`、`questionTypeId`、`difficulty`、`score` 和 `answerConfig`，并基于题型的答案模式完成校验。

#### Scenario: 管理员录入合法题目
- **WHEN** 有权限的管理员在 `exam-web` 提交合法的题目录入请求
- **THEN** `exam-service` 必须校验必填字段、题型存在性、难度值、分值格式和答案配置结构
- **THEN** `exam-service` 必须持久化新题目并返回题目 DTO 摘要，而不是直接暴露持久化实体

#### Scenario: 管理员编辑已有题目
- **WHEN** 有权限的管理员提交某个已有题目的合法更新请求
- **THEN** `exam-service` 必须持久化变更后的字段
- **THEN** 后续通过 `GET /api/admin/questions` 或 `GET /api/admin/questions/{id}` 查询时必须返回更新后的值

#### Scenario: 答案配置与题型模式不匹配时被拒绝
- **WHEN** 管理员提交的 `answerConfig` 不符合当前题型的 `answerMode`
- **THEN** `exam-service` 必须以校验失败响应拒绝该请求
- **THEN** `exam-web` 必须向管理员展示可读的错误原因，且不得写入不合法题目数据

### Requirement: `exam-service` 应支持题目删除与条件查询
系统 MUST 允许有权限的管理员通过受保护的 REST 接口删除题目，并按照关键字、题型和难度执行分页查询。删除后的题目 MUST 不再出现在常规管理列表中。

#### Scenario: 管理员删除题目
- **WHEN** 有权限的管理员确认删除某个已有题目
- **THEN** `exam-service` 必须使该题目不再出现在常规分页查询结果中
- **THEN** `exam-web` 在刷新后不得继续在有效管理列表中展示该题目

#### Scenario: 管理员组合条件查询题目
- **WHEN** 有权限的管理员按关键字、题型或难度组合提交题目查询
- **THEN** `exam-service` 必须仅返回满足条件的分页记录
- **THEN** `exam-web` 必须在统一数据区展示对应的结果、空态或错误态

### Requirement: `exam-service` 应支持题型管理并保护已被使用的题型
系统 MUST 允许有权限的管理员维护题型基础信息。题型契约 MUST 至少包含 `name`、`answerMode`、`sort` 和 `remark`。当题型已被有效题目引用时，删除请求 MUST 被拒绝。

#### Scenario: 管理员新增合法题型
- **WHEN** 有权限的管理员提交合法的题型新增请求
- **THEN** `exam-service` 必须持久化新的题型记录
- **THEN** `exam-web` 后续加载题目表单时必须能够获取该题型作为可选项

#### Scenario: 管理员编辑已有题型
- **WHEN** 有权限的管理员提交某个已有题型的合法更新请求
- **THEN** `exam-service` 必须持久化更新后的题型信息
- **THEN** 后续题目表单与题型列表必须返回更新后的值

#### Scenario: 管理员删除未被引用的题型
- **WHEN** 有权限的管理员删除一个未被任何有效题目引用的题型
- **THEN** `exam-service` 必须删除或逻辑删除该题型
- **THEN** `exam-web` 后续获取题型列表时不得继续返回该题型

#### Scenario: 管理员删除已被题目引用的题型
- **WHEN** 有权限的管理员删除一个仍被有效题目引用的题型
- **THEN** `exam-service` 必须以可读错误响应拒绝该请求
- **THEN** 现有题目数据必须保持不变

### Requirement: `exam-service` 应复用带有 TraceNo 关联的脱敏日志能力记录题库操作
系统 MUST 在题库管理接口中复用现有管理端的 `TraceNo`、请求日志、响应日志、异常日志和日志脱敏规则，并额外记录题目录入、编辑、删除和题型维护的可检索业务日志。

#### Scenario: 题库操作可通过业务日志追踪
- **WHEN** 有权限的管理员完成题目录入、编辑、删除、题型新增、题型更新或题型删除操作
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联的业务日志
- **THEN** 日志必须包含操作人摘要、操作类型、资源标识和结果摘要

#### Scenario: 题目答案配置与敏感字段在日志中保持脱敏
- **WHEN** 题库相关请求、响应或校验失败信息中包含答案配置正文或其他敏感字段
- **THEN** `exam-service` 必须在日志中省略原始敏感内容或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守管理端统一的日志脱敏规则
