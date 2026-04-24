## 1. `exam-service` 在线答题会话与保存契约

- [x] 1.1 在 `exam-service` 先编写失败测试，覆盖答题会话首次创建、会话恢复、未确认/未分配/未开始/已结束拦截、截止时间计算、超时后禁止保存和日志脱敏行为
- [x] 1.2 扩展 `exam-service` 的 MySQL DDL、测试 schema 与初始化数据：新增 `exam_answer_session`、`exam_answer_record` 表及索引，为 `paper_question` 增加 `answer_config_snapshot` 字段，并补充既有试卷题目快照回填方案
- [x] 1.3 在 `cn.jack.exam.entity`、`mapper`、`resources/mapper` 中新增答题会话与答案记录模型，扩展试卷题目与考试列表查询，确保题目按 `display_order` 返回且可恢复已保存答案
- [x] 1.4 在 `cn.jack.exam.dto.candidate`、`service.candidate` 与 `controller.candidate` 中实现 `PUT /api/candidate/exams/{planId}/answer-session`、`PUT /api/candidate/exams/{planId}/questions/{paperQuestionId}/answer` 以及扩展后的 `GET /api/candidate/exams` 契约，明确本次不扩展到提交试卷或成绩单
- [x] 1.5 为在线答题会话创建/恢复、答案保存和超时拒绝补充 TraceNo 关联业务日志与脱敏校验，确保不输出明文答案、完整 Token、完整 Authorization 头或身份证号

## 2. `exam-web` 考生端在线答题页面与交互

- [x] 2.1 在 `exam-web` 先编写失败测试，覆盖考试列表进入答题、不可作答考试禁入、答题页刷新恢复、题目切换、答案保存、答题状态更新和倒计时归零只读
- [x] 2.2 扩展 `exam-web/src/modules/candidate-portal/` 下的 `types`、`services`、`hooks`、`pages` 与 `components`，新增答题页、题号导航、答题状态区和倒计时组件，并保持全局样式接入
- [x] 2.3 修改考生端考试列表页，使其展示进入答题入口与答题进度摘要，只对满足条件的考试开放入口，继续保持不提供提交试卷、成绩单或监考相关动作
- [x] 2.4 实现答题页的问题加载、题目切换、单题答案保存、错误态处理、刷新恢复和超时只读逻辑，确保前端 DTO 与后端契约一致
- [x] 2.5 对 `exam-web` 的纯样式与布局部分做针对性页面验证，确认桌面与移动宽度下题号导航、题目区域、状态区和倒计时都可用，页面不处于裸语义状态

## 3. 联调与验证

- [x] 3.1 运行并通过 `exam-service` 测试，重点验证答题入口边界、题目快照读取、答案保存覆盖、超时拦截、TraceNo 透传与日志脱敏
- [x] 3.2 运行并通过 `exam-web` 测试，重点验证进入答题、切题状态、保存反馈、倒计时、刷新恢复和只读边界
- [x] 3.3 完成 `exam-web` 与 `exam-service` 的接口联调，确认题目加载、答题记录保存、倒计时和答题状态展示一致，且结果未扩展到提交试卷、成绩单或监考大屏
