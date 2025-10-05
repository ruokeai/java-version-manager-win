package com.jdkmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * JAR启动器类
 * 负责从JAR中提取JavaFX模块并设置正确的类路径
 */
public class JarLauncher {
    
    private static final String JAVAFX_MODULES_DIR = "javafx_modules";
    private static final String[] JAVAFX_JARS = {
        "javafx-base.jar", "javafx-controls.jar", 
        "javafx-fxml.jar", "javafx-graphics.jar"
    };
    
    public static void main(String[] args) {
        try {
            // 设置系统编码为UTF-8
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");
            
            // 检查管理员权限
            checkAdminRights();
            
            // 提取JavaFX模块
            extractJavaFxModules();
            
            // 设置JavaFX模块路径
            setupJavaFXClassPath();
            
            // 启动主应用程序
            launchMainApplication(args);
            
        } catch (Exception e) {
            System.err.println("启动应用程序失败: " + e.getMessage());
            e.printStackTrace();
            
            // 显示错误对话框
            try {
                Class<?> alertClass = Class.forName("javafx.scene.control.Alert");
                Class<?> alertTypeClass = Class.forName("javafx.scene.control.Alert$AlertType");
                Object alertType = alertTypeClass.getField("ERROR").get(null);
                Object alert = alertClass.getConstructor(alertTypeClass).newInstance(alertType);
                Method setTitle = alertClass.getMethod("setTitle", String.class);
                Method setHeaderText = alertClass.getMethod("setHeaderText", String.class);
                Method setContentText = alertClass.getMethod("setContentText", String.class);
                Method showAndWait = alertClass.getMethod("showAndWait");
                
                setTitle.invoke(alert, "启动错误");
                setHeaderText.invoke(alert, "无法启动应用程序");
                setContentText.invoke(alert, "启动Java版本管理器时发生错误:\n" + e.getMessage());
                showAndWait.invoke(alert);
            } catch (Exception ex) {
                // 如果JavaFX不可用，使用控制台输出
                System.err.println("无法显示错误对话框: " + ex.getMessage());
            }
            
            System.exit(1);
        }
    }
    
