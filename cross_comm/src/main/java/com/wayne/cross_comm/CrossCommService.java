package com.wayne.cross_comm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayne.aliyun_oss.AliyunOSS;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * 跨语言、多设备通信服务 - Java客户端实现
 * 
 * @author Wayne
 * @since 1.0.0
 */
public class CrossCommService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossCommService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String serverIp;
    private final int serverPort;
    private final String clientId;
    private final int heartbeatInterval; // 心跳间隔（秒）
    
    private WebSocketClient webSocketClient;
    private boolean isConnected = false;
    private ScheduledExecutorService heartbeatExecutor;
    private ScheduledFuture<?> heartbeatTask;
    
    // 存储注册的消息处理器
    private final List<MessageHandler> messageHandlers = new ArrayList<>();
    
    // 客户端列表缓存
    private volatile Map<String, Object> lastClientList = null;
    
    // OSS文件存储服务
    private AliyunOSS ossService;
    
    /**
     * 构造函数（带OSS配置）
     * 
     * @param serverIp 服务器IP地址
     * @param serverPort 服务器端口
     * @param clientId 客户端唯一ID，如果为null则自动生成
     * @param heartbeatInterval 心跳间隔（秒）
     * @param ossEndpoint OSS endpoint
     * @param ossBucketName OSS bucket名称
     * @param ossAccessKeyId OSS Access Key ID
     * @param ossAccessKeySecret OSS Access Key Secret
     */
    public CrossCommService(String serverIp, int serverPort, String clientId, int heartbeatInterval,
                           String ossEndpoint, String ossBucketName, String ossAccessKeyId, String ossAccessKeySecret) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.clientId = clientId != null ? clientId : generateClientId();
        this.heartbeatInterval = heartbeatInterval;
        
        // 初始化OSS服务（使用传入的配置）
        initOSSService(ossEndpoint, ossBucketName, ossAccessKeyId, ossAccessKeySecret);
        
        logger.info("CrossCommService initialized: clientId={}", this.clientId);
    }
    
    /**
     * 构造函数（从环境变量读取OSS配置）
     * 
     * @param serverIp 服务器IP地址
     * @param serverPort 服务器端口
     * @param clientId 客户端唯一ID，如果为null则自动生成
     * @param heartbeatInterval 心跳间隔（秒）
     */
    public CrossCommService(String serverIp, int serverPort, String clientId, int heartbeatInterval) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.clientId = clientId != null ? clientId : generateClientId();
        this.heartbeatInterval = heartbeatInterval;
        
        // 初始化OSS服务（从环境变量获取OSS配置）
        String endpoint = System.getenv("OSS_ENDPOINT");
        String bucketName = System.getenv("OSS_BUCKET_NAME");
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        initOSSService(endpoint, bucketName, accessKeyId, accessKeySecret);
        
        logger.info("CrossCommService initialized: clientId={}", this.clientId);
    }
    
    /**
     * 便捷构造函数，使用默认心跳间隔
     * 
     * @param serverIp 服务器IP地址
     * @param serverPort 服务器端口
     */
    public CrossCommService(String serverIp, int serverPort) {
        this(serverIp, serverPort, null, 30);
    }
    
    /**
     * 初始化OSS服务
     * 
     * @param endpoint OSS endpoint
     * @param bucketName OSS bucket名称
     * @param accessKeyId OSS Access Key ID
     * @param accessKeySecret OSS Access Key Secret
     */
    private void initOSSService(String endpoint, String bucketName, String accessKeyId, String accessKeySecret) {
        try {
            if (endpoint != null && bucketName != null && accessKeyId != null && accessKeySecret != null) {
                this.ossService = new AliyunOSS(endpoint, bucketName, accessKeyId, accessKeySecret, false);
                System.out.println("✓ OSS服务初始化成功，支持文件传输功能");
            } else {
                if (endpoint == null && bucketName == null && accessKeyId == null && accessKeySecret == null) {
                    // 如果都是null，说明可能是从环境变量读取失败
                    System.out.println("⚠ OSS环境变量未配置，文件传输功能将被禁用");
                    System.out.println("   需要配置：OSS_ENDPOINT, OSS_BUCKET_NAME, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET");
                } else {
                    // 如果部分配置缺失
                    System.out.println("⚠ OSS配置不完整，文件传输功能将被禁用");
                }
                this.ossService = null;
            }
        } catch (Exception e) {
            System.out.println("⚠ OSS服务初始化失败: " + e.getMessage());
            this.ossService = null;
        }
    }
    
    /**
     * 生成客户端唯一ID
     * 
     * @return 客户端ID
     */
    private String generateClientId() {
        try {
            // 获取MAC地址
            byte[] mac = NetworkInterface.getByInetAddress(
                java.net.InetAddress.getLocalHost()).getHardwareAddress();
            
            StringBuilder macStr = new StringBuilder();
            if (mac != null) {
                for (byte b : mac) {
                    macStr.append(String.format("%02x", b));
                }
            }
            
            // 生成随机ID
            String randomId = UUID.randomUUID().toString().substring(0, 8);
            return macStr.toString() + "_" + randomId;
        } catch (Exception e) {
            // 如果获取MAC地址失败，使用UUID
            return "java_" + UUID.randomUUID().toString().substring(0, 16);
        }
    }
    
    /**
     * 连接到服务器
     * 
     * @return 是否连接成功
     */
    public boolean connect() {
        try {
            URI serverUri = new URI("ws://" + serverIp + ":" + serverPort);
            System.out.println("正在连接到服务器: " + serverUri);
            
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("✓ WebSocket连接已建立！");
                    logger.info("WebSocket连接已建立");
                    isConnected = true;
                    
                    // 发送登录消息
                    try {
                        sendLoginMessage();
                        startHeartbeat();
                        System.out.println("✓ 客户端已登录，ID: " + clientId);
                    } catch (Exception e) {
                        System.out.println("✗ 发送登录消息失败: " + e.getMessage());
                        logger.error("发送登录消息失败", e);
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    try {
                        handleIncomingMessage(message);
                    } catch (Exception e) {
                        System.out.println("✗ 处理接收消息失败: " + e.getMessage());
                        logger.error("处理接收消息失败", e);
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("⚠ WebSocket连接已关闭: code=" + code + ", reason=" + reason);
                    logger.info("WebSocket连接已关闭: code={}, reason={}, remote={}", code, reason, remote);
                    isConnected = false;
                    stopHeartbeat();
                }
                
                @Override
                public void onError(Exception ex) {
                    System.out.println("✗ WebSocket连接错误: " + ex.getMessage());
                    logger.error("WebSocket连接错误", ex);
                    isConnected = false;
                }
            };
            
            System.out.println("等待连接建立（最多5秒）...");
            boolean connected = webSocketClient.connectBlocking(5, TimeUnit.SECONDS);
            if (!connected) {
                System.out.println("✗ 连接服务器超时");
                logger.error("连接服务器超时");
                return false;
            }
            
            System.out.println("✓ 客户端已成功连接到服务器");
            logger.info("客户端 {} 已连接到服务器 {}:{}", clientId, serverIp, serverPort);
            return true;
            
        } catch (Exception e) {
            System.out.println("✗ 连接服务器失败: " + e.getMessage());
            logger.error("连接服务器失败", e);
            return false;
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (isConnected && webSocketClient != null) {
            try {
                // 发送登出消息
                sendLogoutMessage();
                
                // 停止心跳
                stopHeartbeat();
                
                // 关闭连接
                webSocketClient.closeBlocking();
                
                logger.info("客户端 {} 已断开连接", clientId);
            } catch (Exception e) {
                logger.error("断开连接时发生错误", e);
            }
        }
    }
    
    /**
     * 发送登录消息
     */
    private void sendLoginMessage() throws JsonProcessingException {
        Message loginMsg = new Message(
            generateMsgId(),
            clientId,
            "server",
            CommMsgType.LOGIN,
            new HashMap<>(),
            System.currentTimeMillis() / 1000.0
        );
        
        webSocketClient.send(loginMsg.toJson());
        logger.debug("发送登录消息");
    }
    
    /**
     * 发送登出消息
     */
    private void sendLogoutMessage() throws JsonProcessingException {
        Message logoutMsg = new Message(
            generateMsgId(),
            clientId,
            "server",
            CommMsgType.LOGOUT,
            new HashMap<>(),
            System.currentTimeMillis() / 1000.0
        );
        
        webSocketClient.send(logoutMsg.toJson());
        logger.debug("发送登出消息");
    }
    
    /**
     * 生成消息ID
     * 
     * @return 消息ID
     */
    private String generateMsgId() {
        return clientId + "_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 检查是否为文件类型消息
     * 
     * @param msgType 消息类型
     * @return 是否为文件类型
     */
    private boolean isFileMessageType(CommMsgType msgType) {
        return msgType == CommMsgType.FILE || msgType == CommMsgType.IMAGE || msgType == CommMsgType.FOLDER;
    }
    
    /**
     * 启动心跳
     */
    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatTask = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (isConnected) {
                    sendHeartbeat();
                }
            } catch (Exception e) {
                logger.error("发送心跳失败", e);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.SECONDS);
        
        logger.debug("心跳任务已启动，间隔: {} 秒", heartbeatInterval);
    }
    
    /**
     * 停止心跳
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        logger.debug("心跳任务已停止");
    }
    
    /**
     * 发送心跳消息
     */
    private void sendHeartbeat() throws JsonProcessingException {
        Message heartbeatMsg = new Message(
            generateMsgId(),
            clientId,
            "server",
            CommMsgType.HEARTBEAT,
            new HashMap<>(),
            System.currentTimeMillis() / 1000.0
        );
        
        webSocketClient.send(heartbeatMsg.toJson());
        logger.trace("发送心跳消息");
    }
    
    /**
     * 处理接收到的消息
     * 
     * @param messageJson 消息JSON字符串
     */
    private void handleIncomingMessage(String messageJson) {
        try {
            Message message = Message.fromJson(messageJson);
            
            // 处理特殊消息类型
            if (message.getMsgType() == CommMsgType.LIST_CLIENTS_RESPONSE) {
                handleClientListResponse(message);
                return;
            }
            
            // 过滤自己发送的消息
            if (clientId.equals(message.getFromClientId())) {
                return;
            }
            
            // 显示收到的消息（除了心跳消息）
            if (message.getMsgType() != CommMsgType.HEARTBEAT) {
                System.out.println("📥 收到消息 [" + message.getMsgType().getValue() + "] 从 " + 
                                 message.getFromClientId() + ": " + message.getContent());
            }
            
            // 调用注册的消息处理器
            for (MessageHandler handler : messageHandlers) {
                if (handler.shouldHandle(message)) {
                    try {
                        handler.handle(message);
                    } catch (Exception e) {
                        System.out.println("✗ 消息处理器执行失败: " + e.getMessage());
                        logger.error("消息处理器执行失败", e);
                    }
                }
            }
            
        } catch (JsonProcessingException e) {
            System.out.println("✗ 解析接收消息失败: " + e.getMessage());
            logger.error("解析接收消息失败: {}", messageJson, e);
        }
    }
    
    /**
     * 处理客户端列表响应
     * 
     * @param message 响应消息
     */
    private void handleClientListResponse(Message message) {
        try {
            String contentStr = message.getContent().toString();
            JsonNode contentNode = objectMapper.readTree(contentStr);
            Map<String, Object> clientListData = objectMapper.convertValue(contentNode, Map.class);
            lastClientList = clientListData;
            
            logger.info("收到客户端列表，共 {} 个客户端", 
                       clientListData.get("total_count"));
        } catch (Exception e) {
            logger.error("处理客户端列表响应失败", e);
        }
    }
    
    /**
     * 发送消息
     * 
     * @param content 消息内容
     * @param msgType 消息类型
     * @param toClientId 目标客户端ID，"all"表示发送给所有在线客户端
     * @return 是否发送成功
     */
    public boolean sendMessage(Object content, CommMsgType msgType, String toClientId) {
        if (!isConnected || webSocketClient == null) {
            logger.error("客户端未连接到服务器");
            return false;
        }
        
        try {
            // 处理不同类型的消息内容
            Object processedContent = processMessageContent(content, msgType);
            
            Message message = new Message(
                generateMsgId(),
                clientId,
                toClientId,
                msgType,
                processedContent,
                System.currentTimeMillis() / 1000.0
            );
            
            // 对于文件类型消息，设置OSS key
            if (isFileMessageType(msgType)) {
                message.setOssKey(processedContent.toString());
            }
            
            webSocketClient.send(message.toJson());
            System.out.println("📤 已发送消息 [" + msgType.getValue() + "] 到 " + toClientId + ": " + processedContent);
            logger.info("消息已发送: {} -> {}", msgType.getValue(), toClientId);
            return true;
            
        } catch (IOException e) {
            System.out.println("✗ 文件处理失败: " + e.getMessage());
            logger.error("文件处理失败", e);
            return false;
        } catch (Exception e) {
            System.out.println("✗ 发送消息失败: " + e.getMessage());
            logger.error("发送消息失败", e);
            return false;
        }
    }
    
    /**
     * 发送消息给所有在线客户端
     * 
     * @param content 消息内容
     * @param msgType 消息类型
     * @return 是否发送成功
     */
    public boolean sendMessage(Object content, CommMsgType msgType) {
        return sendMessage(content, msgType, "all");
    }
    
    /**
     * 发送文件
     * 
     * @param filePath 文件路径
     * @param toClientId 目标客户端ID，"all"表示发送给所有在线客户端
     * @return 是否发送成功
     */
    public boolean sendFile(String filePath, String toClientId) {
        return sendMessage(filePath, CommMsgType.FILE, toClientId);
    }
    
    /**
     * 发送文件给所有在线客户端
     * 
     * @param filePath 文件路径
     * @return 是否发送成功
     */
    public boolean sendFile(String filePath) {
        return sendFile(filePath, "all");
    }
    
    /**
     * 发送图片
     * 
     * @param imagePath 图片路径
     * @param toClientId 目标客户端ID，"all"表示发送给所有在线客户端
     * @return 是否发送成功
     */
    public boolean sendImage(String imagePath, String toClientId) {
        return sendMessage(imagePath, CommMsgType.IMAGE, toClientId);
    }
    
    /**
     * 发送图片给所有在线客户端
     * 
     * @param imagePath 图片路径
     * @return 是否发送成功
     */
    public boolean sendImage(String imagePath) {
        return sendImage(imagePath, "all");
    }
    
    /**
     * 发送文件夹
     * 
     * @param directoryPath 文件夹路径
     * @param toClientId 目标客户端ID，"all"表示发送给所有在线客户端
     * @return 是否发送成功
     */
    public boolean sendDirectory(String directoryPath, String toClientId) {
        return sendMessage(directoryPath, CommMsgType.FOLDER, toClientId);
    }
    
    /**
     * 发送文件夹给所有在线客户端
     * 
     * @param directoryPath 文件夹路径
     * @return 是否发送成功
     */
    public boolean sendDirectory(String directoryPath) {
        return sendDirectory(directoryPath, "all");
    }
    
    /**
     * 处理消息内容
     * 
     * @param content 原始内容
     * @param msgType 消息类型
     * @return 处理后的内容
     */
    private Object processMessageContent(Object content, CommMsgType msgType) throws JsonProcessingException, IOException {
        switch (msgType) {
            case TEXT:
                return content.toString();
                
            case JSON:
                if (content instanceof String) {
                    // 验证JSON格式
                    objectMapper.readTree((String) content);
                    return content;
                } else {
                    // 将对象转换为JSON字符串
                    return objectMapper.writeValueAsString(content);
                }
                
            case DICT:
                if (content instanceof String) {
                    return content;
                } else {
                    return objectMapper.writeValueAsString(content);
                }
                
            case BYTES:
                if (content instanceof byte[]) {
                    return Base64.getEncoder().encodeToString((byte[]) content);
                } else {
                    throw new IllegalArgumentException("BYTES类型消息内容必须是byte[]");
                }
                
            case IMAGE:
            case FILE:
                if (ossService == null) {
                    throw new IllegalStateException("OSS服务未初始化，无法发送文件");
                }
                String filePath = content.toString();
                // 生成OSS key：使用文件名 + 时间戳，避免重名冲突
                String fileName = Paths.get(filePath).getFileName().toString();
                String ossKey = String.format("%s_%d_%s", clientId, System.currentTimeMillis(), fileName);
                boolean success = ossService.uploadFile(ossKey, filePath);
                if (success) {
                    return ossKey; // 返回OSS key
                } else {
                    throw new RuntimeException("文件上传失败: " + filePath);
                }
                
            case FOLDER:
                if (ossService == null) {
                    throw new IllegalStateException("OSS服务未初始化，无法发送文件夹");
                }
                String dirPath = content.toString();
                // uploadDirectory需要两个参数：localPath和prefix
                // 使用目录名作为OSS前缀
                String dirName = Paths.get(dirPath).getFileName().toString();
                boolean dirSuccess = ossService.uploadDirectory(dirPath, dirName);
                if (dirSuccess) {
                    return dirName; // 返回OSS prefix
                } else {
                    throw new RuntimeException("文件夹上传失败: " + dirPath);
                }
                
            default:
                return content;
        }
    }
    
    /**
     * 获取客户端列表
     * 
     * @param onlyShowOnline 是否只显示在线客户端
     * @param timeoutSeconds 等待响应的超时时间（秒）
     * @return 客户端列表数据
     */
    public Map<String, Object> listClients(boolean onlyShowOnline, int timeoutSeconds) {
        if (!isConnected || webSocketClient == null) {
            logger.error("客户端未连接到服务器");
            return null;
        }
        
        try {
            // 清空之前的响应
            lastClientList = null;
            
            Map<String, Object> requestContent = new HashMap<>();
            requestContent.put("only_show_online", onlyShowOnline);
            
            Message requestMsg = new Message(
                generateMsgId(),
                clientId,
                "server",
                CommMsgType.LIST_CLIENTS,
                objectMapper.writeValueAsString(requestContent),
                System.currentTimeMillis() / 1000.0
            );
            
            webSocketClient.send(requestMsg.toJson());
            logger.info("已发送客户端列表请求 (only_online={})", onlyShowOnline);
            
            // 等待响应
            long startTime = System.currentTimeMillis();
            long timeoutMillis = timeoutSeconds * 1000L;
            
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                if (lastClientList != null) {
                    return lastClientList;
                }
                Thread.sleep(100);
            }
            
            logger.warn("请求客户端列表超时 ({} 秒)", timeoutSeconds);
            return null;
            
        } catch (Exception e) {
            logger.error("请求客户端列表失败", e);
            return null;
        }
    }
    
    /**
     * 获取在线客户端列表
     * 
     * @param timeoutSeconds 超时时间（秒）
     * @return 客户端列表数据
     */
    public Map<String, Object> listOnlineClients(int timeoutSeconds) {
        return listClients(true, timeoutSeconds);
    }
    
    /**
     * 注册消息处理器（通过对象实例）
     * 
     * @param handlerObject 包含注解方法的对象实例
     */
    public void registerMessageHandlers(Object handlerObject) {
        Class<?> clazz = handlerObject.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        
        for (Method method : methods) {
            MessageListener annotation = method.getAnnotation(MessageListener.class);
            if (annotation != null) {
                // 检查方法签名
                if (method.getParameterCount() != 1 || 
                    !method.getParameterTypes()[0].equals(Message.class)) {
                    logger.warn("消息处理方法 {} 的签名不正确，应该接受一个Message参数", 
                               method.getName());
                    continue;
                }
                
                MessageHandler handler = new MessageHandler(
                    handlerObject, method, annotation.msgType(), annotation.fromClientId(),
                    annotation.downloadDirectory(), ossService
                );
                messageHandlers.add(handler);
                
                logger.info("注册消息处理器: {}.{}", clazz.getSimpleName(), method.getName());
            }
        }
    }
    
    // Getters
    public String getClientId() {
        return clientId;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 消息处理器内部类
     */
    private static class MessageHandler {
        private final Object handlerObject;
        private final Method method;
        private final CommMsgType[] msgTypes;
        private final String fromClientId;
        private final String downloadDirectory;
        private final AliyunOSS ossService;
        
        public MessageHandler(Object handlerObject, Method method, 
                            CommMsgType[] msgTypes, String fromClientId,
                            String downloadDirectory, AliyunOSS ossService) {
            this.handlerObject = handlerObject;
            this.method = method;
            this.msgTypes = msgTypes;
            this.fromClientId = fromClientId;
            this.downloadDirectory = downloadDirectory;
            this.ossService = ossService;
            
            // 确保方法可访问
            method.setAccessible(true);
        }
        
        public boolean shouldHandle(Message message) {
            // 检查消息类型过滤
            if (msgTypes.length > 0) {
                boolean typeMatched = false;
                for (CommMsgType type : msgTypes) {
                    if (type == message.getMsgType()) {
                        typeMatched = true;
                        break;
                    }
                }
                if (!typeMatched) {
                    return false;
                }
            }
            
            // 检查发送方过滤
            if (!fromClientId.isEmpty() && !fromClientId.equals(message.getFromClientId())) {
                return false;
            }
            
            return true;
        }
        
        public void handle(Message message) throws Exception {
            // 对于文件类型消息，如果指定了下载目录，则自动下载文件
            if (isFileMessage(message.getMsgType()) && !downloadDirectory.isEmpty() && ossService != null) {
                try {
                    String ossKey = message.getOssKey();
                    if (ossKey != null && !ossKey.isEmpty()) {
                        String localPath = downloadFileFromOSS(ossKey, message.getMsgType());
                        if (localPath != null) {
                            // 修改消息内容为本地文件路径
                            message.setContent(localPath);
                            System.out.println("📁 已下载文件: " + ossKey + " -> " + localPath);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("✗ 文件下载失败: " + e.getMessage());
                    // 继续处理原始消息
                }
            }
            
            method.invoke(handlerObject, message);
        }
        
        private boolean isFileMessage(CommMsgType msgType) {
            return msgType == CommMsgType.FILE || msgType == CommMsgType.IMAGE || msgType == CommMsgType.FOLDER;
        }
        
        private String downloadFileFromOSS(String ossKey, CommMsgType msgType) {
            try {
                if (msgType == CommMsgType.FOLDER) {
                    // 下载文件夹
                    boolean success = ossService.downloadFilesWithPrefix(ossKey, downloadDirectory);
                    return success ? downloadDirectory : null;
                } else {
                    // 下载单个文件
                    boolean success = ossService.downloadFile(ossKey, downloadDirectory);
                    if (success) {
                        // 返回本地文件完整路径
                        return downloadDirectory + "/" + ossKey;
                    }
                    return null;
                }
            } catch (Exception e) {
                System.out.println("OSS下载失败: " + e.getMessage());
                return null;
            }
        }
    }
} 