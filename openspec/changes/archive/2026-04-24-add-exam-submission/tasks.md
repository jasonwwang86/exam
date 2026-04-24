## 1. `exam-service` 提交试卷状态机与自动交卷能力

- [x] 1.1 在 `exam-service` 先编写失败测试，覆盖主动交卷成功、重复交卷幂等、截止后自动交卷、已交卷后拒绝保存答案、考试列表提交状态摘要和日志脱敏行为
- [x] 1.2 扩展 `exam-service` 的 MySQL DDL、测试 schema 与初始化数据：为 `exam_answer_session` 增加最终提交字段、必要索引和历史 `TIME_EXPIRED` 终态迁移方案
- [x] 1.3 在 `cn.jack.exam.entity`、`mapper`、`resources/mapper` 中扩展答题会话状态模型与查询 SQL，支持最终提交状态读取、自动交卷扫描和状态幂等更新
- [x] 1.4 在 `cn.jack.exam.dto.candidate`、`service.candidate` 与 `controller.candidate` 中实现 `POST /api/candidate/exams/{planId}/submission`，并同步修改考试列表、答题会话、保存答案接口的终态行为与 DTO 契约
- [x] 1.5 为主动交卷、自动交卷、重复交卷命中和已交卷后保存拒绝补充 TraceNo 关联业务日志与脱敏校验，确保不输出明文答案、完整 Token、完整 Authorization 头或身份证号

## 2. `exam-web` 考生端交卷确认与结果展示

- [x] 2.1 在 `exam-web` 先编写失败测试，覆盖交卷确认弹窗、取消交卷不发请求、主动交卷成功后的结果展示、倒计时归零后的自动交卷结果、列表页已提交状态和防重复触发
- [x] 2.2 扩展 `exam-web/src/modules/candidate-portal/` 下的 `types`、`services`、`hooks`、`pages` 与 `components`，新增或补齐交卷确认交互、交卷结果视图和相关状态管理
- [x] 2.3 修改在线答题页与考试列表页，使其根据服务端终态展示“交卷中/已提交/自动提交”结果摘要，并在已最终提交后禁用保存答案和再次进入答题
- [x] 2.4 接入 `POST /api/candidate/exams/{planId}/submission` 及扩展后的列表/会话 DTO，完成主动交卷、自动交卷、刷新恢复和错误态提示逻辑
- [x] 2.5 对 `exam-web` 的交卷结果页、确认弹窗与考试列表状态文案做针对性页面验证；本次按验收指令仅执行桌面宽度页面验收，确认未处于裸语义状态，且未扩展到成绩单或监考大屏

## 3. 联调与验证

- [x] 3.1 运行并通过 `exam-service` 测试，重点验证主动交卷、自动交卷、重复提交保护、终态持久化、已交卷后拒绝写入和日志脱敏
- [x] 3.2 运行并通过 `exam-web` 测试，重点验证交卷确认、结果展示、倒计时归零自动交卷、列表状态摘要和重复点击防护
- [x] 3.3 完成 `exam-service` 基于 H2 本地测试库的接口验证，确认主动提交、到时自动提交、提交状态记录与防重复提交一致，且结果未扩展到成绩单、判分或监考大屏
