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

当前仓库中的 `exam-web/vite.config.ts` 已配置开发代理，会将 `/api` 请求默认转发到 `http://127.0.0.1:8080`。

如果你的后端不是运行在 `http://127.0.0.1:8080`，可以在启动前端前覆盖代理目标：

```bash
cd exam-web
EXAM_WEB_API_PROXY_TARGET=http://127.0.0.1:9090 npm run dev
```

补充说明：

1. 浏览器开发者工具中看到的请求地址仍然会是前端站点地址，例如 `http://localhost:5173/api/admin/auth/login`。
2. 这是因为浏览器先请求 Vite 开发服务器，再由 Vite 在服务端代理转发到后端目标地址。
3. 只要代理生效，请求不会再停留在前端开发服务器本身处理，也不会再出现当前这类 404。

如果要进行前后端本地联调，仍建议采用以下任一方式：

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
