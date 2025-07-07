package com.wayne.cross_comm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 消息结构体
 * 
 * @author Wayne
 * @since 1.0.0
 */
public class Message {
    
    @JsonProperty("msg_id")
    private String msgId;
    
    @JsonProperty("from_client_id")
    private String fromClientId;
    
    @JsonProperty("to_client_id")
    private String toClientId;
    
    @JsonProperty("msg_type")
    private CommMsgType msgType;  // 直接存储枚举，使用@JsonValue序列化
    
    @JsonProperty("content")
    private Object content;
    
    @JsonProperty("timestamp")
    private double timestamp;
    
    @JsonProperty("oss_key")
    private String ossKey;  // 兼容Python版本的OSS文件传输字段
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 默认构造函数，用于JSON反序列化
    public Message() {
    }
    
    public Message(String msgId, String fromClientId, String toClientId, 
                  CommMsgType msgType, Object content, double timestamp) {
        this.msgId = msgId;
        this.fromClientId = fromClientId;
        this.toClientId = toClientId;
        this.msgType = msgType;
        this.content = content;
        this.timestamp = timestamp;
        this.ossKey = null;  // 默认为null
    }
    
    // Getters and Setters
    public String getMsgId() {
        return msgId;
    }
    
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
    
    public String getFromClientId() {
        return fromClientId;
    }
    
    public void setFromClientId(String fromClientId) {
        this.fromClientId = fromClientId;
    }
    
    public String getToClientId() {
        return toClientId;
    }
    
    public void setToClientId(String toClientId) {
        this.toClientId = toClientId;
    }
    
    public CommMsgType getMsgType() {
        return this.msgType;
    }
    
    public void setMsgType(CommMsgType msgType) {
        this.msgType = msgType;
    }
    
    public void setMsgType(String msgTypeStr) {
        this.msgType = CommMsgType.fromValue(msgTypeStr);
    }
    
    public Object getContent() {
        return content;
    }
    
    public void setContent(Object content) {
        this.content = content;
    }
    
    public double getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getOssKey() {
        return ossKey;
    }
    
    public void setOssKey(String ossKey) {
        this.ossKey = ossKey;
    }
    
    /**
     * 将消息转换为JSON字符串
     * 
     * @return JSON字符串
     * @throws JsonProcessingException 序列化失败时抛出
     */
    public String toJson() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }
    
    /**
     * 从JSON字符串创建消息对象
     * 
     * @param json JSON字符串
     * @return 消息对象
     * @throws JsonProcessingException 反序列化失败时抛出
     */
    public static Message fromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, Message.class);
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "msgId='" + msgId + '\'' +
                ", fromClientId='" + fromClientId + '\'' +
                ", toClientId='" + toClientId + '\'' +
                ", msgType=" + msgType +
                ", content=" + content +
                ", timestamp=" + timestamp +
                ", ossKey='" + ossKey + '\'' +
                '}';
    }
} 