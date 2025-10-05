package com.jdkmanager.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDK信息数据模型
 * 表示一个检测到的JDK安装，包含版本、路径、架构等信息
 */
public class JdkInfo {
    private final String version;
    private final Path path;
    private final boolean is64Bit;
    private boolean isCurrent;
    
    /**
     * 构造函数
     * @param version JDK版本号
     * @param path JDK安装路径
     * @param is64Bit 是否为64位JDK
     */
    public JdkInfo(String version, Path path, boolean is64Bit) {
        this.version = Objects.requireNonNull(version, "版本号不能为空");
        this.path = Objects.requireNonNull(path, "路径不能为空");
        this.is64Bit = is64Bit;
        this.isCurrent = false;
    }
    
    /**
     * 从给定路径创建JdkInfo对象
     * @param jdkPath JDK安装路径
     * @return JdkInfo对象，如果路径不是有效JDK则返回null
     */
    public static JdkInfo fromPath(Path jdkPath) {
        if (!isValidJdk(jdkPath)) {
            return null;
        }
        
        try {
            String version = extractVersion(jdkPath);
            if (version == null) {
                version = "未知版本";
            }
            
            boolean is64Bit = checkArchitecture(jdkPath);
            
            return new JdkInfo(version, jdkPath, is64Bit);
        } catch (Exception e) {
            System.err.println("从路径创建JdkInfo失败: " + jdkPath + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证路径是否为有效的JDK安装
     * @param path 要验证的路径
     * @return 如果是有效JDK返回true
     */
    public static boolean isValidJdk(Path path) {
        if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
            return false;
        }
        
        // 检查是否存在bin目录和关键工具
        Path binDir = path.resolve("bin");
        Path javaExe = binDir.resolve("java.exe");
        Path javacExe = binDir.resolve("javac.exe");
        
        return Files.exists(javaExe) && Files.exists(javacExe);
    }
    
    /**
     * 从release文件或java -version获取版本信息
     * @param jdkPath JDK路径
     * @return 版本字符串
     */
    private static String extractVersion(Path jdkPath) {
        // 首先尝试从release文件读取
        Path releaseFile = jdkPath.resolve("release");
        if (Files.exists(releaseFile)) {
            try {
                String content = Files.readString(releaseFile);
                // 查找JAVA_VERSION行
                Pattern pattern = Pattern.compile("JAVA_VERSION=\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (Exception e) {
                System.err.println("读取release文件失败: " + e.getMessage());
            }
        }
        
        // 如果release文件不存在或读取失败，尝试执行java -version
        try {
            ProcessBuilder pb = new ProcessBuilder(
                jdkPath.resolve("bin").resolve("java.exe").toString(),
                "-version"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
            
            // 解析版本信息 - 支持多种格式
            String versionOutput = output.toString();
            
            // 尝试匹配 "version \"版本号\" 格式
            Pattern pattern = Pattern.compile("version \"([^\"]+)\"");
            Matcher matcher = pattern.matcher(versionOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 尝试匹配 "version 版本号" 格式
            pattern = Pattern.compile("version\\s+([0-9]+\\.[0-9]+[^\\s]*)");
            matcher = pattern.matcher(versionOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 尝试匹配 Java 17.0.16+8-LTS 格式
            pattern = Pattern.compile("openjdk version \"([0-9]+\\.[0-9]+[^\\s]*)");
            matcher = pattern.matcher(versionOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // 尝试匹配 "openjdk version 版本号" 格式
            pattern = Pattern.compile("openjdk version\\s+([0-9]+\\.[0-9]+[^\\s]*)");
            matcher = pattern.matcher(versionOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
        } catch (Exception e) {
            System.err.println("执行java -version失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 检查JDK架构
     * @param jdkPath JDK路径
     * @return 如果是64位返回true
     */
    private static boolean checkArchitecture(Path jdkPath) {
        // 检查路径中是否包含64位标识
        String pathStr = jdkPath.toString().toLowerCase();
        if (pathStr.contains("x64") || pathStr.contains("64")) {
            return true;
        }
        
        // 尝试通过java命令检查架构
        try {
            ProcessBuilder pb = new ProcessBuilder(
                jdkPath.resolve("bin").resolve("java.exe").toString(),
                "-d64",
                "-version"
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0; // 如果-d64参数被接受，则是64位JDK
        } catch (Exception e) {
            // 如果执行失败，默认假设为32位
            return false;
        }
    }
    
    // Getter方法
    public String getVersion() { 
        return version; 
    }
    
    public Path getPath() { 
        return path; 
    }
    
    public boolean is64Bit() { 
        return is64Bit; 
    }
    
    public boolean isCurrent() { 
        return isCurrent; 
    }
    
    public void setCurrent(boolean current) { 
        this.isCurrent = current; 
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JdkInfo jdkInfo = (JdkInfo) o;
        return path.equals(jdkInfo.path);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
    
    @Override
    public String toString() {
        return String.format("JdkInfo{version='%s', path='%s', 64bit=%s, current=%s}",
                version, path, is64Bit, isCurrent);
    }
}