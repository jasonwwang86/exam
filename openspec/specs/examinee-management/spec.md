# examinee-management Specification

## Purpose
TBD - created by archiving change add-examinee-management. Update Purpose after archive.
## Requirements
### Requirement: `exam-web` 应提供由 `exam-service` 支撑的考生管理模块入口
系统 MUST 在 `exam-web` 中为有权限的管理员提供考生管理能力，并由受保护的 `exam-service` 接口支撑。考生管理视图 MUST 支持关键字搜索、状态筛选、分页与列表操作，且不得向浏览器直接暴露持久化实体。

#### Scenario: 管理员按条件查询考生
- **WHEN** 具备考生查询权限的管理员在 `exam-web` 打开考生管理页面并提交关键字或状态筛选条件
- **THEN** `exam-web` 必须调用 `GET /api/admin/examinees`
- **THEN** `exam-service` 必须返回管理端展示所需的分页结果，至少包含考生 ID、考生编号、姓名、手机号、状态和更新时间

#### Scenario: 无权限管理员不能进入考生模块
- **WHEN** 已登录管理员缺少考生管理菜单权限或相关 API 权限
- **THEN** `exam-web` 必须隐藏或阻止进入考生管理入口及相关操作按钮
- **THEN** `exam-service` 必须拒绝对应的受保护考生接口请求

### Requirement: 登录成功后的管理端主页面应作为统一模块入口
系统 MUST 在管理员登录成功后进入统一的管理端主页面，该页面 MUST 作为模块导航容器，至少包含首页与新增的考生管理模块入口，并为后续其他管理模块保留一致的接入方式。

#### Scenario: 登录成功后可见统一模块入口
- **WHEN** 管理员在 `exam-web` 成功登录并进入主页面
- **THEN** 主页面必须展示统一布局下的模块导航区域
- **THEN** 已授权管理员必须能够看到首页入口和考生管理入口

#### Scenario: 后续模块可沿用同一主页面接入
- **WHEN** 后续其他管理模块接入管理端
- **THEN** 系统必须允许这些模块沿用登录成功后的同一主页面布局与导航机制
- **THEN** 本次新增的考生管理模块不得要求单独定义一套独立首页框架

### Requirement: `exam-service` 应支持考生基础信息新增与编辑
系统 MUST 允许有权限的管理员通过 `exam-web` 与 `exam-service` 之间的显式 DTO 契约新增和编辑考生记录。基础信息契约 MUST 至少包含 `examineeNo`、`name`、`gender`、`idCardNo`、`phone`、`email`、`status` 和 `remark`。

#### Scenario: 管理员新增合法考生
- **WHEN** 有权限的管理员在 `exam-web` 提交合法的考生新增请求
- **THEN** `exam-service` 必须校验必填字段并持久化新的考生记录
- **THEN** `exam-service` 必须返回新增后的考生 DTO 摘要，而不是直接暴露持久化实体

#### Scenario: 管理员编辑已有考生
- **WHEN** 有权限的管理员提交某个已有考生的合法更新请求
- **THEN** `exam-service` 必须持久化变更后的字段
- **THEN** 后续通过 `GET /api/admin/examinees` 查询时必须返回更新后的值

#### Scenario: 唯一字段冲突被拒绝
- **WHEN** 新增或编辑请求复用了其他有效考生已占用的 `examineeNo` 或 `idCardNo`
- **THEN** `exam-service` 必须以校验失败响应拒绝该请求
- **THEN** `exam-web` 必须向管理员展示可读的冲突原因，且不得覆盖原有记录

### Requirement: `exam-service` 应支持考生删除与状态管理
系统 MUST 允许有权限的管理员通过受保护的 REST 接口删除考生，并在启用与禁用之间切换考生状态。

#### Scenario: 管理员删除考生
- **WHEN** 有权限的管理员确认删除某个已有考生
- **THEN** `exam-service` 必须使该考生不再出现在常规列表查询与导出结果中
- **THEN** `exam-web` 在刷新后不得继续在有效管理列表中展示该考生

#### Scenario: 管理员禁用考生
- **WHEN** 有权限的管理员在 `exam-web` 将某个考生状态调整为禁用
- **THEN** `exam-service` 必须持久化禁用状态
- **THEN** 后续列表查询必须将该考生显示为禁用

#### Scenario: 管理员重新启用考生
- **WHEN** 有权限的管理员将已禁用考生重新调整为启用
- **THEN** `exam-service` 必须持久化启用状态
- **THEN** 后续列表查询必须将该考生显示为启用

### Requirement: `exam-web` 与 `exam-service` 应支持考生数据批量导入导出
系统 MUST 为考生基础信息提供批量导入与导出能力，并在 `exam-web` 与 `exam-service` 之间使用同一套稳定的 Excel 兼容列定义。导出 MUST 遵循当前查询条件，导入 MUST 逐行校验并返回结构化结果摘要。

#### Scenario: 管理员导出筛选后的考生列表
- **WHEN** 有权限的管理员在 `exam-web` 基于当前查询条件触发导出
- **THEN** `exam-service` 必须生成仅包含当前筛选结果的导出文件，且列顺序符合约定
- **THEN** `exam-web` 必须为管理员发起文件下载

#### Scenario: 管理员导入合法批量文件
- **WHEN** 有权限的管理员上传所有行都满足导入契约的批量文件
- **THEN** `exam-service` 必须创建导入文件中的考生记录
- **THEN** `exam-service` 必须返回成功行数与失败行数统计

#### Scenario: 管理员导入包含校验失败行的文件
- **WHEN** 有权限的管理员上传的批量文件中存在格式错误行或唯一字段冲突行
- **THEN** `exam-service` 必须继续校验剩余行，而不是在首个错误处直接中断
- **THEN** `exam-service` 必须返回行号与可读失败原因，供 `exam-web` 展示

### Requirement: `exam-service` 应复用带有 TraceNo 关联的脱敏日志能力记录考生操作
系统 MUST 在考生管理接口中复用现有管理端的 `TraceNo`、请求日志、响应日志、异常日志和日志脱敏规则，并额外记录新增、编辑、删除、状态变更、导入与导出的可检索业务日志。

#### Scenario: 考生操作可通过业务日志追踪
- **WHEN** 有权限的管理员完成新增、编辑、删除、状态变更、导入或导出操作
- **THEN** `exam-service` 必须输出与当前 `TraceNo` 关联的业务日志
- **THEN** 日志必须包含操作人摘要、操作类型、结果摘要以及脱敏后的考生标识摘要

#### Scenario: 敏感个人字段在日志中保持脱敏
- **WHEN** 考生相关请求、响应或校验失败信息中包含身份证号、手机号或其他个人敏感字段
- **THEN** `exam-service` 必须在日志中省略这些原始值或替换为脱敏摘要
- **THEN** `exam-service` 必须继续遵守管理端统一的日志脱敏规则

