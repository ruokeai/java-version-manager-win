package com.jdkmanager.env;

import com.jdkmanager.exception.EnvironmentVariableException;
import com.jdkmanager.exception.InsufficientPrivilegeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Windows环境变量管理器
 * 提供Windows系统环境变量的读取、修改和通知功能
 */
public class EnvironmentManager {
    private static final String JAVA_HOME = "JAVA_HOME";
    private static final String PATH = "PATH";
    
    // Windows环境变量更新通知常量
    private static final int HWND_BROADCAST = 0xFFFF;
    private static final int WM_SETTINGCHANGE = 0x1A;
    
    /**
     * 设置JAVA_HOME环境变量
     * @param jdkPath JDK安装路径
     * @param scope 环境变量作用域
     * @return 如果设置成功返回true
     * @throws EnvironmentVariableException 如果设置失败
     */
    public boolean setJavaHome(Path jdkPath, EnvVariableScope scope) throws EnvironmentVariableException {
        if (jdkPath == null) {
            throw new IllegalArgumentException("JDK路径不能为空");
        }
        
        String value = jdkPath.toAbsolutePath().toString();
        return setEnvironmentVariable(JAVA_HOME, value, scope);
    }
    
    /**
     * 获取当前JAVA_HOME值
     * @param scope 环境变量作用域
     * @return JAVA_HOME值，如果不存在返回Optional.empty()
     */
    public Optional<String> getJavaHome(EnvVariableScope scope) {
        return getEnvironmentVariable(JAVA_HOME, scope);
    }
    
