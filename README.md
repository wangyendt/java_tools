# Java Tools

这是一个Java工具集合，包含多个独立的模块，主要用于飞书机器人API调用、阿里云OSS操作和OpenAI API调用等功能。

## 模块说明

- `lark_sdk_bot`: 基于飞书开放平台SDK的机器人实现
- `lark_bot`: 基于HTTP请求的飞书机器人实现
- `lark_custom_bot`: 飞书自定义机器人实现
- `aliyun_oss`: 阿里云OSS客户端工具
- `openai`: OpenAI API调用工具
- `cross_comm`: 跨语言、多设备通信库（Java客户端）
- `android_example`: Android示例工程

## 本地验证

### 1. 环境要求
- JDK 8+
- Maven 3.6+

### 2. 构建命令

```bash
# 在根目录执行，构建所有模块
mvn clean install

# 构建单个模块（在对应模块目录下执行）
cd [module_name]
mvn clean package
```

### 3. 运行命令

```bash
# 方式1：使用maven exec插件运行（在对应模块目录下执行）
cd [module_name]
mvn exec:java

# 方式2：直接运行jar包
cd [module_name]
java -jar target/[module_name]-[version].jar

# 示例：运行aliyun_oss模块
cd aliyun_oss
mvn exec:java
# 或
java -jar target/aliyun_oss-1.1.1.jar
```

### 4. 模块配置说明

#### lark_sdk_bot
需要在 `Main.java` 中配置：
- APP_ID
- APP_SECRET

#### lark_bot
需要在 `Main.java` 中配置：
- APP_ID
- APP_SECRET

#### lark_custom_bot
需要在 `Main.java` 中配置：
- WEBHOOK_URL

#### aliyun_oss
需要在 `Main.java` 中配置：
- ACCESS_KEY_ID
- ACCESS_KEY_SECRET
- ENDPOINT
- BUCKET_NAME

#### openai
需要在 `Main.java` 中配置：
- API_KEY

#### cross_comm
需要在 `Main.java` 中配置：
- SERVER_IP（默认：localhost）
- SERVER_PORT（默认：9898）
- OSS配置（用于文件传输功能）：
  - OSS_ENDPOINT
  - OSS_BUCKET_NAME
  - OSS_ACCESS_KEY_ID
  - OSS_ACCESS_KEY_SECRET

## 部署到GitHub Maven Package

### 1. 配置认证

在 `~/.m2/settings.xml` 中添加：

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>你的GitHub用户名</username>
      <password>你的GitHub Personal Access Token</password>
    </server>
  </servers>
</settings>
```

### 2. 版本号更新
部署新版本前，需要更新以下文件中的版本号：

- 根目录 `pom.xml`
- 各模块目录下的 `pom.xml`

### 3. 依赖范围设置
确保在各模块的 `pom.xml` 中将依赖的 scope 设置为 provided：

```xml
<dependency>
    <groupId>com.wayne</groupId>
    <artifactId>模块名</artifactId>
    <version>${version}</version>
    <scope>provided</scope>
</dependency>
```

### 4. 执行部署
```bash
mvn clean deploy
```

## 在Android项目中使用

### 1. 配置GitHub Package Registry

在项目根目录的 `build.gradle` 中添加：

```gradle
allprojects {
    repositories {
        maven {
            name = "GitHub Packages"
            url = uri("https://maven.pkg.github.com/wangyendt/java_tools")
            credentials {
                username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
            }
        }
    }
}
```

### 2. 添加依赖

在app模块的 `build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.wayne:lark-sdk-bot:最新版本号'
    implementation 'com.wayne:lark-bot:最新版本号'
    implementation 'com.wayne:lark-custom-bot:最新版本号'
    implementation 'com.wayne:aliyun-oss:最新版本号'
    implementation 'com.wayne:openai:最新版本号'
}
```

### 3. 使用示例

#### 飞书机器人示例
```java
// 使用SDK机器人
LarkBot bot = new LarkBot(APP_ID, APP_SECRET);
bot.sendTextToUser(openId, "测试消息");

