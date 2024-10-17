# java_tools

## Lark Bot API

这是一个用于与飞书机器人API交互的Java库。

### 使用方法

1. 在你的pom.xml中添加以下依赖:

```xml
<dependency>
    <groupId>com.wayne</groupId>
    <artifactId>lark-bot-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

2. 在你的代码中使用LarkBotApi:

```java
import com.wayne.larkbot.LarkBotApi;
public class YourClass {
  public static void main(String[] args) {
    String webhook = "your_webhook_url";
    String secret = "your_secret";
    String appId = "your_app_id";
    String appSecret = "your_app_secret";
    LarkBotApi bot = new LarkBotApi(webhook, secret, appId, appSecret);
    // 发送文本消息
    bot.sendText("Hello, World!", false);
    // 上传并发送图片
    String imageKey = bot.uploadImage("path/to/your/image.png");
    if (!imageKey.isEmpty()) {
    	bot.sendImage(imageKey);
    }
  }
}
```


### 功能

- 发送文本消息
- 发送富文本消息
- 发送图片
- 发送交互卡片
- 上传图片

详细的API文档请参考代码注释。
