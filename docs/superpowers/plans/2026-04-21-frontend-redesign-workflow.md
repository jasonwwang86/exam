# Frontend Redesign Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改变现有登录、权限、路由与考生管理业务能力的前提下，完成 `exam-web` 管理端的整体验证、设计、规格化与前端重构，让页面视觉与信息架构允许完全重做，但外部功能保持稳定。

**Architecture:** 本次工作分为“行为冻结 -> Figma 方向探索 -> OpenSpec 变更建模 -> 共享层重构 -> 关键页面迁移 -> 回归验证”六个阶段推进。实现时只重构 `exam-web` 的展示层与共享页面骨架，不修改 `exam-service` 的 REST 契约、DTO、数据库结构与日志策略。

**Tech Stack:** React, TypeScript, Vite, React Router, Axios, Ant Design, CSS Modules, Figma MCP, OpenSpec

---

## 执行原则

- 保留功能，不保留旧视觉。
- 先定设计方向，再落代码，不在页面里边写边试风格。
- 共享骨架优先于单页重写，先改 `shared/` 再改业务模块。
- `exam-service` 默认不动；如实施中发现接口缺口，需要单独起 OpenSpec 变更，不在本次范围内顺手扩展。
- `exam-web` 的纯视觉调整不强制 TDD，但菜单高亮、路由承载、权限显隐等行为改动仍要补测试。

## 约束基线

### 必须保持不变的能力

- 管理员登录成功/失败流程
- 本地 Token 恢复会话逻辑
- 登录后默认跳转规则
- 基于 `menus` 的侧边导航来源
- 基于 `permissions` 的页面与操作权限控制
- 考生管理的查询、新增、编辑、删除、启停、导入、导出能力
- 现有后端 REST 接口、DTO 字段和 JSON 结构

### 本次重点重做的前端文件

- `exam-web/src/shared/styles/global.css`
- `exam-web/src/shared/layouts/AdminLayout.tsx`
- `exam-web/src/shared/layouts/AdminLayout.module.css`
- `exam-web/src/shared/components/admin-page/AdminPage.tsx`
- `exam-web/src/shared/components/admin-page/AdminPage.module.css`
- `exam-web/src/modules/auth/pages/LoginPage.tsx`
- `exam-web/src/modules/auth/pages/LoginPage.module.css`
- `exam-web/src/modules/dashboard/pages/DashboardPage.tsx`
- `exam-web/src/modules/dashboard/pages/DashboardPage.module.css`
- `exam-web/src/modules/examinees/pages/ExamineeManagementPage.tsx`
- `exam-web/src/modules/examinees/pages/ExamineeManagementPage.module.css`

## Phase 0: 行为冻结与基线确认

**Files:**
- Reference: `exam-web/src/App.tsx`
- Reference: `exam-web/src/modules/auth/services/authApi.ts`
- Reference: `exam-web/src/modules/examinees/services/examineeApi.ts`
- Reference: `openspec/specs/admin-console-ui/spec.md`
- Create: `docs/frontend-redesign/behavior-freeze-checklist.md`

- [ ] **Step 1: 整理不可变行为清单**

  在 `docs/frontend-redesign/behavior-freeze-checklist.md` 中记录以下内容：

  ```md
  # Behavior Freeze Checklist

  ## Auth
  - 登录成功后写入本地 Token
  - 刷新后使用本地 Token 恢复会话
  - Token 无效时清空本地状态并回到 `/login`

  ## Routing
  - 具备 `dashboard:view` 时默认进入 `/dashboard`
  - 否则具备 `examinee:view` 时默认进入 `/examinees`
  - 否则进入 `/no-permission`

  ## Permission
  - 菜单来源于 `currentUser.menus`
  - 考生管理按钮与动作显隐来源于 `permissions`

  ## Examinee Management
  - 列表查询、新增、编辑、删除、状态切换、导入、导出能力全部保留
  ```

- [ ] **Step 2: 人工确认现状页面与功能**

  建议检查的页面：

  - `/login`
  - `/dashboard`
  - `/examinees`
  - `/no-permission`

  记录当前问题时只写“视觉/信息架构问题”，不要把功能正常项误记为缺陷。

