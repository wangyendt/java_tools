package com.wayne.larkbot;

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

    public void addContentInLine(Map<String, Object> content) {
        List<List<Map<String, Object>>> contents = getContentsList();
        if (contents.isEmpty()) {
            contents.add(new ArrayList<>());
        }
        contents.get(contents.size() - 1).add(content);
    }

    public void addContentsInLine(List<Map<String, Object>> contents) {
        List<List<Map<String, Object>>> existingContents = getContentsList();
        if (existingContents.isEmpty()) {
            existingContents.add(new ArrayList<>());
        }
        existingContents.get(existingContents.size() - 1).addAll(contents);
    }

    public void addContentInNewLine(Map<String, Object> content) {
        List<List<Map<String, Object>>> contents = getContentsList();
        List<Map<String, Object>> newLine = new ArrayList<>();
        newLine.add(content);
        contents.add(newLine);
    }

    public void addContentsInNewLine(List<Map<String, Object>> contents) {
        List<List<Map<String, Object>>> existingContents = getContentsList();
        existingContents.add(contents);
    }

    private List<List<Map<String, Object>>> getContentsList() {
        Map<String, Object> zhCnContent = content.get("zh_cn");
        if (zhCnContent != null) {
            Object contentObj = zhCnContent.get("content");
            if (contentObj instanceof List) {
                return (List<List<Map<String, Object>>>) contentObj;
            }
        }
        return new ArrayList<>();
    }
}
