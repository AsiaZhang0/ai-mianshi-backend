# AI 面试官 (ai-mianshi)

> 基于 Spring Cloud 微服务 + LangChain4j AI Agent 的智能面试系统

## 项目简介

AI 面试官是一个智能面试系统，用户上传简历 PDF 后，AI 面试官会基于简历内容进行个性化技术面试。系统支持 RAG 知识库检索、长期记忆存储、面试日志记录，并集成了百度实时语音识别。

### 功能演示

![演示视频](./20260627_221628.mp4)

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| **语言** | Java | 21 |
| **框架** | Spring Boot + Spring Cloud | 3.5.x / 2025.0.1 |
| **注册中心** | Nacos | 3.2.0 |
| **RPC** | Apache Dubbo | 3.3.0 |
| **网关** | Spring Cloud Gateway | WebFlux 响应式 |
| **认证鉴权** | Sa-Token + Redis | 1.44.0 |
| **限流熔断** | Sentinel（Nacos 持久化） | 1.8.6 |
| **ORM** | MyBatis-Plus | 3.5.x |
| **数据库** | MySQL + PostgreSQL（pgvector） | 8.x |
| **搜索引擎** | Elasticsearch | 8.x |
| **消息队列** | Kafka（KRaft 模式） | 3.9.1 |
| **缓存** | Redis（Lettuce）+ Caffeine | - |
| **AI 框架** | LangChain4j（agentic / easy-rag / pgvector） | 1.16.x-beta26 |
| **大模型** | 通义千问 qwen3.6-plus + text-embedding-v4 | - |
| **语音识别** | 百度实时 ASR（WebSocket） | - |
| **文件存储** | 腾讯云 COS | - |
| **PDF 解析** | Apache Tika | 3.3.1 |
| **API 文档** | Knife4j（Gateway 聚合） | 4.4.0 |
| **日志** | Log4j2 + Disruptor 异步 | - |

---

## 项目架构

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (:8081)                     │
│         Sa-Token 鉴权 · Sentinel 限流 · CORS · Knife4j        │
└──────────┬──────────┬──────────┬──────────┬─────────────────┘
           │          │          │          │
    ┌──────▼───┐ ┌───▼────┐ ┌───▼────┐ ┌───▼────┐
    │   User   │ │Questions│ │Q-Bank  │ │  File  │
    │  :8082   │ │  :8083  │ │  :8084 │ │  :8086 │
    │  注册登录  │ │ 题目管理 │ │ 题库管理 │ │ COS上传 │
    └────┬─────┘ └───┬────┘ └───┬────┘ └───┬────┘
         │           │Kafka     │          │
         │    ┌──────▼──────┐   │          │
         │    │   Agent     │   │          │
         │    │   :8085     │◄──┘          │
         │    │  AI 面试官   │              │
         │    │  百度 ASR   │              │
         │    └──────┬──────┘              │
         │           │                     │
    ┌────▼───────────▼─────────────────────▼────┐
    │         Dubbo RPC（跨服务调用）              │
    └────────────────────────────────────────────┘
```

---

## 模块说明

| 模块 | 端口 | 说明 |
|------|------|------|
| **gateway** | 8081 | API 网关，统一入口、鉴权、限流、文档聚合 |
| **user** | 8082 | 用户注册/登录/管理，Sa-Token 认证 |
| **questions** | 8083 | 题目 CRUD，Elasticsearch 搜索，Kafka 事件发布 |
| **questionBank** | 8084 | 题库管理与题库-题目关联 |
| **agent** | 8085 | **核心模块**，AI 面试 Agent、RAG 检索、百度 ASR |
| **file** | 8086 | 文件上传/删除，对接腾讯云 COS |
| **common** | - | 公共模块：异常处理、统一响应、错误码 |
| **model** | - | 实体类、VO、DTO |
| **client** | - | Dubbo RPC 接口定义 |
| **post** | - | 帖子模块（预留） |

---

## 核心流程

```
上传简历 PDF → Apache Tika 解析 → AI 生成用户画像（SSE 流式）
                ↓
         点击"开始面试"
                ↓
   AI Agent 构建（Caffeine 缓存 + Redis 持久化记忆）
                ↓
          多轮面试对话 ← 百度实时语音识别（WebSocket）
       ┌────────┼────────┐
       ↓        ↓        ↓
   RagTool  MemoryTool  LogTool
  语义检索   长期记忆    面试日志
  (pgvector) (更新画像)  (记录事件)
```

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- PostgreSQL 16+（pgvector 扩展）
- Redis 7.0+
- Kafka 3.9+（KRaft 模式）
- Elasticsearch 8.x
- Nacos 2.x

### 启动中间件

```bash
# Kafka（KRaft 模式）
docker-compose up -d

# 其他中间件请自行部署（MySQL、PostgreSQL、Redis、ES、Nacos）
```

### 初始化数据库

```sql
-- MySQL
CREATE DATABASE IF NOT EXISTS mianshi;

-- PostgreSQL + pgvector
CREATE DATABASE "ai-mianshi";
CREATE EXTENSION IF NOT EXISTS vector;
```

### 启动服务

按以下顺序启动各模块：

```bash
# 1. 先启动 Nacos
# 2. 启动基础服务
cd user && mvn spring-boot:run        # :8082
cd questions && mvn spring-boot:run   # :8083
cd questionBank && mvn spring-boot:run # :8084
cd file && mvn spring-boot:run        # :8086
cd agent && mvn spring-boot:run       # :8085
cd gateway && mvn spring-boot:run     # :8081
```

### API 文档

启动后访问：`http://localhost:8081/doc.html`

---

## 配置文件

各模块的配置文件位于 `{module}/src/main/resources/application.yaml`，主要配置项包括：

- 数据库连接（MySQL / PostgreSQL）
- Redis 连接
- Nacos 注册中心地址
- Kafka Broker 地址
- Elasticsearch 地址
- 通义千问 API Key
- 百度 ASR App ID / API Key / Secret Key
- 腾讯云 COS SecretId / SecretKey

---

## 项目结构

```
ai-mianshi/
├── gateway/          # API 网关
├── user/             # 用户服务
├── questions/        # 题目服务
├── questionBank/     # 题库服务
├── agent/            # AI 面试 Agent（核心）
├── file/             # 文件服务
├── common/           # 公共模块
├── model/            # 实体模型
├── client/           # Dubbo 接口
├── post/             # 帖子模块（预留）
├── scripts/          # 运维脚本
├── docker-compose.yml
└── pom.xml
```
