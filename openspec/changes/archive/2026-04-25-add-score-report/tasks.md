## 1. `exam-service` 成绩查询契约与结果模型

- [x] 1.1 在 `exam-service` 先编写失败测试，覆盖本人权限校验、已交卷未出分、已出分查询成功、结果不存在拒绝、逐题摘要汇总、`TraceNo` 透传与日志脱敏行为
- [x] 1.2 扩展 `exam-service` 的 MySQL DDL、测试 schema 与初始化数据：新增 `exam_result`、`exam_result_item` 表及索引，并明确与 `exam_answer_session`、`exam_answer_record` 的关联约束
- [x] 1.3 在 `cn.jack.exam.entity`、`mapper`、`resources/mapper` 中新增成绩结果模型与查询 SQL，补充考试列表成绩摘要查询、成绩详情聚合查询及结果项排序规则
- [x] 1.4 在 `cn.jack.exam.dto.candidate`、`service.candidate` 与 `controller.candidate` 中实现扩展后的 `GET /api/candidate/exams` 和新增 `GET /api/candidate/exams/{planId}/score-report` 契约，明确本模块不扩展到在线答题、提交试卷或动态大屏
- [x] 1.5 为成绩列表摘要加载、成绩详情查询、待出分拒绝和越权访问补充 TraceNo 关联业务日志与日志脱敏校验，确保不输出明文答案、完整 Token、完整 Authorization 头或身份证号

## 2. `exam-web` 成绩单页面与成绩详情展示

- [x] 2.1 在 `exam-web` 先编写失败测试，覆盖考试列表成绩状态展示、待出分考试禁入、已出分考试进入详情、详情页加载失败提示和 DTO 数据转换逻辑
- [x] 2.2 扩展 `exam-web/src/modules/candidate-portal/` 下的 `types`、`services`、`hooks`、`pages` 与 `components`，新增成绩详情页、成绩总览区、试卷信息区、提交摘要区和作答摘要列表
- [x] 2.3 修改考生端考试列表页，使其同时展示进行中、已交卷与已出分考试的状态摘要，并仅对满足条件的记录开放成绩单入口，继续保持不提供动态大屏、考试计划或监考相关动作
- [x] 2.4 实现成绩详情页的数据加载、空态/异常态处理、分数展示、试卷信息展示和逐题作答摘要展示，确保前端 DTO 与后端 REST 契约一致
- [x] 2.5 对 `exam-web` 的纯样式与布局部分做针对性页面验证，确认桌面与移动宽度下成绩总览、信息区和作答摘要均可读，页面不处于裸语义状态

## 3. 联调与验证

- [x] 3.1 运行并通过 `exam-service` 测试，重点验证结果模型查询条件、本人访问边界、待出分分支、逐题汇总顺序、`TraceNo` 透传与日志脱敏
- [x] 3.2 运行并通过 `exam-web` 测试，重点验证成绩入口展示、待出分禁入、详情页加载、摘要渲染和错误态处理
- [x] 3.3 使用 `exam-service` 测试配置完成 `exam-web` 与 `exam-service` 联调，确认分数结果、成绩详情、试卷信息与作答摘要一致，且实现结果未扩展到在线答题、提交试卷、动态大屏或考试计划
