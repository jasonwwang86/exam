## Why

试卷管理与考生管理已经具备独立建模的基础，但仓库还缺少把试卷、考试时间和考生范围编排为一次可执行考试的独立模块，导致管理端无法完成完整的考务安排。现在需要先落地一个边界清晰的考试计划 change，在不扩展到考生端登录、在线答题或监考大屏的前提下，让管理员能够创建、安排并维护考试计划。

## What Changes

- `exam-web` 新增落在 `src/modules/exam-plan-management/` 的考试计划管理模块，提供考试计划列表、创建、编辑、详情查看、状态切换，以及计划配置流程中的考试时间、试卷关联和考生范围设置，并接入登录成功后的统一管理端主页面。
- `exam-service` 新增考试计划 REST API 与显式 DTO，覆盖分页查询、详情获取、创建、更新、状态变更和考生范围查询，保持资源化命名并避免直接暴露持久化实体。
- `exam-service` 新增考试计划、考试计划考生范围关联相关的 MySQL 表结构、MyBatis-Plus Mapper/XML、数据库迁移脚本与菜单/API 权限种子数据，沿用现有 `TraceNo`、请求/响应/异常日志和脱敏约束。
- 包含范围：考试时间配置、试卷关联、考生范围设置、考试状态管理；影响 `exam-web` 与 `exam-service`。
- 不包含范围：试卷管理能力本身、考生管理能力本身、考生端登录、在线答题、考试提交流程、监考大屏、动态大屏以及其他超出“管理端考试计划编排与维护”的能力。

## Capabilities

### New Capabilities
- `exam-plan-management`: 管理端在 `exam-web` 与 `exam-service` 中维护考试计划基础信息、考试时间、关联试卷、考生范围与计划状态的能力。

### Modified Capabilities
- 无。

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `GET /api/admin/exam-plans`, `GET /api/admin/exam-plans/{id}`, `POST /api/admin/exam-plans`, `PUT /api/admin/exam-plans/{id}`, `PUT /api/admin/exam-plans/{id}/examinees`, `GET /api/admin/exam-plans/{id}/examinees`, `PATCH /api/admin/exam-plans/{id}/status`
- Planned DTOs: 考试计划查询条件、考试计划分页项、考试计划详情响应、考试计划创建/编辑请求、考试状态更新请求、考试计划考生范围更新请求、考试计划考生范围项、试卷选项摘要、考生选项摘要
- Database impact: 新增考试计划表与考试计划考生范围关联表，补充索引、逻辑删除字段、状态字段、时间字段以及管理端菜单/API 权限初始化数据
- Dependencies: 复用既有管理端登录、权限控制、统一管理台壳层、考生管理提供的考生主数据、试卷管理提供的试卷主数据、`TraceNo` 透传与日志脱敏基础能力
