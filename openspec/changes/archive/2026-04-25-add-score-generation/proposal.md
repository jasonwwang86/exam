## Why

当前仓库已经完成在线答题、提交试卷和成绩单查询能力，但考试主流程仍缺少“提交后生成成绩结果”这一段闭环。现在考生交卷后只能停留在“待出分”，`exam-service` 也没有把 `exam_answer_session`、`exam_answer_record` 转换为 `exam_result`、`exam_result_item` 的稳定机制，因此需要新增一个边界清晰的“判分/成绩生成” change 来补齐结果生成链路。

## What Changes

- `exam-service` 新增成绩生成服务，基于既有的 `exam_answer_session`、`exam_answer_record`、`paper_question.answer_config_snapshot` 和 `exam_result` / `exam_result_item` 结果模型，为已最终提交的考试生成可展示的成绩结果。
- `exam-service` 在主动交卷与自动交卷链路中触发成绩生成或保证成绩生成入口可被幂等触发，使结果输出不再依赖手工灌数。
- `exam-service` 为 `SINGLE_CHOICE`、`MULTIPLE_CHOICE`、`TRUE_FALSE`、`TEXT` 四类现有题型定义自动判分与结果摘要规则，并明确本次 `TEXT` 仅按题库已配置的 `acceptedAnswers` 进行自动判定，不扩展到人工阅卷。
- `exam-service` 为成绩生成补充 `TraceNo` 关联日志、脱敏规则、幂等保护与失败验证，保证结果可追踪、可重放、可校验。
- `exam-web` 不新增成绩页面或管理入口，继续复用既有 `candidate-portal` 中的考试列表与成绩详情页；当 `exam-service` 生成结果后，现有成绩单能力即可直接展示。
- 包含范围：自动判分、成绩结果生成、结果落库、交卷后结果闭环、日志与验证。
- 不包含范围：在线答题改造、提交页面重构、人工阅卷、成绩发布审批、成绩更正、排名统计、标准答案解析、动态大屏、监考大屏、考试计划调整。

## Capabilities

### New Capabilities
- `score-generation`: 为 `exam-service` 增加交卷后自动判分、成绩结果生成、逐题结果汇总和结果幂等落库能力。

### Modified Capabilities
- `exam-submission`: `exam-service` 的最终交卷能力从“只写提交终态”扩展为“写提交终态后生成或确保可生成成绩结果”，但不改变 `exam-web` 现有交卷页面职责。

## Impact

- Affected systems: 主要影响 `exam-service`，`exam-web` 继续复用既有 `src/modules/candidate-portal/` 中的列表与成绩详情页面，不新增独立前端模块
- Affected code: `exam-service/src/main/java/cn/jack/exam/service/candidate`、`dto/candidate`、`mapper`、`entity`、`config`、`src/test/**`
- Planned REST/API impact: 优先复用既有 `POST /api/candidate/exams/{planId}/submission`、自动交卷调度与现有成绩查询接口，不默认新增对外 REST 端点；如需补充内部触发入口，应保持 REST 资源命名清晰并限制在本次范围内
- Database impact: 复用已落地的 `exam_result`、`exam_result_item` 表；如实现需要，可补充与成绩生成幂等、查询性能或状态表达相关的兼容性字段/索引
- Planned DTO impact: 可能扩展既有提交结果 DTO 或新增内部结果生成读写 DTO，但不得破坏现有考生端成绩单契约稳定性
- Dependencies: 依赖既有 `online-answering-flow`、`exam-submission`、题库答案配置与 `add-score-report` 已定义的结果模型；继续复用统一 `TraceNo`、请求/响应/异常日志与日志脱敏能力
