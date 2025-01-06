package com.wayne.openai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String API_KEY = "sk-4556e299ea3b4401b87adcbda3cebb19";
    private static final String BASE_URL = "https://api.deepseek.com/v1";

    public static void main(String[] args) {
        try {
            OpenAI openAI = new OpenAI(BASE_URL, API_KEY);

            // 创建聊天消息列表
            List<OpenAI.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统消息
            messages.add(OpenAI.ChatMessage.builder()
                .role("system")
                .content("你是一个有帮助的AI助手。")
                .build());
            
            // 添加用户消息
            messages.add(OpenAI.ChatMessage.builder()
                .role("user")
                .content("你好！请给我讲一个简短的笑话。")
                .build());

            // 1. 测试非流式回复
            System.out.println("\n=== 测试非流式回复 ===");
            System.out.println("发送请求到API...");
            OpenAI.ChatCompletionResponse response = openAI.createChatCompletion(
                messages,
                "deepseek-chat"
            );

            System.out.println("\n收到响应:");
            System.out.println("ID: " + response.getId());
            System.out.println("模型: " + response.getModel());
            System.out.println("总Token数: " + response.getTotalTokens());
            System.out.println("\nAI回复:");
            for (OpenAI.ChatMessage message : response.getMessages()) {
                System.out.println(message.getContent());
            }

            // 2. 测试流式回复
            System.out.println("\n=== 测试流式回复 ===");
            System.out.println("发送请求到API...");
            System.out.println("\nAI回复:");
            StringBuilder streamContent = new StringBuilder();
            openAI.createStreamingChatCompletion(
                messages,
                "deepseek-chat",
                content -> {
                    System.out.print(content);
                    streamContent.append(content);
                }
            );
            System.out.println("\n流式回复完成！");

        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 