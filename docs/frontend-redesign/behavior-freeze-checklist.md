# Behavior Freeze Checklist

## Purpose

本清单用于约束 `exam-web` 前端重设计的边界。本次允许完全推翻现有视觉与信息架构，但不允许改变既有登录、权限、路由和考生管理能力。

## Scope

- In scope: `exam-web` 的视觉、布局、信息架构、页面层级、共享页面骨架
- Out of scope: `exam-service` 的接口、DTO、数据库、日志、权限模型

## Auth

- 登录成功后必须将后端返回的 Token 写入本地存储键 `TOKEN_STORAGE_KEY`
- 页面初始化时若本地存在 Token，必须尝试恢复会话
- 会话恢复成功后，必须按当前用户权限跳转到允许访问的默认页面
- 会话恢复失败后，必须清空本地 Token，并跳转到 `/login`
- 登录失败时必须展示错误提示，不得伪装成成功或静默失败
- 退出登录后必须调用退出接口，清空本地 Token，并返回 `/login`

## Routing

- 未登录状态访问受保护页面时，必须回到 `/login`
- 已登录且拥有 `dashboard:view` 权限时，默认进入 `/dashboard`
- 已登录但没有 `dashboard:view`、拥有 `examinee:view` 权限时，默认进入 `/examinees`
- 已登录但没有上述页面权限时，默认进入 `/no-permission`
- 已登录访问 `/login` 时，必须被重定向到当前账号的默认授权页面
- 未匹配路由必须重定向到当前账号的默认授权页面

## Permission

- 侧边导航必须继续来源于 `currentUser.menus`
- 当前菜单高亮逻辑必须继续按当前路由路径匹配
- 仪表盘页面的可见性必须继续受 `dashboard:view` 控制
- 考生管理页面的可见性必须继续受 `examinee:view` 控制
- 考生管理中的新增、编辑、删除、启停、导入、导出动作显隐必须继续受对应 `permissions` 控制

## Examinee Management

- 列表查询必须继续支持 `keyword` 与 `status` 条件
- 列表请求必须继续走现有 `listExaminees` 接口
- 新增必须继续走现有 `createExaminee` 接口
- 编辑必须继续走现有 `updateExaminee` 接口
- 删除必须继续走现有 `deleteExaminee` 接口
- 状态切换必须继续走现有 `updateExamineeStatus` 接口
- 导入必须继续走现有 `importExaminees` 接口
- 导出必须继续走现有 `exportExaminees` 接口
- 表单字段语义必须继续保持：考生编号、姓名、性别、身份证号、手机号、邮箱、状态、备注

## REST Contract

- 不新增前端请求字段
- 不修改现有请求字段名
- 不修改现有响应字段名
- 不调整前后端的权限字段语义
- 不新增需要后端配合的新接口

## Allowed Redesign Surface

- 登录页可完全重做布局与视觉语言
- 登录后统一壳层可完全重做导航、头部、品牌区、内容区结构
- 首页可完全重做模块概览与信息承载方式
- 考生管理页可完全重做筛选区、工具栏、表格承载、状态样式与弹窗视觉
- 可重做全局色板、字体层级、背景、边框、阴影、间距体系

## Review Checklist

- [ ] 登录成功、失败、退出行为未变化
- [ ] 会话恢复与失效回退行为未变化
- [ ] 默认路由跳转行为未变化
- [ ] 页面权限控制未变化
- [ ] 菜单来源与高亮行为未变化
- [ ] 考生管理查询与 CRUD 行为未变化
- [ ] 导入导出行为未变化
- [ ] 所有变化仅发生在前端展示层
