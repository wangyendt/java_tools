package com.wayne.android_example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wayne.cross_comm.CommMsgType;
import com.wayne.cross_comm.CrossCommService;
import com.wayne.cross_comm.Message;
import com.wayne.cross_comm.MessageListener;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Android跨语言通信示例
 * 
 * @author Wayne
 */
public class CrossCommActivity extends AppCompatActivity {
    
    private static final String TAG = "CrossCommActivity";
    
    private EditText etServerIp;
    private EditText etServerPort;
    private EditText etMessage;
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnSendMessage;
    private Button btnListClients;
    private TextView tvStatus;
    private TextView tvMessages;
    
    private CrossCommService crossCommService;
    private Handler mainHandler;
    private ExecutorService executor;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cross_comm);
        
        initServices();  // 先初始化服务（包括mainHandler）
        initViews();     // 再初始化视图（会调用updateConnectionStatus）
        setupListeners();
    }
    
    private void initViews() {
        etServerIp = findViewById(R.id.et_server_ip);
        etServerPort = findViewById(R.id.et_server_port);
        etMessage = findViewById(R.id.et_message);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnSendMessage = findViewById(R.id.btn_send_message);
        btnListClients = findViewById(R.id.btn_list_clients);
        tvStatus = findViewById(R.id.tv_status);
        tvMessages = findViewById(R.id.tv_messages);
        
        // 设置默认值
        etServerIp.setText("localhost");  // 替换为你的服务器IP
        etServerPort.setText("9898");
        etMessage.setText("Hello from Android!");
        
        // 现在 mainHandler 已经初始化了，可以安全调用
        updateConnectionStatus(false);
    }
    
    private void initServices() {
        mainHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newCachedThreadPool();
    }
    
    private void setupListeners() {
        btnConnect.setOnClickListener(v -> connectToServer());
        btnDisconnect.setOnClickListener(v -> disconnectFromServer());
        btnSendMessage.setOnClickListener(v -> sendMessage());
        btnListClients.setOnClickListener(v -> listClients());
    }
    
    private void connectToServer() {
        String serverIp = etServerIp.getText().toString().trim();
        String serverPortStr = etServerPort.getText().toString().trim();
        
        if (serverIp.isEmpty() || serverPortStr.isEmpty()) {
            showToast("请输入服务器地址和端口");
            return;
        }
        
        try {
            int serverPort = Integer.parseInt(serverPortStr);
            
            executor.execute(() -> {
                try {
                    Log.i(TAG, "正在连接到服务器: " + serverIp + ":" + serverPort);
                    updateStatusOnMainThread("正在连接...");
                    
                    // 创建CrossCommService实例
                    crossCommService = new CrossCommService(
                            serverIp, serverPort, "android_client", 30,
                            "xxx","xxx","xxx","xxx"
                    );
                    
                    // 注册消息处理器
                    MessageHandlerExample handler = new MessageHandlerExample();
                    crossCommService.registerMessageHandlers(handler);
                    
                    // 连接到服务器
                    boolean connected = crossCommService.connect();
                    
                    if (connected) {
                        Log.i(TAG, "连接成功，客户端ID: " + crossCommService.getClientId());
                        updateStatusOnMainThread("已连接 (ID: " + crossCommService.getClientId() + ")");
                        updateConnectionStatus(true);
                        showToastOnMainThread("连接成功！");
                        
                        // 发送上线通知
                        crossCommService.sendMessage("Android客户端已上线", CommMsgType.TEXT);
                    } else {
                        Log.e(TAG, "连接失败");
                        updateStatusOnMainThread("连接失败");
                        updateConnectionStatus(false);
                        showToastOnMainThread("连接失败，请检查服务器地址");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "连接异常", e);
                    updateStatusOnMainThread("连接异常: " + e.getMessage());
                    updateConnectionStatus(false);
                    showToastOnMainThread("连接异常: " + e.getMessage());
                }
            });
            
        } catch (NumberFormatException e) {
            showToast("端口号格式错误");
        }
    }
    
    private void disconnectFromServer() {
        if (crossCommService != null && crossCommService.isConnected()) {
            executor.execute(() -> {
                try {
                    Log.i(TAG, "断开连接");
                    updateStatusOnMainThread("正在断开连接...");
                    
                    // 发送下线通知
                    crossCommService.sendMessage("Android客户端下线", CommMsgType.TEXT);
                    
                    // 断开连接
                    crossCommService.disconnect();
                    
                    updateStatusOnMainThread("已断开连接");
                    updateConnectionStatus(false);
                    showToastOnMainThread("已断开连接");
                } catch (Exception e) {
                    Log.e(TAG, "断开连接异常", e);
                    updateStatusOnMainThread("断开连接异常: " + e.getMessage());
                    showToastOnMainThread("断开连接异常: " + e.getMessage());
                }
            });
        }
    }
    
    private void sendMessage() {
        if (crossCommService == null || !crossCommService.isConnected()) {
            showToast("请先连接到服务器");
            return;
        }
        
        String message = etMessage.getText().toString().trim();
        if (message.isEmpty()) {
            showToast("请输入消息内容");
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.i(TAG, "发送消息: " + message);
                boolean success = crossCommService.sendMessage(message, CommMsgType.TEXT);
                
                if (success) {
                    appendMessageOnMainThread("[发送] " + message);
                    showToastOnMainThread("消息发送成功");
                } else {
                    showToastOnMainThread("消息发送失败");
                }
            } catch (Exception e) {
                Log.e(TAG, "发送消息异常", e);
                showToastOnMainThread("发送消息异常: " + e.getMessage());
            }
        });
    }
    
    private void listClients() {
        if (crossCommService == null || !crossCommService.isConnected()) {
            showToast("请先连接到服务器");
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.i(TAG, "获取客户端列表");
                Map<String, Object> clients = crossCommService.listOnlineClients(5);
                
                if (clients != null) {
                    int totalCount = (Integer) clients.get("total_count");
                    String message = "在线客户端数量: " + totalCount;
                    appendMessageOnMainThread("[系统] " + message);
                    showToastOnMainThread(message);
                } else {
                    showToastOnMainThread("获取客户端列表失败");
                }
            } catch (Exception e) {
                Log.e(TAG, "获取客户端列表异常", e);
                showToastOnMainThread("获取客户端列表异常: " + e.getMessage());
            }
        });
    }
    
    private void updateStatusOnMainThread(String status) {
        mainHandler.post(() -> tvStatus.setText("状态: " + status));
    }
    
    private void updateConnectionStatus(boolean connected) {
        mainHandler.post(() -> {
            btnConnect.setEnabled(!connected);
            btnDisconnect.setEnabled(connected);
            btnSendMessage.setEnabled(connected);
            btnListClients.setEnabled(connected);
        });
    }
    
    private void appendMessageOnMainThread(String message) {
        mainHandler.post(() -> {
            String currentText = tvMessages.getText().toString();
            String newText = currentText + "\n" + message;
            tvMessages.setText(newText);
        });
    }
    
    private void showToastOnMainThread(String message) {
        mainHandler.post(() -> showToast(message));
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理资源
        if (crossCommService != null && crossCommService.isConnected()) {
            executor.execute(() -> crossCommService.disconnect());
        }
        
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    /**
     * 消息处理器示例类
     */
    public class MessageHandlerExample {
        
        /**
         * 处理文本消息
         */
        @MessageListener(msgType = {CommMsgType.TEXT})
        public void handleTextMessage(Message message) {
            String content = message.getContent().toString();
            String fromClient = message.getFromClientId();
            
            Log.i(TAG, "收到文本消息: " + content + " 来自: " + fromClient);
            appendMessageOnMainThread("[收到] " + fromClient + ": " + content);
        }
        
        /**
         * 处理JSON消息
         */
        @MessageListener(msgType = {CommMsgType.JSON})
        public void handleJsonMessage(Message message) {
            String content = message.getContent().toString();
            String fromClient = message.getFromClientId();
            
            Log.i(TAG, "收到JSON消息: " + content + " 来自: " + fromClient);
            appendMessageOnMainThread("[JSON] " + fromClient + ": " + content);
        }
        
        /**
         * 处理字典消息
         */
        @MessageListener(msgType = {CommMsgType.DICT})
        public void handleDictMessage(Message message) {
            String content = message.getContent().toString();
            String fromClient = message.getFromClientId();
            
            Log.i(TAG, "收到字典消息: " + content + " 来自: " + fromClient);
            appendMessageOnMainThread("[字典] " + fromClient + ": " + content);
        }
        
        /**
         * 处理字节数据消息
         */
        @MessageListener(msgType = {CommMsgType.BYTES})
        public void handleBytesMessage(Message message) {
            try {
                // 解码Base64字符串
                byte[] decodedBytes = android.util.Base64.decode(
                    message.getContent().toString(), android.util.Base64.DEFAULT);
                String decodedContent = new String(decodedBytes);
                String fromClient = message.getFromClientId();
                
                Log.i(TAG, "收到字节消息: " + decodedContent + " 来自: " + fromClient);
                appendMessageOnMainThread("[字节] " + fromClient + ": " + decodedContent);
            } catch (Exception e) {
                Log.e(TAG, "解码字节消息失败", e);
            }
        }
        
        /**
         * 处理文件消息
         */
        @MessageListener(msgType = {CommMsgType.FILE})
        public void handleFileMessage(Message message) {
            String ossKey = message.getOssKey();
            String fromClient = message.getFromClientId();
            
            Log.i(TAG, "收到文件消息: " + ossKey + " 来自: " + fromClient);
            appendMessageOnMainThread("[文件] " + fromClient + ": " + ossKey);
        }
        
        /**
         * 处理图片消息
         */
        @MessageListener(msgType = {CommMsgType.IMAGE})
        public void handleImageMessage(Message message) {
            String ossKey = message.getOssKey();
            String fromClient = message.getFromClientId();
            
            Log.i(TAG, "收到图片消息: " + ossKey + " 来自: " + fromClient);
            appendMessageOnMainThread("[图片] " + fromClient + ": " + ossKey);
        }
        
        /**
         * 处理文件夹消息
         */
        @MessageListener(msgType = {CommMsgType.FOLDER})
        public void handleFolderMessage(Message message) {
            String ossKey = message.getOssKey();
            String fromClient = message.getFromClientId();
            
            Log.i(TAG, "收到文件夹消息: " + ossKey + " 来自: " + fromClient);
            appendMessageOnMainThread("[文件夹] " + fromClient + ": " + ossKey);
        }
        
        /**
         * 处理来自Python客户端的特殊消息
         */
        @MessageListener(fromClientId = "python_client")
        public void handlePythonClientMessage(Message message) {
            String content = message.getContent().toString();
            String msgType = message.getMsgType().getValue();
            
            Log.i(TAG, "收到Python客户端消息: " + content + " 类型: " + msgType);
            appendMessageOnMainThread("[Python] " + content + " (" + msgType + ")");
        }
    }
} 