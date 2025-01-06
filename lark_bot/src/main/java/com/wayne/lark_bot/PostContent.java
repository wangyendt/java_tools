package com.wayne.lark_bot;

import java.util.*;

public class PostContent {
    private final Map<String, Map<String, Object>> content;

    public PostContent(String title) {
        this.content = new HashMap<>();
        Map<String, Object> zhCn = new HashMap<>();
        zhCn.put("title", title);
        zhCn.put("content", new ArrayList<List<Map<String, Object>>>());
        this.content.put("zh_cn", zhCn);
    }

    public Map<String, Map<String, Object>> getContent() {
        return content;
    }

    public void setTitle(String title) {
        Map<String, Object> zhCnContent = content.get("zh_cn");
        if (zhCnContent != null) {
            zhCnContent.put("title", title);
        }
    }

    public static List<String> listTextStyles() {
        return Arrays.asList("bold", "underline", "lineThrough", "italic");
    }

    public Map<String, Object> makeTextContent(String text, List<String> styles, boolean unescape) {
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("tag", "text");
        textContent.put("text", text);
        textContent.put("style", styles != null ? styles : new ArrayList<>());
        textContent.put("unescape", unescape);
        return textContent;
    }

    public Map<String, Object> makeLinkContent(String text, String link, List<String> styles) {
        Map<String, Object> linkContent = new HashMap<>();
        linkContent.put("tag", "a");
        linkContent.put("text", text);
        linkContent.put("href", link);
        linkContent.put("style", styles != null ? styles : new ArrayList<>());
        return linkContent;
    }

    public Map<String, Object> makeAtContent(String atUserId, List<String> styles) {
        Map<String, Object> atContent = new HashMap<>();
        atContent.put("tag", "at");
        atContent.put("user_id", atUserId);
        atContent.put("style", styles != null ? styles : new ArrayList<>());
        return atContent;
    }

    public Map<String, Object> makeImageContent(String imageKey) {
        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("tag", "img");
        imageContent.put("image_key", imageKey);
        return imageContent;
    }

    public Map<String, Object> makeMediaContent(String fileKey, String imageKey) {
        Map<String, Object> mediaContent = new HashMap<>();
        mediaContent.put("tag", "media");
        mediaContent.put("image_key", imageKey);
        mediaContent.put("file_key", fileKey);
        return mediaContent;
    }

    public Map<String, Object> makeEmojiContent(String emojiType) {
        Map<String, Object> emojiContent = new HashMap<>();
        emojiContent.put("tag", "emotion");
        emojiContent.put("emoji_type", emojiType);
        return emojiContent;
    }

    public Map<String, Object> makeHrContent() {
        Map<String, Object> hrContent = new HashMap<>();
        hrContent.put("tag", "hr");
        return hrContent;
    }

    public Map<String, Object> makeCodeBlockContent(String language, String text) {
        Map<String, Object> codeContent = new HashMap<>();
        codeContent.put("tag", "code_block");
        codeContent.put("language", language);
        codeContent.put("text", text);
        return codeContent;
    }

    public Map<String, Object> makeMarkdownContent(String mdText) {
        Map<String, Object> markdownContent = new HashMap<>();
        markdownContent.put("tag", "md");
        markdownContent.put("text", mdText);
        return markdownContent;
    }

    @SuppressWarnings("unchecked")
    public void addContentInLine(Map<String, Object> content) {
        Map<String, Object> zhCnContent = this.content.get("zh_cn");
        if (zhCnContent != null) {
            List<List<Map<String, Object>>> contentList = (List<List<Map<String, Object>>>) zhCnContent.get("content");
            if (contentList.isEmpty()) {
                contentList.add(new ArrayList<>());
            }
            contentList.get(contentList.size() - 1).add(content);
        }
    }

    @SuppressWarnings("unchecked")
    public void addContentsInLine(List<Map<String, Object>> contents) {
        Map<String, Object> zhCnContent = this.content.get("zh_cn");
        if (zhCnContent != null) {
            List<List<Map<String, Object>>> contentList = (List<List<Map<String, Object>>>) zhCnContent.get("content");
            if (contentList.isEmpty()) {
                contentList.add(new ArrayList<>());
            }
            contentList.get(contentList.size() - 1).addAll(contents);
        }
    }

    @SuppressWarnings("unchecked")
    public void addContentInNewLine(Map<String, Object> content) {
        Map<String, Object> zhCnContent = this.content.get("zh_cn");
        if (zhCnContent != null) {
            List<List<Map<String, Object>>> contentList = (List<List<Map<String, Object>>>) zhCnContent.get("content");
            List<Map<String, Object>> newLine = new ArrayList<>();
            newLine.add(content);
            contentList.add(newLine);
        }
    }

    @SuppressWarnings("unchecked")
    public void addContentsInNewLine(List<Map<String, Object>> contents) {
        Map<String, Object> zhCnContent = this.content.get("zh_cn");
        if (zhCnContent != null) {
            List<List<Map<String, Object>>> contentList = (List<List<Map<String, Object>>>) zhCnContent.get("content");
            contentList.add(contents);
        }
    }
}
