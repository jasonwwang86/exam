# OpenSpec 场景建模模板与使用约束

## 1. 文档目的

本文档用于明确 `exam` 在“原型优先、按场景建 OpenSpec、按模块落地实现”模式下的具体执行方式，提供可直接复用的模板，并约束何时使用 `openspec new + continue`，何时使用 `openspec propose`。

本文档是团队工作模板，不替代具体 change 中的 `proposal.md`、`spec.md`、`design.md`、`tasks.md`。

## 2. 先说结论

对 `exam` 而言，默认建议如下：

- 有原型、跨前后端、跨多个模块、仍需澄清边界时：优先使用 `openspec new + continue`
- 需求已经非常清晰、范围较小、希望快速出完整草案时：可使用 `openspec propose`
- 无论使用哪种入口，change 都应优先按“场景”命名和建模
- 真正进入实现、拆任务和写代码时，再按 `exam-web`、`exam-service` 和稳定业务模块收口

## 3. “按场景建，再按模块收口” 的执行流程

### 3.1 第一步：从原型提炼场景

不要先问“这是哪个模块”，而要先回答以下问题：

- 用户要完成什么事
- 从哪个入口进入
- 关键步骤有哪些
- 成功和失败分别怎么结束
- 涉及哪些已有业务模块

如果一段流程满足“用户可感知、可独立验收、跨页面或跨接口仍属于同一业务闭环”，就应作为一个场景 change。

示例：

- `add-online-answering-flow`
- `add-exam-plan-management`
- `align-admin-dashboard-with-live-features`

### 3.2 第二步：创建 change

推荐命名规则：

- 动词 + 业务场景，使用 kebab-case
- 优先表达用户场景或能力场景，不要表达页面细节或技术细节

推荐：

- `add-exam-scheduling-flow`
- `add-candidate-identity-confirmation`
- `align-admin-dashboard-with-live-features`

不推荐：

- `add-create-page`
- `add-form-fields`
- `add-dto`

### 3.3 第三步：先写场景级 proposal

`proposal.md` 的职责是明确：

- 为什么要做这段场景
- 这段场景做什么
- 这段场景不做什么
- 影响哪些 capability
- 影响 `exam-web`、`exam-service` 哪些边界

这一层仍以场景为主，不急着落到代码目录。

### 3.4 第四步：把场景拆成 capability specs

proposal 确认后，再把 change 落到一个或多个稳定 capability 上。

常见拆法：

- 一个场景只新增一个能力：`specs/<capability>/spec.md`
- 一个场景会修改多个已有能力：同一 change 下修改多个 `specs/<capability>/spec.md`

这一步开始体现“按模块收口”：

- change 还是场景
- spec 已经开始表达长期稳定能力

### 3.5 第五步：写 design，收口前后端边界

`design.md` 要完成从“场景叙事”到“实现落点”的收口，至少写清楚：

- 页面流程和状态流转
- REST 资源和 DTO
- 哪些能力落在 `exam-web`
- 哪些能力落在 `exam-service`
- 哪些地方复用已有模块
- 测试和联调策略

### 3.6 第六步：写 tasks，按实现边界拆

`tasks.md` 默认按以下三块拆：

1. `exam-service`
2. `exam-web`
3. 联调与验证

必要时再在每块下面按模块进一步细分，但不要按原型视觉区块拆任务。

### 3.7 第七步：代码实现按模块落地

到实现阶段，统一收口到稳定边界：

- 前端：`exam-web/src/modules/<module>/pages|components|services|hooks|types`
- 前端共享：`exam-web/src/shared/`
- 后端：`controller`、`service`、`dto`、`mapper`、`entity`、`config`

## 4. 推荐的 OpenSpec 工作方式

对于 `exam`，默认推荐使用 spec-driven 顺序：

1. `proposal`
2. `specs`
3. `design`
4. `tasks`

也就是：

`先定义场景 -> 再定义能力约束 -> 再定义实现设计 -> 最后定义执行任务`

这是比“先想实现”更稳的顺序，尤其适合原型优先和跨前后端协作场景。

## 5. 模板使用约束

### 5.1 总体约束

- 模板是结构，不是内容填空游戏
- 模板中的标题可以复用，但内容必须结合本次场景重写
- 不得把原型文案直接复制为需求规格
- 不得把 `context`、`rules`、`instruction` 等工具输出原样写入 OpenSpec 文件
- proposal、spec、design、tasks 默认使用中文，英文术语仅作补充

### 5.2 proposal 使用约束

- proposal 以场景为单位，不以单个 DTO、单个页面控件或单个表字段为单位
- proposal 必须写清楚不做什么，避免场景无限扩边界
- proposal 的 Capabilities 必须是稳定能力名，不是一次性页面名

### 5.3 spec 使用约束

- spec 描述长期能力约束，不描述一次性开发动作
- requirement 必须能长期成立，不能写成“本次先临时支持”
- scenario 必须可验证，能落到测试或联调检查项

