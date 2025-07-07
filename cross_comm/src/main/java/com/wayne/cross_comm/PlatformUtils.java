package com.wayne.cross_comm;

import java.io.File;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;

/**
 * 平台适配工具类
 * 处理Android和桌面平台的差异
 * 
 * @author Wayne
 * @since 1.0.0
 */
public class PlatformUtils {
    
    private static final String TAG = "CrossComm";
    private static boolean isAndroid = false;
    private static Class<?> androidLogClass = null;
    
    static {
        // 检测是否在Android环境
        try {
            androidLogClass = Class.forName("android.util.Log");
            isAndroid = true;
        } catch (ClassNotFoundException e) {
            isAndroid = false;
        }
    }
    
    /**
     * 检查是否在Android平台运行
     * 
     * @return 是否为Android平台
     */
    public static boolean isAndroid() {
        return isAndroid;
    }
    
    /**
     * 统一的日志接口 - INFO级别
     * 
     * @param message 日志消息
     */
    public static void logInfo(String message) {
        if (isAndroid) {
            try {
                Method method = androidLogClass.getMethod("i", String.class, String.class);
                method.invoke(null, TAG, message);
            } catch (Exception e) {
                System.out.println("[INFO] " + message);
            }
        } else {
            try {
                // 使用SLF4J
                Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
                Method getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String.class);
                Object logger = getLoggerMethod.invoke(null, "CrossCommService");
                
                Class<?> loggerClass = Class.forName("org.slf4j.Logger");
                Method infoMethod = loggerClass.getMethod("info", String.class);
                infoMethod.invoke(logger, message);
            } catch (Exception e) {
                System.out.println("[INFO] " + message);
            }
        }
    }
    
    /**
     * 统一的日志接口 - ERROR级别
     * 
     * @param message 日志消息
     */
    public static void logError(String message) {
        if (isAndroid) {
            try {
                Method method = androidLogClass.getMethod("e", String.class, String.class);
                method.invoke(null, TAG, message);
            } catch (Exception e) {
                System.err.println("[ERROR] " + message);
            }
        } else {
            try {
                // 使用SLF4J
                Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
                Method getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String.class);
                Object logger = getLoggerMethod.invoke(null, "CrossCommService");
                
                Class<?> loggerClass = Class.forName("org.slf4j.Logger");
                Method errorMethod = loggerClass.getMethod("error", String.class);
                errorMethod.invoke(logger, message);
            } catch (Exception e) {
                System.err.println("[ERROR] " + message);
            }
        }
    }
    
    /**
     * 统一的日志接口 - ERROR级别（带异常）
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void logError(String message, Throwable throwable) {
        if (isAndroid) {
            try {
                Method method = androidLogClass.getMethod("e", String.class, String.class, Throwable.class);
                method.invoke(null, TAG, message, throwable);
            } catch (Exception e) {
                System.err.println("[ERROR] " + message);
                throwable.printStackTrace();
            }
        } else {
            try {
                // 使用SLF4J
                Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
                Method getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String.class);
                Object logger = getLoggerMethod.invoke(null, "CrossCommService");
                
                Class<?> loggerClass = Class.forName("org.slf4j.Logger");
                Method errorMethod = loggerClass.getMethod("error", String.class, Throwable.class);
                errorMethod.invoke(logger, message, throwable);
            } catch (Exception e) {
                System.err.println("[ERROR] " + message);
                throwable.printStackTrace();
            }
        }
    }
    
    /**
     * 统一的日志接口 - WARN级别
     * 
     * @param message 日志消息
     */
    public static void logWarn(String message) {
        if (isAndroid) {
            try {
                Method method = androidLogClass.getMethod("w", String.class, String.class);
                method.invoke(null, TAG, message);
            } catch (Exception e) {
                System.out.println("[WARN] " + message);
            }
        } else {
            try {
                // 使用SLF4J
                Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
                Method getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String.class);
                Object logger = getLoggerMethod.invoke(null, "CrossCommService");
                
                Class<?> loggerClass = Class.forName("org.slf4j.Logger");
                Method warnMethod = loggerClass.getMethod("warn", String.class);
                warnMethod.invoke(logger, message);
            } catch (Exception e) {
                System.out.println("[WARN] " + message);
            }
        }
    }
    
    /**
     * 统一的日志接口 - DEBUG级别
     * 
     * @param message 日志消息
     */
    public static void logDebug(String message) {
        if (isAndroid) {
            try {
                Method method = androidLogClass.getMethod("d", String.class, String.class);
                method.invoke(null, TAG, message);
            } catch (Exception e) {
                // Debug级别在控制台不输出
            }
        } else {
            try {
                // 使用SLF4J
                Class<?> loggerFactoryClass = Class.forName("org.slf4j.LoggerFactory");
                Method getLoggerMethod = loggerFactoryClass.getMethod("getLogger", String.class);
                Object logger = getLoggerMethod.invoke(null, "CrossCommService");
                
                Class<?> loggerClass = Class.forName("org.slf4j.Logger");
                Method debugMethod = loggerClass.getMethod("debug", String.class);
                debugMethod.invoke(logger, message);
            } catch (Exception e) {
                // Debug级别在控制台不输出
            }
        }
    }
    
    /**
     * 获取文件名（兼容File API）
     * 
     * @param filePath 文件路径
     * @return 文件名
     */
    public static String getFileName(String filePath) {
        File file = new File(filePath);
        return file.getName();
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }
    
    /**
     * 检查是否为目录
     * 
     * @param dirPath 目录路径
     * @return 是否为目录
     */
    public static boolean isDirectory(String dirPath) {
        File file = new File(dirPath);
        return file.exists() && file.isDirectory();
    }
    
    /**
     * 创建目录
     * 
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public static boolean createDirectories(String dirPath) {
        File file = new File(dirPath);
        return file.mkdirs() || file.exists();
    }
    
    /**
     * 生成客户端唯一ID
     * 
     * @return 客户端ID
     */
    public static String generateClientId() {
        try {
            // 尝试获取MAC地址
            StringBuilder macStr = new StringBuilder();
            
            if (isAndroid) {
                // Android上获取MAC地址需要特殊处理
                try {
                    // 在Android 6.0+上，MAC地址获取受限，使用设备ID
                    macStr.append("android_");
                } catch (Exception e) {
                    macStr.append("android_unknown_");
                }
            } else {
                // 桌面平台获取MAC地址
                try {
                    byte[] mac = NetworkInterface.getByInetAddress(
                        InetAddress.getLocalHost()).getHardwareAddress();
                    
                    if (mac != null) {
                        for (byte b : mac) {
                            macStr.append(String.format("%02x", b));
                        }
                        macStr.append("_");
                    }
                } catch (Exception e) {
                    macStr.append("unknown_");
                }
            }
            
            // 添加随机ID
            String randomId = UUID.randomUUID().toString().substring(0, 8);
            return macStr.toString() + randomId;
            
        } catch (Exception e) {
            // 如果所有方法都失败，使用UUID
            String platform = isAndroid ? "android" : "java";
            return platform + "_" + UUID.randomUUID().toString().substring(0, 16);
        }
    }
    
    /**
     * 获取Android权限状态（仅Android平台）
     * 
     * @param permission 权限名称
     * @return 权限是否已授权
     */
    public static boolean hasPermission(String permission) {
        if (!isAndroid) {
            return true; // 桌面平台不需要权限检查
        }
        
        try {
            // 在Android上检查权限需要Context，这里简化处理
            // 实际使用时需要传入Context参数
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取应用的缓存目录
     * 
     * @return 缓存目录路径
     */
    public static String getCacheDir() {
        if (isAndroid) {
            // Android上需要从Context获取，这里返回默认值
            return "/data/data/app_package/cache";
        } else {
            // 桌面平台使用用户临时目录
            return System.getProperty("java.io.tmpdir");
        }
    }
    
    /**
     * 简单的反射工具 - 获取所有方法
     * 
     * @param clazz 类
     * @return 方法数组
     */
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }
    
    /**
     * 检查方法是否有指定注解
     * 
     * @param method 方法
     * @param annotationClass 注解类
     * @return 是否有注解
     */
    public static boolean hasAnnotation(Method method, Class<?> annotationClass) {
        return method.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) annotationClass);
    }
    
    /**
     * 获取方法的注解
     * 
     * @param method 方法
     * @param annotationClass 注解类
     * @return 注解对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAnnotation(Method method, Class<T> annotationClass) {
        return (T) method.getAnnotation((Class<? extends java.lang.annotation.Annotation>) annotationClass);
    }
} 