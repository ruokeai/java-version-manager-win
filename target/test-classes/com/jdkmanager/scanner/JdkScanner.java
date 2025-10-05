package com.jdkmanager.scanner;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * JDK扫描器接口
 * 定义了JDK扫描和管理的核心功能
 */
public interface JdkScanner {
    
    /**
     * 扫描所有已知路径中的JDK
     * @return 发现的JDK列表
     */
    List<JdkInfo> scanForJdks();
    
    /**
     * 异步扫描JDK
     * @return CompletableFuture包含发现的JDK列表
     */
    CompletableFuture<List<JdkInfo>> scanForJdksAsync();
    
    /**
     * 添加自定义扫描路径
     * @param path 要添加的路径
     * @return 如果路径已存在返回false，添加成功返回true
     */
    boolean addCustomPath(Path path);
    
    /**
     * 移除自定义扫描路径
     * @param path 要移除的路径
     * @return 如果路径不存在返回false，移除成功返回true
     */
    boolean removeCustomPath(Path path);
    
    /**
     * 获取所有自定义路径
     * @return 自定义路径列表
     */
    List<Path> getCustomPaths();
    
    /**
     * 验证给定路径是否为有效JDK
     * @param path 要验证的路径
     * @return 如果是有效JDK返回true
     */
    boolean validateJdk(Path path);
    
    /**
     * 扫描单个目录中的JDK
     * @param directory 要扫描的目录
     * @return 发现的JDK列表
     */
    List<JdkInfo> scanDirectory(Path directory);
    
    /**
     * 检测当前激活的JDK
     * @param jdks JDK列表
     */
    void detectCurrentJdk(List<JdkInfo> jdks);
}