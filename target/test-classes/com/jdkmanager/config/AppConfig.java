package com.jdkmanager.config;

import com.jdkmanager.env.EnvVariableScope;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * 应用程序配置管理
 * 负责保存和加载用户偏好设置
 */
public class AppConfig {
    private static final String CUSTOM_PATHS_KEY = "custom_paths";
    private static final String LAST_SELECTED_SCOPE_KEY = "last_selected_scope";
    private static final String WINDOW_WIDTH_KEY = "window_width";
    private static final String WINDOW_HEIGHT_KEY = "window_height";
    private static final String WINDOW_X_KEY = "window_x";
    private static final String WINDOW_Y_KEY = "window_y";
    
    private final Preferences preferences;
    
    /**
     * 构造函数
     */
    public AppConfig() {
        this.preferences = Preferences.userNodeForPackage(AppConfig.class);
    }
    
    /**
     * 保存自定义扫描路径
     * @param paths 路径列表
     */
    public void saveCustomPaths(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            preferences.remove(CUSTOM_PATHS_KEY);
            return;
        }
        
        String pathArray = paths.stream()
            .map(Path::toString)
            .collect(Collectors.joining(";"));
        preferences.put(CUSTOM_PATHS_KEY, pathArray);
    }
    
    /**
     * 加载自定义扫描路径
     * @return 路径列表
     */
    public List<Path> loadCustomPaths() {
        String pathsString = preferences.get(CUSTOM_PATHS_KEY, "");
        if (pathsString == null || pathsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        return Arrays.stream(pathsString.split(";"))
            .filter(s -> s != null && !s.trim().isEmpty())
            .map(String::trim)
            .map(Paths::get)
            .collect(Collectors.toList());
    }
    
    /**
     * 保存最后选择的环境变量作用域
     * @param scope 作用域
     */
    public void saveLastSelectedScope(EnvVariableScope scope) {
        if (scope != null) {
            preferences.put(LAST_SELECTED_SCOPE_KEY, scope.name());
        }
    }
    
    /**
     * 加载最后选择的环境变量作用域
     * @return 作用域，默认为USER
     */
    public EnvVariableScope loadLastSelectedScope() {
        String scopeName = preferences.get(LAST_SELECTED_SCOPE_KEY, EnvVariableScope.USER.name());
        try {
            return EnvVariableScope.valueOf(scopeName);
        } catch (IllegalArgumentException e) {
            return EnvVariableScope.USER;
        }
    }
    
    /**
     * 保存窗口尺寸
     * @param width 窗口宽度
     * @param height 窗口高度
     */
    public void saveWindowSize(double width, double height) {
        preferences.putDouble(WINDOW_WIDTH_KEY, width);
        preferences.putDouble(WINDOW_HEIGHT_KEY, height);
    }
    
    /**
     * 保存窗口位置
     * @param x 窗口X坐标
     * @param y 窗口Y坐标
     */
    public void saveWindowPosition(double x, double y) {
        preferences.putDouble(WINDOW_X_KEY, x);
        preferences.putDouble(WINDOW_Y_KEY, y);
    }
    
    /**
     * 加载窗口宽度
     * @return 窗口宽度
     */
    public double loadWindowWidth() {
        return preferences.getDouble(WINDOW_WIDTH_KEY, 800.0);
    }
    
    /**
     * 加载窗口高度
     * @return 窗口高度
     */
    public double loadWindowHeight() {
        return preferences.getDouble(WINDOW_HEIGHT_KEY, 600.0);
    }
    
    /**
     * 加载窗口X坐标
     * @return 窗口X坐标
     */
    public double loadWindowX() {
        return preferences.getDouble(WINDOW_X_KEY, -1.0); // -1表示居中
    }
    
    /**
     * 加载窗口Y坐标
     * @return 窗口Y坐标
     */
    public double loadWindowY() {
        return preferences.getDouble(WINDOW_Y_KEY, -1.0); // -1表示居中
    }
    
    /**
     * 保存窗口状态
     * @param width 窗口宽度
     * @param height 窗口高度
     * @param x 窗口X坐标
     * @param y 窗口Y坐标
     */
    public void saveWindowState(double width, double height, double x, double y) {
        saveWindowSize(width, height);
        saveWindowPosition(x, y);
    }
    
    /**
     * 清除所有配置
     */
    public void clearAll() {
        try {
            preferences.clear();
        } catch (Exception e) {
            System.err.println("清除配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否有保存的配置
     * @return 如果有配置返回true
     */
    public boolean hasConfiguration() {
        try {
            return preferences.keys().length > 0;
        } catch (Exception e) {
            return false;
        }
    }
}