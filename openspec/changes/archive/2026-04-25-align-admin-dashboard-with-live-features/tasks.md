## 1. `exam-service` dashboard 汇总接口与统计实现

- [x] 1.1 在 `exam-service` 先编写失败测试，覆盖 `GET /api/admin/dashboard/summary` 的已授权访问、未登录拒绝、无权限拒绝和响应结构校验
- [x] 1.2 在 `exam-service` 先编写 service 层失败测试，覆盖本月新增考生、本月新增题目、本月新增试卷、本月开考计划四个指标的月份边界与非本月过滤规则
- [x] 1.3 若统计依赖新增自定义查询，则为 `ExamineeMapper`、`QuestionMapper`、`PaperMapper`、`ExamPlanMapper` 补充 mapper/custom SQL 测试，验证软删除过滤与时间字段过滤
- [x] 1.4 在 `cn.jack.exam.dto.admin`、`cn.jack.exam.service.dashboard`、`cn.jack.exam.controller.admin` 中实现 dashboard summary 响应 DTO、聚合 service 和受保护 REST 接口，替换当前占位 `{ "message": "ok" }` 响应
- [x] 1.5 在现有 mapper 或对应 XML 中补充本月统计查询实现，确保不新增 MySQL 表或修改现有表结构，并与测试配置下的数据口径保持一致

## 2. `exam-web` dashboard 页面与数据接入

- [x] 2.1 在 `exam-web` 先编写失败测试，覆盖 `/dashboard` 使用真实 summary 数据渲染、个人信息基于当前登录态展示、常用功能基于当前账号菜单展示以及 summary 失败态展示
- [x] 2.2 在 `exam-web/src/modules/dashboard/` 下新增或调整 `services/dashboardApi.ts` 与 `types.ts`，使前端 DTO 与 `GET /api/admin/dashboard/summary` 契约保持一致
- [x] 2.3 重构 `DashboardPage.tsx`，将页面结构收敛为 `个人信息`、`本月数据`、`常用功能` 三个区块，移除静态趋势图、系统概览和硬编码功能入口
- [x] 2.4 调整 `DashboardPage.module.css` 与相关页面展示，使个人信息只展示当前系统已有真实字段，并为本月数据区块补充加载态、失败态或空态承载
- [x] 2.5 校验 `exam-web` 目录落点符合 `modules/dashboard` 与 `shared` 分层，不在根目录平铺 dashboard 业务逻辑或接口代码

## 3. 联调与验证

- [x] 3.1 运行并通过 `exam-service` 的相关测试集，确认 controller、service 与 mapper/custom SQL 覆盖 summary 契约、统计口径和权限边界
- [x] 3.2 运行并通过 `exam-web` 的 Vitest 用例，确认 dashboard 页面在成功、失败和不同菜单权限场景下都符合预期
- [x] 3.3 使用测试配置完成 `exam-web` 与 `exam-service` 联调，确认 `/dashboard` 真实展示个人信息、本月数据和常用功能，且不再出现静态趋势图与系统概览占位内容
