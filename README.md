# java-version-manager-win

一个适用于 Windows 操作系统的 Java 版本管理器应用程序，可以帮助您轻松管理和切换不同的 JDK 版本。

## 快速开始

### 环境要求

- JDK 11 或更高版本
- Apache Maven 3.6 或更高版本
- Windows 操作系统

### 构建项目

```bash
# 完整构建（推荐）
mvn clean package

# 快速构建（跳过测试）
mvn clean package -DskipTests
```

### 运行应用程序

```bash
# 以管理员身份运行
"start-jdk-manager.bat"
```

## 主要功能

- 自动扫描系统中已安装的 JDK 版本
- 显示每个 JDK 的详细信息（版本、路径、安装时间等）
- 支持设置环境变量（JAVA_HOME）
- 直观的图形用户界面

## 项目结构

```
javabb/
├── pom.xml                    # Maven 配置文件
├── src/                       # 源代码目录
├── target/                    # 构建输出目录
├── docs/                      # 文档目录
└── "start-jdk-manager.bat"        # 启动脚本
```

## 常见问题

### 模块冲突问题

如果遇到 "Two versions of module javafx.base found" 错误，运行修复脚本：

```bash
fix-modules-win.bat
```

### JavaFX 模块缺失

如果 `modules-win` 目录为空，重新构建项目：

```bash
mvn clean package
```

## 开发

项目使用标准 Maven 结构，主要类：

- `com.jdkmanager.JdkManagerApp` - 主应用程序类
- `com.jdkmanager.JarLauncher` - JAR 启动器
- `com.jdkmanager.ui.MainController` - 主界面控制器
- `com.jdkmanager.scanner.JdkScanner` - JDK 扫描器

## 许可证

本项目采用 MIT 许可证。
