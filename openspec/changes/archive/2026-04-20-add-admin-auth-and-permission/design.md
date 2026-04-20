## Context

本次变更对应路线图中的“管理端基础权限与登录”模块，是后续考生管理、题库管理等管理端能力的前置基础。当前仓库尚未落地 `exam-web` 与 `exam-service` 的应用代码，因此本设计除了说明认证鉴权方案外，也需要明确后续代码在前后端目录和后端包结构中的落位方式。

本模块同时影响 `exam-web` 与 `exam-service`，属于典型的前后端边界协同变更，且包含以下复杂点：

- 需要定义统一的 REST 认证契约，确保登录态初始化、权限加载和退出行为一致
- 需要引入基础用户、角色、权限与会话模型，并兼顾 MySQL 表结构与 MyBatis-Plus Mapper 设计
- 需要同时解决 `exam-web` 的路由/菜单可见性控制与 `exam-service` 的接口访问拦截
- 需要把范围收敛在“管理端基础权限与登录”，避免提前扩展到考生管理、题库管理、大屏可视化或复杂审计

## Goals / Non-Goals

**Goals:**

- 为 `exam-service` 提供最小可用的管理员账号密码登录能力
- 为 `exam-service` 提供基于用户、角色、权限的基础数据模型和接口鉴权能力
- 为 `exam-web` 提供管理端登录页、登录态恢复、受保护路由、无权限处理与菜单可见控制能力
- 明确前后端 REST 契约、DTO 结构、MySQL 表结构与 MyBatis-Plus Mapper 职责
- 允许后续业务模块直接复用当前管理员身份和权限体系

**Non-Goals:**

- 不实现考生管理、题库管理、试卷管理或任何具体业务管理功能
- 不实现复杂审计日志、登录风控、多因子认证、验证码、单点登录
- 不实现细粒度数据权限、组织机构权限、字段级权限
- 不实现完整的菜单管理后台；本阶段仅要求支持基础菜单权限或接口权限控制
- 不引入 Redis、OAuth、第三方身份源等额外基础设施

## Decisions

### 1. 认证机制采用数据库持久化的 Bearer Token，而不是服务端 Session Cookie

**Decision**

`exam-service` 采用随机生成的 Bearer Token 作为登录态凭证，登录成功后写入管理员会话表；`exam-web` 在登录成功后持久化 Token，并在后续请求中通过 `Authorization: Bearer <token>` 传递。

**Rationale**

- 前后端分离场景下，Bearer Token 比 Session Cookie 更容易与 React 管理端和 REST API 对齐
- 数据库持久化会话比纯 JWT 更适合首期能力：支持退出失效、过期控制和后续扩展，而不需要额外缓存组件
- 该方案不依赖 Redis 等外部组件，适合当前仓库仍处于模块起步阶段的实际状态

**Alternatives considered**

- Session Cookie：浏览器端使用方便，但跨域、Cookie 配置与 CSRF 处理会让首期集成复杂度变高
- 纯 JWT：实现轻量，但退出失效、权限变更即时生效和会话撤销能力较弱，不适合管理端基础权限场景

### 2. 后端以“用户-角色-权限-会话”六表模型提供最小可用授权基础

**Decision**

`exam-service` 在 MySQL 中引入以下基础表：

- `admin_user`
- `admin_role`
- `admin_permission`
- `admin_user_role`
- `admin_role_permission`
- `admin_session`

其中：

- `admin_user` 存储管理员基础账号信息、密码摘要、状态
- `admin_role` 定义角色
- `admin_permission` 定义权限项，至少支持 `MENU` 与 `API` 两类
- `admin_user_role`、`admin_role_permission` 管理多对多关联
- `admin_session` 记录 Token、登录时间、过期时间、最后活动时间、失效状态

**Rationale**

- 能覆盖本模块要求的基础用户、角色、权限模型和登录态机制
- 会话表使退出、会话过期和基础安全治理具备明确落点
- 权限表区分 `MENU` 与 `API`，可同时支撑 `exam-web` 菜单控制与 `exam-service` 接口鉴权

**Mapper / package layout**

后端代码统一放在 `exam-service` 中，并遵循 `cn.jack.exam` 包结构：

- `cn.jack.exam.controller.admin`：认证接口与当前用户接口
- `cn.jack.exam.service.auth`：登录、退出、当前用户权限装配、Token 校验
- `cn.jack.exam.dto.auth`：`AdminLoginRequest`、`AdminLoginResponse`、`AdminCurrentUserResponse`
- `cn.jack.exam.dto.permission`：权限与菜单返回对象
- `cn.jack.exam.entity`：用户、角色、权限、会话实体
- `cn.jack.exam.mapper`：六张表对应的 MyBatis-Plus Mapper 及关联查询
- `cn.jack.exam.config`：认证拦截器、参数解析器、Web MVC 配置
- `cn.jack.exam.common`：权限类型枚举、状态枚举、认证常量
- `cn.jack.exam.exception`：认证失败、权限不足等异常

**Alternatives considered**

- 将权限直接固化在代码枚举中：实现更快，但后续无法支撑角色配置和模块扩展
- 只建用户和角色，不建权限表：无法满足菜单权限或接口权限控制要求

### 3. `exam-web` 使用“登录页 + 应用启动鉴权初始化 + 路由守卫 + 菜单过滤”的组合策略

