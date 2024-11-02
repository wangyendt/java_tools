package com.wayne.lark_bot;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import okhttp3.*;
import org.json.JSONObject;


public class LarkBot {
	private static final MediaType JSON = MediaType.parse("application/json");
    private final OkHttpClient client;
    private final String botAppId;
    private final String botSecret;
    private String accessToken;  // 用于存储访问令牌
    private final Gson gson = new Gson();

    public LarkBot(String appId, String appSecret) {
        this.botAppId = appId;
        this.botSecret = appSecret;
        this.client = new OkHttpClient().newBuilder()
            .build();
        // // 初始化时获取访问令牌
        // refreshAccessToken();
    }


    /**
     * 获取租户访问令牌
     * @return 租户访问令牌
     */
    private String getTenantAccessToken() throws Exception {
        // 创建一个数组来存储结果
        final String[] result = new String[1];
        
        Thread thread = new Thread(() -> {
            try {
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
                        result[0] = jsonResponse.getString("tenant_access_token");
                    } else {
                        System.err.println("Failed to get tenant access token: " + responseBody);
                        result[0] = null;
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while getting tenant access token: " + e.getMessage());
                e.printStackTrace();
                result[0] = null;
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
        
        if (result[0] == null) {
            throw new Exception("Failed to get tenant access token");
        }
        
        return result[0];
    }


    /**
     * 获取用户信息
     * @param emails 邮箱列表
     * @param mobiles 手机号列表
     * @return 用户信息或null
     */
    public List<Map<String, Object>> getUserInfo(List<String> emails, List<String> mobiles) throws Exception {
		// 创建一个数组来存储结果
		final List<Map<String, Object>>[] result = new List[1];
		
		Thread thread = new Thread(() -> {
			try {
				accessToken = getTenantAccessToken();
				
				MediaType mediaType = MediaType.parse("application/json");
				
				// 构建请求体
				Map<String, Object> requestMap = new HashMap<>();
				requestMap.put("emails", emails);
				requestMap.put("mobiles", mobiles);
				requestMap.put("include_resigned", true);
				
				RequestBody body = RequestBody.create(mediaType, gson.toJson(requestMap));
				
				Request request = new Request.Builder()
					.url("https://open.feishu.cn/open-apis/contact/v3/users/batch_get_id?user_id_type=open_id")
					.post(body)
					.addHeader("Content-Type", "application/json")
					.addHeader("Authorization", "Bearer " + accessToken)
					.build();

				try (Response response = client.newCall(request).execute()) {
					String responseBody = response.body().string();
					Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
					
					// 检查响应状态
					if (response.isSuccessful()) {
						Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
						result[0] = data != null ? (List<Map<String, Object>>) data.get("user_list") : null;
					} else {
						System.err.println(String.format("Failed to get user info: code:%s, msg:%s",
							responseMap.get("code"), responseMap.get("msg")));
						result[0] = null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				result[0] = null;
			}
		});
		
		thread.start();
		thread.join(); // 等待线程执行完成
		
		return result[0];
    }

    /**
     * 获取聊天群组列表
     * @return 聊天群组信息列表
     */
    public List<Map<String, Object>> getGroupList() throws Exception {
		// 创建一个数组来存储结果
		final List<Map<String, Object>>[] result = new List[1];
		
		Thread thread = new Thread(() -> {
			try {
				// 获取访问令牌
				accessToken = getTenantAccessToken();
				
				// 创建请求
				Request request = new Request.Builder()
					.url("https://open.feishu.cn/open-apis/im/v1/chats?page_size=20&sort_type=ByCreateTimeAsc")
					.get()
					.addHeader("Authorization", "Bearer " + accessToken)
					.build();

				// 发送请求并处理响应
				try (Response response = client.newCall(request).execute()) {
					String responseBody = response.body().string();
					Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
					
					// 检查响应状态
					if (response.isSuccessful()) {
						Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
						result[0] = data != null ? (List<Map<String, Object>>) data.get("items") : List.of();
					} else {
						System.err.println(String.format("Failed to get group list: code:%s, msg:%s",
							responseMap.get("code"), responseMap.get("msg")));
						result[0] = List.of();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				result[0] = List.of();
			}
		});
		
		thread.start();
		thread.join(); // 等待线程执行完成
		
		return result[0];
    }

    /**
     * 根据群组名称获取聊天群组ID列表
     * @param groupName 群组名称
     * @return 匹配群组名称的聊天群组ID列表
     * @throws Exception 如果获取群组列表失败
     */
    public List<String> getGroupChatIdByName(String groupName) throws Exception {
        return getGroupList().stream()
                .filter(group -> groupName.equals(group.get("name")))
                .map(group -> (String) group.get("chat_id"))
                .collect(Collectors.toList());
    }

    /**
     * 获取特定群组聊天的成员列表
     * @param groupChatId 群组聊天ID
     * @return 群组成员信息列表
     * @throws Exception 如果获取成员列表失败
     */
    public List<Map<String, Object>> getMembersInGroupByGroupChatId(String groupChatId) throws Exception {
        // 创建一个数组来存储结果
        final List<Map<String, Object>>[] result = new List[1];
        
        Thread thread = new Thread(() -> {
            try {
                // 获取访问令牌
                accessToken = getTenantAccessToken();
                
                // 创建请求
                Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/chats/" + groupChatId + "/members")
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                    
                    // 检查响应状态
                    if (response.isSuccessful()) {
                        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                        result[0] = data != null ? (List<Map<String, Object>>) data.get("items") : List.of();
                    } else {
                        System.err.println(String.format("Failed to get chat members: code:%s, msg:%s",
                            responseMap.get("code"), responseMap.get("msg")));
                        result[0] = List.of();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                result[0] = List.of();
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
        
        return result[0];
    }

    /**
     * 根据群组聊天ID和成员名称获取成员的open_id列表
     * @param groupChatId 群组聊天ID
     * @param memberName 成员名称
     * @return 匹配成员名称的open_id列表
     * @throws Exception 如果获取成员列表失败
     */
    public List<String> getMemberOpenIdByName(String groupChatId, String memberName) throws Exception {
        List<Map<String, Object>> members = getMembersInGroupByGroupChatId(groupChatId);
        return members.stream()
                .filter(member -> memberName.equals(member.get("name")))
                .map(member -> (String) member.get("member_id"))
                .collect(Collectors.toList());
    }


    /**
     * 发送文本消息
     * @param receiveIdType 接收者ID类型 ('open_id' 或 'chat_id')
     * @param receiveId 接收者的ID
     * @param msgType 消息类型
     * @param content 消息内容
     * @return 消息发送结果
     */
    private String sendMessage(String receiveIdType, String receiveId, String msgType, String content) throws Exception {
        // 创建一个数组来存储结果
        final String[] result = new String[1];
        
        Thread thread = new Thread(() -> {
            try {
                // 获取访问令牌
                accessToken = getTenantAccessToken();
                
                // 构建消息内容
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("receive_id", receiveId);
                requestMap.put("msg_type", msgType);
                requestMap.put("content", content);
                requestMap.put("uuid", UUID.randomUUID().toString());
                
                String jsonBody = gson.toJson(requestMap);
                
                // 创建请求
                Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=" + receiveIdType)
                    .post(RequestBody.create(jsonBody, JSON))
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                    
                    // 检查响应状态
                    if (response.isSuccessful()) {
                        result[0] = gson.toJson(responseMap.get("data"));
                    } else {
                        System.err.println(String.format("Failed to send message: code:%s, msg:%s",
                            responseMap.get("code"), responseMap.get("msg")));
                        result[0] = "{}";
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                result[0] = "{}";
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
        
        return result[0];
    }

    /**
     * 发送文本消息给特定用户
     * @param userOpenId 用户的open_id
     * @param text 文本消息
     * @return 消息发送结果
     */
    public String sendTextToUser(String userOpenId, String text) throws Exception {
        Map<String, String> contentMap = Map.of("text", text);
        String content = gson.toJson(contentMap); // 使用Gson将Map转换为JSON字符串
        return sendMessage("open_id", userOpenId, "text", content);
    }

    /**
     * 发送文本消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param text 文本消息
     * @return 消息发送结果
     */
    public String sendTextToChat(String chatId, String text) throws Exception {
        Map<String, String> contentMap = Map.of("text", text);
        String content = gson.toJson(contentMap); // 使用Gson将Map转换为JSON字符串
        return sendMessage("chat_id", chatId, "text", content);
    }

    /**
     * 发送图片消息给特定用户
     * @param userOpenId 用户的open_id
     * @param imageKey 图片的key
     * @return 消息发送结果
     */
    public String sendImageToUser(String userOpenId, String imageKey) throws Exception {
        String content = String.format("{\"image_key\":\"%s\"}", imageKey);
        return sendMessage("open_id", userOpenId, "image", content);
    }

    /**
     * 发送图片消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param imageKey 图片的key
     * @return 消息发送结果
     */
    public String sendImageToChat(String chatId, String imageKey) throws Exception {
        String content = String.format("{\"image_key\":\"%s\"}", imageKey);
        return sendMessage("chat_id", chatId, "image", content);
    }

    /**
     * 发送交互消息给特定用户
     * @param userOpenId 用户的open_id
     * @param interactive 交互消息内容
     * @return 消息发送结果
     */
    public String sendInteractiveToUser(String userOpenId, Map<String, Object> interactive) throws Exception {
        String content = gson.toJson(interactive);
        return sendMessage("open_id", userOpenId, "interactive", content);
    }

    /**
     * 发送交互消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param interactive 交互消息内容
     * @return 消息发送结果
     */
    public String sendInteractiveToChat(String chatId, Map<String, Object> interactive) throws Exception {
        String content = gson.toJson(interactive);
        return sendMessage("chat_id", chatId, "interactive", content);
    }

    /**
     * 发送共享聊天消息给特定用户
     * @param userOpenId 用户的open_id
     * @param sharedChatId 共享聊天ID
     * @return 消息发送结果
     */
    public String sendSharedChatToUser(String userOpenId, String sharedChatId) throws Exception {
        String content = String.format("{\"chat_id\":\"%s\"}", sharedChatId);
        return sendMessage("open_id", userOpenId, "share_chat", content);
    }

    /**
     * 发送共享聊天消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param sharedChatId 共享聊天ID
     * @return 消息发送结果
     */
    public String sendSharedChatToChat(String chatId, String sharedChatId) throws Exception {
        String content = String.format("{\"chat_id\":\"%s\"}", sharedChatId);
        return sendMessage("chat_id", chatId, "share_chat", content);
    }

    /**
     * 发送共享用户消息给特定用户
     * @param userOpenId 用户的open_id
     * @param sharedUserId 共享用户的ID
     * @return 消息发送结果
     */
    public String sendSharedUserToUser(String userOpenId, String sharedUserId) throws Exception {
        String content = String.format("{\"user_id\":\"%s\"}", sharedUserId);
        return sendMessage("open_id", userOpenId, "share_user", content);
    }

    /**
     * 发送共享用户消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param sharedUserId 共享用户的ID
     * @return 消息发送结果
     */
    public String sendSharedUserToChat(String chatId, String sharedUserId) throws Exception {
        String content = String.format("{\"user_id\":\"%s\"}", sharedUserId);
        return sendMessage("chat_id", chatId, "share_user", content);
    }

    /**
     * 发送音频消息给特定用户
     * @param userOpenId 用户的open_id
     * @param fileKey 音频的key
     * @return 消息发送结果
     */
    public String sendAudioToUser(String userOpenId, String fileKey) throws Exception {
        String content = String.format("{\"file_key\":\"%s\"}", fileKey);
        return sendMessage("open_id", userOpenId, "audio", content);
    }

    /**
     * 发送音频消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param fileKey 音频的key
     * @return 消息发送结果
     */
    public String sendAudioToChat(String chatId, String fileKey) throws Exception {
        String content = String.format("{\"file_key\":\"%s\"}", fileKey);
        return sendMessage("chat_id", chatId, "audio", content);
    }

    /**
     * 发送媒体消息给特定用户
     * @param userOpenId 用户的open_id
     * @param fileKey 媒体的key
     * @return 消息发送结果
     */
    public String sendMediaToUser(String userOpenId, String fileKey) throws Exception {
        String content = String.format("{\"file_key\":\"%s\"}", fileKey);
        return sendMessage("open_id", userOpenId, "media", content);
    }

    /**
     * 发送媒体消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param fileKey 媒体的key
     * @return 消息发送结果
     */
    public String sendMediaToChat(String chatId, String fileKey) throws Exception {
        String content = String.format("{\"file_key\":\"%s\"}", fileKey);
        return sendMessage("chat_id", chatId, "media", content);
    }

    /**
     * 发送文件消息给特定用户
     * @param userOpenId 用户的open_id
     * @param fileKey 文件的key
     * @return 消息发送结果
     */
    public String sendFileToUser(String userOpenId, String fileKey) throws Exception {
        String content = String.format("{\"file_key\":\"%s\"}", fileKey);
        return sendMessage("open_id", userOpenId, "file", content);
    }

    /**
     * 发送文件消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param fileKey 文件的key
     * @return 消息发送结果
     */
    public String sendFileToChat(String chatId, String fileKey) throws Exception {
        String content = String.format("{\"file_key\":\"%s\"}", fileKey);
        return sendMessage("chat_id", chatId, "file", content);
    }

    /**
     * 发送系统消息给特定用户
     * @param userOpenId 用户的open_id
     * @param systemMsgText 系统消息内容
     * @return 消息发送结果
     */
    public String sendSystemMsgToUser(String userOpenId, String systemMsgText) throws Exception {
        Map<String, Object> systemMessage = Map.of(
            "type", "divider",
            "params", Map.of(
                "divider_text", Map.of(
                    "text", systemMsgText,
                    "i18n_text", Map.of("zh_CN", systemMsgText)
                )
            ),
            "options", Map.of("need_rollup", true)
        );
        String content = gson.toJson(systemMessage);
        return sendMessage("open_id", userOpenId, "system", content);
    }

    /**
     * 发送帖子消息给特定用户
     * @param userOpenId 用户的open_id
     * @param postContent 帖子消息内容
     * @return 消息发送结果
     */
    public String sendPostToUser(String userOpenId, Object postContent) throws Exception {
        String content = gson.toJson(postContent);
        return sendMessage("open_id", userOpenId, "post", content);
    }

    /**
     * 发送帖子消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param postContent 帖子消息内容
     * @return 消息发送结果
     */
    public String sendPostToChat(String chatId, Map<String, Object> postContent) throws Exception {
        String content = gson.toJson(postContent);
        return sendMessage("chat_id", chatId, "post", content);
    }

    /**
     * 上传图片到飞书
     * @param imagePath 本地图片文件路径
     * @return 上传图片的key，如果上传失败则返回空字符串
     */
    public String uploadImage(String imagePath) throws Exception {
        // 创建一个数组来存储结果
        final String[] result = new String[1];
        
        Thread thread = new Thread(() -> {
            try {
                // 获取访问令牌
                accessToken = getTenantAccessToken();
                
                File file = new File(imagePath);
                // 构建multipart请求体
                RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image_type", "message")
                    .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                    .build();

                // 创建请求
                Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/images")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                    
                    // 检查响应状态
                    if (response.isSuccessful()) {
                        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                        result[0] = data != null ? (String) data.get("image_key") : "";
                    } else {
                        System.err.println(String.format("Failed to upload image: code:%s, msg:%s",
                            responseMap.get("code"), responseMap.get("msg")));
                        result[0] = "";
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while uploading image: " + e.getMessage());
                e.printStackTrace();
                result[0] = "";
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
        
        return result[0];
    }

    /**
     * 下载图片并保存到本地
     * @param imageKey 图片的key
     * @param imageSavePath 本地保存路
     */
    public void downloadImage(String imageKey, String imageSavePath) throws Exception {
        Thread thread = new Thread(() -> {
            try {
                // 获取访问令牌
                accessToken = getTenantAccessToken();
                
                // 创建请求
                Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/images/" + imageKey)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("Failed to download image: " + response.code());
                        return;
                    }

                    // 保存图片到文件
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        File file = new File(imageSavePath);
                        try (FileOutputStream fos = new FileOutputStream(file);
                             InputStream is = responseBody.byteStream()) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while downloading image: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
    }

    /**
     * 上传文件到飞书
     * @param filePath 本地文件路径
     * @param fileType 文件类型
     * @return 上传文件的key，如果上传失败则返回空字符串
     */
    public String uploadFile(String filePath, String fileType) throws Exception {
        // 创建一个数组来存储结果
        final String[] result = new String[1];
        
        Thread thread = new Thread(() -> {
            try {
                // 获取访问令牌
                accessToken = getTenantAccessToken();
                
                File file = new File(filePath);
                // 构建multipart请求体
                RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file_type", fileType)
                    .addFormDataPart("file_name", file.getName())
                    .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                    .build();

                // 创建请求
                Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/files")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
                    
                    // 检查响应状态
                    if (response.isSuccessful()) {
                        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                        result[0] = data != null ? (String) data.get("file_key") : "";
                    } else {
                        System.err.println(String.format("Failed to upload file: code:%s, msg:%s",
                            responseMap.get("code"), responseMap.get("msg")));
                        result[0] = "";
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while uploading file: " + e.getMessage());
                e.printStackTrace();
                result[0] = "";
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
        
        return result[0];
    }

    /**
     * 下载文件并保存到本地
     * @param fileKey 文件的key
     * @param fileSavePath 本地保存路径
     */
    public void downloadFile(String fileKey, String fileSavePath) throws Exception {
        Thread thread = new Thread(() -> {
            try {
                // 获取访问令牌
                accessToken = getTenantAccessToken();
                
                // 创建请求
                Request request = new Request.Builder()
                    .url("https://open.feishu.cn/open-apis/im/v1/files/" + fileKey)
                    .get()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

                // 发送请求并处理响应
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.err.println("Failed to download file: " + response.code());
                        return;
                    }

                    // 保存文件到本地
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        File file = new File(fileSavePath);
                        try (FileOutputStream fos = new FileOutputStream(file);
                             InputStream is = responseBody.byteStream()) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception occurred while downloading file: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        thread.start();
        thread.join(); // 等待线程执行完成
    }
}