- [ ] **Step 3: 确认 OpenSpec 现有约束**

  Run: `sed -n '1,220p' openspec/specs/admin-console-ui/spec.md`

  Expected:
  - 统一主页面壳层要求仍成立
  - 列表页统一结构要求仍成立
  - 本次属于“升级视觉表达”，不是推翻功能边界

## Phase 1: Figma 方向探索

**Files:**
- Create: `docs/frontend-redesign/figma-prompts.md`
- Output: Figma 方向稿链接

- [ ] **Step 1: 产出 2 套全新视觉方向**

  将以下提示词写入 `docs/frontend-redesign/figma-prompts.md`，供 `Figma` 插件使用：

  ```md
  使用 Figma 插件为 exam-web 重做后台管理端视觉方案，功能保持不变，但允许完全推翻现有布局。

  页面范围：
  1. 管理员登录页
  2. 登录后的统一后台框架
  3. 管理首页
  4. 考生管理页

  功能约束：
  - 保留用户名、密码、错误提示、登录提交
  - 保留侧边导航、顶部用户信息、退出登录
  - 保留首页概览信息
  - 保留考生管理的筛选、列表、新增、编辑、删除、状态切换、导入、导出

  设计要求：
  - 不沿用当前页面结构，可以完全重做
  - 避免默认 Ant Design 卡片堆砌感
  - 强化企业级控制台的秩序感与专业度
  - 保持桌面端与移动端都能落地
  - 输出 2 套方向：一套深色中控台，一套浅色高密度工作台
  ```

- [ ] **Step 2: 组织 Figma 评审结论**

  每套方案至少评估以下维度：

  - 首屏识别度
  - 信息密度是否适合后台
  - 筛选区与表格区关系是否清晰
  - 登录页与主界面是否同一产品体系
  - 后续新增模块能否复用当前骨架

- [ ] **Step 3: 选定 1 套视觉方向并记录结论**

  在评审结论中明确：

  - 选中的方案名称
  - 放弃另一方案的原因
  - 必须保留的亮点
  - 需要调整的细节

## Phase 2: OpenSpec 变更建模

**Files:**
- Create: `openspec/changes/redesign-admin-console-ui/proposal.md`
- Create: `openspec/changes/redesign-admin-console-ui/design.md`
- Create: `openspec/changes/redesign-admin-console-ui/tasks.md`

- [ ] **Step 1: 创建新的 OpenSpec change**

  Run: `openspec new change "redesign-admin-console-ui"`

  Expected:
  - 生成 `openspec/changes/redesign-admin-console-ui/`

- [ ] **Step 2: 编写 proposal**

  `proposal.md` 需要覆盖：

  - 为什么现有前端页面需要重设计
  - 本次只改 `exam-web`
  - 行为层、REST 契约、权限模型不变
  - 设计来源于 Figma 定稿，而不是沿用旧原型微调

- [ ] **Step 3: 编写 design**

  `design.md` 需要覆盖：

  - 新的后台信息架构
  - 全局视觉 token 方案
  - `shared/` 中沉淀哪些共享层
  - Ant Design 保留哪些组件能力、重写哪些展示层
  - 登录页、首页、考生页各自采用什么布局模式

- [ ] **Step 4: 编写 tasks**

  `tasks.md` 至少拆出：

  - 共享样式与基础 token
  - 后台统一壳层
  - 登录页
  - 首页
  - 考生管理页
  - 行为测试与人工验收

## Phase 3: 共享层重构

**Files:**
- Modify: `exam-web/src/shared/styles/global.css`
- Modify: `exam-web/src/shared/layouts/AdminLayout.tsx`
- Modify: `exam-web/src/shared/layouts/AdminLayout.module.css`
- Modify: `exam-web/src/shared/components/admin-page/AdminPage.tsx`
- Modify: `exam-web/src/shared/components/admin-page/AdminPage.module.css`

- [ ] **Step 1: 先重做全局 token 和基础背景**

  目标：

  - 确立新的颜色、阴影、圆角、边框、背景层次
  - 去掉当前“轻玻璃 + 默认蓝色渐变”的通用感
  - 为登录页与后台主界面提供统一主题基础

