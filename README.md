# ForgeAutoShutdown（1.19.2 Forge, Java 17）

按计划或投票自动关闭 Minecraft 服务器，并提供可选看门狗功能。

## 原项目与来源

本仓库为以下项目的移植/分支：

- 原始项目：https://github.com/abused/ForgeAutoShutdown
- 1.12.2 基底项目：https://gitlab.com/targren/forgeautoshutdown

## 运行要求

- Minecraft Forge 1.19.2（建议 Forge 43.2.x）
- Java 17
- 服务器必装；客户端如需本地化提示请同时安装（否则会显示语言键）

## 功能

- 定时关闭（按时间或按运行时长）
- 可选的关闭前警告与“空服再关”延迟
- 玩家投票关服（`/shutdown`）
- 看门狗检测卡死或低 TPS（谨慎开启）

## 命令

- `/shutdown` 发起投票（开启投票时）
- `/shutdown yes` 或 `/shutdown no` 进行投票

## 配置

服务端配置文件：

- `world/serverconfig/forgeautoshutdown-server.toml`

分类：Schedule / Voting / Watchdog / Messages。

## 构建

使用 Java 17：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew build
```

产物目录：`build/libs/`

## 许可证

MIT