// 使用自定义机器人
LarkCustomBot customBot = new LarkCustomBot(WEBHOOK_URL);
customBot.sendText("测试消息");
```

#### 阿里云OSS示例
```java
AliyunOSS oss = new AliyunOSS(ACCESS_KEY_ID, ACCESS_KEY_SECRET, ENDPOINT, BUCKET_NAME);
oss.uploadFile("本地文件路径", "OSS文件路径");
```

#### OpenAI API示例
```java
OpenAI openai = new OpenAI(API_KEY);
String response = openai.chatCompletion("你的提问");
```

### 4. 注意事项

1. 确保Android项目的 `minSdkVersion` 不低于21
2. 添加网络权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
3. 如果使用文件上传功能，需要添加存储权限：
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## CrossComm 跨语言通信库详细使用指南

### 概述

CrossComm是一个基于WebSocket的跨语言、多设备通信库的Java客户端实现。它提供了简洁的API，让Java应用能够与其他语言（如Python）编写的客户端进行实时通信。

### 特性

- 🚀 **简洁易用**: 类似Python装饰器的注解方式处理消息
- 🔗 **WebSocket通信**: 基于WebSocket协议，支持实时双向通信
- 📝 **多种消息类型**: 支持文本、JSON、字典、字节数据、文件、图片、文件夹
- 📁 **文件传输**: 基于阿里云OSS，支持文件、图片、文件夹的上传下载
- 📥 **自动下载**: 支持指定下载目录，自动下载接收的文件
- 🎯 **消息过滤**: 支持按消息类型和发送方过滤消息
- ❤️ **自动心跳**: 内置心跳机制，保持连接稳定
- 📋 **客户端管理**: 支持获取在线客户端列表
- 🎭 **双模式运行**: 支持监听模式（只接收）和发送模式（可发送+接收）
- 🌐 **跨平台兼容**: 与Python/Swift版本完全兼容，支持互相通信

### 快速运行

进入 `cross_comm` 目录，使用Maven命令运行：

```bash
cd cross_comm

# 运行监听模式（只接收消息）
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="listener"

# 运行发送模式（可发送消息）
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="sender"
```

### 运行模式说明

#### 监听模式 (Listener)
- **用途**: 专门用于接收和处理消息
- **特点**: 启动后自动连接服务器，持续监听所有类型的消息
- **适用场景**: 后台服务监控、消息日志记录、被动接收通知
- **操作**: 按 `Ctrl+C` 退出

#### 发送模式 (Sender)
- **用途**: 既可以发送消息，也可以接收消息
- **特点**: 启动后会发送测试消息，然后进入交互式命令行
- **适用场景**: 主动发送消息、调试和测试、交互式通信
- **交互命令**:
  - 直接输入文本：发送文本消息
  - `json:{"key":"value"}`：发送JSON消息
  - `bytes:content`：发送字节数据
  - `file:文件路径`：发送文件
  - `image:图片路径`：发送图片
  - `folder:文件夹路径`：发送文件夹
  - `list`：获取在线客户端列表
  - `quit`：退出程序

### 支持的消息类型

- `TEXT` - 文本消息
- `JSON` - JSON格式消息
- `DICT` - 字典/映射消息
- `BYTES` - 字节数据消息
- `FILE` - 文件消息（通过OSS传输）
- `IMAGE` - 图片消息（通过OSS传输）
- `FOLDER` - 文件夹消息（通过OSS传输）

### 程序化使用示例

#### 1. 创建客户端实例

```java
import com.wayne.cross_comm.CrossCommService;
import com.wayne.cross_comm.CommMsgType;
import com.wayne.cross_comm.MessageListener;
import com.wayne.cross_comm.Message;

// 连接到默认服务器
CrossCommService client = new CrossCommService("localhost", 9898);

// 或指定客户端ID和心跳间隔
CrossCommService client = new CrossCommService("localhost", 9898, "my_java_client", 30);
```

#### 2. 创建消息处理器

```java
public class MyMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MyMessageHandler.class);
    
    // 处理文本消息
    @MessageListener(msgType = {CommMsgType.TEXT})
    public void handleTextMessage(Message message) {
        logger.info("收到文本消息: {}", message.getContent());
        logger.info("来自: {}", message.getFromClientId());
    }
    
    // 处理JSON消息
    @MessageListener(msgType = {CommMsgType.JSON})
    public void handleJsonMessage(Message message) {
        logger.info("收到JSON消息: {}", message.getContent());
    }
    
    // 处理文件消息（自动下载到指定目录）
    @MessageListener(msgType = {CommMsgType.FILE}, downloadDirectory = "./downloads/files")
    public void handleFileMessage(Message message) {
        logger.info("收到文件: {}", message.getContent()); // 本地文件路径
        logger.info("OSS Key: {}", message.getOssKey());  // 原始OSS路径
    }
    
    // 处理特定客户端的消息
    @MessageListener(fromClientId = "python_client")
    public void handlePythonClientMessage(Message message) {
        logger.info("收到来自Python客户端的消息: {}", message.getContent());
    }
    
    // 处理所有类型的消息
    @MessageListener
    public void handleAllMessages(Message message) {
        logger.debug("收到消息: {} ({})", message.getContent(), message.getMsgType().getValue());
    }
}
```

#### 3. 连接和发送消息

```java
// 创建消息处理器并注册
MyMessageHandler handler = new MyMessageHandler();
client.registerMessageHandlers(handler);

