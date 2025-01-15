package com.wayne.aliyun_oss;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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

            System.out.println("\n所有测试完成");
        } catch (IOException e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 