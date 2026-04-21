## Why

当前 `exam-web` 已具备管理员登录、统一壳层和考生管理能力，但页面整体仍明显停留在“默认组件换肤”的阶段，缺少成熟考试业务系统应有的稳定感、工作台秩序和长期使用可读性。结合新的参考样式，本次需要将前端管理端重设计为更贴近“考试平台 / 政企业务后台”的浅色扁平工作台，同时保持现有功能、权限和接口行为不变。

## What Changes

- 以 `Direction C - Enterprise Exam Platform` 为基准，重构 `exam-web` 的统一后台视觉与信息架构。
- 重构登录后的统一主页面壳层，使其采用浅色、扁平、左侧固定导航、顶部工作区标签页、白底主工作区的业务后台结构。
- 重构管理首页，使其以个人信息、统计概览、常用功能和趋势图表构成真实工作台，而不是简单的信息卡片拼贴。
- 重构列表型业务页的统一表达方式，使筛选区、工具栏和表格区形成稳定的三段式布局，优先保障业务扫描效率。
- 继续保留现有登录、权限、路由、考生管理、导入导出和 REST 契约行为，不新增 `exam-service` 接口，不修改 DTO 和数据库结构。

## Capabilities

### New Capabilities

- 无

### Modified Capabilities

- `admin-console-ui`: 将统一壳层与页面结构要求从通用企业管理台升级为更接近成熟考试业务系统的浅色扁平工作台，包括左侧导航、顶部工作区标签、白底工作区、首页工作台布局和列表页三段式业务结构。

## Impact

- Affected code: `exam-web/src/shared/layouts/**`、`exam-web/src/shared/components/**`、`exam-web/src/shared/styles/**`、`exam-web/src/modules/auth/pages/**`、`exam-web/src/modules/dashboard/pages/**`、`exam-web/src/modules/examinees/pages/**`
- APIs: 无新增或修改的 REST 端点；继续使用现有管理员认证与考生管理接口
- Dependencies: 继续使用现有 `Ant Design` 与前端技术栈，不新增新的 UI 框架
- Systems: 仅影响 `exam-web`；`exam-service`、MySQL、MyBatis-Plus Mapper、TraceNo 与日志策略不在本次变更范围内
