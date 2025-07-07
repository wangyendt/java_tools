package com.wayne.cross_comm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Scanner;

/**
 * CrossCommService 测试主类
 * 
 * @author Wayne
 * @since 1.0.0
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    // 服务器配置 - 静态变量，方便修改
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9898;
    private static final int HEARTBEAT_INTERVAL = 30; // 心跳间隔（秒）
    
    // OSS配置 - 静态变量，方便修改
    // ⚠️ 重要提示：请将以下配置替换为你的实际阿里云OSS配置
    // 如果不需要文件传输功能，可以保持默认值，程序会自动禁用文件传输
    private static final String OSS_ENDPOINT = "xxx";
    private static final String OSS_BUCKET_NAME = "xxx";
    private static final String OSS_ACCESS_KEY_ID = "xxx";
    private static final String OSS_ACCESS_KEY_SECRET = "xxx";
    
    public static void main(String[] args) {
        logger.info("=== 跨语言通信服务 Java 客户端测试 ===");
        logger.info("服务器配置: {}:{}", SERVER_IP, SERVER_PORT);
        
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String mode = args[0].toLowerCase();
        switch (mode) {
            case "listener":
            case "listen":
                runListenerMode();
                break;
            case "sender":
            case "send":
                runSenderMode();
                break;
            default:
                logger.error("未知模式: {}", mode);
                printUsage();
        }
    }
    
    /**
     * 打印使用说明
     */
    private static void printUsage() {
        System.out.println("使用方法:");
        System.out.println("  java -jar cross_comm.jar listener  # 运行监听模式（只接收消息）");
        System.out.println("  java -jar cross_comm.jar sender    # 运行发送模式（可发送消息）");
        System.out.println();
        System.out.println("或使用Maven运行:");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.wayne.cross_comm.Main\" -Dexec.args=\"listener\"");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"com.wayne.cross_comm.Main\" -Dexec.args=\"sender\"");
    }
    
    /**
     * 运行监听模式 - 只接收和处理消息
     */
    private static void runListenerMode() {
        logger.info("=== 监听模式 - 只接收消息 ===");
        
        // 创建通信服务实例（使用静态OSS配置）
        CrossCommService client = new CrossCommService(
            SERVER_IP, SERVER_PORT, "java_listener", HEARTBEAT_INTERVAL,
            OSS_ENDPOINT, OSS_BUCKET_NAME, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET
        );
        logger.info("客户端ID: {}", client.getClientId());
        
        // 创建消息处理器类
        MessageHandlerExample handlers = new MessageHandlerExample();
        
        // 注册消息处理器
        client.registerMessageHandlers(handlers);
        
        try {
            // 连接到服务器
            boolean connected = client.connect();
            if (!connected) {
                logger.error("连接服务器失败");
                return;
            }
            
            logger.info("连接成功！开始监听消息...");
            logger.info("按 Ctrl+C 退出程序");
            
            // 等待连接稳定
            Thread.sleep(1000);
            
            // 发送一条上线通知
            client.sendMessage("Java监听客户端已上线", CommMsgType.TEXT);
            
            // 保持程序运行，持续监听消息
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("正在关闭监听客户端...");
                client.disconnect();
            }));
            
            // 无限循环保持程序运行
            while (client.isConnected()) {
                Thread.sleep(5000);
                
                // 可选：定期获取客户端列表
                if (System.currentTimeMillis() % 30000 < 5000) { // 大约每30秒执行一次
                    Map<String, Object> onlineClients = client.listOnlineClients(3);
                    if (onlineClients != null) {
                        logger.info("当前在线客户端数量: {}", onlineClients.get("total_count"));
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("监听模式运行时发生错误", e);
        } finally {
            client.disconnect();
            logger.info("监听客户端已停止");
        }
    }
    
    /**
     * 运行发送模式 - 可以发送消息和交互
     */
    private static void runSenderMode() {
        logger.info("=== 发送模式 - 可发送消息 ===");
        
        // 创建通信服务实例（使用静态OSS配置）
        CrossCommService client = new CrossCommService(
            SERVER_IP, SERVER_PORT, "java_sender", HEARTBEAT_INTERVAL,
            OSS_ENDPOINT, OSS_BUCKET_NAME, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET
        );
        logger.info("客户端ID: {}", client.getClientId());
        
        // 创建消息处理器类
        MessageHandlerExample handlers = new MessageHandlerExample();
        
        // 注册消息处理器
        client.registerMessageHandlers(handlers);
        
        try {
            // 连接到服务器
            boolean connected = client.connect();
            if (!connected) {
                logger.error("连接服务器失败");
                return;
            }
            
            logger.info("连接成功！");
            
            // 等待连接稳定
            Thread.sleep(1000);
            
            // 演示发送各种类型的消息
            logger.info("\n=== 发送消息示例 ===");
            
            // 1. 发送文本消息给所有在线客户端
            client.sendMessage("Hello from Java sender!", CommMsgType.TEXT);
            
            // 2. 发送JSON消息
            client.sendMessage("{\"type\": \"notification\", \"data\": \"test from java sender\"}", CommMsgType.JSON);
            
            // 3. 发送字典消息
            java.util.Map<String, Object> dictContent = new java.util.HashMap<>();
            dictContent.put("action", "update");
            dictContent.put("id", 123);
            dictContent.put("source", "java_sender");
            client.sendMessage(dictContent, CommMsgType.DICT);
            
            // 4. 发送字节数据
            byte[] binaryData = "Binary data from Java sender".getBytes();
            client.sendMessage(binaryData, CommMsgType.BYTES);
            
            // 5. 文件传输示例（需要OSS配置）
            logger.info("\n=== 文件传输示例 ===");
            try {
                // 检查测试文件是否存在
                java.io.File testFile = new java.io.File("test_files/test.txt");
                if (testFile.exists()) {
                    logger.info("发送测试文件...");
                    boolean fileSuccess = client.sendFile("test_files/test.txt");
                    if (fileSuccess) {
                        logger.info("✓ 测试文件发送成功");
                    } else {
                        logger.warn("✗ 测试文件发送失败（可能需要配置OSS）");
                    }
                } else {
                    logger.warn("测试文件不存在，跳过文件传输演示");
                }
                
                // 如果test_files目录存在，尝试发送文件夹
                java.io.File testDir = new java.io.File("test_files");
                if (testDir.exists() && testDir.isDirectory()) {
                    logger.info("发送测试文件夹...");
                    boolean dirSuccess = client.sendDirectory("test_files");
                    if (dirSuccess) {
                        logger.info("✓ 测试文件夹发送成功");
                    } else {
                        logger.warn("✗ 测试文件夹发送失败（可能需要配置OSS）");
                    }
                }
            } catch (Exception e) {
                logger.warn("文件传输演示失败: {}", e.getMessage());
            }
            
            // 等待一下让消息发送完成
            Thread.sleep(500);
            
            // 6. 获取客户端列表
            logger.info("\n=== 获取客户端列表示例 ===");
            
            // 获取所有客户端（包括离线）
            Map<String, Object> allClients = client.listClients(false, 5);
            if (allClients != null) {
                logger.info("所有客户端数量: {}", allClients.get("total_count"));
                printClientList(allClients);
            }
            
            // 获取在线客户端
            Map<String, Object> onlineClients = client.listOnlineClients(5);
            if (onlineClients != null) {
                logger.info("在线客户端数量: {}", onlineClients.get("total_count"));
                printClientList(onlineClients);
            }
            
            // 交互式命令行
            logger.info("\n=== 交互式命令行 ===");
            logger.info("输入消息发送给所有客户端");
            logger.info("特殊命令:");
            logger.info("  list         - 获取在线客户端列表");
            logger.info("  json:内容    - 发送JSON消息");
            logger.info("  bytes:内容   - 发送字节消息");
            logger.info("  file:路径    - 发送文件");
            logger.info("  image:路径   - 发送图片");
            logger.info("  folder:路径  - 发送文件夹");
            logger.info("  quit         - 退出程序");
            
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("请输入消息: ");
                String input = scanner.nextLine().trim();
                
                if ("quit".equalsIgnoreCase(input)) {
                    break;
                } else if ("list".equalsIgnoreCase(input)) {
                    // 获取客户端列表
                    Map<String, Object> clients = client.listOnlineClients(5);
                    if (clients != null) {
                        logger.info("在线客户端数量: {}", clients.get("total_count"));
                        printClientList(clients);
                    } else {
                        logger.warn("获取客户端列表失败");
                    }
                } else if (input.startsWith("json:")) {
                    // 发送JSON消息
                    String jsonContent = input.substring(5);
                    client.sendMessage(jsonContent, CommMsgType.JSON);
                    logger.info("JSON消息已发送: {}", jsonContent);
                } else if (input.startsWith("bytes:")) {
                    // 发送字节消息
                    String bytesContent = input.substring(6);
                    client.sendMessage(bytesContent.getBytes(), CommMsgType.BYTES);
                    logger.info("字节消息已发送: {}", bytesContent);
                } else if (input.startsWith("file:")) {
                    // 发送文件
                    String filePath = input.substring(5);
                    boolean success = client.sendFile(filePath);
                    if (success) {
                        logger.info("文件已发送: {}", filePath);
                    } else {
                        logger.error("文件发送失败: {}", filePath);
                    }
                } else if (input.startsWith("image:")) {
                    // 发送图片
                    String imagePath = input.substring(6);
                    boolean success = client.sendImage(imagePath);
                    if (success) {
                        logger.info("图片已发送: {}", imagePath);
                    } else {
                        logger.error("图片发送失败: {}", imagePath);
                    }
                } else if (input.startsWith("folder:")) {
                    // 发送文件夹
                    String folderPath = input.substring(7);
                    boolean success = client.sendDirectory(folderPath);
                    if (success) {
                        logger.info("文件夹已发送: {}", folderPath);
                    } else {
                        logger.error("文件夹发送失败: {}", folderPath);
                    }
                } else if (!input.isEmpty()) {
                    // 发送普通文本消息
                    client.sendMessage(input, CommMsgType.TEXT);
                    logger.info("文本消息已发送: {}", input);
                }
            }
            
        } catch (Exception e) {
            logger.error("发送模式运行时发生错误", e);
        } finally {
            // 断开连接
            client.disconnect();
            logger.info("发送客户端已停止");
        }
    }
    
    /**
     * 打印客户端列表
     * 
     * @param clientListData 客户端列表数据
     */
    @SuppressWarnings("unchecked")
    private static void printClientList(Map<String, Object> clientListData) {
        try {
            java.util.List<Map<String, Object>> clients = 
                (java.util.List<Map<String, Object>>) clientListData.get("clients");
            
            if (clients != null && !clients.isEmpty()) {
                for (Map<String, Object> clientInfo : clients) {
                    String clientId = (String) clientInfo.get("client_id");
                    String status = (String) clientInfo.get("status");
                    logger.info("  - {}: {}", clientId, status);
                }
            } else {
                logger.info("  (无客户端)");
            }
        } catch (Exception e) {
            logger.error("打印客户端列表失败", e);
        }
    }
    
    /**
     * 消息处理器示例类
     */
    public static class MessageHandlerExample {
        
        private final Logger logger = LoggerFactory.getLogger(MessageHandlerExample.class);
        
        /**
         * 处理文本消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.TEXT})
        public void handleTextMessage(Message message) {
            logger.info("📝 收到文本消息: {}", message.getContent());
            logger.info("   来自: {}", message.getFromClientId());
            logger.info("   时间: {}", new java.util.Date((long)(message.getTimestamp() * 1000)));
        }
        
        /**
         * 处理JSON消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.JSON})
        public void handleJsonMessage(Message message) {
            logger.info("🔍 收到JSON消息: {}", message.getContent());
            logger.info("   来自: {}", message.getFromClientId());
        }
        
        /**
         * 处理字典消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.DICT})
        public void handleDictMessage(Message message) {
            logger.info("📋 收到字典消息: {}", message.getContent());
            logger.info("   来自: {}", message.getFromClientId());
        }
        
        /**
         * 处理字节数据消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.BYTES})
        public void handleBytesMessage(Message message) {
            try {
                // 解码Base64字符串
                byte[] decodedBytes = java.util.Base64.getDecoder()
                    .decode(message.getContent().toString());
                String decodedContent = new String(decodedBytes);
                
                logger.info("💾 收到字节消息: {}", decodedContent);
                logger.info("   原始长度: {} 字节", decodedBytes.length);
                logger.info("   来自: {}", message.getFromClientId());
            } catch (Exception e) {
                logger.error("解码字节消息失败", e);
            }
        }
        
        /**
         * 处理文件消息（自动下载到downloads/files目录）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.FILE}, downloadDirectory = "./downloads/files")
        public void handleFileMessage(Message message) {
            logger.info("📁 收到文件消息: {}", message.getContent());
            logger.info("   OSS Key: {}", message.getOssKey());
            logger.info("   来自: {}", message.getFromClientId());
            // content字段现在包含下载后的本地文件路径
        }
        
        /**
         * 处理图片消息（自动下载到downloads/images目录）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.IMAGE}, downloadDirectory = "./downloads/images")
        public void handleImageMessage(Message message) {
            logger.info("🖼️ 收到图片消息: {}", message.getContent());
            logger.info("   OSS Key: {}", message.getOssKey());
            logger.info("   来自: {}", message.getFromClientId());
            // content字段现在包含下载后的本地图片路径
        }
        
        /**
         * 处理文件夹消息（自动下载到downloads/folders目录）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.FOLDER}, downloadDirectory = "./downloads/folders")
        public void handleFolderMessage(Message message) {
            logger.info("📂 收到文件夹消息: {}", message.getContent());
            logger.info("   OSS Key: {}", message.getOssKey());
            logger.info("   来自: {}", message.getFromClientId());
            // content字段现在包含下载后的本地文件夹路径
        }
        
        /**
         * 处理文件消息但不自动下载（只显示OSS信息）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.FILE, CommMsgType.IMAGE, CommMsgType.FOLDER})
        public void handleFileMessageInfo(Message message) {
            logger.info("ℹ️ 文件消息信息 - 类型: {}, OSS Key: {}, 来自: {}",
                       message.getMsgType().getValue(), 
                       message.getOssKey(),
                       message.getFromClientId());
        }
        
        /**
         * 处理特定客户端的消息（Python客户端）
         * 
         * @param message 消息对象
         */
        @MessageListener(fromClientId = "python_client")
        public void handlePythonClientMessage(Message message) {
            logger.info("🐍 收到来自Python客户端的消息: {} ({})", 
                       message.getContent(), message.getMsgType().getValue());
        }
        
        /**
         * 处理所有以"python"开头的客户端消息
         * 
         * @param message 消息对象
         */
        @MessageListener
        public void handlePythonMessages(Message message) {
            if (message.getFromClientId().toLowerCase().contains("python")) {
                logger.info("🐍 收到Python相关客户端消息: {} 来自 {}", 
                           message.getContent(), message.getFromClientId());
            }
        }
        
        /**
         * 通用消息处理器（处理所有消息类型）- 设置为debug级别避免过多输出
         * 
         * @param message 消息对象
         */
        @MessageListener // 不指定过滤条件，监听所有消息
        public void handleAllMessages(Message message) {
            logger.debug("[通用处理器] {}: {} 来自 {}", 
                        message.getMsgType().getValue(), 
                        message.getContent(),
                        message.getFromClientId());
        }
    }
} 