    /**
     * 更新PATH环境变量，添加JDK的bin目录
     * @param jdkPath JDK安装路径
     * @param scope 环境变量作用域
     * @return 如果更新成功返回true
     * @throws EnvironmentVariableException 如果更新失败
     */
    public boolean updatePath(Path jdkPath, EnvVariableScope scope) throws EnvironmentVariableException {
        if (jdkPath == null) {
            throw new IllegalArgumentException("JDK路径不能为空");
        }
        
        try {
            String jdkBinPath = jdkPath.resolve("bin").toAbsolutePath().toString();
            Optional<String> currentPath = getEnvironmentVariable(PATH, scope);
            
            String newPath;
            if (currentPath.isPresent()) {
                // 移除旧的JDK bin路径，添加新的
                newPath = removeOldJdkFromPath(currentPath.get()) + ";" + jdkBinPath;
            } else {
                newPath = jdkBinPath;
            }
            
            return setEnvironmentVariable(PATH, newPath, scope);
        } catch (Exception e) {
            throw new EnvironmentVariableException("更新PATH失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 切换JDK版本
     * @param jdkPath 新的JDK路径
     * @param scope 环境变量作用域
     * @return 如果切换成功返回true
     * @throws EnvironmentVariableException 如果切换失败
     */
    public boolean switchJdk(Path jdkPath, EnvVariableScope scope) throws EnvironmentVariableException {
        System.out.println("=== 开始切换JDK ===");
        System.out.println("目标JDK路径: " + jdkPath);
        System.out.println("作用域: " + scope.getDisplayName());
        
        try {
            // 设置JAVA_HOME
            System.out.println("设置JAVA_HOME...");
            boolean javaHomeSet = setJavaHome(jdkPath, scope);
            if (!javaHomeSet) {
                throw new EnvironmentVariableException("设置JAVA_HOME失败");
            }
            System.out.println("✓ JAVA_HOME设置成功");
            
            // 更新PATH
            System.out.println("更新PATH...");
            boolean pathUpdated = updatePath(jdkPath, scope);
            if (!pathUpdated) {
                throw new EnvironmentVariableException("更新PATH失败");
            }
            System.out.println("✓ PATH更新成功");
            
            // 通知系统环境变量已更改
            System.out.println("通知系统环境变量更改...");
            notifyEnvironmentChange();
            System.out.println("✓ 环境变量更改通知已发送");
            
            // 验证设置是否生效
            System.out.println("验证环境变量设置...");
            Optional<String> newJavaHome = getJavaHome(scope);
            if (newJavaHome.isPresent()) {
                System.out.println("验证" + scope.getDisplayName() + "JAVA_HOME: " + newJavaHome.get());
                if (!newJavaHome.get().equals(jdkPath.toAbsolutePath().toString())) {
                    System.out.println("⚠️ 警告: 设置的JAVA_HOME与实际读取的不一致");
                    System.out.println("  设置值: " + jdkPath.toAbsolutePath().toString());
                    System.out.println("  读取值: " + newJavaHome.get());
                } else {
                    System.out.println("✓ JAVA_HOME验证成功");
                }
            } else {
                System.out.println("✗ 无法读取" + scope.getDisplayName() + "JAVA_HOME");
            }
            
            System.out.println("=== JDK切换完成 ===");
            return true;
        } catch (Exception e) {
            System.err.println("=== JDK切换失败 ===");
            System.err.println("错误: " + e.getMessage());
            throw new EnvironmentVariableException("切换JDK失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查是否有管理员权限
     * @return 如果有管理员权限返回true
     */
    public boolean hasAdminRights() {
        try {
            // 尝试访问系统级注册表项来检查权限
            Optional<String> systemPath = getEnvironmentVariable(PATH, EnvVariableScope.SYSTEM);
            return systemPath.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 设置环境变量
     * @param name 变量名
     * @param value 变量值
     * @param scope 作用域
     * @return 如果设置成功返回true
     * @throws EnvironmentVariableException 如果设置失败
     */
    private boolean setEnvironmentVariable(String name, String value, EnvVariableScope scope) throws EnvironmentVariableException {
        try {
            // 使用reg命令设置环境变量
            String command = String.format("reg add \"%s\" /v \"%s\" /t REG_EXPAND_SZ /d \"%s\" /f", 
                scope.getRegistryPath(), name, value);
            
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // 读取错误信息
                String errorOutput = readProcessError(process);
                if (errorOutput.contains("Access is denied") || errorOutput.contains("拒绝访问")) {
                    throw new InsufficientPrivilegeException("权限不足，无法修改" + scope.getDisplayName() + "环境变量");
                }
                throw new EnvironmentVariableException("设置环境变量失败: " + errorOutput);
            }
            
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnvironmentVariableException("设置环境变量被中断", e);
        } catch (IOException e) {
            throw new EnvironmentVariableException("设置环境变量IO错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取环境变量
     * @param name 变量名
     * @param scope 作用域
     * @return 变量值
     */
    private Optional<String> getEnvironmentVariable(String name, EnvVariableScope scope) {
        try {
            // 使用reg命令查询环境变量
            String command = String.format("reg query \"%s\" /v \"%s\"", 
                scope.getRegistryPath(), name);
            
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "gbk"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return Optional.empty();
            }
            
            // 解析输出获取变量值
            String result = output.toString();
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith(name)) {
                    int index = line.indexOf("REG_");
                    if (index != -1) {
                        return Optional.of(line.substring(index + line.substring(index).indexOf(" ") + 1).trim());
                    }
                }
            }
            
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("获取环境变量失败: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 从PATH中移除旧的JDK路径
     * @param currentPath 当前PATH值
     * @return 清理后的PATH
     */
    private String removeOldJdkFromPath(String currentPath) {
        List<String> pathEntries = new ArrayList<>(Arrays.asList(currentPath.split(";")));
        
        // 移除包含java或jdk的bin路径
        pathEntries.removeIf(path -> {
            String lowerPath = path.toLowerCase();
            return (lowerPath.contains("java") || lowerPath.contains("jdk")) && 
                   lowerPath.contains("bin");
        });
        
        return String.join(";", pathEntries);
    }
    
    /**
     * 通知系统环境变量已更改
     */
    private void notifyEnvironmentChange() {
        try {
            // 使用Windows API发送WM_SETTINGCHANGE消息
            // 这里使用一个简化的方法，通过调用PowerShell来发送消息
            String psCommand = "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class Win32 { [DllImport(\"user32.dll\")] public static extern IntPtr SendMessageTimeout(IntPtr hWnd, uint Msg, IntPtr wParam, string lParam, uint fuFlags, uint uTimeout, out IntPtr lpdwResult); }'; [Win32]::SendMessageTimeout(0xFFFF, 0x1A, [IntPtr]::Zero, \"Environment\", 0, 1000, [ref]$null)";
            String command = "powershell -Command \"" + psCommand + "\"";
            
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("通知环境变量更改失败: " + e.getMessage());
            // 这个失败不是致命的，不影响主要功能
        }
    }
    
    /**
     * 读取进程错误输出
     * @param process 进程对象
     * @return 错误输出字符串
     */
    private String readProcessError(Process process) {
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        } catch (IOException e) {
            // 忽略读取错误
        }
        return errorOutput.toString();
    }
}