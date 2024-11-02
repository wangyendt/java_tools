package com.wayne.lark_custom_bot;

public class Main {
    public static void main(String[] args) {
        // 替换为您的实际配置
        String webhook = "https://open.feishu.cn/open-apis/bot/v2/hook/4f6c295c-b3d0-4d76-9757-8b423658ae41";
        String secret = "";
        String botAppId = "cli_a785d99779791013";
        String botSecret = "bt1JJe4iOy3L7ifsSZsOddDm5xV4xjAT";

        LarkCustomBot api = new LarkCustomBot(webhook, secret, botAppId, botSecret);
        
        // 发送文本消息
        api.sendText("这是一条测试消息", false);
        
        // 发送带有@所有人的消息
        // api.sendText("重要通知", true);
        
        // 上传并发送图片
        // String imageKey = api.uploadImage("path/to/your/image.jpg");
        // if (!imageKey.isEmpty()) {
        //     api.sendImage(imageKey);
        // }
    }
} 