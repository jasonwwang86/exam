## ADDED Requirements

### Requirement: `exam-web` 应提供由 `exam-service` 支撑的考试计划管理模块入口与配置流程
系统 MUST 在 `exam-web` 中为有权限的管理员提供考试计划管理能力，并由受保护的 `exam-service` 接口支撑。考试计划页面 MUST 在统一管理台壳层内，以查询条件区、操作区和数据表格区呈现计划列表，并提供计划创建、编辑、详情查看和状态切换入口；计划配置流程 MUST 覆盖考试时间、试卷关联和考生范围设置，且不得直接暴露持久化实体。

#### Scenario: 管理员按条件查询考试计划
- **WHEN** 具备考试计划查询权限的管理员在 `exam-web` 打开考试计划页面并提交关键字或状态筛选条件
- **THEN** `exam-web` 必须调用 `GET /api/admin/exam-plans`
- **THEN** `exam-service` 必须返回管理端展示所需的分页结果，至少包含考试计划 ID、计划名称、试卷名称、开始时间、结束时间、已选考生数量、状态和更新时间

#### Scenario: 管理员通过统一入口发起考试计划配置
- **WHEN** 具备考试计划维护权限的管理员在考试计划页面点击顶部操作入口或某条计划记录的维护入口
- **THEN** `exam-web` 必须允许管理员发起 `新增计划`、`编辑计划` 或查看计划详情
- **THEN** 计划配置必须通过模块内工作流承载，而不是跳转到独立首页框架

#### Scenario: 无权限管理员不能进入考试计划模块
- **WHEN** 已登录管理员缺少考试计划菜单权限或相关 API 权限
- **THEN** `exam-web` 必须隐藏或阻止进入考试计划入口及相关操作按钮
- **THEN** `exam-service` 必须拒绝对应的受保护考试计划接口请求

### Requirement: `exam-service` 应支持考试时间配置与试卷关联
系统 MUST 允许有权限的管理员通过 `exam-web` 与 `exam-service` 之间的显式 DTO 契约维护考试计划基础信息。考试计划基础属性契约 MUST 至少包含 `name`、`paperId`、`startTime`、`endTime` 和 `remark`。`exam-service` MUST 校验时间窗口合法，并确保其覆盖所选试卷的 `durationMinutes`。

#### Scenario: 管理员创建带有试卷关联和时间配置的考试计划
- **WHEN** 有权限的管理员提交合法的考试计划创建请求，且请求中的 `paperId` 对应有效试卷
- **THEN** `exam-service` 必须持久化计划名称、关联试卷、开始时间、结束时间和备注
- **THEN** `exam-service` 必须默认将新计划状态设置为 `DRAFT`

#### Scenario: 管理员更新考试计划基础配置
- **WHEN** 有权限的管理员提交某个已有考试计划的合法基础信息更新请求
- **THEN** `exam-service` 必须持久化变更后的计划名称、试卷关联、开始时间、结束时间或备注字段
- **THEN** 后续通过 `GET /api/admin/exam-plans` 或 `GET /api/admin/exam-plans/{id}` 查询时必须返回更新后的值

#### Scenario: 非法时间窗口被拒绝
- **WHEN** 管理员提交的 `startTime` 不早于 `endTime`，或时间窗口长度小于所选试卷的 `durationMinutes`
- **THEN** `exam-service` 必须以校验失败响应拒绝该请求
- **THEN** `exam-web` 必须向管理员展示可读的错误原因

#### Scenario: 无效试卷不能关联到考试计划
- **WHEN** 管理员提交不存在、已删除或无权访问的 `paperId`
- **THEN** `exam-service` 必须拒绝该请求
- **THEN** 已有考试计划配置必须保持不变

### Requirement: `exam-web` 与 `exam-service` 应支持基于现有考生数据的考试范围设置
系统 MUST 允许有权限的管理员基于现有考生管理能力设置考试计划的考生范围。考生范围 MUST 以显式考生关联方式落库，并通过 `PUT /api/admin/exam-plans/{id}/examinees` 维护；`GET /api/admin/exam-plans/{id}/examinees` MUST 返回当前计划已选考生范围，供 `exam-web` 展示与复核。

