package com.jdkmanager.scanner;

import com.jdkmanager.scanner.JdkInfo;
import com.jdkmanager.scanner.JdkScanner;
import com.jdkmanager.scanner.DefaultJdkScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
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
    
    @Test
    @DisplayName("测试JDK去重功能")
    void testRemoveDuplicateJdks() {
        // 创建测试用的JDK列表，包含重复项
        List<JdkInfo> jdksWithDuplicates = new ArrayList<>();
        
        // 添加相同的JDK多次（相同路径和版本）
        Path jdkPath = Path.of("C:\\Program Files\\Java\\jdk-11.0.15");
        JdkInfo jdk1 = new JdkInfo("11.0.15", jdkPath, true);
        JdkInfo jdk2 = new JdkInfo("11.0.15", jdkPath, true); // 相同路径和版本
        
        // 添加不同的JDK
        Path jdkPath2 = Path.of("C:\\Program Files\\Java\\jdk-17.0.5");
        JdkInfo jdk3 = new JdkInfo("17.0.5", jdkPath2, true);
        
        // 添加相同路径但不同版本的JDK
        JdkInfo jdk4 = new JdkInfo("11.0.16", jdkPath, true);
        
        jdksWithDuplicates.add(jdk1);
        jdksWithDuplicates.add(jdk2); // 重复项
        jdksWithDuplicates.add(jdk3);
        jdksWithDuplicates.add(jdk4);
        
        System.out.println("去重前的JDK数量: " + jdksWithDuplicates.size());
        
        // 使用反射调用私有方法进行测试
        try {
            java.lang.reflect.Method method = DefaultJdkScanner.class.getDeclaredMethod("removeDuplicateJdks", List.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<JdkInfo> uniqueJdks = (List<JdkInfo>) method.invoke(scanner, jdksWithDuplicates);
            
            System.out.println("去重后的JDK数量: " + uniqueJdks.size());
            
            // 验证去重结果
            assertEquals(3, uniqueJdks.size(), "去重后应该有3个JDK");
            
            // 验证包含的JDK
            assertTrue(uniqueJdks.contains(jdk1), "应该包含第一个JDK");
            assertTrue(uniqueJdks.contains(jdk3), "应该包含第三个JDK");
            assertTrue(uniqueJdks.contains(jdk4), "应该包含第四个JDK");
            
            // 验证重复项被移除
            long countJdk11_0_15 = uniqueJdks.stream()
                .filter(jdk -> jdk.getVersion().equals("11.0.15") && jdk.getPath().equals(jdkPath))
                .count();
            assertEquals(1, countJdk11_0_15, "相同路径和版本的JDK应该只有一个");
            
        } catch (Exception e) {
            fail("测试去重功能时发生异常: " + e.getMessage());
        }
    }
}