**Decision**

`exam-web` 在管理端实现以下前端能力：

- 独立登录页负责账号密码提交
- 应用启动时若本地存在 Token，则主动调用 `GET /api/admin/auth/me` 恢复用户信息与权限
- 路由层对受保护页面执行登录校验；未登录跳转到登录页
- 菜单与路由元数据配置权限标识，根据 `/me` 返回的权限集合进行过滤
- 对已登录但无权限访问的页面展示无权限页，而不是回退到空白页

**Rationale**

- `/me` 作为单一初始化入口，可以让前端不依赖登录响应中的冗余结构，降低契约耦合
- 路由守卫与菜单过滤组合能够同时覆盖直接访问 URL 和常规菜单点击两种路径
- 该策略能服务当前首期模块，也便于后续业务模块直接在路由元数据中声明权限码

**Alternatives considered**

- 登录响应一次性返回全部菜单树并直接持久化：能减少一次请求，但菜单与权限状态更难刷新
- 仅做前端菜单隐藏，不做路由守卫：不能阻止用户直接访问受保护地址

### 4. 接口权限控制采用认证拦截 + 权限注解/映射校验，而不是仅依赖前端限制

**Decision**

`exam-service` 对管理端受保护接口执行两层校验：

- 第一层：认证拦截器校验 Token 是否存在、会话是否有效、用户是否启用
- 第二层：权限校验器根据接口声明的权限码或路径方法映射判断当前用户是否具备访问权

权限元数据优先推荐使用显式权限码声明，便于后续维护，例如控制器方法标注所需权限码；对于通用接口可采用白名单放行。

**Rationale**

- 管理端安全不能只依赖 `exam-web` 菜单隐藏，接口必须由服务端兜底
- 显式权限码比仅按 URL 模糊匹配更稳定，也更容易与权限表数据对应
- 白名单机制可以让登录、退出、健康检查等无需鉴权的接口保持清晰

**Alternatives considered**

- 只在前端做菜单控制：无法防止绕过 UI 直接调用接口
- 仅按 URL 模式匹配权限：维护成本较低，但接口变更时更容易引入隐式授权错误

### 5. REST 契约收敛为三类基础认证接口

**Decision**

首期只定义三类基础接口：

- `POST /api/admin/auth/login`
  - request: `AdminLoginRequest { username, password }`
  - response: `AdminLoginResponse { token, tokenType, expiresAt, user }`
- `GET /api/admin/auth/me`
  - response: `AdminCurrentUserResponse { userId, username, displayName, roles, permissions, menus }`
- `POST /api/admin/auth/logout`
  - request: 无业务体，基于当前 Token 识别会话
  - response: 通用成功响应

其中 `permissions` 返回权限码集合，`menus` 返回当前用户可见的基础菜单项。菜单项仅覆盖当前已定义的管理端基础导航，不承诺在本阶段支持完整后台菜单配置。

**Rationale**

- 三个接口足以支持登录、启动恢复、退出三条主线
- `/me` 汇总当前用户与权限上下文，适合作为 `exam-web` 的初始化入口
- 不在首期加入刷新 Token、修改密码、重置密码等能力，避免范围失控

**Alternatives considered**

- 增加刷新 Token 接口：更完整，但超出当前最小可用目标
- 拆分用户信息和权限菜单为多个接口：更细粒度，但会增加前端初始化复杂度

## Risks / Trade-offs

- [Token 存储在前端存在被窃取风险] → 首期先采用最小可用方案，并限制仅管理端使用；实现阶段需优先考虑缩短过期时间、统一登出、避免在 URL 传播 Token
- [数据库会话校验在高并发下会增加一次查询成本] → 当前为管理端基础模块，流量可接受；后续若需要可增加本地缓存或 Redis
- [权限模型过于简化可能无法覆盖后续复杂业务] → 当前明确只提供基础 RBAC，后续高级权限能力通过新的 OpenSpec change 扩展
- [当前仓库尚无前后端工程代码] → 任务中需先补齐基础目录与认证骨架，再实现业务逻辑，避免设计与实际工程脱节
- [菜单权限与接口权限的编码不一致会导致联调成本上升] → 统一约定权限码命名规则，并由 `exam-service` 的 `/me` 接口向 `exam-web` 返回同一套权限标识

## Migration Plan

1. 在 `exam-service` 中创建基础认证鉴权代码结构、数据库表结构与初始化数据脚本
2. 在 `exam-service` 中实现登录、当前用户信息、退出接口以及认证/权限拦截
3. 在 `exam-web` 中搭建管理端登录页、登录态管理、受保护路由和菜单过滤
4. 通过联调验证登录、登录态恢复、未登录跳转、无权限拦截和退出流程
5. 发布时先初始化默认管理员、角色与基础权限数据，再开放管理端入口

**Rollback strategy**

- 若上线后出现认证链路问题，可临时下线管理端入口并回滚本次认证鉴权相关表结构与接口发布
- 因本次变更为首个基础模块，不涉及既有生产业务迁移；回滚重点是确保默认管理员数据和认证中间件一并撤回

## Open Questions

- 当前无。若后续确认管理端需要长会话或多端并发登录限制，可在实现阶段前补充新的变更说明，但不纳入本次范围。
