## 1. `exam-service` 考生端认证与考试查询契约

- [x] 1.1 在 `exam-service` 先编写失败测试，覆盖考生登录成功/失败、未确认拦截、确认换发、可参加考试过滤、权限边界与日志脱敏行为
- [x] 1.2 新增考生端 DTO、鉴权配置与必要的 Mapper/XML 查询，复用 `examinee`、`exam_plan`、`exam_plan_examinee`、`paper` 数据完成 `/api/candidate/auth/login`、`/api/candidate/profile`、`/api/candidate/profile/confirm`、`/api/candidate/exams` REST 契约
- [x] 1.3 在 `cn.jack.exam.controller.candidate`、`service.candidate` 与相关 `config` 中实现考生 Token 签发、会话确认标记与考试列表过滤逻辑，并确认本次实现不扩展到在线答题、交卷或成绩单
- [x] 1.4 为考生端登录、身份确认与考试查询补充 TraceNo 关联业务日志和脱敏校验，确保不输出明文身份证号、完整 Token 或完整 Authorization 头

## 2. `exam-web` 考生端登录、确认页与考试列表

- [x] 2.1 在 `exam-web` 先编写失败测试，覆盖考生端登录表单校验、独立路由守卫、未确认重定向、确认成功后的跳转、考试列表展示与空态
- [x] 2.2 在 `exam-web/src/modules/candidate-portal/` 下新增类型、服务、页面与必要组件骨架，使前端 DTO 与后端 REST 契约保持一致，并与管理端壳层隔离
- [x] 2.3 实现考生登录页、身份信息确认页和可参加考试列表页，接入统一样式、加载态、错误态与待考试说明，且不在页面中提供在线答题、提交试卷或成绩单入口
- [x] 2.4 实现考生端登录态存储、确认状态控制与页面跳转逻辑，确保未登录或未确认用户不能直接进入考试列表页
- [x] 2.5 在 `exam-web` 先编写失败测试，再实现考生端“刷新考试列表”“退出登录”按钮与缓存清理/覆盖逻辑，且继续保持不扩展到在线答题、提交试卷或成绩单

## 3. 联调与验证

- [x] 3.1 运行并通过 `exam-service` 的测试集，重点验证：错误凭据登录拒绝、禁用考生登录拒绝、未确认无法查询考试列表、已结束/未发布考试过滤、TraceNo 透传与日志脱敏
- [x] 3.2 运行并通过 `exam-web` 的测试集，并针对考生端登录、身份确认、列表空态和只读边界做针对性页面验证
- [x] 3.3 完成 `exam-web` 与 `exam-service` 的接口联调，确认登录响应、身份信息字段、确认后会话行为、考试列表字段与错误提示一致，且实现结果未扩展到在线答题、交卷或成绩单
