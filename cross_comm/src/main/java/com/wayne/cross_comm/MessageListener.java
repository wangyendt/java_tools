package com.wayne.cross_comm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 消息监听器注解
 * 类似于Python版本中的装饰器，用于标注消息处理方法
 * 
 * @author Wayne
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageListener {
    
    /**
     * 监听的消息类型，默认为所有类型
     * 
     * @return 消息类型数组
     */
    CommMsgType[] msgType() default {};
    
    /**
     * 监听特定发送方的消息，默认为所有发送方
     * 
     * @return 发送方客户端ID
     */
    String fromClientId() default "";
    
    /**
     * 文件下载目录（仅对FILE、IMAGE、FOLDER类型消息有效）
     * 如果为空字符串，文件消息不会自动下载，只传递OSS key
     * 
     * @return 下载目录路径
     */
    String downloadDirectory() default "";
} 