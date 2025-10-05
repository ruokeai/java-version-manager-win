package com.jdkmanager.scanner;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 默认JDK扫描器实现
 * 提供JDK扫描和管理的具体实现
 */
public class DefaultJdkScanner implements JdkScanner {
    
    // Windows系统常见JDK安装路径
    private static final List<String> DEFAULT_PATHS = Arrays.asList(
        "C:\\Program Files\\Java",
        "C:\\Program Files (x86)\\Java",
        "C:\\Program Files\\Microsoft",  // 添加Microsoft JDK路径
        System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Java"
    );
    
    // 自定义扫描路径列表
    private final List<Path> customPaths;
    
    // 单线程执行器用于异步操作
    private final ExecutorService executorService;
    
    /**
     * 构造函数
     */
    public DefaultJdkScanner() {
        this.customPaths = new ArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 构造函数（带初始自定义路径）
     * @param initialCustomPaths 初始自定义路径列表
     */
    public DefaultJdkScanner(List<Path> initialCustomPaths) {
        this.customPaths = new ArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
        
        // 初始化自定义路径
        if (initialCustomPaths != null) {
            for (Path path : initialCustomPaths) {
                if (path != null && Files.exists(path) && Files.isDirectory(path)) {
                    Path normalizedPath = path.normalize().toAbsolutePath();
                    // 检查是否已存在
                    boolean exists = customPaths.stream()
                        .anyMatch(existing -> existing.normalize().toAbsolutePath().equals(normalizedPath));
                    if (!exists) {
                        customPaths.add(normalizedPath);
                    }
                }
            }
        }
    }
    
    /**
     * 初始化自定义路径（用于已创建的扫描器实例）
     * @param paths 自定义路径列表
     */
    public void initializeCustomPaths(List<Path> paths) {
        if (paths != null) {
            for (Path path : paths) {
                if (path != null && Files.exists(path) && Files.isDirectory(path)) {
                    Path normalizedPath = path.normalize().toAbsolutePath();
                    // 检查是否已存在
                    boolean exists = customPaths.stream()
                        .anyMatch(existing -> existing.normalize().toAbsolutePath().equals(normalizedPath));
                    if (!exists) {
                        customPaths.add(normalizedPath);
                    }
                }
            }
        }
    }
    
    @Override
    public List<JdkInfo> scanForJdks() {
        List<JdkInfo> allJdks = new ArrayList<>();
        
        // 扫描默认路径
        for (String defaultPath : DEFAULT_PATHS) {
            Path path = Path.of(defaultPath);
            if (Files.exists(path)) {
                allJdks.addAll(scanDirectory(path));
            }
        }
        
        // 扫描自定义路径
        for (Path customPath : customPaths) {
            if (Files.exists(customPath)) {
                allJdks.addAll(scanDirectory(customPath));
            }
        }
        
        // 去重处理：相同路径和版本的JDK只保留一个
        allJdks = removeDuplicateJdks(allJdks);
        
        // 检测当前JDK
        detectCurrentJdk(allJdks);
        
        return allJdks;
    }
    
    @Override
    public CompletableFuture<List<JdkInfo>> scanForJdksAsync() {
        return CompletableFuture.supplyAsync(this::scanForJdks, executorService);
    }
    
    @Override
    public boolean addCustomPath(Path path) {
        if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
            return false;
        }
        
        // 检查路径是否已存在
        Path normalizedPath = path.normalize().toAbsolutePath();
        for (Path existingPath : customPaths) {
            if (existingPath.normalize().toAbsolutePath().equals(normalizedPath)) {
                return false; // 路径已存在
            }
        }
        
        return customPaths.add(normalizedPath);
    }
    
    @Override
    public boolean removeCustomPath(Path path) {
        if (path == null) {
            return false;
        }
        
        Path normalizedPath = path.normalize().toAbsolutePath();
        return customPaths.removeIf(existingPath -> 
            existingPath.normalize().toAbsolutePath().equals(normalizedPath));
    }
    
    @Override
    public List<Path> getCustomPaths() {
        return new ArrayList<>(customPaths);
    }
    
    @Override
    public boolean validateJdk(Path path) {
        return JdkInfo.isValidJdk(path);
    }
    
