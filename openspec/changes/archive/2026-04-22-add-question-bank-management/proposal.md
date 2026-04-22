## Why

管理端基础权限与登录已经具备，考生管理也已形成独立模块，但仓库仍缺少后续试卷管理与考试计划所依赖的稳定试题主数据来源。现在需要先落地边界清晰的题库管理 change，在不扩展到试卷管理、考试计划或考生答题流程的前提下，让管理员能够维护试题及其基础属性。

## What Changes

- `exam-web` 新增落在 `src/modules/question-bank/` 的题库管理模块，提供题目查询、录入、编辑、删除以及题型管理入口，并继续接入登录成功后的统一管理端主页面，而不是为单一模块重复建设独立首页框架。
- `exam-service` 新增题库管理 REST API 与显式 DTO，覆盖题目分页查询、详情获取、创建、更新、删除，以及题型的查询、创建、更新、删除，保持资源化命名并避免直接暴露持久化实体。
- `exam-service` 新增题目与题型相关的 MySQL 表结构、MyBatis-Plus Mapper/XML、数据库迁移脚本与菜单/API 权限种子数据，沿用现有 `TraceNo`、请求/响应/异常日志和脱敏约束。
- 包含范围：题目录入、题目编辑、题目删除、题目查询、题型管理、难度配置、分值配置、答案配置；影响 `exam-web` 与 `exam-service`。
- 不包含范围：试卷管理、考试计划、考生答题、题目分类、组卷规则、批量导入导出与其他超出“试题基础属性管理”的能力。

## Capabilities

### New Capabilities
- `question-bank-management`: 管理端在 `exam-web` 与 `exam-service` 中维护试题、题型以及难度/分值/答案等基础属性配置能力。

### Modified Capabilities
- None.

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `GET /api/admin/questions`, `GET /api/admin/questions/{id}`, `POST /api/admin/questions`, `PUT /api/admin/questions/{id}`, `DELETE /api/admin/questions/{id}`, `GET /api/admin/question-types`, `POST /api/admin/question-types`, `PUT /api/admin/question-types/{id}`, `DELETE /api/admin/question-types/{id}`
- Planned DTOs: 题目查询条件、题目分页项、题目详情响应、题目创建/编辑请求、题型列表项、题型创建/编辑请求、通用删除响应或空响应
- Database impact: 新增题型表与题目表，补充索引/唯一约束、逻辑删除字段以及管理端菜单/API 权限初始化数据
- Dependencies: 复用既有管理端登录、权限控制、统一管理台壳层、`TraceNo` 透传与日志脱敏基础能力
