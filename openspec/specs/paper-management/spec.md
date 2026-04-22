# paper-management Specification

## Purpose
TBD - created by archiving change add-paper-management. Update Purpose after archive.
## Requirements
### Requirement: `exam-web` 应提供由 `exam-service` 支撑的试卷管理模块入口
系统 MUST 在 `exam-web` 中为有权限的管理员提供试卷管理能力，并由受保护的 `exam-service` 接口支撑。试卷管理页面 MUST 在统一管理台壳层内，以查询条件区、操作区和数据表格区呈现试卷列表，并提供试卷基础信息维护与题目明细维护入口；页面不得直接暴露持久化实体。

#### Scenario: 管理员按条件查询试卷
- **WHEN** 具备试卷查询权限的管理员在 `exam-web` 打开试卷管理页面并提交关键字查询条件
- **THEN** `exam-web` 必须调用 `GET /api/admin/papers`
- **THEN** `exam-service` 必须返回管理端展示所需的分页结果，至少包含试卷 ID、试卷名称、题目数量、总分、时长和更新时间

#### Scenario: 管理员通过统一入口发起试卷维护
- **WHEN** 具备试卷维护权限的管理员在试卷管理页面点击顶部操作入口或某条试卷记录的维护入口
- **THEN** `exam-web` 必须允许管理员发起 `新增试卷`、`编辑基础信息` 或进入 `题目明细维护`
- **THEN** 试卷基础信息与题目明细必须通过模块内受保护工作流承载，而不是跳转到独立首页框架

#### Scenario: 无权限管理员不能进入试卷模块
- **WHEN** 已登录管理员缺少试卷菜单权限或相关 API 权限
- **THEN** `exam-web` 必须隐藏或阻止进入试卷管理入口及相关操作按钮
- **THEN** `exam-service` 必须拒绝对应的受保护试卷接口请求

### Requirement: `exam-service` 应支持试卷基础信息维护与时长配置
系统 MUST 允许有权限的管理员通过 `exam-web` 与 `exam-service` 之间的显式 DTO 契约维护试卷基础信息。试卷基础属性契约 MUST 至少包含 `name`、`description`、`durationMinutes` 和 `remark`，并在试卷详情与分页结果中返回当前汇总后的 `totalScore`。

#### Scenario: 管理员创建空试卷基础信息
- **WHEN** 有权限的管理员在 `exam-web` 提交合法的试卷创建请求且尚未添加题目
- **THEN** `exam-service` 必须持久化试卷基础信息
- **THEN** `exam-service` 必须返回 `totalScore=0` 且允许管理员后续继续维护题目明细

#### Scenario: 管理员更新试卷基础信息
- **WHEN** 有权限的管理员提交某个已有试卷的合法基础信息更新请求
- **THEN** `exam-service` 必须持久化变更后的名称、说明、时长或备注字段
- **THEN** 后续通过 `GET /api/admin/papers` 或 `GET /api/admin/papers/{id}` 查询时必须返回更新后的值

#### Scenario: 非法时长配置被拒绝
- **WHEN** 管理员提交的 `durationMinutes` 为空、非正整数或超出系统允许范围
- **THEN** `exam-service` 必须以校验失败响应拒绝该请求
- **THEN** `exam-web` 必须向管理员展示可读的错误原因

#### Scenario: 管理员删除试卷
- **WHEN** 有权限的管理员确认删除某个已有试卷
- **THEN** `exam-service` 必须使该试卷不再出现在常规分页查询结果中
- **THEN** `exam-web` 在刷新后不得继续在有效管理列表中展示该试卷

### Requirement: `exam-web` 与 `exam-service` 应支持基于题库的手工组选题
系统 MUST 允许有权限的管理员在试卷管理模块中基于题库现有试题执行手工组卷。组选题工作流 MUST 只允许选择题库中的有效题目，并通过显式 DTO 把选中的题目加入指定试卷。

#### Scenario: 管理员在组选题弹层中查询候选题目
- **WHEN** 管理员在试卷题目明细维护界面打开组选题弹层并输入关键字、题型或难度筛选条件
- **THEN** `exam-web` 必须调用题库提供的受保护查询能力获取候选题目
- **THEN** 候选列表必须至少展示题目 ID、题干摘要、题型、难度和默认分值，供管理员选择加入试卷

#### Scenario: 管理员将题库题目加入试卷
- **WHEN** 管理员在组选题工作流中选择一个或多个有效题目并确认加入当前试卷
- **THEN** `exam-web` 必须调用 `POST /api/admin/papers/{paperId}/questions`
- **THEN** `exam-service` 必须为每个新增题目明细持久化试卷题目记录，并带入题库题目的必要快照与默认分值

#### Scenario: 无效题目不能被加入试卷
- **WHEN** 管理员尝试把不存在、已删除或无权访问的题库题目加入试卷
- **THEN** `exam-service` 必须拒绝该请求
- **THEN** `exam-web` 必须向管理员展示失败原因且不得写入无效题目明细

### Requirement: `exam-service` 应支持试卷题目明细维护并同步总分
系统 MUST 允许有权限的管理员维护试卷题目明细。题目明细契约 MUST 至少包含 `paperQuestionId`、`questionId`、`itemScore`、`displayOrder` 以及题干/题型/难度快照字段。`exam-service` MUST 在新增、编辑、删除或重排题目明细后重新汇总试卷总分。

#### Scenario: 管理员调整试卷题目分值或顺序
- **WHEN** 有权限的管理员提交某个试卷题目明细的合法更新请求
- **THEN** `exam-service` 必须持久化新的 `itemScore` 或 `displayOrder`
- **THEN** 后续查询试卷详情或题目明细时必须返回更新后的值和排序结果

#### Scenario: 题目明细变更后总分自动更新
- **WHEN** 管理员成功新增、编辑、删除或重排某个试卷题目明细
- **THEN** `exam-service` 必须基于当前有效题目明细的 `itemScore` 重新计算并持久化试卷 `totalScore`
- **THEN** `exam-web` 后续展示的试卷列表、详情和基础信息区域必须返回同步后的总分

#### Scenario: 同一题目不能在同一试卷中重复出现
- **WHEN** 管理员尝试把同一个有效题库题目重复加入同一张试卷
- **THEN** `exam-service` 必须以可读错误响应拒绝该请求
- **THEN** 已存在的试卷题目明细必须保持不变

#### Scenario: 管理员移除试卷题目
- **WHEN** 有权限的管理员删除某个已有试卷题目明细
- **THEN** `exam-service` 必须使该题目明细不再出现在当前试卷的有效题目列表中
- **THEN** `exam-service` 必须同步更新该试卷的总分

### Requirement: `exam-service` 应复用带有 TraceNo 关联的脱敏日志能力记录试卷操作
系统 MUST 在试卷管理接口中复用现有管理端的 `TraceNo`、请求日志、响应日志、异常日志和日志脱敏规则，并额外记录试卷创建、编辑、删除、加题、调分、移除和排序的可检索业务日志。

#### Scenario: 试卷操作可通过业务日志追踪
- **WHEN** 有权限的管理员完成试卷创建、编辑、删除、加题、调分、移除或排序操作
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联的业务日志
- **THEN** 日志必须包含操作人摘要、操作类型、资源标识和结果摘要

#### Scenario: 试卷题干快照与敏感字段在日志中保持脱敏
- **WHEN** 试卷相关请求、响应或校验失败信息中包含题干快照正文、备注或其他敏感字段
- **THEN** `exam-service` 必须在日志中省略原始敏感内容或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守管理端统一的日志脱敏规则

