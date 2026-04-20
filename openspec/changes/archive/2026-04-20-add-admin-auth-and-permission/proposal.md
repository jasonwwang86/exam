## Why

当前仓库已经明确按模块逐步推进开发，管理端基础权限与登录是第一阶段的基础能力。如果没有统一的管理员登录、身份保持和权限控制机制，后续的考生管理、题库管理等管理端模块都无法安全、稳定地落地。

## What Changes

- 为 `exam-web` 增加管理端登录页、登录态管理、基础鉴权守卫与权限路由控制，仅覆盖管理端基础登录进入与受保护页面访问。
- 为 `exam-service` 增加管理员认证接口、登录态校验机制，以及基于用户、角色、权限的基础鉴权能力。
- 为 `exam-service` 设计基础用户、角色、权限、角色权限关联、用户角色关联等模型，并明确对应的 DTO、Mapper 与 MySQL 表结构调整。
- 定义管理端基础认证 REST 契约，包括登录接口、当前登录用户信息接口、退出接口，并约束稳定的请求/响应 JSON 结构。
- 为管理端菜单权限或接口权限建立最小可用控制策略，保证未登录用户无法访问受保护页面，已登录但无权用户无法访问受限菜单或接口。
- 明确本模块包含范围：登录页、登录接口、Token 或 Session 机制、基础用户/角色/权限模型、菜单权限或接口权限控制。
- 明确本模块不包含范围：考生管理、题库管理、大屏可视化、复杂审计能力、细粒度业务授权编排。

## Capabilities

### New Capabilities

- `admin-authentication`: 覆盖 `exam-web` 管理端登录页、登录态持久化与受保护路由，以及 `exam-service` 的登录、退出、当前用户信息与登录态校验能力。
- `admin-access-control`: 覆盖 `exam-service` 的用户-角色-权限基础模型与接口鉴权，以及 `exam-web` 的菜单权限或路由权限控制能力。

### Modified Capabilities

- None.

## Impact

- Affected systems: `exam-web`、`exam-service`
- Frontend impact (`exam-web`):
  - 新增管理端登录页与登录表单提交流程
  - 新增登录态存储、路由守卫、权限初始化与无权限处理
  - 新增与认证相关的 API 调用与前端类型定义
- Backend impact (`exam-service`):
  - 新增认证控制器、认证服务、权限校验逻辑
  - 在 `cn.jack.exam` 下新增或扩展 `controller`、`service`、`dto`、`entity`、`mapper`、`config`、`common` 包中的认证鉴权相关代码
  - 新增或调整 MySQL 表：管理员用户表、角色表、权限表、用户角色关联表、角色权限关联表
  - 新增或调整 MyBatis-Plus Mapper 与 SQL 映射
- REST APIs:
  - `POST /api/admin/auth/login`
  - `GET /api/admin/auth/me`
  - `POST /api/admin/auth/logout`
- Request/response DTOs:
  - `AdminLoginRequest`
  - `AdminLoginResponse`
  - `AdminCurrentUserResponse`
  - `AdminPermissionItemResponse`
- Assumptions:
  - 本阶段优先支持管理端账号密码登录
  - 本阶段采用一种统一登录态机制（Token 或 Session），由设计文档确定具体方案
  - 初始化管理员、角色、权限数据可通过数据库初始化脚本或应用启动预置完成
