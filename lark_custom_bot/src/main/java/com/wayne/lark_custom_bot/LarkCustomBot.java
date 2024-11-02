package com.wayne.lark_custom_bot;

import okhttp3.*;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;




public class LarkCustomBot {
    private static final Logger logger = Logger.getLogger(LarkCustomBot.class.getName());
    private final String webhook;
    private final String secret;
    private final String botAppId;
    private final String botSecret;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType OCTET_STREAM = MediaType.get("application/octet-stream");

    public LarkCustomBot(String webhook, String secret, String botAppId, String botSecret) {
        this.webhook = webhook;
        this.secret = secret;
        this.botAppId = botAppId;
        this.botSecret = botSecret;
        this.client = new OkHttpClient();
    }

    public void sendText(String text, boolean mentionAll) {
        JSONObject content = new JSONObject();
        content.put("text", mentionAll ? text + " <at user_id=\"all\">所有人</at>" : text);

        JSONObject data = new JSONObject();
        data.put("msg_type", "text");
        data.put("content", content);

        sendRequest(data);
    }

    public void sendPost(List<List<JSONObject>> content, String title) {
        JSONObject postContent = new JSONObject();
        postContent.put("title", title);
        postContent.put("content", new JSONArray(content));

        JSONObject zhCn = new JSONObject();
        zhCn.put("zh_cn", postContent);

        JSONObject post = new JSONObject();
        post.put("post", zhCn);

        JSONObject data = new JSONObject();
        data.put("msg_type", "post");
        data.put("content", post);

        sendRequest(data);
    }

    public void sendShareChat(String shareChatId) {
        JSONObject content = new JSONObject();
        content.put("share_chat_id", shareChatId);

        JSONObject data = new JSONObject();
        data.put("msg_type", "share_chat");
        data.put("content", content);

        sendRequest(data);
    }

    public void sendImage(String imageKey) {
        JSONObject content = new JSONObject();
        content.put("image_key", imageKey);

        JSONObject data = new JSONObject();
        data.put("msg_type", "image");
        data.put("content", content);

        sendRequest(data);
    }

    public void sendInteractive(JSONObject card) {
        JSONObject data = new JSONObject();
        data.put("msg_type", "interactive");
        data.put("card", card);

        sendRequest(data);
    }

    public String uploadImage(String filePath) {
        if (!new File(filePath).exists()) {
            logger.warning("Image file does not exist");
            return "";
        }

        if (botAppId.isEmpty() || botSecret.isEmpty()) {
            logger.warning("Bot app ID or secret is missing");
            return "";
        }

        try {
            String tenantAccessToken = getTenantAccessToken();
            String url = "https://open.feishu.cn/open-apis/im/v1/images";

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image_type", "message")
                    .addFormDataPart("image", new File(filePath).getName(),
                            RequestBody.create(new File(filePath), OCTET_STREAM))
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + tenantAccessToken)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);

                if (jsonResponse.has("data") && jsonResponse.getJSONObject("data").has("image_key")) {
                    return jsonResponse.getJSONObject("data").getString("image_key");
                } else {
                    logger.warning("Failed to upload image: " + responseBody);
                    return "";
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to upload image: " + e.getMessage());
            return "";
        }
    }

    private String getTenantAccessToken() throws Exception {
        String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
        JSONObject payload = new JSONObject();
        payload.put("app_id", botAppId);
        payload.put("app_secret", botSecret);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (jsonResponse.has("tenant_access_token")) {
                return jsonResponse.getString("tenant_access_token");
            } else {
                throw new Exception("Failed to get tenant access token: " + responseBody);
            }
        }
    }

    private void sendRequest(JSONObject data) {
        new Thread(() -> {
            try {
                logger.info("1");
                if (!secret.isEmpty()) {
                    long timestamp = System.currentTimeMillis() / 1000;
                    data.put("timestamp", timestamp);
                }
                logger.info("12");

                RequestBody body = RequestBody.create(data.toString(), JSON);
                Request request = new Request.Builder()
                        .url(webhook)
                        .post(body)
                        .build();
                logger.info("13");

                try (Response response = client.newCall(request).execute()) {
                    logger.info("14");
                    String responseBody = response.body().string();
                    logger.info("Response: " + responseBody);

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.has("code")) {
                        logger.warning("Message sending failed: " + responseBody);
                    } else if (jsonResponse.optInt("StatusCode") == 0) {
                        logger.info("Message sent successfully");
                    }
                }
            } catch (Exception e) {
                logger.severe("Failed to send message: " + e.getMessage());
            }
        }).start();
    }

    private String generateSignature(long timestamp, String secret) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    public static JSONObject createTextContent(String text, boolean unescape) {
        JSONObject content = new JSONObject();
        content.put("tag", "text");
        content.put("text", text);
        content.put("un_escape", unescape);
        return content;
    }

    public static JSONObject createLinkContent(String href, String text) {
        JSONObject content = new JSONObject();
        content.put("tag", "a");
        content.put("href", href);
        content.put("text", text);
        return content;
    }

    public static JSONObject createAtContent(String userId, String userName) {
        JSONObject content = new JSONObject();
        content.put("tag", "at");
        content.put("user_id", userId);
        content.put("user_name", userName);
        return content;
    }

    public static JSONObject createImageContent(String imageKey, Integer width, Integer height) {
        JSONObject content = new JSONObject();
        content.put("tag", "img");
        content.put("image_key", imageKey);
        if (width != null) content.put("width", width);
        if (height != null) content.put("height", height);
        return content;
    }
}
