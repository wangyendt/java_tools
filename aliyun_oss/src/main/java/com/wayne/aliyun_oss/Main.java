package com.wayne.aliyun_oss;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class Main {
    private static final String OSS_ENDPOINT = "xxx";
    private static final String OSS_BUCKET = "xxx";
    private static final String OSS_API_KEY = "xxx";
    private static final String OSS_API_SECRET = "xxx";

    public static void main(String[] args) {
        try {
            AliyunOSS oss = new AliyunOSS(OSS_ENDPOINT, OSS_BUCKET, OSS_API_KEY, OSS_API_SECRET);

            // 创建测试文件
            File testFile = new File("test.txt");
            Files.write(testFile.toPath(), "Hello, World!".getBytes());

            // 创建测试目录
            File testDir = new File("test_dir");
            testDir.mkdirs();
            Files.write(new File(testDir, "file1.txt").toPath(), "File 1".getBytes());
            Files.write(new File(testDir, "file2.txt").toPath(), "File 2".getBytes());
            File subDir = new File(testDir, "subdir");
            subDir.mkdirs();
            Files.write(new File(subDir, "file3.txt").toPath(), "File 3".getBytes());

            // 1. 上传文件
            System.out.println("\n1. 测试上传文件");
            oss.uploadFile("test.txt", testFile.getAbsolutePath());
            oss.uploadFile("1/test.txt", testFile.getAbsolutePath());
            oss.uploadFile("1/test2.txt", testFile.getAbsolutePath());
            oss.uploadFile("2/test3.txt", testFile.getAbsolutePath());
            oss.uploadFile("2/test4.txt", testFile.getAbsolutePath());

            // 2. 上传目录
            System.out.println("\n2. 测试上传目录");
            oss.uploadDirectory(testDir.getAbsolutePath(), "test_dir");

            // 3. 上传文本
            System.out.println("\n3. 测试上传文本");
            oss.uploadText("hello.txt", "Hello, World!");
            oss.uploadText("test.txt", "Hello, World!");

            // 4. 列举所有文件
            System.out.println("\n4. 测试列举文件");
            List<String> files = oss.listAllKeys();
            System.out.println("文件列表：");
            for (String file : files) {
                System.out.println("  - " + file);
            }

            // 5. 列举指定前缀的文件
            System.out.println("\n5. 测试列举指定前缀的文件");
            List<String> filesWithPrefix1 = oss.listKeysWithPrefix("1/");
            System.out.println("前缀为 '1/' 的文件列表：");
            for (String file : filesWithPrefix1) {
                System.out.println("  - " + file);
            }

            // 6. 测试列举目录内容
            System.out.println("\n6. 测试列举目录内容");
            // 列举根目录
            System.out.println("根目录内容：");
            List<AliyunOSS.DirectoryItem> rootContents = oss.listDirectoryContents("");
            for (AliyunOSS.DirectoryItem item : rootContents) {
                System.out.println("  " + (item.isDirectory() ? "📁" : "📄") + " " + item.getName() + (item.isDirectory() ? "/" : ""));
            }

            // 列举 test_dir 目录
            System.out.println("\ntest_dir 目录内容：");
            List<AliyunOSS.DirectoryItem> testDirContents = oss.listDirectoryContents("test_dir");
            for (AliyunOSS.DirectoryItem item : testDirContents) {
                System.out.println("  " + (item.isDirectory() ? "📁" : "📄") + " " + item.getName() + (item.isDirectory() ? "/" : ""));
            }

            // 列举 micro_hand_gesture/raw_data 目录
            System.out.println("\nmicro_hand_gesture/raw_data 目录内容：");
            List<AliyunOSS.DirectoryItem> handGestureContents = oss.listDirectoryContents("micro_hand_gesture/raw_data");
            for (AliyunOSS.DirectoryItem item : handGestureContents) {
                System.out.println("  " + (item.isDirectory() ? "📁" : "📄") + " " + item.getName() + (item.isDirectory() ? "/" : ""));
            }

            // 7. 测试读取文件内容
            System.out.println("\n7. 测试读取文件内容");
            // 读取文本文件
            String content = oss.readFileContent("test.txt");
            if (content != null) {
                System.out.println("test.txt 的内容：\n" + content);
            }

            // 尝试读取文件夹（应该会失败）
            content = oss.readFileContent("test_dir/");
            if (content == null) {
                System.out.println("成功检测到文件夹，拒绝读取");
            }

            // 读取不存在的文件
            content = oss.readFileContent("nonexistent.txt");
            if (content == null) {
                System.out.println("成功检测到文件不存在");
            }

            // 8. 下载文件
            System.out.println("\n8. 测试下载文件");
            File downloadDir = new File("downloads");
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

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

            // 9. 下载目录
            System.out.println("\n9. 测试下载目录");
            File testDirDownload = new File(downloadDir, "test_dir");
            if (!testDirDownload.exists()) {
                testDirDownload.mkdirs();
            }
            oss.downloadDirectory("test_dir/", testDirDownload.getAbsolutePath());

            // 10. 下载指定前缀的文件
            System.out.println("\n10. 测试下载指定前缀的文件");
            File prefix2Dir = new File(downloadDir, "2");
            if (!prefix2Dir.exists()) {
                prefix2Dir.mkdirs();
            }
            oss.downloadFilesWithPrefix("2/", prefix2Dir.getAbsolutePath());

            // 11. 删除文件
            System.out.println("\n11. 测试删除文件");
            oss.deleteFile("test.txt");
            oss.deleteFile("hello.txt");

            // 12. 删除指定前缀的文件
            System.out.println("\n12. 测试删除指定前缀的文件");
            oss.deleteFilesWithPrefix("1/");
            oss.deleteFilesWithPrefix("2/");
            oss.deleteFilesWithPrefix("test_dir/");

            // 13. 测试新增的键值存在检查功能
            System.out.println("\n13. 测试键值存在检查");
            // 先上传一个测试文件
            oss.uploadText("key_exists_test.txt", "用于测试键值存在性的文件");
            // 检查存在的文件
            boolean keyExists = oss.keyExists("key_exists_test.txt");
            System.out.println("key_exists_test.txt 存在: " + keyExists);
            // 检查不存在的文件
            boolean keyNotExists = oss.keyExists("not_exists_file.txt");
            System.out.println("not_exists_file.txt 存在: " + keyNotExists);

            // 14. 测试获取文件元数据
            System.out.println("\n14. 测试获取文件元数据");
            Map<String, Object> metadata = oss.getFileMetadata("key_exists_test.txt");
            if (metadata != null) {
                System.out.println("文件元数据:");
                for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue());
                }
            }

            // 15. 测试复制对象
            System.out.println("\n15. 测试复制对象");
            // 复制文件
            boolean copySuccess = oss.copyObject("key_exists_test.txt", "key_exists_test_copy.txt");
            System.out.println("复制结果: " + copySuccess);
            // 验证复制后的文件内容
            String copiedContent = oss.readFileContent("key_exists_test_copy.txt");
            System.out.println("复制后的文件内容: " + copiedContent);

            // 16. 测试移动对象
            System.out.println("\n16. 测试移动对象");
            // 移动文件
            boolean moveSuccess = oss.moveObject("key_exists_test_copy.txt", "key_exists_test_moved.txt");
            System.out.println("移动结果: " + moveSuccess);
            // 验证源文件不存在
            boolean srcExists = oss.keyExists("key_exists_test_copy.txt");
            System.out.println("源文件仍然存在: " + srcExists);
            // 验证目标文件存在
            boolean destExists = oss.keyExists("key_exists_test_moved.txt");
            System.out.println("目标文件存在: " + destExists);

            // 17. 测试下载文件使用 useBasename=true
            System.out.println("\n17. 测试下载文件使用basename");
            File flatDir = new File(downloadDir, "flat");
            if (!flatDir.exists()) {
                flatDir.mkdirs();
            }
            // 使用 useBasename=true 下载文件
            oss.downloadFile("test_dir/subdir/file3.txt", flatDir.getAbsolutePath(), true);
            System.out.println("文件应当被下载为: " + new File(flatDir, "file3.txt").getAbsolutePath());
            
            // 18. 测试下载目录使用 useBasename=true
            System.out.println("\n18. 测试下载目录使用basename");
            File flatDir2 = new File(downloadDir, "flat2");
            if (!flatDir2.exists()) {
                flatDir2.mkdirs();
            }
            // 重新上传测试目录
            oss.uploadDirectory(testDir.getAbsolutePath(), "test_dir_download");
            // 使用 useBasename=true 下载目录
            oss.downloadDirectory("test_dir_download/", flatDir2.getAbsolutePath(), true);
            System.out.println("所有文件应当被平铺下载到: " + flatDir2.getAbsolutePath());

            // 清理测试文件
            oss.deleteFile("key_exists_test.txt");
            oss.deleteFile("key_exists_test_moved.txt");
            oss.deleteFilesWithPrefix("test_dir_download/");

            System.out.println("\n所有测试完成");
        } catch (IOException e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 