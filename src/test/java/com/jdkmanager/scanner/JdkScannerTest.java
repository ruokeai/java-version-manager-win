package com.jdkmanager.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

/**
 * JDK扫描器测试类
 */
public class JdkScannerTest {
    
    private JdkScanner scanner;
    
    @BeforeEach
    void setUp() {
        scanner = new DefaultJdkScanner();
    }
    
    @Test
    @DisplayName("测试扫描当前JDK路径")
    void testScanCurrentJdkPath() {
        // 获取当前JAVA_HOME路径
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.trim().isEmpty()) {
            Path currentJdkPath = Path.of(javaHome);
            
            // 验证当前JDK路径是有效的
            assertTrue(JdkInfo.isValidJdk(currentJdkPath), "当前JDK路径应该是有效的");
            
            // 尝试从路径创建JdkInfo
            JdkInfo jdkInfo = JdkInfo.fromPath(currentJdkPath);
            assertNotNull(jdkInfo, "应该能够从当前JDK路径创建JdkInfo");
            assertNotNull(jdkInfo.getVersion(), "版本号不应该为空");
            assertNotEquals("未知版本", jdkInfo.getVersion(), "版本号应该是可识别的");
            
            System.out.println("检测到的JDK: " + jdkInfo);
        } else {
            System.out.println("JAVA_HOME未设置，跳过当前JDK测试");
        }
    }
    
    @Test
    @DisplayName("测试扫描默认路径")
    void testScanDefaultPaths() {
        List<JdkInfo> jdks = scanner.scanForJdks();
        
        System.out.println("扫描到的JDK数量: " + jdks.size());
        
        for (JdkInfo jdk : jdks) {
            System.out.println("JDK: " + jdk);
            assertNotNull(jdk.getVersion(), "版本号不应该为空");
            assertNotNull(jdk.getPath(), "路径不应该为空");
        }
        
        // 如果当前系统有JDK，应该至少检测到一个
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.trim().isEmpty()) {
            assertTrue(jdks.size() > 0, "应该至少检测到一个JDK");
        }
    }
    
    @Test
    @DisplayName("测试版本解析")
    void testVersionExtraction() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.trim().isEmpty()) {
            Path currentJdkPath = Path.of(javaHome);
            JdkInfo jdkInfo = JdkInfo.fromPath(currentJdkPath);
            
            if (jdkInfo != null) {
                String version = jdkInfo.getVersion();
                System.out.println("解析到的版本: " + version);
                
                // 验证版本格式
                assertTrue(version.matches("\\d+\\.\\d+.*"), 
                    "版本号应该符合格式 x.x.x: " + version);
            }
        }
    }
    
    @Test
    @DisplayName("测试当前JDK检测")
    void testDetectCurrentJdk() {
        List<JdkInfo> jdks = scanner.scanForJdks();
        
        if (!jdks.isEmpty()) {
            // 检测当前JDK
            scanner.detectCurrentJdk(jdks);
            
            // 验证只有一个JDK被标记为当前
            long currentCount = jdks.stream()
                .filter(JdkInfo::isCurrent)
                .count();
            
            System.out.println("标记为当前的JDK数量: " + currentCount);
            
            // 应该只有一个当前JDK
            assertTrue(currentCount <= 1, "最多只能有一个当前JDK");
            
            // 如果有当前JDK，验证其路径与JAVA_HOME匹配
            jdks.stream()
                .filter(JdkInfo::isCurrent)
                .findFirst()
                .ifPresent(currentJdk -> {
                    String javaHome = System.getenv("JAVA_HOME");
                    if (javaHome != null) {
                        Path javaHomePath = Path.of(javaHome).normalize().toAbsolutePath();
                        Path jdkPath = currentJdk.getPath().normalize().toAbsolutePath();
                        assertEquals(javaHomePath, jdkPath, "当前JDK路径应该与JAVA_HOME匹配");
                    }
                });
        }
    }
}