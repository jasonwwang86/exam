## 1. `exam-service` 自动判分与成绩结果生成

- [x] 1.1 在 `exam-service` 先编写失败测试，覆盖主动交卷触发生成、自动交卷触发生成、判分规则（单选/多选/判断/文本）、失败不回滚交卷终态、重复触发幂等更新、补偿重试、`TraceNo` 透传与日志脱敏
- [x] 1.2 评估并补充 `exam-service` 的 MySQL DDL、测试 schema、实体、DTO 与 Mapper：在保持 `exam_result`、`exam_result_item` 兼容的前提下，补齐成绩生成所需字段、索引、查询方法与结果项全量替换能力
- [x] 1.3 在 `cn.jack.exam.service.candidate` 中实现独立的成绩生成服务与判分规则组件，基于 `exam_answer_session`、`exam_answer_record`、`paper_question.answer_config_snapshot` 生成聚合成绩与逐题结果摘要
- [x] 1.4 在 `cn.jack.exam.service.candidate.CandidateAnsweringService` 的主动交卷与自动交卷链路中接入“同步尝试生成 + 失败保留待出分”逻辑，保持既有 REST 契约稳定且不扩展到人工阅卷或发布流程
- [x] 1.5 为已最终提交但尚未生成结果的会话补充补偿扫描或等效后端重试机制，并验证结果快照按唯一键幂等更新、结果项按题序全量替换
- [x] 1.6 为成绩生成成功、失败、重复触发与补偿重试补充 `TraceNo` 关联业务日志和日志脱敏校验，确保不输出明文答案、标准答案正文、完整 Token、完整 Authorization 头或身份证号

## 2. `exam-web` 既有成绩链路最小适配与回归

- [x] 2.1 在 `exam-web` 先编写失败测试，覆盖交卷后刷新考试列表可观察到已出分摘要、既有成绩详情页继续兼容生成后的 DTO，以及本次不新增独立成绩生成页面或管理入口
- [x] 2.2 仅在测试暴露状态缓存或数据刷新问题时，最小调整 `exam-web/src/modules/candidate-portal/` 下的状态恢复、缓存清理或接口调用时机；不得扩展到在线答题 UI、人工阅卷入口或新页面建设
- [x] 2.3 对 `exam-web` 现有成绩列表与成绩详情页面做针对性验证，确认在本次 change 下仍通过既有 `candidate-portal` 路由承接结果展示，且没有扩展到动态大屏、发布审批或成绩统计

## 3. 联调与验证

- [x] 3.1 运行并通过 `exam-service` 测试，按 controller、service、mapper/custom SQL、config 分层验证交卷后成绩生成、补偿重试、幂等替换、判分规则、日志与脱敏行为
- [x] 3.2 运行并通过 `exam-web` 测试，重点验证交卷后列表出分可见性、成绩详情兼容性和既有候选端状态流无回退
- [x] 3.3 使用 `exam-service` 测试配置完成 `exam-web` 与 `exam-service` 联调，确认“交卷 -> 结果生成 -> 列表出分 -> 成绩详情”链路一致，且实现结果未扩展到人工阅卷、成绩发布、动态大屏、监考大屏或考试计划
