package com.jdkmanager.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JdkInfo类的单元测试
 */
class JdkInfoTest {
    
    @Test
    void testCreateJdkInfo() {
        String version = "11.0.15";
        Path path = Path.of("C:\\java\\jdk-11");
        boolean is64Bit = true;
        
        JdkInfo jdkInfo = new JdkInfo(version, path, is64Bit);
        
        assertEquals(version, jdkInfo.getVersion());
        assertEquals(path, jdkInfo.getPath());
        assertEquals(is64Bit, jdkInfo.is64Bit());
        assertFalse(jdkInfo.isCurrent());
    }
    
    @Test
    void testSetCurrent() {
        JdkInfo jdkInfo = new JdkInfo("17.0.5", Path.of("C:\\java\\jdk-17"), true);
        
        assertFalse(jdkInfo.isCurrent());
        jdkInfo.setCurrent(true);
        assertTrue(jdkInfo.isCurrent());
        jdkInfo.setCurrent(false);
        assertFalse(jdkInfo.isCurrent());
    }
    
    @Test
    void testIsValidJdkWithValidPath(@TempDir Path tempDir) throws Exception {
        // 创建有效的JDK目录结构
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);
        Files.createFile(binDir.resolve("java.exe"));
        Files.createFile(binDir.resolve("javac.exe"));
        
        assertTrue(JdkInfo.isValidJdk(tempDir));
    }
    
    @Test
    void testIsValidJdkWithInvalidPath(@TempDir Path tempDir) {
        // 空目录
        assertFalse(JdkInfo.isValidJdk(tempDir));
        
        // 缺少关键文件
        Path binDir = tempDir.resolve("bin");
        try {
            Files.createDirectories(binDir);
            Files.createFile(binDir.resolve("java.exe"));
            // 缺少javac.exe
        } catch (Exception e) {
            // 忽略异常
        }
        assertFalse(JdkInfo.isValidJdk(tempDir));
    }
    
    @Test
    void testIsValidJdkWithNonExistentPath() {
        Path nonExistentPath = Path.of("C:\\non\\existent\\path");
        assertFalse(JdkInfo.isValidJdk(nonExistentPath));
    }
    
    @Test
    void testIsValidJdkWithNullPath() {
        assertFalse(JdkInfo.isValidJdk(null));
    }
    
    @Test
    void testFromPathWithValidJdk(@TempDir Path tempDir) throws Exception {
        // 创建有效的JDK目录结构
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);
        Files.createFile(binDir.resolve("java.exe"));
        Files.createFile(binDir.resolve("javac.exe"));
        
        // 创建release文件
        Path releaseFile = tempDir.resolve("release");
        Files.writeString(releaseFile, "JAVA_VERSION=\"11.0.15\"");
        
        JdkInfo jdkInfo = JdkInfo.fromPath(tempDir);
        
        assertNotNull(jdkInfo);
        assertEquals("11.0.15", jdkInfo.getVersion());
        assertEquals(tempDir, jdkInfo.getPath());
    }
    
    @Test
    void testFromPathWithInvalidPath(@TempDir Path tempDir) {
        JdkInfo jdkInfo = JdkInfo.fromPath(tempDir);
        assertNull(jdkInfo);
    }
    
    @Test
    void testEquals() {
        Path path1 = Path.of("C:\\java\\jdk-11");
        Path path2 = Path.of("C:\\java\\jdk-17");
        
        JdkInfo jdk1a = new JdkInfo("11.0.15", path1, true);
        JdkInfo jdk1b = new JdkInfo("11.0.15", path1, true);
        JdkInfo jdk2 = new JdkInfo("17.0.5", path2, true);
        
        assertEquals(jdk1a, jdk1b);
        assertNotEquals(jdk1a, jdk2);
        assertNotEquals(jdk1a, null);
        assertNotEquals(jdk1a, "not a JdkInfo");
    }
    
    @Test
    void testHashCode() {
        Path path1 = Path.of("C:\\java\\jdk-11");
        Path path2 = Path.of("C:\\java\\jdk-17");
        
        JdkInfo jdk1a = new JdkInfo("11.0.15", path1, true);
        JdkInfo jdk1b = new JdkInfo("11.0.15", path1, true);
        JdkInfo jdk2 = new JdkInfo("17.0.5", path2, true);
        
        assertEquals(jdk1a.hashCode(), jdk1b.hashCode());
        assertNotEquals(jdk1a.hashCode(), jdk2.hashCode());
    }
    
    @Test
    void testToString() {
        String version = "11.0.15";
        Path path = Path.of("C:\\java\\jdk-11");
        boolean is64Bit = true;
        boolean isCurrent = false;
        
        JdkInfo jdkInfo = new JdkInfo(version, path, is64Bit);
        jdkInfo.setCurrent(isCurrent);
        
        String toString = jdkInfo.toString();
        assertTrue(toString.contains(version));
        assertTrue(toString.contains(path.toString()));
        assertTrue(toString.contains("64bit=true"));
        assertTrue(toString.contains("current=false"));
    }
}