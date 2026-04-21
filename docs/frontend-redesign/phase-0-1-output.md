# Phase 0 And Phase 1 Output

## Status

- Phase 0 completed
- Phase 1 completed

## Deliverables

- Behavior freeze checklist: `docs/frontend-redesign/behavior-freeze-checklist.md`
- Figma prompts: `docs/frontend-redesign/figma-prompts.md`
- Figma direction file: `https://www.figma.com/design/spGSVC6OUN2HUlyrHqs3Jr`

## Current Behavior Baseline

根据 `exam-web/src/App.tsx`、`exam-web/src/modules/auth/services/authApi.ts` 与 `exam-web/src/modules/examinees/services/examineeApi.ts`，本次重设计默认冻结以下行为：

- 登录成功后写入本地 Token，并尝试恢复会话
- 未登录或恢复失败时统一回到 `/login`
- 默认授权页按 `dashboard:view` -> `examinee:view` -> `/no-permission` 顺序决策
- 左侧菜单来源于 `currentUser.menus`
- 仪表盘与考生管理页的页面可见性继续由权限决定
- 考生管理继续保留查询、新增、编辑、删除、状态切换、导入、导出

## Current UI Diagnosis

当前前端页面的主要问题不是功能缺失，而是表达层过于保守，具体表现为：

- 登录页、统一后台壳层、首页、考生管理页的视觉语言一致性不足
- 页面主要依赖默认 `Card` 承载，导致后台工作台缺乏明确的主次层级
- 首页更像“占位型概览页”，不像真实的管理工作入口
- 考生管理页的信息层级还不够稳定，筛选区、工具区、表格区之间的关系可以更强
- 全局样式仍明显带有“默认蓝白后台”的通用感，品牌识别度不足

## Figma Directions Created

已在 Figma 文件中生成两套方向探索板：

### Direction A: Dark Command Center

特点：

- 深色企业中控台风格
- 更强调稳定导航、集中监控和控制台气质
- 适合把后台做成更强的“运营驾驶舱”

风险：

- 如果执行不好，表格与表单可读性会下降
- 深色体系对后续细节和对比度要求更高

### Direction B: Light Precision Workspace

特点：

- 浅色高密度工作台风格
- 更强调版式秩序、扫描效率和长期使用舒适度
- 更容易和现有 `Ant Design` 生态结合，但仍可做出高级感

风险：

- 如果层级控制不严格，容易重新退化成普通卡片后台

## Provisional Recommendation

当前更推荐优先深化 `Direction B: Light Precision Workspace`，原因如下：

- 与 `exam-web` 当前管理端功能规模更匹配
- 更容易在现有 React + Ant Design 技术栈中稳定落地
- 对登录页、首页和列表页的统一性更友好
- 后续随着模块增加，扩展成本和维护成本更可控

`Direction A` 仍然值得保留作为备选，如果你更希望后台整体气质更偏企业中控台或数据驾驶舱，可以继续沿这条路线深化。

## Reference-based Refinement

根据补充参考图，推荐进一步把方向收敛为：

- 以 `Direction B` 为主
- 但不走“现代产品后台”路线的强设计感版本
- 改为更接近“成熟考试管理系统 / 政企业务后台”的轻量业务风格

新的收敛特征：

- 浅色扁平
- 左侧固定导航
- 顶部工作区标签页
- 白底主工作区
- 筛选区、工具栏、表格区的业务结构非常明确
- 首页带统计、快捷入口、趋势图表
- 列表页强调稳定、规整、可读性，而不是视觉表现力

这意味着后续实现时，应该少做大面积装饰背景、弱化玻璃感和情绪化渐变，更多通过布局、边界、留白和细线条建立秩序感。

## Suggested Next Step

下一步建议直接进入：

1. 选定一个方向作为主方案
2. 基于该方向创建 OpenSpec change：`redesign-admin-console-ui`
3. 先重构 `shared` 层，再迁移登录页、首页、考生管理页
