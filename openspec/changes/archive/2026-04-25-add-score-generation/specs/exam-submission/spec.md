## MODIFIED Requirements

### Requirement: `exam-service` 应持久化最终提交状态并防止重复提交
系统 MUST 以当前考生当前考试的唯一答题会话作为最终提交状态载体，并持久化至少一种最终提交状态和最终提交时间。`exam-service` MUST 保证同一考生同一考试只会产生一次有效终态迁移；重复或并发交卷请求不得生成第二次提交，也不得覆盖第一次终态的提交方式与时间。对首次成功进入 `SUBMITTED` 或 `AUTO_SUBMITTED` 终态的会话，`exam-service` MUST 在保留交卷事实的前提下同步尝试触发成绩生成；若成绩生成失败，不得回滚既有交卷终态。

#### Scenario: 首次主动交卷写入最终状态
- **WHEN** 已确认考生在截止时间前对处于进行中的答题会话发起交卷
- **THEN** `exam-service` 必须将该会话更新为 `SUBMITTED` 终态并记录最终提交时间
- **THEN** 后续考试列表、答题会话查询和提交结果展示都必须反映该最终状态

#### Scenario: 首次主动交卷后同步尝试生成成绩
- **WHEN** 某个答题会话第一次成功进入 `SUBMITTED` 终态
- **THEN** `exam-service` 必须在同次交卷流程内同步尝试触发成绩生成
- **THEN** 即使成绩生成失败，交卷结果仍必须保持为已提交而不是回退为进行中

#### Scenario: 重复交卷请求不会产生第二次提交
- **WHEN** 同一会话已经完成交卷后再次收到交卷请求，或多个交卷请求并发命中同一会话
- **THEN** `exam-service` 必须返回既有最终提交结果或明确的已提交提示
- **THEN** `exam-service` 不得再写入第二次最终状态迁移或新的最终提交时间

#### Scenario: 已最终提交的会话不能继续保存答案
- **WHEN** 已处于 `SUBMITTED` 或 `AUTO_SUBMITTED` 终态的会话再次调用保存答案接口
- **THEN** `exam-service` 必须拒绝该写入请求
- **THEN** `exam-service` 不得改变原有答案记录和最终提交状态

### Requirement: `exam-service` 应在截止时间到达时自动交卷
系统 MUST 在答题截止时间到达时把尚未最终提交的答题会话自动收口为最终提交状态。自动交卷 MUST 在考生在线和离线两种场景下都成立，并且 MUST 与主动交卷共用同一套终态约束和防重复提交流程。对首次进入 `AUTO_SUBMITTED` 终态的会话，`exam-service` MUST 使用与主动交卷一致的成绩生成逻辑同步尝试生成结果，但本次能力 MUST 不因此扩展新的交卷页面或成绩发布工作流。

#### Scenario: 倒计时归零时在线考生看到自动交卷结果
- **WHEN** 考生仍停留在 `exam-web` 在线答题页且剩余时间减至零
- **THEN** `exam-web` 必须向 `exam-service` 请求当前考试的最终提交结果
- **THEN** `exam-service` 必须将该会话收口为 `AUTO_SUBMITTED` 或返回既有自动交卷结果
- **THEN** `exam-web` 必须展示自动交卷结果，而不是仅停留在超时只读提示

#### Scenario: 离线考生仍由服务端自动交卷
- **WHEN** 考生关闭页面、断网或浏览器冻结后，某个进行中的答题会话到达截止时间
- **THEN** `exam-service` 必须通过定时扫描或等效后端机制把该会话更新为 `AUTO_SUBMITTED`
- **THEN** 后续考试列表或答题会话查询必须观察到已自动交卷的最终状态，而不是永久停留在超时未提交状态

#### Scenario: 自动交卷后同步尝试生成成绩
- **WHEN** 某个答题会话第一次成功进入 `AUTO_SUBMITTED` 终态
- **THEN** `exam-service` 必须使用与主动交卷一致的规则同步尝试生成成绩结果
- **THEN** 若生成失败，对外仍必须保持“已自动交卷但待出分”的状态语义

#### Scenario: 截止后的交卷请求以自动交卷结果为准
- **WHEN** 考生在截止时间之后才发起交卷请求，而该会话尚未完成最终状态迁移
- **THEN** `exam-service` 必须将该会话落为 `AUTO_SUBMITTED`
- **THEN** `exam-service` 不得把这次请求记录为新的主动交卷结果
