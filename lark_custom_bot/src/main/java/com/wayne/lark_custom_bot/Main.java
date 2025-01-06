package com.wayne.lark_custom_bot;

import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;

public class Main {
    public static void main(String[] args) {
        // 替换为您的实际配置
        String webhook = "xxx";
        String secret = "xxx";
        String botAppId = "xxx";
        String botSecret = "xxx";

        LarkCustomBot bot = new LarkCustomBot(webhook, secret, botAppId, botSecret);
        
        // 发送文本消息
        bot.sendText("这是一条测试消息", false);
        
        // 创建一个富文本消息
        List<List<JSONObject>> content = new ArrayList<>();
        List<JSONObject> paragraph1 = new ArrayList<>();
        
        // 添加文本内容
        paragraph1.add(LarkCustomBot.createTextContent("这是一段普通文本 ", false));
        
        // 添加@某人
        paragraph1.add(LarkCustomBot.createAtContent("all", "所有人"));
        
        // 添加链接
        paragraph1.add(LarkCustomBot.createTextContent(" 请查看", false));
        paragraph1.add(LarkCustomBot.createLinkContent("https://www.example.com", "这个链接"));
        
        // 创建第二段落
        List<JSONObject> paragraph2 = new ArrayList<>();
        
        // 添加图片（需要先上传图片获取image_key）
        // String imageKey = bot.uploadImage("path/to/your/image.jpg");
        // if (!imageKey.isEmpty()) {
        //     paragraph2.add(LarkCustomBot.createImageContent(imageKey, 300, 200));
        // }
        
        // 将所有段落添加到内容中
        content.add(paragraph1);
        content.add(paragraph2);
        
        // 发送消息
        bot.sendPost(content, "这是标题");
    }
} 