### 5.4 design 使用约束

- design 必须体现前后端边界和 REST + DTO 契约
- design 必须说明跨模块页面如何收口到模块实现
- design 必须写明 TDD 适用范围和验证重点

### 5.5 tasks 使用约束

- tasks 必须按 `exam-service`、`exam-web`、联调验证拆分
- 后端 tasks 不得长期只写 controller 测试，存在业务规则时必须有 service 测试
- 前端 tasks 中，业务逻辑、校验、状态处理应明确 TDD 或验证方式
- tasks 应映射到实现边界，不应映射到原型视觉区块

## 6. 模板一：场景型 proposal.md

```md
## Why

当前系统已经具备【已有基础能力】，但还不能支持【本次场景】。如果缺少该能力，用户在【业务入口】无法完成【目标动作】，并会导致【业务影响】。

现在需要在不扩展到【明确排除范围】的前提下，补齐【本次场景名称】这一段独立可验收的业务闭环。

## What Changes

- `exam-web` 新增或调整【页面/交互范围】，覆盖【入口、主流程、关键状态】。
- `exam-service` 新增或调整【REST API / DTO / Service / 持久化范围】。
- 补充【状态流转 / 校验规则 / 日志 / TraceNo / 权限】能力。
- 包含范围：`【列出 3-5 个本次必须交付的能力】`
- 不包含范围：`【列出 3-5 个本次明确不做的能力】`

## Capabilities

### New Capabilities

- `【capability-name】`: 定义【该稳定能力】的前后端行为约束。

### Modified Capabilities

- `【existing-capability-name】`: 调整【受影响的已有能力】以支持【本次场景】。

## Impact

- Affected systems: `exam-web`, `exam-service`
- Planned REST endpoints: `【接口列表】`
- Planned DTOs: `【DTO 列表】`
- Database impact: `【无 / 新增表 / 调整字段 / 索引 / 约束】`
- Dependencies: `【依赖的既有模块、权限、日志、TraceNo、鉴权等】`
```

## 7. 模板二：capability spec.md

```md
## ADDED Requirements

### Requirement: `【capability-name】` 应支持【核心能力】
系统 MUST 为【角色】提供【能力描述】。`exam-web` 必须【前端行为要求】；`exam-service` 必须【后端行为要求】。

#### Scenario: 【正常主流程】
- **WHEN** 【触发条件】
- **THEN** `exam-web` 必须【页面行为】
- **THEN** `exam-service` 必须【服务行为】

#### Scenario: 【失败或拒绝场景】
- **WHEN** 【非法条件、权限不足或状态不满足】
- **THEN** `exam-service` 必须【拒绝逻辑】
- **THEN** `exam-web` 必须【错误反馈】

### Requirement: `【capability-name】` 应遵守【规则名称】
系统 MUST 对【状态、校验、数据范围、日志或权限】执行稳定约束。

#### Scenario: 【规则成立场景】
- **WHEN** 【前置条件】
- **THEN** `exam-service` 必须【规则结果】
- **THEN** `exam-web` 必须【展示或限制结果】
```

## 8. 模板三：场景型 design.md

```md
## Context

当前仓库已经具备【已有能力】。本次 change 需要补齐【场景名称】，同时保持范围收敛在【边界】，避免提前扩展到【非目标范围】。

## Goals / Non-Goals

**Goals:**
- 在 `exam-web` 中实现【页面和交互能力】
- 在 `exam-service` 中实现【REST / DTO / 业务规则 / 持久化能力】
- 复用【既有模块、权限、日志、TraceNo】能力

**Non-Goals:**
- 不实现【本次不做的相关能力】
- 不重做【既有稳定模块】

## Decisions

### Decision 1: 页面场景如何收口到前端模块

`exam-web` 在 `src/modules/【module-name】/` 下承载【页面容器、组件、服务、类型】。若页面需要复用【其他模块】数据，则通过【服务调用 / 共享组件】方式接入，而不是复制模块实现。

### Decision 2: REST 契约如何设计

后端统一暴露以下接口：
- `【endpoint 1】`
- `【endpoint 2】`

其中：
- `【接口 1 的职责】`
- `【接口 2 的职责】`
- `【为什么不用更大或更小的接口颗粒度】`

### Decision 3: 状态、规则与持久化如何定义

- 状态枚举：`【状态列表】`
- 核心校验：`【列出 3-5 条】`
- 持久化影响：`【新增/调整的表与约束】`

### Decision 4: 测试与验证策略

- `exam-service`：先写失败测试，再写实现；覆盖【controller / service / mapper / config】实际影响范围
- `exam-web`：对【状态处理、接口封装、表单校验、权限逻辑】先写失败测试
- 非 TDD 项：`【样式或展示调整的验证方式】`

## Risks / Trade-offs

- `【风险 1】` -> `【控制方式】`
- `【风险 2】` -> `【控制方式】`

## Migration Plan

1. 先完成 `exam-service` 的【契约和规则】实现
2. 再完成 `exam-web` 的【页面和交互】实现
3. 最后完成联调、日志、TraceNo 与场景验收

## Open Questions

- `【仍待确认的问题；如果没有，可写“当前无阻塞问题”】`
```

