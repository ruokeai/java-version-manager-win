package com.jdkmanager.exception;

/**
 * JDK管理相关异常基类
 * 所有JDK管理器相关的自定义异常都应该继承此类
 */
public class JdkManagerException extends Exception {
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public JdkManagerException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public JdkManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}