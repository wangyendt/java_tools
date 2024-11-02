package com.wayne.larkbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {
    private static final String APP_ID = "cli_a785d99779791013";
    private static final String APP_SECRET = "bt1JJe4iOy3L7ifsSZsOddDm5xV4xjAT";
    // private static final String TEST_OPEN_ID = "ou_2d8624f6d14876755a3d2071385d02ab";
    
    public static void main(String[] args) {
        try {
            // 创建工具类实例
            LarkBot bot = new LarkBot(APP_ID, APP_SECRET);

            List<String> emails = List.of("zhangsan@z.com", "lisi@a.com");
            List<String> mobiles = List.of("13267080069", "13022222222");
    
            List<Map<String, Object>> userInfo = bot.getUserInfo(emails, mobiles);
            if (userInfo != null) {
                System.out.println("User Info: \n" + userInfo + "\n");
            } else {
                System.out.println("Failed to retrieve user info.");
            }
            String userOpenId = (String) userInfo.get(0).get("user_id");

            // 示例调用 getGroupList 方法
            List<Map<String, Object>> groupList = bot.getGroupList();
            if (!groupList.isEmpty()) {
                System.out.println("Group List: \n" + groupList + "\n");
            } else {
                System.out.println("Failed to retrieve group list.");
            }

            // 调用getGroupChatIdByName方法
            String groupName = "测试2";
            List<String> chatIds = bot.getGroupChatIdByName(groupName);
            System.out.println("Chat IDs for group '" + groupName + "': \n" + chatIds + "\n");


            // 调用getMembersInGroupByGroupChatId方法
            String groupChatId = "oc_92cb3c0e308204acbbbad5305590f9a6";
            List<Map<String, Object>> members = bot.getMembersInGroupByGroupChatId(groupChatId);
            System.out.println("Members in group chat ID '" + groupChatId + "': \n" + members + "\n");


            // 示例1：使用TextContent
            String atAll = TextContent.makeAtAllPattern();
            String boldText = TextContent.makeBoldPattern("重要通知");
            String link = TextContent.makeUrlPattern("https://example.com", "点击这里");
            String textContent = "这是一段text content: \n" + atAll + boldText + link;
            
            System.out.println("Text Content示例:");
            System.out.println(atAll);
            System.out.println(boldText);
            System.out.println(link);
            
            // 示例2：使用PostContent
            PostContent post = new PostContent("测试公告");
            
            // 添加一行带格式的文本
            Map<String, Object> poseContent = post.makeTextContent(
                "这是一个测试消息",
                Arrays.asList("bold", "italic"),
                false
            );
            post.addContentInLine(poseContent);
            
            // 添加新的一行，包含链接和@提醒
            List<Map<String, Object>> secondLine = new ArrayList<>();
            secondLine.add(post.makeLinkContent("点击查看详情", "https://example.com", null));
            secondLine.add(post.makeAtContent("all", null));
            post.addContentsInNewLine(secondLine);
            
            System.out.println("\nPost Content示例:");
            System.out.println(post.getContent());




            // 发送测试消息
            String result = bot.sendTextToUser(
                userOpenId,
                "test content"
            );
            // 发送测试消息
            result = bot.sendTextToUser(
                userOpenId,
                "test content 2"
            );
            System.out.println("消息发送结果：" + result);


            // 调用 uploadImage 方法
            String imagePath = "/Users/wayne/Downloads/IMU标定和姿态结算.drawio.png";
            String imageKey = bot.uploadImage(imagePath);
            if (!imageKey.isEmpty()) {
                System.out.println("Image uploaded successfully, image key: " + imageKey);
            } else {
                System.out.println("Failed to upload image.");
            }

            bot.sendImageToUser(userOpenId, imageKey);
            bot.downloadImage(imageKey, "/Users/wayne/Downloads/lark_bots/java3/test.png");

            String fileKey = bot.uploadFile("/Users/wayne/Downloads/xr2-platform.jks", "stream");
            bot.sendFileToChat(chatIds.get(0), fileKey);
            bot.downloadFile(fileKey, "/Users/wayne/Downloads/lark_bots/java3/test.jks");

            bot.sendPostToUser(userOpenId, post.getContent());
            bot.sendTextToUser(userOpenId, textContent);
            bot.sendTextToChat(chatIds.get(0), textContent);
            
        } catch (Exception e) {
            System.err.println("发送消息失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}