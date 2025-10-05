package com.jdkmanager.exception;

/**
 * 权限不足异常
 * 当操作需要更高权限但当前权限不足时抛出此异常
 */
public class InsufficientPrivilegeException extends EnvironmentVariableException {
    
    /**
     * 构造函数
     * @param message 异常消息
     */
    public InsufficientPrivilegeException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * @param message 异常消息
     * @param cause 原因异常
     */
    public InsufficientPrivilegeException(String message, Throwable cause) {
        super(message, cause);
    }
}