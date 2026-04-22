## Why

题库管理已经具备可复用的试题主数据，但仓库仍缺少把试题组织为试卷的独立模块，导致后续考试计划无法建立在稳定的试卷实体之上。现在需要先落地边界清晰的试卷管理 change，在不扩展到考试计划、在线答题或判分流程的前提下，让管理员能够手工组卷并维护试卷基础信息与题目明细。

## What Changes

- `exam-web` 新增落在 `src/modules/paper-management/` 的试卷管理模块，提供试卷分页查询、新增、编辑、删除、详情查看以及手工组卷交互，并继续接入登录成功后的统一管理端主页面。
- `exam-service` 新增试卷管理 REST API 与显式 DTO，覆盖试卷分页查询、详情获取、创建、更新、删除，以及试卷题目明细的查询、添加、调整、删除和排序，保持资源化命名并避免直接暴露持久化实体。
- `exam-service` 新增试卷、试卷题目明细相关的 MySQL 表结构、MyBatis-Plus Mapper/XML、数据库迁移脚本与菜单/API 权限种子数据，沿用现有 `TraceNo`、请求/响应/异常日志和脱敏约束。
- 包含范围：手工组卷、试卷基础信息维护、试卷题目明细维护、总分配置、时长配置；影响 `exam-web` 与 `exam-service`。
- 不包含范围：题库管理能力本身、考试计划、在线答题、自动组卷、随机抽题、阅卷判分、成绩计算、考生作答记录及其他超出“管理端试卷组织与维护”的能力。

## Capabilities

### New Capabilities
- `paper-management`: 管理端在 `exam-web` 与 `exam-service` 中维护试卷基础信息、题目明细以及手工组卷流程的能力。

### Modified Capabilities
- None.

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `GET /api/admin/papers`, `GET /api/admin/papers/{id}`, `POST /api/admin/papers`, `PUT /api/admin/papers/{id}`, `DELETE /api/admin/papers/{id}`, `GET /api/admin/papers/{id}/questions`, `POST /api/admin/papers/{id}/questions`, `PUT /api/admin/papers/{id}/questions/{paperQuestionId}`, `DELETE /api/admin/papers/{id}/questions/{paperQuestionId}`
- Planned DTOs: 试卷查询条件、试卷分页项、试卷详情响应、试卷创建/编辑请求、试卷题目明细列表项、试卷题目明细创建/编辑请求、试卷总分汇总响应
- Database impact: 新增试卷表与试卷题目明细表，补充索引、唯一约束、逻辑删除字段、总分/时长字段以及管理端菜单/API 权限初始化数据
- Dependencies: 复用既有管理端登录、权限控制、统一管理台壳层、题库管理提供的试题数据、`TraceNo` 透传与日志脱敏基础能力
