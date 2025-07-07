package com.wayne.cross_comm;

import java.util.Map;
import java.util.Scanner;

/**
 * CrossCommService 测试主类
 * 
 * @author Wayne
 * @since 1.0.0
 */
public class Main {
    
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
        PlatformUtils.logInfo("=== 跨语言通信服务 Java 客户端测试 ===");
        PlatformUtils.logInfo("服务器配置: " + SERVER_IP + ":" + SERVER_PORT);
        
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
                PlatformUtils.logError("未知模式: " + mode);
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
        PlatformUtils.logInfo("=== 监听模式 - 只接收消息 ===");
        
        // 创建通信服务实例（使用静态OSS配置）
        CrossCommService client = new CrossCommService(
            SERVER_IP, SERVER_PORT, "java_listener", HEARTBEAT_INTERVAL,
            OSS_ENDPOINT, OSS_BUCKET_NAME, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET
        );
        PlatformUtils.logInfo("客户端ID: " + client.getClientId());
        
        // 创建消息处理器类
        MessageHandlerExample handlers = new MessageHandlerExample();
        
        // 注册消息处理器
        client.registerMessageHandlers(handlers);
        
        try {
            // 连接到服务器
            boolean connected = client.connect();
            if (!connected) {
                PlatformUtils.logError("连接服务器失败");
                return;
            }
            
            PlatformUtils.logInfo("连接成功！开始监听消息...");
            PlatformUtils.logInfo("按 Ctrl+C 退出程序");
            
            // 等待连接稳定
            Thread.sleep(1000);
            
            // 发送一条上线通知
            client.sendMessage("Java监听客户端已上线", CommMsgType.TEXT);
            
            // 保持程序运行，持续监听消息
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                PlatformUtils.logInfo("正在关闭监听客户端...");
                client.disconnect();
            }));
            
            // 无限循环保持程序运行
            while (client.isConnected()) {
                Thread.sleep(5000);
                
                // 可选：定期获取客户端列表
                if (System.currentTimeMillis() % 30000 < 5000) { // 大约每30秒执行一次
                    Map<String, Object> onlineClients = client.listOnlineClients(3);
                    if (onlineClients != null) {
                        PlatformUtils.logInfo("当前在线客户端数量: " + onlineClients.get("total_count"));
                    }
                }
            }
            
        } catch (Exception e) {
            PlatformUtils.logError("监听模式运行时发生错误", e);
        } finally {
            client.disconnect();
            PlatformUtils.logInfo("监听客户端已停止");
        }
    }
    
    /**
     * 运行发送模式 - 可以发送消息和交互
     */
    private static void runSenderMode() {
        PlatformUtils.logInfo("=== 发送模式 - 可发送消息 ===");
        
        // 创建通信服务实例（使用静态OSS配置）
        CrossCommService client = new CrossCommService(
            SERVER_IP, SERVER_PORT, "java_sender", HEARTBEAT_INTERVAL,
            OSS_ENDPOINT, OSS_BUCKET_NAME, OSS_ACCESS_KEY_ID, OSS_ACCESS_KEY_SECRET
        );
        PlatformUtils.logInfo("客户端ID: " + client.getClientId());
        
        // 创建消息处理器类
        MessageHandlerExample handlers = new MessageHandlerExample();
        
        // 注册消息处理器
        client.registerMessageHandlers(handlers);
        
        try {
            // 连接到服务器
            boolean connected = client.connect();
            if (!connected) {
                PlatformUtils.logError("连接服务器失败");
                return;
            }
            
            PlatformUtils.logInfo("连接成功！");
            
            // 等待连接稳定
            Thread.sleep(1000);
            
            // 演示发送各种类型的消息
            PlatformUtils.logInfo("\n=== 发送消息示例 ===");
            
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
            PlatformUtils.logInfo("\n=== 文件传输示例 ===");
            try {
                // 检查测试文件是否存在
                java.io.File testFile = new java.io.File("test_files/test.txt");
                PlatformUtils.logInfo("检查测试文件: " + testFile.getAbsolutePath());
                PlatformUtils.logInfo("文件存在: " + testFile.exists() + ", 大小: " + testFile.length() + " 字节");
                
                if (testFile.exists()) {
                    PlatformUtils.logInfo("发送测试文件...");
                    try {
                        boolean fileSuccess = client.sendFile("test_files/test.txt");
                        if (fileSuccess) {
                            PlatformUtils.logInfo("✓ 测试文件发送成功");
                        } else {
                            PlatformUtils.logWarn("✗ 测试文件发送失败（可能需要配置OSS）");
                        }
                    } catch (Exception e) {
                        PlatformUtils.logError("文件发送异常: " + e.getMessage(), e);
                    }
                } else {
                    PlatformUtils.logWarn("测试文件不存在，跳过文件传输演示");
                }
                
                // 测试图片发送
                java.io.File testImage = new java.io.File("test_files/test_image.bmp");
                PlatformUtils.logInfo("检查测试图片: " + testImage.getAbsolutePath());
                PlatformUtils.logInfo("图片存在: " + testImage.exists() + ", 大小: " + testImage.length() + " 字节");
                
                if (testImage.exists()) {
                    PlatformUtils.logInfo("发送测试图片...");
                    try {
                        boolean imageSuccess = client.sendImage("test_files/test_image.bmp");
                        if (imageSuccess) {
                            PlatformUtils.logInfo("✓ 测试图片发送成功");
                        } else {
                            PlatformUtils.logWarn("✗ 测试图片发送失败");
                        }
                    } catch (Exception e) {
                        PlatformUtils.logError("图片发送异常: " + e.getMessage(), e);
                    }
                }
                
                // 如果test_files目录存在，尝试发送文件夹
                java.io.File testDir = new java.io.File("test_files");
                PlatformUtils.logInfo("检查测试目录: " + testDir.getAbsolutePath());
                PlatformUtils.logInfo("目录存在: " + testDir.exists() + ", 是目录: " + testDir.isDirectory());
                
                if (testDir.exists() && testDir.isDirectory()) {
                    // 列出目录内容
                    String[] files = testDir.list();
                    if (files != null) {
                        PlatformUtils.logInfo("目录包含 " + files.length + " 个文件:");
                        for (String file : files) {
                            PlatformUtils.logInfo("  - " + file);
                        }
                    }
                    
                    PlatformUtils.logInfo("发送测试文件夹...");
                    try {
                        boolean dirSuccess = client.sendDirectory("test_files");
                        if (dirSuccess) {
                            PlatformUtils.logInfo("✓ 测试文件夹发送成功");
                        } else {
                            PlatformUtils.logWarn("✗ 测试文件夹发送失败");
                        }
                    } catch (Exception e) {
                        PlatformUtils.logError("文件夹发送异常: " + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                PlatformUtils.logError("文件传输演示失败: " + e.getMessage(), e);
            }
            
            // 等待一下让消息发送完成
            Thread.sleep(500);
            
            // 6. 获取客户端列表
            PlatformUtils.logInfo("\n=== 获取客户端列表示例 ===");
            
            // 获取所有客户端（包括离线）
            Map<String, Object> allClients = client.listClients(false, 5);
            if (allClients != null) {
                PlatformUtils.logInfo("所有客户端数量: " + allClients.get("total_count"));
                printClientList(allClients);
            }
            
            // 获取在线客户端
            Map<String, Object> onlineClients = client.listOnlineClients(5);
            if (onlineClients != null) {
                PlatformUtils.logInfo("在线客户端数量: " + onlineClients.get("total_count"));
                printClientList(onlineClients);
            }
            
            // 交互式命令行
            PlatformUtils.logInfo("\n=== 交互式命令行 ===");
            PlatformUtils.logInfo("输入消息发送给所有客户端");
            PlatformUtils.logInfo("特殊命令:");
            PlatformUtils.logInfo("  list         - 获取在线客户端列表");
            PlatformUtils.logInfo("  json:内容    - 发送JSON消息");
            PlatformUtils.logInfo("  bytes:内容   - 发送字节消息");
            PlatformUtils.logInfo("  file:路径    - 发送文件");
            PlatformUtils.logInfo("  image:路径   - 发送图片");
            PlatformUtils.logInfo("  folder:路径  - 发送文件夹");
            PlatformUtils.logInfo("  quit         - 退出程序");
            
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
                        PlatformUtils.logInfo("在线客户端数量: " + clients.get("total_count"));
                        printClientList(clients);
                    } else {
                        PlatformUtils.logWarn("获取客户端列表失败");
                    }
                } else if (input.startsWith("json:")) {
                    // 发送JSON消息
                    String jsonContent = input.substring(5);
                    client.sendMessage(jsonContent, CommMsgType.JSON);
                    PlatformUtils.logInfo("JSON消息已发送: " + jsonContent);
                } else if (input.startsWith("bytes:")) {
                    // 发送字节消息
                    String bytesContent = input.substring(6);
                    client.sendMessage(bytesContent.getBytes(), CommMsgType.BYTES);
                    PlatformUtils.logInfo("字节消息已发送: " + bytesContent);
                } else if (input.startsWith("file:")) {
                    // 发送文件
                    String filePath = input.substring(5);
                    boolean success = client.sendFile(filePath);
                    if (success) {
                        PlatformUtils.logInfo("文件已发送: " + filePath);
                    } else {
                        PlatformUtils.logError("文件发送失败: " + filePath);
                    }
                } else if (input.startsWith("image:")) {
                    // 发送图片
                    String imagePath = input.substring(6);
                    boolean success = client.sendImage(imagePath);
                    if (success) {
                        PlatformUtils.logInfo("图片已发送: " + imagePath);
                    } else {
                        PlatformUtils.logError("图片发送失败: " + imagePath);
                    }
                } else if (input.startsWith("folder:")) {
                    // 发送文件夹
                    String folderPath = input.substring(7);
                    boolean success = client.sendDirectory(folderPath);
                    if (success) {
                        PlatformUtils.logInfo("文件夹已发送: " + folderPath);
                    } else {
                        PlatformUtils.logError("文件夹发送失败: " + folderPath);
                    }
                } else if (!input.isEmpty()) {
                    // 发送普通文本消息
                    client.sendMessage(input, CommMsgType.TEXT);
                    PlatformUtils.logInfo("文本消息已发送: " + input);
                }
            }
            
        } catch (Exception e) {
            PlatformUtils.logError("发送模式运行时发生错误", e);
        } finally {
            // 断开连接
            client.disconnect();
            PlatformUtils.logInfo("发送客户端已停止");
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
                    PlatformUtils.logInfo("  - " + clientId + ": " + status);
                }
            } else {
                PlatformUtils.logInfo("  (无客户端)");
            }
        } catch (Exception e) {
            PlatformUtils.logError("打印客户端列表失败", e);
        }
    }
    
    /**
     * 消息处理器示例类
     */
    public static class MessageHandlerExample {
        
        /**
         * 处理文本消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.TEXT})
        public void handleTextMessage(Message message) {
            PlatformUtils.logInfo("📝 收到文本消息: " + message.getContent());
            PlatformUtils.logInfo("   来自: " + message.getFromClientId());
            PlatformUtils.logInfo("   时间: " + new java.util.Date((long)(message.getTimestamp() * 1000)));
        }
        
        /**
         * 处理JSON消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.JSON})
        public void handleJsonMessage(Message message) {
            PlatformUtils.logInfo("🔍 收到JSON消息: " + message.getContent());
            PlatformUtils.logInfo("   来自: " + message.getFromClientId());
        }
        
        /**
         * 处理字典消息
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.DICT})
        public void handleDictMessage(Message message) {
            PlatformUtils.logInfo("📋 收到字典消息: " + message.getContent());
            PlatformUtils.logInfo("   来自: " + message.getFromClientId());
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
                
                PlatformUtils.logInfo("💾 收到字节消息: " + decodedContent);
                PlatformUtils.logInfo("   原始长度: " + decodedBytes.length + " 字节");
                PlatformUtils.logInfo("   来自: " + message.getFromClientId());
            } catch (Exception e) {
                PlatformUtils.logError("解码字节消息失败", e);
            }
        }
        
        /**
         * 处理文件消息（自动下载到downloads/files目录）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.FILE}, downloadDirectory = "./downloads/files")
        public void handleFileMessage(Message message) {
            PlatformUtils.logInfo("📁 收到文件消息: " + message.getContent());
            PlatformUtils.logInfo("   OSS Key: " + message.getOssKey());
            PlatformUtils.logInfo("   来自: " + message.getFromClientId());
            // content字段现在包含下载后的本地文件路径
        }
        
        /**
         * 处理图片消息（自动下载到downloads/images目录）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.IMAGE}, downloadDirectory = "./downloads/images")
        public void handleImageMessage(Message message) {
            PlatformUtils.logInfo("🖼️ 收到图片消息: " + message.getContent());
            PlatformUtils.logInfo("   OSS Key: " + message.getOssKey());
            PlatformUtils.logInfo("   来自: " + message.getFromClientId());
            // content字段现在包含下载后的本地图片路径
        }
        
        /**
         * 处理文件夹消息（自动下载到downloads/folders目录）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.FOLDER}, downloadDirectory = "./downloads/folders")
        public void handleFolderMessage(Message message) {
            PlatformUtils.logInfo("📂 收到文件夹消息: " + message.getContent());
            PlatformUtils.logInfo("   OSS Key: " + message.getOssKey());
            PlatformUtils.logInfo("   来自: " + message.getFromClientId());
            // content字段现在包含下载后的本地文件夹路径
        }
        
        /**
         * 处理文件消息但不自动下载（只显示OSS信息）
         * 
         * @param message 消息对象
         */
        @MessageListener(msgType = {CommMsgType.FILE, CommMsgType.IMAGE, CommMsgType.FOLDER})
        public void handleFileMessageInfo(Message message) {
            PlatformUtils.logInfo("ℹ️ 文件消息信息 - 类型: " + message.getMsgType().getValue() + 
                       ", OSS Key: " + message.getOssKey() + ", 来自: " + message.getFromClientId());
        }
        
        /**
         * 处理特定客户端的消息（Python客户端）
         * 
         * @param message 消息对象
         */
        @MessageListener(fromClientId = "python_client")
        public void handlePythonClientMessage(Message message) {
            PlatformUtils.logInfo("🐍 收到来自Python客户端的消息: " + message.getContent() + 
                       " (" + message.getMsgType().getValue() + ")");
        }
        
        /**
         * 处理所有以"python"开头的客户端消息
         * 
         * @param message 消息对象
         */
        @MessageListener
        public void handlePythonMessages(Message message) {
            if (message.getFromClientId().toLowerCase().contains("python")) {
                PlatformUtils.logInfo("🐍 收到Python相关客户端消息: " + message.getContent() + 
                           " 来自 " + message.getFromClientId());
            }
        }
        
        /**
         * 通用消息处理器（处理所有消息类型）- 设置为debug级别避免过多输出
         * 
         * @param message 消息对象
         */
        @MessageListener // 不指定过滤条件，监听所有消息
        public void handleAllMessages(Message message) {
            PlatformUtils.logDebug("[通用处理器] " + message.getMsgType().getValue() + ": " + 
                        message.getContent() + " 来自 " + message.getFromClientId());
        }
    }
} 