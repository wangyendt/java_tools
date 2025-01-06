package com.wayne.android_example;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.wayne.aliyun_oss.AliyunOSS;
import com.wayne.lark_bot.LarkBot;
import com.wayne.lark_bot.PostContent;
import com.wayne.lark_bot.TextContent;
import com.wayne.lark_custom_bot.LarkCustomBot;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private TextView tvLog;
    private ExecutorService executor;
    
    // 配置信息
    private static final String LARK_BOT_WEBHOOK = "xxx";
    private static final String LARK_BOT_SECRET = "xxx";
    private static final String LARK_BOT_APP_ID = "xxx";
    private static final String LARK_BOT_APP_SECRET = "xxx";
    
    private static final String OSS_ENDPOINT = "xxx";
    private static final String OSS_BUCKET = "xxx";
    private static final String OSS_API_KEY = "xxx";
    private static final String OSS_API_SECRET = "xxx";

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST = 2;
    private String selectedImagePath;
    private Runnable pendingOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查权限
        checkPermissions();

        executor = Executors.newSingleThreadExecutor();
        tvLog = findViewById(R.id.tvLog);

        // Lark Bot 测试
        Button btnTestLarkBot = findViewById(R.id.btnTestLarkBot);
        btnTestLarkBot.setOnClickListener(v -> testLarkBot());

        Button btnTestLarkBotImage = findViewById(R.id.btnTestLarkBotImage);
        btnTestLarkBotImage.setOnClickListener(v -> testLarkBotImage());

        // Lark Custom Bot 测试
        Button btnTestLarkCustomBot = findViewById(R.id.btnTestLarkCustomBot);
        btnTestLarkCustomBot.setOnClickListener(v -> testLarkCustomBot());

        // Aliyun OSS 测试
        Button btnTestOSSUpload = findViewById(R.id.btnTestOSSUpload);
        btnTestOSSUpload.setOnClickListener(v -> testOSSUpload());

        Button btnTestOSSDownload = findViewById(R.id.btnTestOSSDownload);
        btnTestOSSDownload.setOnClickListener(v -> testOSSDownload());

        Button btnTestOSSList = findViewById(R.id.btnTestOSSList);
        btnTestOSSList.setOnClickListener(v -> testOSSList());

        Button btnTestOSSDelete = findViewById(R.id.btnTestOSSDelete);
        btnTestOSSDelete.setOnClickListener(v -> testOSSDelete());
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingOperation != null) {
                    pendingOperation.run();
                    pendingOperation = null;
                }
            } else {
                log("需要存储权限才能访问文件");
            }
        }
    }

    private void log(String message) {
        runOnUiThread(() -> {
            tvLog.append(message + "\n");
        });
    }

    private void testLarkBot() {
        executor.execute(() -> {
            try {
                LarkBot bot = new LarkBot(LARK_BOT_APP_ID, LARK_BOT_APP_SECRET);
                
                // 1. 获取群组列表
                log("\n1. 获取群组列表");
                List<Map<String, Object>> groupList = bot.getGroupList();
                for (Map<String, Object> group : groupList) {
                    log("群组: " + group);
                }

                // 2. 获取特定群组的ID
                log("\n2. 获取特定群组的ID");
                List<String> groupChatIds = bot.getGroupChatIdByName("测试3");
                if (groupChatIds.isEmpty()) {
                    log("未找到群组");
                    return;
                }
                String groupChatId = groupChatIds.get(0);
                log("群组ID: " + groupChatId);

                // 3. 获取群成员信息
                log("\n3. 获取群成员信息");
                List<Map<String, Object>> members = bot.getMembersInGroupByGroupChatId(groupChatId);
                for (Map<String, Object> member : members) {
                    log("成员: " + member);
                }

                // 4. 获取特定成员的open_id
                log("\n4. 获取特定成员的open_id");
                List<String> memberOpenIds = bot.getMemberOpenIdByName(groupChatId, "王也");
                if (memberOpenIds.isEmpty()) {
                    log("未找到指定成员");
                    return;
                }
                String specificMemberUserOpenId = memberOpenIds.get(0);
                log("成员open_id: " + specificMemberUserOpenId);

                // 5. 获取用户信息
                log("\n5. 获取用户信息");
                List<Map<String, Object>> userInfos = bot.getUserInfo(Arrays.asList(), Arrays.asList("13267080069"));
                if (userInfos.isEmpty()) {
                    log("未找到用户信息");
                    return;
                }
                Map<String, Object> userInfo = userInfos.get(0);
                String userOpenId = (String) userInfo.get("user_id");
                log("用户信息: " + userInfo);

                // 6. 发送文本消息
                log("\n6. 发送文本消息");
                // 6.1 发送普通文本消息
                String textResponse = bot.sendTextToUser(userOpenId, "Hello, this is a single chat.\nYou know?");
                log("发送文本消息响应: " + textResponse);

                // 6.2 发送带格式的文本消息
                StringBuilder someText = new StringBuilder();
                someText.append(TextContent.makeAtSomeonePattern(specificMemberUserOpenId, "hi", "haha"));
                someText.append(TextContent.makeAtAllPattern());
                someText.append(TextContent.makeBoldPattern("notice"));
                someText.append(TextContent.makeItalianPattern("italian"));
                someText.append(TextContent.makeUnderlinePattern("underline"));
                someText.append(TextContent.makeDeleteLinePattern("delete line"));
                someText.append(TextContent.makeUrlPattern("www.baidu.com", "百度"));

                String formattedTextResponse = bot.sendTextToChat(groupChatId, "Hi, this is a group.\n" + someText.toString());
                log("发送格式化文本消息响应: " + formattedTextResponse);

                // 7. 上传和发送图片
                log("\n7. 上传和发送图片");
                File imageFile = new File(getExternalFilesDir(null), "test_image.jpg");
                // TODO: 确保test_image.jpg存在
                String imageKey = bot.uploadImage(imageFile.getAbsolutePath());
                if (!imageKey.isEmpty()) {
                    String imageToUserResponse = bot.sendImageToUser(userOpenId, imageKey);
                    log("发送图片到用户响应: " + imageToUserResponse);

                    String imageToChatResponse = bot.sendImageToChat(groupChatId, imageKey);
                    log("发送图片到群组响应: " + imageToChatResponse);
                }

                // 8. 分享群组和用户
                log("\n8. 分享群组和用户");
                String shareChatToUserResponse = bot.sendSharedChatToUser(userOpenId, groupChatId);
                log("分享群组到用户响应: " + shareChatToUserResponse);

                String shareChatToChatResponse = bot.sendSharedChatToChat(groupChatId, groupChatId);
                log("分享群组到群组响应: " + shareChatToChatResponse);

                String shareUserToUserResponse = bot.sendSharedUserToUser(userOpenId, userOpenId);
                log("分享用户到用户响应: " + shareUserToUserResponse);

                String shareUserToChatResponse = bot.sendSharedUserToChat(groupChatId, userOpenId);
                log("分享用户到群组响应: " + shareUserToChatResponse);

                // 9. 上传和发送文件
                log("\n9. 上传和发送文件");
                File testFile = new File(getExternalFilesDir(null), "test.txt");
                Files.write(testFile.toPath(), "Test content".getBytes());
                String fileKey = bot.uploadFile(testFile.getAbsolutePath(), "stream");
                if (!fileKey.isEmpty()) {
                    String fileToUserResponse = bot.sendFileToUser(userOpenId, fileKey);
                    log("发送文件到用户响应: " + fileToUserResponse);

                    String fileToChatResponse = bot.sendFileToChat(groupChatId, fileKey);
                    log("发送文件到群组响应: " + fileToChatResponse);
                }

                // 10. 发送富文本消息
                log("\n10. 发送富文本消息");
                Map<String, Object> post = new HashMap<>();
                post.put("title", "我是标题");
                List<List<Map<String, Object>>> content = new ArrayList<>();

                PostContent postContent = new PostContent("我是标题");

                // 添加文本内容
                List<Map<String, Object>> line1 = new ArrayList<>();
                Map<String, Object> text1 = postContent.makeTextContent("这是第一行", Arrays.asList("bold"), false);
                line1.add(text1);
                content.add(line1);

                // 添加@提醒
                List<Map<String, Object>> line3 = new ArrayList<>();
                Map<String, Object> at = postContent.makeAtContent(specificMemberUserOpenId, Arrays.asList("bold", "italic"));
                line3.add(at);
                content.add(line3);

                // 添加表情和Markdown
                List<Map<String, Object>> line4 = new ArrayList<>();
                Map<String, Object> emoji = postContent.makeEmojiContent("OK");
                line4.add(emoji);

                Map<String, Object> markdown = postContent.makeMarkdownContent("**helloworld**");
                line4.add(markdown);
                content.add(line4);

                // 添加代码块
                List<Map<String, Object>> line6 = new ArrayList<>();
                Map<String, Object> code = postContent.makeCodeBlockContent("swift", "print(\"Hello, World!\")");
                line6.add(code);
                content.add(line6);

                post.put("content", content);
                String postResponse = bot.sendPostToChat(groupChatId, post);
                log("发送富文本消息响应: " + postResponse);

                log("\n所有测试完成");
            } catch (Exception e) {
                log("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingOperation = this::openImagePicker;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                selectedImagePath = getPathFromUri(uri);
                if (selectedImagePath != null) {
                    log("已选择图片：" + selectedImagePath);
                    // 如果是从图片选择器返回，立即发送图片
                    testLarkBotImage();
                }
            } catch (Exception e) {
                log("获取图片路径失败: " + e.getMessage());
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    private void testLarkBotImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingOperation = this::testLarkBotImage;
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
                return;
            }
        }

        executor.execute(() -> {
            try {
                if (selectedImagePath == null) {
                    runOnUiThread(() -> openImagePicker());
                    return;
                }

                File imageFile = new File(selectedImagePath);
                if (!imageFile.exists() || !imageFile.canRead()) {
                    log("无法访问图片文件：" + selectedImagePath);
                    return;
                }

                LarkBot bot = new LarkBot(LARK_BOT_APP_ID, LARK_BOT_APP_SECRET);
                
                // 先获取群组ID
                List<String> groupChatIds = bot.getGroupChatIdByName("测试3");
                if (groupChatIds.isEmpty()) {
                    log("未找到目标群组");
                    return;
                }
                String groupChatId = groupChatIds.get(0);
                log("找到群组ID: " + groupChatId);

                // 上传并发送图片
                String imageKey = bot.uploadImage(selectedImagePath);
                if (imageKey != null && !imageKey.isEmpty()) {
                    bot.sendImageToChat(groupChatId, imageKey);
                    log("发送图片消息成功");
                } else {
                    log("图片上传失败");
                }
                selectedImagePath = null;
            } catch (Exception e) {
                log("发送图片消息失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void testLarkCustomBot() {
        executor.execute(() -> {
            try {
                LarkCustomBot bot = new LarkCustomBot(LARK_BOT_WEBHOOK, LARK_BOT_SECRET, LARK_BOT_APP_ID, LARK_BOT_APP_SECRET);

                // 1. 发送文本消息
                log("\n1. 发送文本消息");
                bot.sendText("Hello, this is a text message", false);
                log("发送文本消息成功");

                // 2. 发送富文本消息
                log("\n2. 发送富文本消息");
                List<List<JSONObject>> content = new ArrayList<>();

                PostContent postContent = new PostContent("Post Content");

                // 添加文本内容
                List<JSONObject> line1 = new ArrayList<>();
                Map<String, Object> textContent = postContent.makeTextContent("这是第一行", Arrays.asList("bold"), false);
                line1.add(new JSONObject(textContent));
                content.add(line1);

                // 添加表情和Markdown
                List<JSONObject> line2 = new ArrayList<>();
                Map<String, Object> emojiContent = postContent.makeEmojiContent("OK");
                line2.add(new JSONObject(emojiContent));
                Map<String, Object> markdownContent = postContent.makeMarkdownContent("**helloworld**");
                line2.add(new JSONObject(markdownContent));
                content.add(line2);

                // 添加代码块
                List<JSONObject> line3 = new ArrayList<>();
                Map<String, Object> codeContent = postContent.makeCodeBlockContent("swift", "print(\"Hello, World!\")");
                line3.add(new JSONObject(codeContent));
                content.add(line3);

                // 发送富文本消息
                bot.sendPost(content, "我是标题");
                log("发送富文本消息成功");

                // 3. 发送图片消息
                log("\n3. 发送图片消息");
                if (selectedImagePath == null) {
                    runOnUiThread(() -> openImagePicker());
                    return;
                }

                File imageFile = new File(selectedImagePath);
                if (!imageFile.exists() || !imageFile.canRead()) {
                    log("无法访问图片文件：" + selectedImagePath);
                    return;
                }

                String imageKey = bot.uploadImage(selectedImagePath);
                if (!imageKey.isEmpty()) {
                    bot.sendImage(imageKey);
                    log("发送图片消息成功");
                } else {
                    log("图片上传失败");
                }
                selectedImagePath = null;

                log("\n所有测试完成");
            } catch (Exception e) {
                log("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void testOSSUpload() {
        executor.execute(() -> {
            try {
                AliyunOSS oss = new AliyunOSS(OSS_ENDPOINT, OSS_BUCKET, OSS_API_KEY, OSS_API_SECRET);

                // 创建测试文件
                File testFile = new File(getExternalFilesDir(null), "test.txt");
                Files.write(testFile.toPath(), "Hello, World!".getBytes());

                // 创建测试目录
                File testDir = new File(getExternalFilesDir(null), "test_dir");
                testDir.mkdirs();
                Files.write(new File(testDir, "file1.txt").toPath(), "File 1".getBytes());
                Files.write(new File(testDir, "file2.txt").toPath(), "File 2".getBytes());
                File subDir = new File(testDir, "subdir");
                subDir.mkdirs();
                Files.write(new File(subDir, "file3.txt").toPath(), "File 3".getBytes());

                // 1. 上传文件
                log("\n1. 测试上传文件");
                oss.uploadFile("test.txt", testFile.getAbsolutePath());
                oss.uploadFile("1/test.txt", testFile.getAbsolutePath());
                oss.uploadFile("1/test2.txt", testFile.getAbsolutePath());
                oss.uploadFile("2/test3.txt", testFile.getAbsolutePath());
                oss.uploadFile("2/test4.txt", testFile.getAbsolutePath());

                // 2. 上传目录
                log("\n2. 测试上传目录");
                oss.uploadDirectory(testDir.getAbsolutePath(), "test_dir");

                // 3. 上传文本
                log("\n3. 测试上传文本");
                oss.uploadText("hello.txt", "Hello, World!");
                oss.uploadText("test.txt", "Hello, World!");

                log("上传测试完成");
            } catch (IOException e) {
                log("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void testOSSList() {
        executor.execute(() -> {
            try {
                AliyunOSS oss = new AliyunOSS(OSS_ENDPOINT, OSS_BUCKET, OSS_API_KEY, OSS_API_SECRET);

                // 4. 列举所有文件
                log("\n4. 测试列举文件");
                List<String> files = oss.listAllKeys();
                log("文件列表：");
                for (String file : files) {
                    log("  - " + file);
                }

                // 5. 列举指定前缀的文件
                log("\n5. 测试列举指定前缀的文件");
                List<String> filesWithPrefix1 = oss.listKeysWithPrefix("1/");
                log("前缀为 '1/' 的文件列表：");
                for (String file : filesWithPrefix1) {
                    log("  - " + file);
                }

                // 6. 测试列举目录内容
                log("\n6. 测试列举目录内容");
                // 列举根目录
                log("根目录内容：");
                List<AliyunOSS.DirectoryItem> rootContents = oss.listDirectoryContents("");
                for (AliyunOSS.DirectoryItem item : rootContents) {
                    log("  " + (item.isDirectory() ? "📁" : "📄") + " " + item.getName() + (item.isDirectory() ? "/" : ""));
                }

                // 列举 test_dir 目录
                log("\ntest_dir 目录内容：");
                List<AliyunOSS.DirectoryItem> testDirContents = oss.listDirectoryContents("test_dir");
                for (AliyunOSS.DirectoryItem item : testDirContents) {
                    log("  " + (item.isDirectory() ? "📁" : "📄") + " " + item.getName() + (item.isDirectory() ? "/" : ""));
                }

                // 列举 micro_hand_gesture/raw_data 目录
                log("\nmicro_hand_gesture/raw_data 目录内容：");
                List<AliyunOSS.DirectoryItem> handGestureContents = oss.listDirectoryContents("micro_hand_gesture/raw_data");
                for (AliyunOSS.DirectoryItem item : handGestureContents) {
                    log("  " + (item.isDirectory() ? "📁" : "📄") + " " + item.getName() + (item.isDirectory() ? "/" : ""));
                }

                log("列举测试完成");
            } catch (IOException e) {
                log("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void testOSSDelete() {
        executor.execute(() -> {
            try {
                AliyunOSS oss = new AliyunOSS(OSS_ENDPOINT, OSS_BUCKET, OSS_API_KEY, OSS_API_SECRET);

                // 1. 删除单个文件
                log("\n1. 测试删除单个文件");
                oss.deleteFile("test.txt");
                oss.deleteFile("hello.txt");
                log("删除单个文件完成");

                // 2. 删除指定前缀的文件
                log("\n2. 测试删除指定前缀的文件");
                oss.deleteFilesWithPrefix("1/");
                oss.deleteFilesWithPrefix("2/");
                oss.deleteFilesWithPrefix("test_dir/");
                log("删除指定前缀文件完成");

                log("删除测试完成");
            } catch (IOException e) {
                log("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void testOSSDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingOperation = this::testOSSDownload;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
                return;
            }
        }

        executor.execute(() -> {
            try {
                AliyunOSS oss = new AliyunOSS(OSS_ENDPOINT, OSS_BUCKET, OSS_API_KEY, OSS_API_SECRET);
                File downloadDir = new File(getExternalFilesDir(null), "downloads");
                if (!downloadDir.exists()) {
                    if (!downloadDir.mkdirs()) {
                        log("创建下载目录失败");
                        return;
                    }
                }

                // 7. 下载文件
                log("\n7. 测试下载文件");
                File defaultDownloadFile = new File(downloadDir, "test.txt");
                if (!defaultDownloadFile.getParentFile().exists()) {
                    defaultDownloadFile.getParentFile().mkdirs();
                }
                oss.downloadFile("test.txt", defaultDownloadFile.getAbsolutePath());

                File test1File = new File(downloadDir, "test1.txt");
                if (!test1File.getParentFile().exists()) {
                    test1File.getParentFile().mkdirs();
                }
                oss.downloadFile("1/test.txt", test1File.getAbsolutePath());

                // 8. 下载目录
                log("\n8. 测试下载目录");
                File testDirDownload = new File(downloadDir, "test_dir");
                if (!testDirDownload.exists()) {
                    if (!testDirDownload.mkdirs()) {
                        log("创建test_dir目录失败");
                        return;
                    }
                }
                oss.downloadDirectory("test_dir/", testDirDownload.getAbsolutePath());

                // 9. 下载指定前缀的文件
                log("\n9. 测试下载指定前缀的文件");
                File prefix2Dir = new File(downloadDir, "2");
                if (!prefix2Dir.exists()) {
                    if (!prefix2Dir.mkdirs()) {
                        log("创建prefix2目录失败");
                        return;
                    }
                }
                oss.downloadFilesWithPrefix("2/", prefix2Dir.getAbsolutePath());

                log("下载测试完成");
            } catch (IOException e) {
                log("测试失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}