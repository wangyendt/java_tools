package com.wayne.larkbot;

public class TextContent {
    
    public static String makeAtAllPattern() {
        return "<at user_id=\"all\"></at>";
    }
    
    public static String makeAtSomeonePattern(String someoneOpenId, String username, String idType) {
        String mentionType;
        switch (idType) {
            case "open_id":
                mentionType = "user_id";
                break;
            case "union_id":
                mentionType = "union_id";
                break;
            case "user_id":
                mentionType = "user_id";
                break;
            default:
                mentionType = "user_id";
                break;
        }
        
        return String.format("<at %s=\"%s\">%s</at>", mentionType, someoneOpenId, username);
    }
    
    public static String makeBoldPattern(String content) {
        return String.format("<b>%s</b>", content);
    }
    
    public static String makeItalianPattern(String content) {
        return String.format("<i>%s</i>", content);
    }
    
    public static String makeUnderlinePattern(String content) {
        return String.format("<u>%s</u>", content);
    }
    
    public static String makeDeleteLinePattern(String content) {
        return String.format("<s>%s</s>", content);
    }
    
    public static String makeUrlPattern(String url, String text) {
        return String.format("[%s](%s)", text, url);
    }
}
