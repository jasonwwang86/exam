## Why

管理端基础登录与权限能力已经具备，但仓库还没有可独立验收的考生主数据模块，后续考试计划、考生确认信息与考试执行流程也缺少稳定的考生数据来源。现在需要先落地一个边界清晰的考生管理 change，在不扩展到考试计划或考生端登录的前提下，让管理员能够完成基础信息维护、查询和状态管理。

## What Changes

- `exam-web` 新增 `modules/examinees` 管理模块，提供考生列表、关键字与状态查询、新增、编辑、删除、状态切换、批量导入和条件导出入口，并接入登录成功后的管理端主页面；该主页面作为模块导航容器，既包含新增的考生管理模块，也为后续其他管理模块保留统一入口。
- `exam-service` 新增考生管理 REST API 与 DTO，覆盖分页查询、创建、更新、删除、状态更新、Excel 批量导入和条件导出，保持资源化命名并避免直接暴露持久化实体。
- `exam-service` 新增考生主数据表、MyBatis-Plus Mapper/XML、数据库迁移脚本与菜单/API 权限种子数据，沿用现有 `TraceNo`、请求/响应/异常日志和脱敏约束。
- 包含范围：考生新增、考生编辑、考生删除、考生查询、考生状态管理、批量导入导出；影响 `exam-web` 与 `exam-service`。
- 不包含范围：登录权限体系本身、题库管理、考试计划、考生端登录、在线答题与其他考试执行流程。

## Capabilities

### New Capabilities
- `examinee-management`: 管理端在 `exam-web` 与 `exam-service` 中维护考生基础信息、查询条件、状态与批量导入导出能力。

### Modified Capabilities
- None.

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `GET /api/admin/examinees`, `POST /api/admin/examinees`, `PUT /api/admin/examinees/{id}`, `DELETE /api/admin/examinees/{id}`, `PATCH /api/admin/examinees/{id}/status`, `POST /api/admin/examinees/import`, `GET /api/admin/examinees/export`
- Planned DTOs: 查询条件、分页响应、创建/编辑请求、状态更新请求、批量导入结果、导出文件响应
- Database impact: 新增考生主数据表与索引/唯一约束，补充管理端菜单/API 权限初始化数据
- Dependencies: 复用既有管理端登录、权限控制、`TraceNo` 透传与日志脱敏基础能力
