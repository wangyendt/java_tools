package com.wayne.aliyun_oss;

import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AliyunOSS {
    private final String endpoint;
    private final String bucketName;
    private final String apiKey;
    private final String apiSecret;
    private final OkHttpClient client;
    private final boolean verbose;

    public AliyunOSS(String endpoint, String bucketName, String apiKey, String apiSecret) {
        this(endpoint, bucketName, apiKey, apiSecret, true);
    }

    public AliyunOSS(String endpoint, String bucketName, String apiKey, String apiSecret, boolean verbose) {
        this.endpoint = endpoint;
        this.bucketName = bucketName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.verbose = verbose;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    private void printInfo(String message) {
        if (verbose) {
            System.out.println("\u001B[32m" + message + "\u001B[0m");
        }
    }

    private void printWarning(String message) {
        if (verbose) {
            System.out.println("\u001B[33mWARNING: " + message + "\u001B[0m");
        }
    }

    private void printError(String message) {
        if (verbose) {
            System.err.println("\u001B[31mERROR: " + message + "\u001B[0m");
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA1"));
            byte[] signData = mac.doFinal(content.getBytes());
            return java.util.Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("签名失败", e);
        }
    }

    private String getAuthorizationHeader(String method, String contentType, String contentMD5, String date, String resource) {
        String canonicalizedResource = resource.isEmpty() ? "/" + bucketName + "/" : "/" + bucketName + "/" + resource;
        String stringToSign = String.join("\n",
                method,
                contentMD5,
                contentType,
                date,
                canonicalizedResource
        );
        String signature = sign(stringToSign);
        return "OSS " + apiKey + ":" + signature;
    }

    private String getDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }

    private String getBaseURL() {
        return "https://" + bucketName + "." + endpoint;
    }

    public boolean uploadFile(String key, String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            printWarning("文件不存在：" + filePath);
            return false;
        }

        byte[] fileData = Files.readAllBytes(file.toPath());
        
        // 计算MD5并进行Base64编码
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md.digest(fileData);
            String contentMD5 = java.util.Base64.getEncoder().encodeToString(md5Bytes);
            
            String date = getDate();
            String authorization = getAuthorizationHeader(
                    "PUT",
                    "application/octet-stream",
                    contentMD5,
                    date,
                    key
            );

            RequestBody requestBody = RequestBody.create(fileData, MediaType.parse("application/octet-stream"));
            Request request = new Request.Builder()
                    .url(getBaseURL() + "/" + key)
                    .put(requestBody)
                    .addHeader("Content-Type", "application/octet-stream")
                    .addHeader("Content-MD5", contentMD5)
                    .addHeader("Date", date)
                    .addHeader("Authorization", authorization)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    printInfo("成功上传文件：" + key);
                    return true;
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                    printWarning("上传文件失败：" + errorMessage);
                    return false;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            printError("计算MD5失败：" + e.getMessage());
            return false;
        }
    }

    public boolean uploadText(String key, String text) throws IOException {
        byte[] textData = text.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5Bytes = md.digest(textData);
            String contentMD5 = java.util.Base64.getEncoder().encodeToString(md5Bytes);
            
            String date = getDate();
            String authorization = getAuthorizationHeader(
                    "PUT",
                    "text/plain",
                    contentMD5,
                    date,
                    key
            );

            RequestBody requestBody = RequestBody.create(textData, MediaType.parse("text/plain"));
            Request request = new Request.Builder()
                    .url(getBaseURL() + "/" + key)
                    .put(requestBody)
                    .addHeader("Content-Type", "text/plain")
                    .addHeader("Content-MD5", contentMD5)
                    .addHeader("Date", date)
                    .addHeader("Authorization", authorization)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    printInfo("成功上传文本：" + key);
                    return true;
                } else {
                    String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                    printWarning("上传文本失败：" + errorMessage);
                    return false;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            printError("计算MD5失败：" + e.getMessage());
            return false;
        }
    }

    public boolean downloadFile(String key) throws IOException {
        return downloadFile(key, null);
    }

    public boolean downloadFile(String key, String rootDir) throws IOException {
        String savePath = rootDir != null ? Paths.get(rootDir, key).toString() : key;

        // 创建必要的目录
        Path savePathObj = Paths.get(savePath);
        Files.createDirectories(savePathObj.getParent());

        String date = getDate();
        String authorization = getAuthorizationHeader("GET", "", "", date, key);

        Request request = new Request.Builder()
                .url(getBaseURL() + "/" + key)
                .get()
                .addHeader("Date", date)
                .addHeader("Authorization", authorization)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                Files.write(savePathObj, response.body().bytes());
                printInfo("成功下载文件：" + key + " -> " + savePath);
                return true;
            } else {
                String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                printWarning("下载文件失败：" + errorMessage);
                return false;
            }
        }
    }

    public List<String> listAllKeys() throws IOException {
        return listKeysWithPrefix("");
    }

    public List<String> listKeysWithPrefix(String prefix) throws IOException {
        String date = getDate();
        String authorization = getAuthorizationHeader("GET", "", "", date, "");

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getBaseURL() + "/").newBuilder()
                .addQueryParameter("max-keys", "1000");
        if (!prefix.isEmpty()) {
            urlBuilder.addQueryParameter("prefix", prefix);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("Date", date)
                .addHeader("Authorization", authorization)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String xmlString = response.body().string();
                return extractKeysFromXML(xmlString);
            } else {
                String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                printWarning("获取文件列表失败：" + errorMessage);
                return new ArrayList<>();
            }
        }
    }

    private List<String> extractKeysFromXML(String xmlString) {
        List<String> keys = new ArrayList<>();
        int index = 0;
        while (true) {
            int keyStart = xmlString.indexOf("<Key>", index);
            if (keyStart == -1) break;
            int keyEnd = xmlString.indexOf("</Key>", keyStart);
            if (keyEnd == -1) break;
            String key = xmlString.substring(keyStart + 5, keyEnd);
            keys.add(key);
            index = keyEnd + 6;
        }
        return keys;
    }

    public boolean deleteFile(String key) throws IOException {
        String date = getDate();
        String authorization = getAuthorizationHeader("DELETE", "", "", date, key);

        Request request = new Request.Builder()
                .url(getBaseURL() + "/" + key)
                .delete()
                .addHeader("Date", date)
                .addHeader("Authorization", authorization)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                printInfo("成功删除文件：" + key);
                return true;
            } else {
                String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                printWarning("删除文件失败：" + errorMessage);
                return false;
            }
        }
    }

    public boolean deleteFilesWithPrefix(String prefix) throws IOException {
        List<String> keys = listKeysWithPrefix(prefix);
        boolean success = true;
        for (String key : keys) {
            if (!deleteFile(key)) {
                success = false;
            }
        }
        return success;
    }

    public boolean uploadDirectory(String localPath, String prefix) throws IOException {
        File dir = new File(localPath);
        if (!dir.exists() || !dir.isDirectory()) {
            printWarning("无法访问目录：" + localPath);
            return false;
        }

        boolean success = true;
        List<File> files = Files.walk(dir.toPath())
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .collect(Collectors.toList());

        for (File file : files) {
            String relativePath = dir.toPath().relativize(file.toPath()).toString();
            String key = prefix.isEmpty() ? relativePath : Paths.get(prefix, relativePath).toString();
            if (!uploadFile(key, file.getAbsolutePath())) {
                success = false;
            }
        }

        return success;
    }

    public boolean downloadDirectory(String prefix, String localPath) throws IOException {
        List<String> keys = listKeysWithPrefix(prefix);
        if (keys.isEmpty()) {
            printWarning("未找到前缀为 " + prefix + " 的文件");
            return false;
        }

        boolean success = true;
        for (String key : keys) {
            if (!downloadFile(key, localPath)) {
                success = false;
            }
        }

        return success;
    }

    public static class DirectoryItem {
        private final String name;
        private final boolean isDirectory;

        public DirectoryItem(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }

        public String getName() {
            return name;
        }

        public boolean isDirectory() {
            return isDirectory;
        }
    }

    public List<DirectoryItem> listDirectoryContents(String prefix) throws IOException {
        List<String> allKeys = listKeysWithPrefix(prefix);
        Map<String, Boolean> contents = new HashMap<>();

        String normalizedPrefix = prefix.isEmpty() ? "" : prefix.endsWith("/") ? prefix : prefix + "/";
        
        for (String key : allKeys) {
            if (!normalizedPrefix.isEmpty() && !key.startsWith(normalizedPrefix)) {
                continue;
            }

            String relativePath = key.substring(normalizedPrefix.length());
            String[] parts = relativePath.split("/");
            
            if (parts.length > 0 && !parts[0].isEmpty()) {
                if (parts.length == 1) {
                    // 这是一个文件
                    contents.put(parts[0], false);
                } else {
                    // 这是一个目录
                    contents.put(parts[0], true);
                }
            }
        }

        return contents.entrySet().stream()
                .map(entry -> new DirectoryItem(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> {
                    if (a.isDirectory != b.isDirectory) {
                        return a.isDirectory ? -1 : 1;
                    }
                    return a.getName().compareTo(b.getName());
                })
                .collect(Collectors.toList());
    }

    public boolean downloadFilesWithPrefix(String prefix, String rootDir) throws IOException {
        List<String> files = listKeysWithPrefix(prefix);
        boolean allSuccess = true;
        for (String file : files) {
            if (!downloadFile(file, rootDir)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    public String readFileContent(String key) throws IOException {
        String date = getDate();
        String authorization = getAuthorizationHeader("GET", "", "", date, key);

        // 检查是否为文件夹（通过检查是否以'/'结尾）
        if (key.endsWith("/")) {
            printWarning("指定的键值 '" + key + "' 是一个文件夹");
            return null;
        }

        // 检查是否有子文件（通过列举带有分隔符的对象）
        String date2 = getDate();
        String authorization2 = getAuthorizationHeader("GET", "", "", date2, "");
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getBaseURL() + "/").newBuilder()
                .addQueryParameter("prefix", key + "/")
                .addQueryParameter("delimiter", "/")
                .addQueryParameter("max-keys", "1");

        Request listRequest = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("Date", date2)
                .addHeader("Authorization", authorization2)
                .build();

        try (Response listResponse = client.newCall(listRequest).execute()) {
            if (listResponse.isSuccessful() && listResponse.body() != null) {
                String xmlString = listResponse.body().string();
                if (xmlString.contains("<Contents>")) {
                    printWarning("指定的键值 '" + key + "' 是一个文件夹");
                    return null;
                }
            }
        }

        // 获取文件内容
        Request request = new Request.Builder()
                .url(getBaseURL() + "/" + key)
                .get()
                .addHeader("Date", date)
                .addHeader("Authorization", authorization)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String content = response.body().string();
                printInfo("成功读取文件内容：" + key);
                return content;
            } else {
                String errorMessage = response.body() != null ? response.body().string() : "未知错误";
                if (response.code() == 404) {
                    printWarning("文件不存在：" + key);
                } else {
                    printWarning("读取文件失败：" + errorMessage);
                }
                return null;
            }
        }
    }
} 