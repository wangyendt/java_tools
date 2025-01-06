package com.wayne.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import okhttp3.*;
import okio.BufferedSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OpenAI {
    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public OpenAI(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Data
    @Builder
    public static class ChatMessage {
        private String role;
        private String content;
    }

    @Data
    @Builder
    public static class ChatCompletionResponse {
        private String id;
        private List<ChatMessage> messages;
        private String model;
        private double totalTokens;
    }

    public ChatCompletionResponse createChatCompletion(List<ChatMessage> messages, String model) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        
        ArrayNode messagesArray = requestBody.putArray("messages");
        for (ChatMessage message : messages) {
            ObjectNode messageNode = messagesArray.addObject();
            messageNode.put("role", message.getRole());
            messageNode.put("content", message.getContent());
        }

        String jsonBody = requestBody.toString();
        System.out.println("请求体: " + jsonBody);

        Request request = new Request.Builder()
            .url(baseUrl + "/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println("响应状态码: " + response.code());
            System.out.println("响应头: " + response.headers());
            System.out.println("响应体: " + responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message() + "\n响应体: " + responseBody);
            }

            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode choices = jsonResponse.get("choices");
            
            List<ChatMessage> responseMessages = new ArrayList<>();
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                responseMessages.add(ChatMessage.builder()
                    .role(message.get("role").asText())
                    .content(message.get("content").asText())
                    .build());
            }

            return ChatCompletionResponse.builder()
                .id(jsonResponse.get("id").asText())
                .messages(responseMessages)
                .model(jsonResponse.get("model").asText())
                .totalTokens(jsonResponse.get("usage").get("total_tokens").asDouble())
                .build();
        }
    }

    public void createStreamingChatCompletion(List<ChatMessage> messages, String model, Consumer<String> onMessage) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("stream", true);
        
        ArrayNode messagesArray = requestBody.putArray("messages");
        for (ChatMessage message : messages) {
            ObjectNode messageNode = messagesArray.addObject();
            messageNode.put("role", message.getRole());
            messageNode.put("content", message.getContent());
        }

        String jsonBody = requestBody.toString();
        System.out.println("请求体: " + jsonBody);

        Request request = new Request.Builder()
            .url(baseUrl + "/chat/completions")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应体为空");
            }

            try (SSEReader reader = new SSEReader(responseBody.source())) {
                String event;
                while ((event = reader.readEvent()) != null) {
                    if (event.equals("[DONE]")) {
                        break;
                    }
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(event);
                        JsonNode choices = jsonResponse.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                onMessage.accept(content);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("解析事件失败: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static class SSEReader implements AutoCloseable {
        private final BufferedSource source;

        public SSEReader(BufferedSource source) {
            this.source = source;
        }

        public String readEvent() throws IOException {
            String line;
            StringBuilder data = new StringBuilder();
            while ((line = source.readUtf8Line()) != null) {
                if (line.isEmpty()) {
                    return data.length() > 0 ? data.toString() : null;
                }
                if (line.startsWith("data: ")) {
                    data.append(line.substring(6));
                }
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }
} 