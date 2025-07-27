# 轻屿记 (LiteIsle)

<div align="center">

<img src="front/public/logopic.png" alt="LiteIsle Logo" width="120" height="120">

**轻屿记是一款桌面端个人工作台，集沉浸式音乐、云同步笔记、专注模块、轻云盘于一体，以极简纯净的设计，打造专注高效的个人数字空间。**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4.15-4FC08D.svg)](https://vuejs.org/)
[![Electron](https://img.shields.io/badge/Electron-28.2.0-47848F.svg)](https://electronjs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

## 📖 项目简介

轻屿记是一个现代化的个人数字工作台，旨在为用户提供一个专注、高效的工作环境。通过整合多种实用功能，帮助用户在数字化时代保持专注力，提升工作效率。

### 🎯 核心理念
- **极简设计** - 去除冗余，专注本质
- **沉浸体验** - 营造专注的工作氛围
- **云端同步** - 随时随地访问您的数据
- **游戏化激励** - 通过岛屿收集系统激发持续专注

### 🎥 界面预览
![界面预览](https://pub-061d1fd03ea74e68849f186c401fde40.r2.dev/2025/07/a4eadb46744debc4fd761a6ae5585e0a.png)

## ✨ 功能特性

### 🎵 沉浸式音乐
- 高品质音乐播放
- 专注背景音乐库
- 自定义播放列表
- 音频格式支持广泛

### 📝 云同步笔记
- Markdown 编辑器
- 实时云端同步
- 文档版本管理
- 富文本编辑支持

### ⏰ 专注模块
- 番茄钟计时器
- 专注时长统计
- 岛屿收集系统
- 专注历史记录

### ☁️ 轻云盘
- 文件上传下载
- 文件夹管理
- 文件分享功能
- 回收站机制

### 🤖 AI 助手
- 基于阿里云通义千问
- 智能文档翻译
- 内容生成辅助
- 上下文理解

## 🛠️ 技术栈

### 后端技术
- **框架**: Spring Boot 3.5.3
- **语言**: Java 21 (Virtual Threads)
- **数据库**: MySQL 8.0+
- **缓存**: Redis + Redisson
- **对象存储**: MinIO
- **认证**: JWT 自定义认证
- **文档**: SpringDoc OpenAPI 3
- **ORM**: MyBatis Plus
- **AI集成**: Spring AI Alibaba (通义千问)

### 前端技术
- **框架**: Vue 3.4.15 + TypeScript
- **构建工具**: Vite 5.0.12
- **桌面端**: Electron 28.2.0
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **UI组件**: Tailwind CSS + Lucide Icons
- **编辑器**: Vditor (Markdown)

### 开发工具
- **构建**: Maven 3.9+
- **代码质量**: Lombok + AspectJ
- **API文档**: Swagger UI
- **实时通信**: WebSocket

## 🚀 快速开始

### 环境要求

- **Java**: 21+
- **Node.js**: 16+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **MinIO**: 最新版本

### 1. 克隆项目

```bash
git clone https://github.com/your-org/liteisle.git
cd liteisle
```

### 2. 数据库初始化

```bash
# 创建数据库
mysql -u root -p < sql/liteisle_db.sql
```

### 3. 后端部署

#### 配置文件
复制配置文件模板并根据您的环境进行修改:

```bash
# 复制配置文件模板
cp src/main/resources/application.yaml.example src/main/resources/application-prod.yaml

# 编辑配置文件，修改以下关键配置项：
# - 数据库连接信息 (username, password)
# - Redis 连接信息 (host, port, password)
# - MinIO 对象存储配置 (endpoint, access-key, secret-key)
# - JWT 密钥 (secret)
# - 邮件服务配置 (username, password)
# - AI 服务 API Key (可选)
```

详细的配置说明请参考 `src/main/resources/application.yaml.example` 文件中的注释。

#### 构建和运行

```bash
# 构建项目
./mvnw clean package -DskipTests

# 运行应用
java -jar target/liteisle-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 4. 前端部署

详细的前端部署说明请参考 [front/README.md](front/README.md)

```bash
cd front

# 安装依赖
npm install

# 配置后端地址
# 编辑 .env.electron 文件
echo "VITE_API_BASE_URL=http://your-server-ip:8080" > .env.electron
echo "VITE_WS_BASE_URL=ws://your-server-ip:8080" >> .env.electron

# 构建前端
npm run build

# 启动桌面应用
npm run electron:dev
```

## 📁 项目结构

```
liteisle/
├── src/main/java/com/liteisle/          # 后端源码
│   ├── controller/                      # REST API 控制器
│   ├── service/                         # 业务逻辑层
│   ├── mapper/                          # 数据访问层
│   ├── config/                          # 配置类
│   ├── util/                           # 工具类
│   └── LiteisleApplication.java        # 启动类
├── front/                              # 前端源码
│   ├── src/                            # Vue 源码
│   ├── public/                         # 静态资源
│   ├── main.cjs                        # Electron 主进程
│   └── package.json                    # 前端依赖
├── sql/                                # 数据库脚本
├── pom.xml                             # Maven 配置
└── README.md                           # 项目文档
```

## 🔧 配置说明

### 后端配置

主要配置项位于 `application.yml`:

- **数据库连接**: MySQL 连接配置
- **Redis配置**: 缓存和会话存储
- **MinIO配置**: 对象存储服务
- **JWT配置**: 用户认证令牌
- **AI配置**: 通义千问 API 密钥

### 前端配置

主要配置项位于 `.env.*` 文件:

- `VITE_API_BASE_URL`: 后端 API 地址
- `VITE_WS_BASE_URL`: WebSocket 地址
- `VITE_UPLOAD_MAX_SIZE`: 最大上传文件大小
- `VITE_UPLOAD_ALLOWED_TYPES`: 允许的文件类型

## 🧪 测试

### 后端测试
```bash
./mvnw test
```

### 前端测试
```bash
cd front
npm run test
```

## 📚 API 文档

启动后端服务后，访问 Swagger UI 文档:
```
http://localhost:8080/swagger-ui.html
```

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

感谢所有为轻屿记项目做出贡献的开发者和用户！

---

<div align="center">
Made with ❤️ by Rabbittank
</div>
