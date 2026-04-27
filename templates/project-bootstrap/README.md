# 项目引导模板

本目录提供一套可直接复用到其他前后端分离项目的基线文档，不会影响当前仓库的 `AGENTS.md` 与 `openspec/config.yaml`。

## 文件说明

- `AGENTS.template.md`：面向 Codex/代理协作的仓库级规范模板
- `openspec.config.template.yaml`：面向 OpenSpec 的通用配置模板

## 建议使用方式

1. 复制 `AGENTS.template.md` 到目标项目根目录并重命名为 `AGENTS.md`
2. 复制 `openspec.config.template.yaml` 到目标项目的 `openspec/config.yaml`
3. 按目标项目实际情况替换模板中的占位符
4. 再补充项目专属约束，不要把项目个性化规则直接写回通用模板

## 需要替换的占位符

- `<project-name>`：项目名称
- `<frontend-root>`：前端根目录，例如 `web`、`admin-web`
- `<backend-root>`：后端根目录，例如 `service`、`api-service`
- `<frontend-stack>`：前端技术栈描述
- `<backend-stack>`：后端技术栈描述
- `<base-package>`：后端根包名，例如 `com.example.project`

## 哪些内容建议保留

- SDD / OpenSpec 优先的工作方式
- 前后端通过 REST 契约解耦
- DTO 不直接暴露持久化实体
- TraceNo、日志脱敏与链路日志要求
- 按层分配后端测试责任
- 后端默认 TDD、前端业务逻辑优先 TDD

## 哪些内容建议按项目调整

- 登录后主页面的命名和布局职责
- 前端目录命名细节
- 后端持久层技术描述，例如 MyBatis-Plus、JPA、Spring Data JDBC
- 数据库类型，例如 MySQL、PostgreSQL
- 鉴权、租户、审计、灰度发布等项目专属规则

## 维护建议

- 通用模板只放跨项目稳定规则
- 项目专属目录、模块名、基础设施约束应保留在各自项目的正式配置中
- 当多个项目都出现相同的新约束时，再回收进本模板