## 9. 模板四：按模块收口的 tasks.md

```md
## 1. `exam-service` 【后端能力名称】

- [ ] 1.1 先编写失败测试，覆盖【分页/详情/创建/更新/状态流转/权限拒绝/日志行为】
- [ ] 1.2 新增或调整【Entity / DTO / Mapper / XML / schema / data】
- [ ] 1.3 实现【主 REST 资源】及其核心校验
- [ ] 1.4 实现【关联资源 / 状态更新 / 聚合查询】及异常分支
- [ ] 1.5 补充 TraceNo、请求/响应/异常日志与关键业务日志验证

## 2. `exam-web` 【前端能力名称】

- [ ] 2.1 先编写失败测试，覆盖【路由可见性 / 列表查询 / 表单校验 / 状态处理 / 无权限态】
- [ ] 2.2 在 `src/modules/【module-name】/` 下新增或调整【types / services / pages / components】
- [ ] 2.3 实现【列表页 / 详情页 / 流程页 / 弹窗】与加载态、空态、错误态
- [ ] 2.4 实现【关键交互】并保证与后端规则一致
- [ ] 2.5 将页面集成到统一壳层，补充非 TDD 的针对性验证

## 3. 联调与验证

- [ ] 3.1 运行并通过 `exam-service` 测试，重点验证【规则、权限、状态流转、TraceNo、日志脱敏】
- [ ] 3.2 运行并通过 `exam-web` 测试，重点验证【关键页面行为和错误反馈】
- [ ] 3.3 完成前后端联调，确认【字段口径、状态语义、异常提示】一致
```

## 10. 模板使用示例

如果原型是“管理员创建并发布考试计划”，推荐这样组织：

- change 名：`add-exam-plan-management`
- proposal：写“为什么要让管理员完成考试计划配置和发布”
- capability：`exam-plan-management`
- design：写计划配置流程、状态流转、REST 资源、考生范围和试卷选择的边界
- tasks：
  - `exam-service`：计划主资源、范围更新、状态流转、测试
  - `exam-web`：列表页、配置流程、选择器、校验、测试
  - 联调：发布时间校验、空范围拒绝、TraceNo、错误提示

## 11. `openspec new + continue` 和 `openspec propose` 的取舍

### 11.1 `openspec new + continue` 的特点

`openspec new change "<name>"` 先创建 change 骨架；之后通过 `continue` 一次只推进一个 artifact。

优点：

- 更适合边界还在澄清中的需求
- 可以先看 proposal，再决定 capability 拆分
- 每个 artifact 都能单独审阅，返工成本更低
- 更适合原型优先、跨模块、跨前后端 change

缺点：

- 速度比 `propose` 慢
- 更依赖中途审阅和持续推进

### 11.2 `openspec propose` 的特点

`propose` 会在一轮中把 proposal、design、tasks 等 apply-ready 产物尽快生成出来。

优点：

- 出稿快
- 适合需求非常清晰、范围较小的 change
- 适合“先快速成稿，后续再微调”的场景

缺点：

- 对复杂场景更容易一次性写得过宽或过浅
- capability 拆分和模块边界更容易在首稿里不够稳
- 如果原型还没收敛，后续返工通常比 `new + continue` 大

### 11.3 对 `exam` 的默认建议

对 `exam`，默认优先建议 `new + continue`，原因是：

- 大多数变更同时影响 `exam-web` 和 `exam-service`
- 经常需要结合原型、REST + DTO、状态流转和日志约束一起收口
- change 往往不是单纯小改，而是有场景边界和模块边界两个层次

因此，以下情况优先用 `new + continue`：

- 有原型但场景边界还需要推敲
- 一个页面涉及多个模块
- 同时影响前端页面、后端接口、数据库和日志
- 需要先确认 capability 拆分是否合理
- 希望 proposal、spec、design、tasks 每一步都可评审

以下情况可以考虑用 `propose`：

- 需求已非常稳定
- change 只影响一个主 capability
- 范围小，复杂规则少
- 目标是快速生成一份完整草案，再做小修

## 12. 最终建议

`exam` 团队默认工作方式建议固定为：

1. 先基于原型识别场景
2. 优先使用 `openspec new + continue` 创建并逐步收敛 change
3. proposal 按场景写，spec 按 capability 写，design 和 tasks 按模块收口
4. 只有在需求非常清晰、范围很小、追求出稿速度时，再使用 `openspec propose`

这个工作方式的目标不是增加流程，而是减少“原型、接口、实现、联调各说各话”的返工。
