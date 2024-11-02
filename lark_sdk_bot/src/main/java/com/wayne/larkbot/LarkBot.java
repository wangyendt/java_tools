package com.wayne.larkbot;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.im.v1.model.*;
import com.lark.oapi.service.contact.v3.model.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Arrays;
import java.io.File;
import com.lark.oapi.core.request.RequestOptions;


public class LarkBot {
    private final Client client;
    private final Gson gson = new Gson(); // 初始化Gson对象

    public LarkBot(String appId, String appSecret) {
        this.client = Client.newBuilder(appId, appSecret).build();
    }

    /**
     * 获取用户信息
     * @param emails 邮箱列表
     * @param mobiles 手机号列表
     * @return 用户信息或null
     */
    public List<Map<String, Object>> getUserInfo(List<String> emails, List<String> mobiles) throws Exception {
        // 创建请求对象
        BatchGetIdUserReq req = BatchGetIdUserReq.newBuilder()
            .batchGetIdUserReqBody(BatchGetIdUserReqBody.newBuilder()
                .emails(emails.toArray(new String[0]))
                .mobiles(mobiles.toArray(new String[0]))
                .includeResigned(true)
                .build())
            .build();

        // 发起请求
        BatchGetIdUserResp resp = client.contact().user().batchGetId(req);

        // 处理服务端错误
        if (!resp.success()) {
            System.err.println(String.format("Failed to get user info: code:%s, msg:%s, reqId:%s",
                resp.getCode(), resp.getMsg(), resp.getRequestId()));
            return null;
        }

        // 返回业务数据
        Map<String, Object> responseData = Jsons.DEFAULT.fromJson(Jsons.DEFAULT.toJson(resp.getData()), Map.class);
        return (List<Map<String, Object>>) responseData.get("user_list");
    }

