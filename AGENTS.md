# AGENTS

## 项目定位

本仓库是一个采用 SDD（规格驱动开发）的前后端分离项目。

- 项目名称：`exam`
- 前端根目录：`exam-web`
- 后端根目录：`exam-service`
- 工作方式：凡是涉及功能、架构或外部行为的变更，优先使用 OpenSpec 产物进行描述，再进入实现阶段

## 技术栈

- 前端：React + TypeScript
- 前端主流框架/工具：Vite、React Router、Axios、Ant Design
- 后端：Java + MyBatis + MySQL
- 后端主流框架/工具：Spring Boot、Spring MVC、Spring Validation、MyBatis-Plus、Maven、Lombok
- API 风格：REST
- 原则：优先采用主流、稳定、广泛使用的框架和库

## 目录约定

### 前端

- 所有前端应用代码都应放在 `exam-web/` 下
- 页面、组件、服务、Hooks、状态管理应保持清晰分层
- API 请求逻辑应尽量集中管理，避免散落在无关 UI 文件中
- 前端类型定义应与后端 DTO 和 REST 契约保持一致

### 后端

- 所有后端应用代码都应放在 `exam-service/` 下
- 包职责应保持单一、清晰、易于定位
- 根包名必须为 `cn.jack.exam`
- `cn.jack.exam` 下推荐使用以下子包：
  - `controller`：负责暴露 REST 接口、基础请求校验与响应封装
  - `service`：负责业务逻辑、流程编排与事务边界
  - `dto`：负责定义接口契约中的请求/响应传输对象
  - `mapper`：负责定义 MyBatis Mapper 接口及对应 SQL 映射
  - `config`：负责放置框架与应用配置
  - `util`：负责放置无状态、可复用的工具类
  - `entity`: 负责放置实体类
  - `exception`: 负责放置异常类
  - `common`: 负责放置通用类，如ENUM, CONSTANTS

## API 与数据规则

- 使用 REST 风格接口，并采用清晰的资源化命名
- 请求和响应模型应显式定义，避免将持久化实体直接暴露给前端
- 前端 API 定义与后端 DTO 需保持一致
- 任何数据库变更都应同时考虑 MySQL 表结构兼容性与 MyBatis Mapper 更新
- 优先保持稳定、可预期的 JSON 结构与字段命名

## SDD 工作规则

- 新功能、跨模块变更、API 变更或架构调整，应先在 OpenSpec 中描述后再实现
- proposal、specs、design、tasks 应明确标注工作归属为 `exam-web`、`exam-service` 或两者同时涉及
- 设计文档在适用时应覆盖前后端边界、REST 契约以及持久化影响
- 任务拆分应区分前端、后端、API 和验证工作

## 实施约定

- 通过 REST 契约保持前后端松耦合
- 优先保证模块边界清晰，避免过早抽象
- 仓库已有稳定模式时，优先复用既有约定
- 根据所修改的层级补充对应测试或验证步骤
- 变更同时影响 UI 与服务端行为时，应明确记录关键假设