    /**
     * 从JAR中提取JavaFX模块到临时目录
     */
    private static void extractJavaFxModules() throws IOException {
        // 获取JAR文件路径
        String jarPath = JarLauncher.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath();
        
        System.out.println("DEBUG: JAR文件路径: " + jarPath);
        
        // 如果不是从JAR运行，跳过提取
        if (!jarPath.endsWith(".jar")) {
            System.out.println("DEBUG: 不是从JAR运行，跳过JavaFX模块提取");
            return;
        }
        
        // 创建临时目录
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), JAVAFX_MODULES_DIR);
        System.out.println("DEBUG: JavaFX临时目录: " + tempDir);
        
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
            System.out.println("DEBUG: 已创建临时目录");
        }
        
        // 检查是否需要提取
        boolean needExtract = false;
        System.out.println("DEBUG: 检查JavaFX模块是否需要提取...");
        for (String jarName : JAVAFX_JARS) {
            Path jarPathInTemp = tempDir.resolve(jarName);
            System.out.println("DEBUG: 检查模块: " + jarPathInTemp + " - 存在: " + Files.exists(jarPathInTemp));
            if (!Files.exists(jarPathInTemp)) {
                needExtract = true;
                break;
            }
        }
        
        if (!needExtract) {
            System.out.println("DEBUG: 所有JavaFX模块已存在，跳过提取");
            return;
        }
        
        System.out.println("DEBUG: 需要提取JavaFX模块，开始从JAR中提取...");
        
        // 从JAR中提取JavaFX模块
        try (JarFile jarFile = new JarFile(new File(jarPath))) {
            System.out.println("DEBUG: 已打开JAR文件，开始查找JavaFX模块...");
            
            // 列出JAR中的所有条目用于调试
            System.out.println("DEBUG: JAR中的条目:");
            java.util.Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("javafx") || name.contains("lib/")) {
                    System.out.println("DEBUG: 找到相关条目: " + name);
                }
            }
            
            for (String jarName : JAVAFX_JARS) {
                String entryName = "lib/" + jarName;
                System.out.println("DEBUG: 查找JAR条目: " + entryName);
                
                JarEntry entry = jarFile.getJarEntry(entryName);
                if (entry != null) {
                    System.out.println("DEBUG: 找到条目，开始提取: " + jarName);
                    try (InputStream is = jarFile.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(tempDir.resolve(jarName).toFile())) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    System.out.println("DEBUG: 成功提取: " + jarName);
                } else {
                    System.out.println("DEBUG: 警告: 未找到JAR条目: " + entryName);
                }
            }
        }
    }
    
    /**
     * 设置JavaFX类路径
     */
    private static void setupJavaFXClassPath() throws Exception {
        // 获取JavaFX模块目录
        Path javafxDir = Paths.get(System.getProperty("java.io.tmpdir"), JAVAFX_MODULES_DIR);
        System.out.println("DEBUG: JavaFX模块目录: " + javafxDir);
        
        // 检查JavaFX模块是否存在
        System.out.println("DEBUG: 检查JavaFX模块是否存在...");
        for (String jarName : JAVAFX_JARS) {
            Path jarPath = javafxDir.resolve(jarName);
            boolean exists = Files.exists(jarPath);
            System.out.println("DEBUG: 检查模块: " + jarPath + " - 存在: " + exists);
            if (!exists) {
                // 尝试检查modules-win目录
                Path alternativePath = Paths.get("target", "modules-win", jarName);
                boolean altExists = Files.exists(alternativePath);
                System.out.println("DEBUG: 检查备用路径: " + alternativePath + " - 存在: " + altExists);
                
                if (!altExists) {
                    throw new IOException("JavaFX模块不存在: " + jarPath + " 或 " + alternativePath);
                }
            }
        }
        
        // 构建类路径字符串
        StringBuilder classPath = new StringBuilder();
        for (String jarName : JAVAFX_JARS) {
            Path jarPath = javafxDir.resolve(jarName);
            if (!Files.exists(jarPath)) {
                // 使用备用路径
                jarPath = Paths.get("target", "modules-win", jarName);
            }
            
            if (classPath.length() > 0) {
                classPath.append(File.pathSeparator);
            }
            classPath.append(jarPath.toString());
            System.out.println("DEBUG: 添加到类路径: " + jarPath);
        }
        
        // 获取当前类路径
        String currentClassPath = System.getProperty("java.class.path", "");
        System.out.println("DEBUG: 当前类路径: " + currentClassPath);
        
        if (currentClassPath.length() > 0) {
            classPath.append(File.pathSeparator).append(currentClassPath);
        }
        
        // 设置新的类路径
        System.setProperty("java.class.path", classPath.toString());
        System.out.println("DEBUG: 新的类路径: " + classPath.toString());
        
        // 设置JavaFX模块系统属性
        System.setProperty("javafx.runtime.path", javafxDir.toString());
        System.setProperty("javafx.runtime.version", "17.0.2");
        System.out.println("DEBUG: 设置JavaFX运行时路径: " + javafxDir);
        
        // 设置模块路径
        String modulePath = System.getProperty("jdk.module.path", "");
        System.out.println("DEBUG: 当前模块路径: " + modulePath);
        
        if (modulePath.length() > 0) {
            modulePath += File.pathSeparator + javafxDir.toString();
        } else {
            modulePath = javafxDir.toString();
        }
        System.setProperty("jdk.module.path", modulePath);
        System.out.println("DEBUG: 新的模块路径: " + modulePath);
    }
    
    /**
     * 检查管理员权限
     */
    private static void checkAdminRights() throws Exception {
        try {
            // 在Windows上检查管理员权限
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("net", "session");
                Process process = pb.start();
                int exitCode = process.waitFor();
                
                if (exitCode != 0) {
                    // 没有管理员权限，显示提示
                    showAdminRightsWarning();
                }
            }
        } catch (Exception e) {
            // 无法检测权限，继续运行
            System.err.println("无法检测管理员权限: " + e.getMessage());
        }
    }
    
    /**
     * 显示管理员权限警告
     */
    private static void showAdminRightsWarning() {
        try {
            // 尝试使用JavaFX显示警告
            Class<?> alertClass = Class.forName("javafx.scene.control.Alert");
            Class<?> alertTypeClass = Class.forName("javafx.scene.control.Alert$AlertType");
            Object alertType = alertTypeClass.getField("WARNING").get(null);
            Object alert = alertClass.getConstructor(alertTypeClass).newInstance(alertType);
            
            Method setTitle = alertClass.getMethod("setTitle", String.class);
            Method setHeaderText = alertClass.getMethod("setHeaderText", String.class);
            Method setContentText = alertClass.getMethod("setContentText", String.class);
            Method showAndWait = alertClass.getMethod("showAndWait");
            
            setTitle.invoke(alert, "权限提示");
            setHeaderText.invoke(alert, "未以管理员身份运行");
            setContentText.invoke(alert, "Java版本管理器需要管理员权限来修改系统环境变量。\n\n" +
                    "某些功能可能无法正常工作。\n\n" +
                    "建议右键点击应用程序并选择'以管理员身份运行'。");
            showAndWait.invoke(alert);
        } catch (Exception e) {
            // 如果JavaFX不可用，使用控制台输出
            System.out.println("警告: 未以管理员身份运行");
            System.out.println("Java版本管理器需要管理员权限来修改系统环境变量。");
            System.out.println("某些功能可能无法正常工作。");
            System.out.println("建议右键点击应用程序并选择'以管理员身份运行'。");
        }
    }
    
    /**
     * 启动主应用程序
     */
    private static void launchMainApplication(String[] args) throws Exception {
        // 设置系统属性，告诉JavaFX模块已准备就绪
        System.setProperty("javafx.modules.ready", "true");
        
        // 调用主应用程序的main方法
        Class<?> mainClass = Class.forName("com.jdkmanager.JdkManagerApp");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }
}