- [ ] **Step 2: 重构 `AdminLayout`**

  目标：

  - 重新定义品牌区、导航区、顶部工具区、内容区
  - 不改变菜单来源与高亮逻辑
  - 为后续模块扩展保留稳定骨架

- [ ] **Step 3: 重构 `AdminPage`**

  目标：

  - 提供统一页面标题区、说明区、工具区、内容承载区
  - 降低页面对单一 `Card` 容器的依赖
  - 让首页和列表页能共用同一套骨架语义

## Phase 4: 关键页面迁移

**Files:**
- Modify: `exam-web/src/modules/auth/pages/LoginPage.tsx`
- Modify: `exam-web/src/modules/auth/pages/LoginPage.module.css`
- Modify: `exam-web/src/modules/dashboard/pages/DashboardPage.tsx`
- Modify: `exam-web/src/modules/dashboard/pages/DashboardPage.module.css`
- Modify: `exam-web/src/modules/examinees/pages/ExamineeManagementPage.tsx`
- Modify: `exam-web/src/modules/examinees/pages/ExamineeManagementPage.module.css`

- [ ] **Step 1: 重做登录页**

  目标：

  - 与主界面保持同一产品语言
  - 保持聚焦式登录，不引入后台壳层
  - 保留错误提示与提交逻辑

- [ ] **Step 2: 重做首页**

  目标：

  - 不再用普通统计卡片拼凑首页
  - 用更清晰的运营概览结构承载模块状态与系统概览
  - 保持“管理首页”作为统一工作台入口

- [ ] **Step 3: 重做考生管理页**

  目标：

  - 重构筛选区、工具区、表格区的层级关系
  - 保留现有动作与权限逻辑
  - 让弹窗、状态标签、空态、错误态更统一

## Phase 5: 测试与验收

**Files:**
- Modify: `exam-web/src/test/modules/admin-pages.test.tsx`
- Modify: `exam-web/src/App.test.tsx`
- Modify: `exam-web/src/modules/examinees/pages/ExamineeManagementPage.test.tsx`
- Modify: `exam-web/src/modules/auth/services/authApi.test.ts`
- Modify: `exam-web/src/modules/examinees/services/examineeApi.test.ts`

- [ ] **Step 1: 先补或调整行为测试**

  关注点：

  - 菜单高亮仍正确
  - 默认路由跳转仍正确
  - 权限不足仍进入无权限页
  - 考生管理关键交互仍可用

- [ ] **Step 2: 运行前端测试**

  Run: `npm test`

  Workdir: `exam-web`

  Expected:
  - 所有现有测试通过
  - 若断言因 DOM 结构变化失效，应改为用户可见行为断言

- [ ] **Step 3: 运行构建验证**

  Run: `npm run build`

  Workdir: `exam-web`

  Expected:
  - 构建成功
  - 无新的 TypeScript 编译错误

- [ ] **Step 4: 做人工视觉验收**

  验收页面：

  - `/login`
  - `/dashboard`
  - `/examinees`

  验收维度：

  - 桌面端与移动端布局
  - 首屏层级是否清晰
  - 表格与工具栏是否拥挤
  - 登录页与主界面是否是同一体系
  - 页面是否仍有明显的默认卡片堆砌感

## 推荐时间线

- Day 1: 行为冻结、Figma 两套方向稿、评审定稿
- Day 2: OpenSpec change、共享层设计定稿
- Day 3: 共享层重构、登录页改造
- Day 4: 首页与考生管理页改造
- Day 5: 测试、构建、人工验收、收尾

## 完成判定

满足以下条件才算完成：

- 视觉方向已在 Figma 定稿
- OpenSpec change 已补齐 proposal、design、tasks
- `exam-web` 的共享层与 3 个关键页面已完成重构
- 登录、权限、路由、考生管理行为保持不变
- 前端测试通过
- 构建通过
- 人工验收通过

## 实施顺序建议

1. 先执行 Phase 0 和 Phase 1，避免在代码里盲改。
2. Figma 定稿后再进入 Phase 2，保证 OpenSpec 记录的是确定方案。
3. 代码阶段严格按“共享层 -> 登录页 -> 首页 -> 考生页”的顺序推进。
4. 不要跳过 Phase 5，否则很容易出现“视觉升级成功、业务链路回归失败”的情况。