    /**
     * 获取聊天群组列表
     * @return 聊天群组信息列表
     */
    public List<Map<String, Object>> getGroupList() throws Exception {
        // 创建请求对象
        ListChatReq req = ListChatReq.newBuilder().build();

        // 发起请求
        ListChatResp resp = client.im().chat().list(req);

        // 处理服务端错误
        if (!resp.success()) {
			System.out.println(String.format("code:%s,msg:%s,reqId:%s, resp:%s",
				resp.getCode(), resp.getMsg(), resp.getRequestId(), Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
			return List.of();
        }

        // 返回业务数据
        return Jsons.DEFAULT.fromJson(Jsons.DEFAULT.toJson(resp.getData().getItems()), List.class);
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
        GetChatMembersReq req = GetChatMembersReq.newBuilder()
                .chatId(groupChatId)
                .build();
    
        GetChatMembersResp resp = client.im().chatMembers().get(req);
    
        if (!resp.success()) {
            System.err.println(String.format("Failed to get chat members: code:%s, msg:%s, reqId:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId()));
            return List.of();
        }
    
        // 解析响应数据
        String jsonData = Jsons.DEFAULT.toJson(resp.getData());
        Map<String, Object> responseData = Jsons.DEFAULT.fromJson(jsonData, Map.class);
    
        // 返回 'items' 列表
        return (List<Map<String, Object>>) responseData.get("items");
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
        // 创建请求对象
        CreateMessageReq req = CreateMessageReq.newBuilder()
            .receiveIdType(receiveIdType)
            .createMessageReqBody(CreateMessageReqBody.newBuilder()
                .receiveId(receiveId)
                .msgType(msgType)
                .content(content)
                .uuid(UUID.randomUUID().toString())
                .build())
            .build();

        // 发起请求
        CreateMessageResp resp = client.im().message().create(req);

        // 处理服务端错误
        if (!resp.success()) {
            System.err.println(String.format("Failed to send message: code:%s, msg:%s, reqId:%s",
                resp.getCode(), resp.getMsg(), resp.getRequestId()));
            return "{}";
        }

        // 返回成功结果
        return Jsons.DEFAULT.toJson(resp.getData());
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
        String content = Jsons.DEFAULT.toJson(interactive);
        return sendMessage("open_id", userOpenId, "interactive", content);
    }

    /**
     * 发送交互消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param interactive 交互消息内容
     * @return 消息发送结果
     */
    public String sendInteractiveToChat(String chatId, Map<String, Object> interactive) throws Exception {
        String content = Jsons.DEFAULT.toJson(interactive);
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
        String content = Jsons.DEFAULT.toJson(systemMessage);
        return sendMessage("open_id", userOpenId, "system", content);
    }

    /**
     * 发送帖子消息给特定用户
     * @param userOpenId 用户的open_id
     * @param postContent 帖子消息内容
     * @return 消息发送结果
     */
    public String sendPostToUser(String userOpenId, Object postContent) throws Exception {
        String content = Jsons.DEFAULT.toJson(postContent);
        return sendMessage("open_id", userOpenId, "post", content);
    }

    /**
     * 发送帖子消息给特定聊天群组
     * @param chatId 群组的chat_id
     * @param postContent 帖子消息内容
     * @return 消息发送结果
     */
    public String sendPostToChat(String chatId, Map<String, Object> postContent) throws Exception {
        String content = Jsons.DEFAULT.toJson(postContent);
        return sendMessage("chat_id", chatId, "post", content);
    }

    /**
     * 上传图片到飞书
     * @param imagePath 本地图片文件路径
     * @return 上传图片的key，如果上传失败则返回空字符串
     */
    public String uploadImage(String imagePath) {
        try {
            File file = new File(imagePath);
            CreateImageReq req = CreateImageReq.newBuilder()
                .createImageReqBody(CreateImageReqBody.newBuilder()
                    .imageType("message")
                    .image(file)
                    .build())
                .build();

            CreateImageResp resp = client.im().image().create(req);

            if (!resp.success()) {
                System.err.println(String.format("Failed to upload image: code:%s, msg:%s, reqId:%s, resp:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId(), Jsons.createGSON(true, false).toJson(JsonParser.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8)))));
                return "";
            }

            // 返回成功结果
            Map<String, Object> responseData = Jsons.DEFAULT.fromJson(Jsons.DEFAULT.toJson(resp.getData()), Map.class);
            return (String) responseData.get("image_key");
        } catch (Exception e) {
            System.err.println("Exception occurred while uploading image: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 下载图片并保存到本地
     * @param imageKey 图片的key
     * @param imageSavePath 本地保存路径
     */
    public void downloadImage(String imageKey, String imageSavePath) {
        try {
            // 创建请求对象
            GetImageReq req = GetImageReq.newBuilder()
                .imageKey(imageKey)
                .build();

            // 发起请求
            GetImageResp resp = client.im().image().get(req);

            // 处理服务端错误
            if (!resp.success()) {
                System.err.println(String.format("Failed to download image: code:%s, msg:%s, reqId:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId()));
                return;
            }

            // 保存图片
            resp.writeFile(imageSavePath);
        } catch (Exception e) {
            System.err.println("Exception occurred while downloading image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 上传文件到飞书
     * @param filePath 本地文件路径
     * @param fileType 文件类型
     * @return 上传文件的key，如果上传失败则返回空字符串
     */
    public String uploadFile(String filePath, String fileType) {
        try {
            File file = new File(filePath);
            CreateFileReq req = CreateFileReq.newBuilder()
                .createFileReqBody(CreateFileReqBody.newBuilder()
                    .fileType(fileType)
                    .fileName(file.getName())
                    .file(file)
                    .build())
                .build();

            CreateFileResp resp = client.im().file().create(req);

            if (!resp.success()) {
                System.err.println(String.format("Failed to upload file: code:%s, msg:%s, reqId:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId()));
                return "";
            }

            return resp.getData().getFileKey();
        } catch (Exception e) {
            System.err.println("Exception occurred while uploading file: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 下载文件并保存到本地
     * @param fileKey 文件的key
     * @param fileSavePath 本地保存路径
     */
    public void downloadFile(String fileKey, String fileSavePath) {
        try {
            GetFileReq req = GetFileReq.newBuilder()
                .fileKey(fileKey)
                .build();

            GetFileResp resp = client.im().file().get(req);

            if (!resp.success()) {
                System.err.println(String.format("Failed to download file: code:%s, msg:%s, reqId:%s",
                    resp.getCode(), resp.getMsg(), resp.getRequestId()));
                return;
            }

            resp.writeFile(fileSavePath);
        } catch (Exception e) {
            System.err.println("Exception occurred while downloading file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
