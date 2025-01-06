# Java Tools

这是一个Java工具集合，包含多个独立的模块，主要用于飞书机器人API调用、阿里云OSS操作和OpenAI API调用等功能。

## 模块说明

- `lark_sdk_bot`: 基于飞书开放平台SDK的机器人实现
- `lark_bot`: 基于HTTP请求的飞书机器人实现
- `lark_custom_bot`: 飞书自定义机器人实现
- `aliyun_oss`: 阿里云OSS客户端工具
- `openai`: OpenAI API调用工具
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

### 3. 模块配置说明

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

## License

Apache License 2.0
