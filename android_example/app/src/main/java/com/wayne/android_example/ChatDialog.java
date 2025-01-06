package com.wayne.android_example;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.wayne.openai.OpenAI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatDialog extends Dialog {
    private static final String TAG = "ChatDialog";
    private RecyclerView recyclerView;
    private EditText editText;
    private Button sendButton;
    private ChatAdapter adapter;
    private OpenAI openAI;
    private List<OpenAI.ChatMessage> messageHistory = new ArrayList<>();

    private static final String API_KEY = "xxx";
    private static final String BASE_URL = "https://api.deepseek.com/v1";

    public ChatDialog(@NonNull Context context) {
        super(context);
        openAI = new OpenAI(BASE_URL, API_KEY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_chat);

        recyclerView = findViewById(R.id.recyclerView);
        editText = findViewById(R.id.editText);
        sendButton = findViewById(R.id.sendButton);

        adapter = new ChatAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> {
            String message = editText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                editText.setText("");
            }
        });
    }

    private void sendMessage(String message) {
        // 添加用户消息到界面
        adapter.addMessage(new ChatMessage("user", message));

        // 添加用户消息到历史记录
        messageHistory.add(OpenAI.ChatMessage.builder()
                .role("user")
                .content(message)
                .build());

        // 添加助手消息占位符到界面
        adapter.addMessage(new ChatMessage("assistant", ""));

        // 在后台线程中调用API
        new Thread(() -> {
            try {
                StringBuilder responseContent = new StringBuilder();
                String[] currentResponse = {""};  // 使用数组来存储当前的完整响应
                
                openAI.createStreamingChatCompletion(
                    messageHistory,
                    "deepseek-chat",
                    content -> {
                        Log.d(TAG, "Received content: " + content);
                        responseContent.append(content);
                        String newResponse = responseContent.toString();
                        
                        // 只更新新增的内容
                        if (!newResponse.equals(currentResponse[0])) {
                            currentResponse[0] = newResponse;
                            recyclerView.post(() -> adapter.updateLastMessage(newResponse));
                        }
                    }
                );

                // 添加助手消息到历史记录
                messageHistory.add(OpenAI.ChatMessage.builder()
                        .role("assistant")
                        .content(responseContent.toString())
                        .build());

            } catch (IOException e) {
                Log.e(TAG, "Error during API call: " + e.getMessage(), e);
                recyclerView.post(() -> adapter.updateLastMessage("Error: " + e.getMessage()));
            }
        }).start();
    }
} 