package com.jdkmanager.exception;

/**
 * 环境变量操作异常
 * 当环境变量读写操作失败时抛出此异常
 */
public class EnvironmentVariableException extends JdkManagerException {
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public EnvironmentVariableException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public EnvironmentVariableException(String message, Throwable cause) {
        super(message, cause);
    }
}