#### Scenario: 管理员为考试计划覆盖考生范围
- **WHEN** 有权限的管理员为某个已有考试计划提交一组合法的考生 ID
- **THEN** `exam-web` 必须调用 `PUT /api/admin/exam-plans/{id}/examinees`
- **THEN** `exam-service` 必须以本次提交结果覆盖该计划当前的有效考生范围

#### Scenario: 管理员查看某个计划已选考生范围
- **WHEN** 有权限的管理员在考试计划详情或配置流程中查看某个计划的考生范围
- **THEN** `exam-web` 必须调用 `GET /api/admin/exam-plans/{id}/examinees`
- **THEN** `exam-service` 必须返回当前计划已选考生的稳定列表数据，至少包含考生 ID、考生编号、姓名和状态

#### Scenario: 无效考生不能加入考试范围
- **WHEN** 管理员尝试把不存在、已删除、已禁用或无权访问的考生加入某个考试计划
- **THEN** `exam-service` 必须拒绝该范围更新请求
- **THEN** `exam-web` 必须向管理员展示失败原因且不得写入无效关联

#### Scenario: 同一考生不能在同一计划中重复出现
- **WHEN** 管理员提交的考生范围中包含重复考生 ID
- **THEN** `exam-service` 必须对同一计划下的重复成员做去重或拒绝处理，且结果必须保持单一有效关联
- **THEN** 后续查询该计划的考生范围时，同一考生不得重复出现

### Requirement: `exam-service` 应支持考试计划状态管理并限制状态流转
系统 MUST 为考试计划提供受保护的状态管理能力。考试计划状态 MUST 支持 `DRAFT`、`PUBLISHED`、`CLOSED`、`CANCELLED`，并通过 `PATCH /api/admin/exam-plans/{id}/status` 变更。系统 MUST 只允许定义好的状态流转，并在发布前校验计划配置完整性。

#### Scenario: 管理员发布配置完整的考试计划
- **WHEN** 有权限的管理员将某个已关联试卷、时间窗口合法且至少包含一名有效考生的 `DRAFT` 计划更新为 `PUBLISHED`
- **THEN** `exam-service` 必须持久化新的计划状态
- **THEN** 后续列表与详情查询必须返回 `PUBLISHED`

#### Scenario: 配置不完整的计划不能发布
- **WHEN** 管理员尝试将缺少试卷、时间配置非法或考生范围为空的考试计划更新为 `PUBLISHED`
- **THEN** `exam-service` 必须拒绝该状态变更请求
- **THEN** `exam-web` 必须向管理员展示可读的失败原因

#### Scenario: 终态计划不能重新编辑或重新开启
- **WHEN** 管理员尝试编辑 `CLOSED` 或 `CANCELLED` 的考试计划，或尝试把这两类终态计划切回其他状态
- **THEN** `exam-service` 必须拒绝该更新或状态变更请求
- **THEN** `exam-web` 必须阻止继续提交无效操作

### Requirement: `exam-service` 应复用带有 TraceNo 关联的脱敏日志能力记录考试计划操作
系统 MUST 在考试计划接口中复用现有管理端的 `TraceNo`、请求日志、响应日志、异常日志和日志脱敏规则，并额外记录计划创建、编辑、考生范围覆盖和状态变更的可检索业务日志。

#### Scenario: 考试计划操作可通过业务日志追踪
- **WHEN** 有权限的管理员完成考试计划创建、编辑、考生范围覆盖或状态变更操作
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联的业务日志
- **THEN** 日志必须包含操作人摘要、操作类型、计划标识、试卷标识或考生数量摘要以及结果摘要

#### Scenario: 考生敏感字段在日志中保持脱敏
- **WHEN** 考试计划相关请求、响应或校验失败信息中包含考生姓名、手机号、身份证号或其他敏感字段
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守管理端统一的日志脱敏规则
