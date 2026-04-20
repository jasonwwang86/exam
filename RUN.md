# RUN

## 适用范围

本文说明本仓库前端 `exam-web` 与后端 `exam-service` 的本地启动方法。

## 环境要求

- Node.js 与 npm
- Java 21
- Maven 3.9+
- MySQL 8.x

## 后端 `exam-service`

### 1. 准备数据库

后端默认读取 `exam-service/src/main/resources/application.yml` 中的 MySQL 配置：

- 地址：`127.0.0.1:3306`
- 数据库：`exam`
- 用户名：`root`
- 密码：`Test123456`

请先确保本地 MySQL 已启动，并已创建数据库：

```sql
CREATE DATABASE exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 启动后端

在仓库根目录执行：

```bash
cd exam-service
mvn spring-boot:run
```

默认情况下，Spring Boot 使用 `8080` 端口。

### 3. 运行后端测试

```bash
cd exam-service
mvn test
```

说明：
- 测试环境使用 `src/test/resources/application.yml` 中的 H2 内存数据库，不依赖本地 MySQL。

## 前端 `exam-web`

### 1. 安装依赖

```bash
cd exam-web
npm install
```

### 2. 启动前端开发服务

```bash
cd exam-web
npm run dev
```

Vite 默认启动在 `http://127.0.0.1:5173/` 或 `http://localhost:5173/`。

### 3. 运行前端测试

```bash
cd exam-web
npm test
```

### 4. 构建前端

```bash
cd exam-web
npm run build
```

## 本地联调说明

当前前端请求使用相对路径 `/api/...`，代码位于 `exam-web/src/modules/auth/services/authApi.ts`。

当前仓库中的 `exam-web/vite.config.ts` 还没有配置本地开发代理，因此仅分别启动前后端时，浏览器中的 `/api` 请求不会自动转发到 `exam-service`。

如果要进行前后端本地联调，建议采用以下任一方式：

1. 在 Vite 中增加 `/api` 到后端 `http://127.0.0.1:8080` 的代理配置。
2. 使用 Nginx、网关或其他反向代理，将前端与后端统一到同一域名下访问。

## 常用命令速查

```bash
# 后端启动
cd exam-service && mvn spring-boot:run

# 后端测试
cd exam-service && mvn test

# 前端安装依赖
cd exam-web && npm install

# 前端启动
cd exam-web && npm run dev

# 前端测试
cd exam-web && npm test

# 前端构建
cd exam-web && npm run build
```
