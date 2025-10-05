package com.jdkmanager.env;

/**
 * 环境变量作用域枚举
 * 定义了环境变量的不同作用域级别
 */
public enum EnvVariableScope {
    /**
     * 用户级环境变量
     * 不需要管理员权限
     * 存储在 HKEY_CURRENT_USER\Environment
     */
    USER("用户级", "HKEY_CURRENT_USER\\Environment"),
    
    /**
     * 系统级环境变量
     * 需要管理员权限
     * 存储在 HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment
     */
    SYSTEM("系统级", "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment");
    
    private final String displayName;
    private final String registryPath;
    
    /**
     * 构造函数
     * @param displayName 显示名称
     * @param registryPath 注册表路径
     */
    EnvVariableScope(String displayName, String registryPath) {
        this.displayName = displayName;
        this.registryPath = registryPath;
    }
    
    /**
     * 获取显示名称
     * @return 显示名称
     */
    public String getDisplayName() { 
        return displayName; 
    }
    
    /**
     * 获取注册表路径
     * @return 注册表路径
     */
    public String getRegistryPath() { 
        return registryPath; 
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}