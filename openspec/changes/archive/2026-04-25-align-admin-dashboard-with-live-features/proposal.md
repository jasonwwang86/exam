## Why

当前 `/dashboard` 页面的大部分内容仍然是静态占位数据，包括个人信息中的部门、岗位，以及本月数据、趋势图和系统概览等区块。这会让管理首页与当前系统实际已接入的功能和真实数据能力脱节，也不利于后续继续把首页作为统一工作台稳定演进。

现在仓库已经具备管理员当前用户信息、考生管理、题库管理、试卷管理和考试计划管理等真实模块能力，适合将管理首页收敛为基于现有能力的真实工作台，并同步补齐对应的 dashboard 汇总接口契约。

## What Changes

- 调整 `exam-web` 管理首页 `/dashboard` 的页面结构，仅保留 `个人信息`、`本月数据`、`常用功能` 三个区块，移除当前静态趋势图和系统概览等占位内容。
- 将 `个人信息` 区块改为展示当前登录账号已有真实字段，如姓名、账号、角色、可访问模块数，不再展示当前系统没有数据来源的部门、岗位等信息。
- 将 `常用功能` 区块改为按当前登录账号的可见菜单动态渲染，只展示当前仓库已接入且当前账号有权限进入的功能入口。
- 为 `exam-service` 增加管理首页汇总能力，完善 `GET /api/admin/dashboard/summary` 的响应结构，返回本月新增考生数、本月新增题目数、本月新增试卷数、本月开考计划数等真实统计数据。
- 为前端新增 dashboard 汇总请求封装与类型定义，并在 `DashboardPage` 中使用真实接口数据渲染本月数据区块，补充对应加载态、空态或异常态处理。
- 本次变更不调整登录接口与当前用户接口的字段结构，不新增 MySQL 表，不修改现有表结构，仅在现有数据表与 Mapper 基础上补充 dashboard 汇总查询。

## Capabilities

### New Capabilities
- `admin-dashboard-summary`: 定义管理首页汇总接口和前端消费方式，覆盖 `/api/admin/dashboard/summary` 的真实统计口径、响应结构以及 dashboard 本月数据区块的展示要求。

### Modified Capabilities
- `admin-console-ui`: 调整管理首页结构要求，使首页仅包含个人信息、本月数据和常用功能，并要求这些区块与当前登录用户和已接入功能保持一致，不再依赖静态趋势图或占位能力说明。

## Impact

- Affected systems: `exam-web`、`exam-service`
- Frontend impact (`exam-web`):
  - 影响 `exam-web/src/modules/dashboard/pages/**`
  - 预计新增或调整 `exam-web/src/modules/dashboard/services/**`、`types` 与相关测试
  - 继续复用 `exam-web/src/shared` 中的统一页面容器，不在根目录平铺业务代码
- Backend impact (`exam-service`):
  - 影响 `cn.jack.exam.controller.admin.AdminDashboardController`
  - 新增 dashboard 汇总 DTO、service、必要的 mapper 查询或聚合统计逻辑
  - 补充 controller/service 层测试，覆盖鉴权、响应结构与统计规则
- REST APIs:
  - 修改 `GET /api/admin/dashboard/summary` 的响应契约，使其从占位响应升级为真实 dashboard 汇总数据
- Request/response DTOs:
  - 新增或调整管理首页汇总响应 DTO，用于承载个人首页本月统计数据
- Data / persistence:
  - 无 MySQL 表结构变更
  - 基于现有考生、题目、试卷、考试计划相关表做本月聚合统计
