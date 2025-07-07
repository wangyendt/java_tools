package com.wayne.cross_comm;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 消息类型枚举
 * 
 * @author Wayne
 * @since 1.0.0
 */
public enum CommMsgType {
    /**
     * 文本消息
     */
    TEXT("text"),
    
    /**
     * JSON消息
     */
    JSON("json"),
    
    /**
     * 字典/映射消息
     */
    DICT("dict"),
    
    /**
     * 字节数据消息
     */
    BYTES("bytes"),
    
    /**
     * 图片消息
     */
    IMAGE("image"),
    
    /**
     * 文件消息
     */
    FILE("file"),
    
    /**
     * 文件夹消息
     */
    FOLDER("folder"),
    
    /**
     * 心跳消息
     */
    HEARTBEAT("heartbeat"),
    
    /**
     * 登录消息
     */
    LOGIN("login"),
    
    /**
     * 登出消息
     */
    LOGOUT("logout"),
    
    /**
     * 列出客户端请求
     */
    LIST_CLIENTS("list_clients"),
    
    /**
     * 列出客户端响应
     */
    LIST_CLIENTS_RESPONSE("list_clients_response"),
    
    /**
     * 登录响应
     */
    LOGIN_RESPONSE("login_response");
    
    private final String value;
    
    CommMsgType(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    /**
     * 根据字符串值获取枚举实例
     * 
     * @param value 字符串值
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 如果找不到对应的枚举值
     */
    public static CommMsgType fromValue(String value) {
        for (CommMsgType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的消息类型: " + value);
    }
} 