// 连接到服务器
boolean connected = client.connect();
if (!connected) {
    logger.error("连接失败");
    return;
}

// 发送不同类型的消息
client.sendMessage("Hello World!", CommMsgType.TEXT);
client.sendMessage("{\"type\": \"notification\"}", CommMsgType.JSON);
client.sendMessage("private message", CommMsgType.TEXT, "target_client_id");

// 发送字节数据
byte[] data = "binary data".getBytes();
client.sendMessage(data, CommMsgType.BYTES);

// 发送字典数据
Map<String, Object> dict = new HashMap<>();
dict.put("action", "update");
dict.put("id", 123);
client.sendMessage(dict, CommMsgType.DICT);

// 发送文件（需要配置OSS）
client.sendFile("./test_files/document.pdf");
client.sendFile("./test_files/report.txt", "specific_client_id");

// 发送图片
client.sendImage("./images/photo.jpg");

// 发送文件夹
client.sendDirectory("./backup_files");

// 获取客户端列表
Map<String, Object> clients = client.listOnlineClients(5); // 5秒超时
if (clients != null) {
    System.out.println("在线客户端数量: " + clients.get("total_count"));
}

// 断开连接
client.disconnect();
```

### @MessageListener 注解说明

用于标注消息处理方法的注解，支持以下参数：

- `msgType`: 指定监听的消息类型数组，默认监听所有类型
- `fromClientId`: 指定监听特定发送方的消息，默认监听所有发送方
- `downloadDirectory`: 文件下载目录（仅对FILE、IMAGE、FOLDER类型有效），为空则不自动下载

#### 示例：

```java
// 只处理文本和JSON消息
@MessageListener(msgType = {CommMsgType.TEXT, CommMsgType.JSON})
public void handleTextAndJson(Message message) { ... }

// 只处理来自特定客户端的消息
@MessageListener(fromClientId = "python_client")
public void handlePythonMessages(Message message) { ... }

// 处理文件消息并自动下载
@MessageListener(msgType = {CommMsgType.FILE}, downloadDirectory = "./downloads")
public void handleFileWithDownload(Message message) { ... }
```

### 主要API方法

| 方法 | 描述 |
|------|------|
| `connect()` | 连接到服务器 |
| `disconnect()` | 断开连接 |
| `sendMessage(content, msgType)` | 发送消息给所有客户端 |
| `sendMessage(content, msgType, toClientId)` | 发送消息给指定客户端 |
| `sendFile(filePath)` | 发送文件给所有客户端 |
| `sendImage(imagePath)` | 发送图片给所有客户端 |
| `sendDirectory(dirPath)` | 发送文件夹给所有客户端 |
| `listClients(onlyOnline, timeout)` | 获取客户端列表 |
| `listOnlineClients(timeout)` | 获取在线客户端列表 |
| `registerMessageHandlers(handler)` | 注册消息处理器 |

### 测试场景示例

#### 场景1：同时运行监听和发送客户端
```bash
# 终端1：启动监听客户端
cd cross_comm
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="listener"

# 终端2：启动发送客户端
cd cross_comm
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="sender"
# 在发送客户端中输入消息，监听客户端会收到
```

#### 场景2：查看在线客户端
```bash
# 启动发送客户端
cd cross_comm
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="sender"
# 输入 "list" 查看当前在线的所有客户端
```

#### 场景3：文件传输测试（需要配置OSS）
```bash
# 启动监听客户端
cd cross_comm
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="listener"

# 启动发送客户端
cd cross_comm
mvn exec:java -Dexec.mainClass="com.wayne.cross_comm.Main" -Dexec.args="sender"
# 输入文件传输命令：
# file:test_files/test.txt    (发送文件)
# image:images/photo.jpg      (发送图片)
# folder:backup               (发送文件夹)
```

### 注意事项

1. **消息处理方法签名**: 使用`@MessageListener`注解的方法必须接受一个`Message`类型的参数
2. **连接管理**: 确保在应用关闭时调用`disconnect()`方法
3. **线程安全**: 消息处理器的方法会在单独的线程中调用，请注意线程安全
4. **错误处理**: 消息处理器中的异常会被捕获并记录，不会影响其他处理器
5. **心跳机制**: 客户端会自动发送心跳，无需手动管理
6. **服务器连接**: 确保服务器地址正确且服务器正在运行

## License

Apache License 2.0
