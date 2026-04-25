## ADDED Requirements

### Requirement: `exam-service` 应提供管理首页本月数据汇总接口
系统 MUST 为 `exam-web` 的管理首页提供受保护的本月数据汇总能力。`GET /api/admin/dashboard/summary` MUST 使用显式响应 DTO 返回真实统计结果，而不是占位消息；返回结果至少包含本月新增考生数、本月新增题目数、本月新增试卷数和本月开考计划数，并保持稳定字段命名，供 `exam-web` 的 `/dashboard` 页面直接展示。

#### Scenario: 已授权管理员获取首页汇总数据
- **WHEN** 已登录且具备 `dashboard:read` 权限的管理员从 `exam-web` 调用 `GET /api/admin/dashboard/summary`
- **THEN** `exam-service` 必须返回包含本月新增考生数、本月新增题目数、本月新增试卷数和本月开考计划数的显式 DTO
- **THEN** 响应不得继续使用 `{ "message": "ok" }` 这类占位结构

#### Scenario: 未登录请求被拒绝
- **WHEN** 未携带合法 Bearer Token 的请求访问 `GET /api/admin/dashboard/summary`
- **THEN** `exam-service` 必须拒绝该请求为未认证
- **THEN** `exam-web` 不得得到可用于渲染本月数据的成功响应

#### Scenario: 无权限管理员不能读取首页汇总
- **WHEN** 已登录管理员缺少 `dashboard:read` 权限而访问 `GET /api/admin/dashboard/summary`
- **THEN** `exam-service` 必须拒绝该请求
- **THEN** 响应不得泄露任何首页统计结果

### Requirement: `exam-service` 应按稳定月份口径统计 dashboard 汇总数据
系统 MUST 以统一月份口径生成管理首页汇总数据。`exam-service` MUST 按当前统计月份边界过滤现有业务数据，其中本月新增考生数基于 `examinee.created_at`，本月新增题目数基于 `question.created_at`，本月新增试卷数基于 `paper.created_at`，本月开考计划数基于 `exam_plan.start_time`。统计过程 MUST 继续遵守现有软删除过滤和有效数据约束，不得将已删除或不应参与统计的数据计入 dashboard。

#### Scenario: 本月新增数据被正确计入
- **WHEN** `exam-service` 统计当前月份内创建的考生、题目、试卷以及开始时间落在当前月份的考试计划
- **THEN** dashboard 汇总结果必须分别返回这些记录的真实数量
- **THEN** `exam-web` 必须能够直接使用这些值渲染本月数据区块

#### Scenario: 非本月或已删除数据不计入汇总
- **WHEN** 存在创建时间不在当前月份、开始时间不在当前月份，或已被软删除的考生、题目、试卷和考试计划记录
- **THEN** `exam-service` 必须将这些记录排除在 dashboard 汇总结果之外
- **THEN** 不同指标之间不得共用模糊或不一致的统计口径

### Requirement: `exam-web` 应以 summary 接口渲染本月数据并处理失败状态
系统 MUST 在 `exam-web/src/modules/dashboard/` 中通过独立的 dashboard service/type 调用 `GET /api/admin/dashboard/summary`，并用返回的真实结果渲染首页 `本月数据` 区块。若该接口加载失败，`exam-web` MUST 在本月数据区块内展示明确的加载失败或空态提示，而不是退回硬编码数字；同时页面中的个人信息和常用功能区块 MUST 仍可继续显示。

#### Scenario: 首页展示真实本月数据
- **WHEN** 管理员进入 `exam-web` 的 `/dashboard` 页面且 summary 接口成功返回
- **THEN** 本月数据区块必须展示接口返回的四个真实统计指标
- **THEN** 页面不得继续展示静态占位数字或趋势图数据

#### Scenario: summary 接口失败时只影响本月数据区块
- **WHEN** `exam-web` 调用 `GET /api/admin/dashboard/summary` 失败或返回异常
- **THEN** `exam-web` 必须在本月数据区块展示清晰的失败提示或空态
- **THEN** `exam-web` 仍必须继续展示个人信息区块和常用功能区块，而不是让整个首页失效