    @Override
    public List<JdkInfo> scanDirectory(Path directory) {
        List<JdkInfo> jdks = new ArrayList<>();
        
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return jdks;
        }
        
        try {
            // 首先检查目录本身是否为JDK
            if (JdkInfo.isValidJdk(directory)) {
                JdkInfo jdkInfo = JdkInfo.fromPath(directory);
                if (jdkInfo != null) {
                    jdks.add(jdkInfo);
                    // 只添加一次，不再继续扫描子目录
                    return jdks;
                }
            }
            
            // 遍历目录中的所有子目录
            List<Path> subdirectories = Files.list(directory)
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
            
            for (Path subdirectory : subdirectories) {
                // 检查是否看起来像JDK目录
                if (looksLikeJdkDirectory(subdirectory)) {
                    JdkInfo jdkInfo = JdkInfo.fromPath(subdirectory);
                    if (jdkInfo != null) {
                        jdks.add(jdkInfo);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("扫描目录失败: " + directory + " - " + e.getMessage());
        }
        
        return jdks;
    }
    
    @Override
    public void detectCurrentJdk(List<JdkInfo> jdks) {
        if (jdks == null || jdks.isEmpty()) {
            return;
        }
        
        System.out.println("=== 开始检测当前JDK ===");
        
        // 首先重置所有JDK的当前状态
        jdks.forEach(jdk -> {
            jdk.setCurrent(false);
            System.out.println("重置JDK状态: " + jdk.getVersion() + " at " + jdk.getPath());
        });
        
        // 优先从注册表读取环境变量，这是最可靠的方法
        // 因为它不受Java应用程序缓存的影响
        try {
            System.out.println("优先从注册表读取环境变量检测当前JDK...");
            String javaHome = getJavaHomeFromRegistry();
            System.out.println("注册表中的JAVA_HOME: " + javaHome);
            if (javaHome != null && !javaHome.trim().isEmpty()) {
                Path currentJdkPath = Path.of(javaHome).normalize().toAbsolutePath();
                System.out.println("尝试通过注册表路径匹配: " + currentJdkPath);
                
                boolean foundByRegistry = false;
                for (JdkInfo jdk : jdks) {
                    Path jdkPath = jdk.getPath().normalize().toAbsolutePath();
                    System.out.println("比较路径: " + jdkPath + " vs " + currentJdkPath);
                    if (jdkPath.equals(currentJdkPath)) {
                        jdk.setCurrent(true);
                        foundByRegistry = true;
                        System.out.println("✓ 通过注册表找到当前JDK: " + jdk.getVersion());
                        break;
                    }
                }
                
                if (foundByRegistry) {
                    System.out.println("=== 当前JDK检测完成（通过注册表） ===");
                    return;
                } else {
                    System.out.println("✗ 注册表方法未找到对应的JDK");
                }
            } else {
                System.out.println("注册表中的JAVA_HOME为空");
            }
        } catch (Exception e) {
            System.err.println("通过注册表检测当前JDK失败: " + e.getMessage());
        }
        
        // 如果注册表方法失败，则通过执行外部命令获取实际的JDK版本和路径
        // 这是最准确的方法，因为它反映的是当前系统实际使用的JDK
        try {
            System.out.println("回退到外部命令检测当前JDK...");
            JavaVersionInfo actualJavaInfo = getActualJavaInfo();
            if (actualJavaInfo != null) {
                System.out.println("检测到实际Java信息:");
                System.out.println("  版本: " + actualJavaInfo.version);
                System.out.println("  JAVA_HOME: " + actualJavaInfo.javaHome);
                System.out.println("  Java路径: " + actualJavaInfo.javaPath);
                
                // 首先尝试通过路径精确匹配
                boolean foundByPath = false;
                if (actualJavaInfo.javaHome != null && !actualJavaInfo.javaHome.isEmpty()) {
                    try {
                        Path currentJdkPath = Path.of(actualJavaInfo.javaHome).normalize().toAbsolutePath();
                        System.out.println("尝试通过路径匹配: " + currentJdkPath);
                        
                        for (JdkInfo jdk : jdks) {
                            Path jdkPath = jdk.getPath().normalize().toAbsolutePath();
                            System.out.println("比较路径: " + jdkPath + " vs " + currentJdkPath);
                            if (jdkPath.equals(currentJdkPath)) {
                                jdk.setCurrent(true);
                                foundByPath = true;
                                System.out.println("✓ 通过路径匹配找到当前JDK: " + jdk.getVersion() + " at " + jdkPath);
                                break;
                            }
                        }
                        
                        if (!foundByPath) {
                            System.out.println("✗ 路径匹配未找到对应的JDK");
                        }
                    } catch (Exception e) {
                        System.err.println("通过路径匹配JDK失败: " + e.getMessage());
                    }
                }
                
                // 如果路径匹配失败，则通过版本匹配
                if (!foundByPath && actualJavaInfo.version != null && !actualJavaInfo.version.isEmpty()) {
                    System.out.println("尝试通过版本匹配: " + actualJavaInfo.version);
                    for (JdkInfo jdk : jdks) {
                        System.out.println("比较版本: " + jdk.getVersion() + " vs " + actualJavaInfo.version);
                        if (matchesVersion(jdk.getVersion(), actualJavaInfo.version)) {
                            jdk.setCurrent(true);
                            System.out.println("✓ 通过版本匹配找到当前JDK: " + jdk.getVersion() + " (实际版本: " + actualJavaInfo.version + ")");
                            break;
                        }
                    }
                    
                    // 检查是否有JDK被标记为当前
                    boolean anyCurrent = jdks.stream().anyMatch(JdkInfo::isCurrent);
                    if (!anyCurrent) {
                        System.out.println("✗ 版本匹配也未找到对应的JDK");
                    }
                }
                
                // 检查是否找到了JDK
                boolean anyCurrent = jdks.stream().anyMatch(JdkInfo::isCurrent);
                if (anyCurrent) {
                    System.out.println("=== 当前JDK检测完成（通过外部命令） ===");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("通过命令检测当前JDK失败: " + e.getMessage());
        }
        
        System.out.println("=== 当前JDK检测完成（未找到匹配） ===");
    }
    
    /**
     * 获取当前实际使用的Java信息，包括版本和路径
     * @return Java版本信息对象
     */
    private JavaVersionInfo getActualJavaInfo() {
        try {
            // 使用where命令查找java.exe的实际路径
            ProcessBuilder wherePb = new ProcessBuilder("cmd", "/c", "where java");
            wherePb.redirectErrorStream(true);
            Process whereProcess = wherePb.start();
            
            StringBuilder whereOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(whereProcess.getInputStream(), "gbk"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    whereOutput.append(line).append("\n");
                }
            }
            
            whereProcess.waitFor();
            
            String javaPath = null;
            String whereResult = whereOutput.toString();
            
            // 解析where命令的输出，获取java.exe的路径
            String[] lines = whereResult.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.endsWith("java.exe") && !line.contains("WindowsApps")) {
                    javaPath = line;
                    break;
                }
            }
            
            if (javaPath == null) {
                System.err.println("未找到java.exe路径");
                return null;
            }
            
            // 从java.exe路径推导出JAVA_HOME
            Path javaExePath = Path.of(javaPath).normalize().toAbsolutePath();
            Path javaHome = javaExePath.getParent().getParent(); // bin目录的上级目录
            
            // 执行java -version获取版本信息
            ProcessBuilder versionPb = new ProcessBuilder("cmd", "/c", "java -version");
            versionPb.redirectErrorStream(true);
            Process versionProcess = versionPb.start();
            
            StringBuilder versionOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(versionProcess.getInputStream(), "gbk"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    versionOutput.append(line).append("\n");
                }
            }
            
            versionProcess.waitFor();
            
            // 解析版本信息
            String versionResult = versionOutput.toString();
            String version = parseJavaVersion(versionResult);
            
            JavaVersionInfo info = new JavaVersionInfo();
            info.version = version;
            info.javaHome = javaHome.toString();
            info.javaPath = javaPath;
            
            System.out.println("检测到实际Java信息:");
            System.out.println("  路径: " + javaPath);
            System.out.println("  JAVA_HOME: " + javaHome);
            System.out.println("  版本: " + version);
            
            return info;
            
        } catch (Exception e) {
            System.err.println("获取实际Java信息失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析java -version命令的输出，提取版本号
     * @param versionOutput java -version命令的输出
     * @return 版本号字符串
     */
    private String parseJavaVersion(String versionOutput) {
        if (versionOutput == null || versionOutput.isEmpty()) {
            return null;
        }
        
        // 尝试多种版本格式匹配
        Pattern[] patterns = {
            // 匹配 "openjdk version \"16.0.2\" 2021-07-20
            Pattern.compile("openjdk version\\s+\"([0-9]+\\.[0-9]+\\.[0-9]+[^\\\"]*)"),
            // 匹配 "version \"17.0.16\""
            Pattern.compile("version\\s+\"([0-9]+\\.[0-9]+\\.[0-9]+[^\\\"]*)"),
            // 匹配 "java version \"1.8.0_462""
            Pattern.compile("java version\\s+\"([0-9]+\\.[0-9]+\\.[0-9]+[^\\\"]*)"),
            // 匹配 "version 17.0.16"
            Pattern.compile("version\\s+([0-9]+\\.[0-9]+\\.[0-9]+[^\\s]*)")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(versionOutput);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * Java版本信息内部类
     */
    private static class JavaVersionInfo {
        String version;
        String javaHome;
        String javaPath;
    }
    
    /**
     * 比较两个JDK版本号是否匹配（宽松比较，只比较主要部分）
     * @param jdkVersion JdkInfo中的版本号
     * @param actualVersion 实际检测到的版本号
     * @return 如果匹配返回true
     */
    private boolean matchesVersion(String jdkVersion, String actualVersion) {
        if (jdkVersion == null || actualVersion == null) {
            return false;
        }
        
        // 提取主版本号部分进行比较
        String mainJdkVersion = extractMainVersion(jdkVersion);
        String mainActualVersion = extractMainVersion(actualVersion);
        
        System.out.println("比较版本: JDK版本=" + mainJdkVersion + ", 实际版本=" + mainActualVersion);
        
        // 完全匹配
        if (mainJdkVersion.equals(mainActualVersion)) {
            return true;
        }
        
        // 提取主版本号（如17.0）进行比较
        String[] jdkParts = mainJdkVersion.split("\\.");
        String[] actualParts = mainActualVersion.split("\\.");
        
        if (jdkParts.length >= 2 && actualParts.length >= 2) {
            String jdkMajorMinor = jdkParts[0] + "." + jdkParts[1];
            String actualMajorMinor = actualParts[0] + "." + actualParts[1];
            
            if (jdkMajorMinor.equals(actualMajorMinor)) {
                System.out.println("通过主版本号匹配: " + jdkMajorMinor);
                return true;
            }
        }
        
        // 提取主版本号（如17）进行比较
        if (jdkParts.length >= 1 && actualParts.length >= 1) {
            String jdkMajor = jdkParts[0];
            String actualMajor = actualParts[0];
            
            if (jdkMajor.equals(actualMajor)) {
                System.out.println("通过主版本号匹配: " + jdkMajor);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 从完整版本号中提取主要部分
     * 例如从"17.0.5"中提取"17.0.5"
     * 例如从"11.0.15+10"中提取"11.0.15"
     * 例如从"1.8.0_462"中提取"1.8.0_462"
     */
    private String extractMainVersion(String fullVersion) {
        if (fullVersion == null || fullVersion.isEmpty()) {
            return "";
        }
        
        // 移除可能的前缀和后缀
        String version = fullVersion.replaceAll("^[\"']", ""); // 移除开头的引号
        version = version.replaceAll("[\\+\\-].*$", ""); // 移除+或-后面的部分
        
        // 提取版本号的主要部分，支持多种格式
        Pattern pattern = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+(?:_[0-9]+)?)");
        Matcher matcher = pattern.matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果没有匹配到三位版本号，尝试两位版本号
        pattern = Pattern.compile("^([0-9]+\\.[0-9]+)");
        matcher = pattern.matcher(version);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 如果都没有匹配，返回清理后的版本字符串
        return version;
    }
    
    /**
     * 判断目录是否看起来像JDK安装目录
     * @param path 要检查的目录
     * @return 如果看起来像JDK目录返回true
     */
    private boolean looksLikeJdkDirectory(Path path) {
        // 检查是否存在bin目录
        Path binDir = path.resolve("bin");
        if (!Files.exists(binDir) || !Files.isDirectory(binDir)) {
            return false;
        }
        
        // 检查是否存在关键工具
        Path javaExe = binDir.resolve("java.exe");
        Path javacExe = binDir.resolve("javac.exe");
        
        if (!Files.exists(javaExe) || !Files.exists(javacExe)) {
            return false;
        }
        
        // 检查是否存在release文件（现代JDK的标志）
        Path releaseFile = path.resolve("release");
        if (Files.exists(releaseFile)) {
            return true;
        }
        
        // 检查目录名是否包含JDK相关关键字
        String dirName = path.getFileName().toString().toLowerCase();
        return dirName.contains("jdk") ||
               dirName.contains("java") ||
               dirName.matches("\\d+.*") || // 以数字开头（如11, 17等）
               dirName.contains("hotspot") || // HotSpot JVM
               dirName.contains("microsoft"); // Microsoft JDK
    }
    
    /**
     * 直接从注册表读取JAVA_HOME环境变量
     * 这样可以避免Java应用程序缓存的环境变量问题
     * @return JAVA_HOME值，如果不存在返回null
     */
    private String getJavaHomeFromRegistry() {
        // 优先读取用户级环境变量，然后读取系统级
        String[] registryPaths = {
            "HKEY_CURRENT_USER\\Environment",
            "HKEY_LOCAL_MACHINE\\SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment"
        };
        
        for (String registryPath : registryPaths) {
            try {
                String javaHome = getEnvironmentVariableFromRegistry(registryPath, "JAVA_HOME");
                if (javaHome != null && !javaHome.trim().isEmpty()) {
                    System.out.println("从注册表读取到JAVA_HOME (" + registryPath + "): " + javaHome);
                    return javaHome;
                }
            } catch (Exception e) {
                System.err.println("从注册表读取JAVA_HOME失败 (" + registryPath + "): " + e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 从注册表读取指定的环境变量
     * @param registryPath 注册表路径
     * @param variableName 变量名
     * @return 变量值，如果不存在返回null
     */
    private String getEnvironmentVariableFromRegistry(String registryPath, String variableName) {
        try {
            // 使用reg命令查询环境变量
            String command = String.format("reg query \"%s\" /v \"%s\"", registryPath, variableName);
            
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
                return null;
            }
            
            // 解析输出获取变量值
            String result = output.toString();
            String[] lines = result.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith(variableName)) {
                    int index = line.indexOf("REG_");
                    if (index != -1) {
                        String value = line.substring(index + line.substring(index).indexOf(" ") + 1).trim();
                        System.out.println("解析注册表值: " + variableName + " = " + value);
                        return value;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("从注册表读取环境变量失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 移除重复的JDK条目
     * 相同路径和版本的JDK只保留一个
     * @param jdks JDK列表
     * @return 去重后的JDK列表
     */
    private List<JdkInfo> removeDuplicateJdks(List<JdkInfo> jdks) {
        if (jdks == null || jdks.isEmpty()) {
            return jdks;
        }
        
        List<JdkInfo> uniqueJdks = new ArrayList<>();
        Set<String> seenJdkKeys = new HashSet<>();
        
        for (JdkInfo jdk : jdks) {
            // 创建基于路径和版本的唯一标识
            String jdkKey = createJdkKey(jdk);
            
            // 如果这个JDK还没有被添加过，则添加到结果列表
            if (!seenJdkKeys.contains(jdkKey)) {
                seenJdkKeys.add(jdkKey);
                uniqueJdks.add(jdk);
            }
        }
        
        System.out.println("JDK去重完成: 原始数量=" + jdks.size() + ", 去重后数量=" + uniqueJdks.size());
        
        return uniqueJdks;
    }
    
    /**
     * 创建JDK的唯一标识
     * 基于标准化后的绝对路径和版本号
     * @param jdk JDK信息
     * @return 唯一标识字符串
     */
    private String createJdkKey(JdkInfo jdk) {
        if (jdk == null) {
            return "";
        }
        
        // 使用标准化后的绝对路径和版本号作为唯一标识
        Path normalizedPath = jdk.getPath().normalize().toAbsolutePath();
        String version = jdk.getVersion();
        
        return normalizedPath.toString().toLowerCase() + "|" + version;
    }
    
    /**
     * 关闭扫描器，释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}