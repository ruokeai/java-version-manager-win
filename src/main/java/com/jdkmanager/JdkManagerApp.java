package com.jdkmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Java版本管理器应用程序主入口
 * 负责启动JavaFX应用程序和初始化主窗口
 */
public class JdkManagerApp extends Application {
    
    private static final String APP_TITLE = "Java版本管理器";
    private static final String FXML_PATH = "/fxml/main.fxml";
    private static final String CSS_PATH = "/css/styles.css";
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // 在模块化环境中加载FXML
            // 使用Class.getResourceAsStream确保在模块路径中正确查找资源
            FXMLLoader loader = new FXMLLoader();
            
            // 在模块化环境中，我们需要确保资源加载路径是正确的
            // 尝试多种方式加载资源，提高兼容性
            URL fxmlUrl = getClass().getResource(FXML_PATH);
            if (fxmlUrl == null) {
                // 尝试使用当前类的ClassLoader
                fxmlUrl = getClass().getClassLoader().getResource("fxml/main.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("无法找到FXML文件: " + FXML_PATH);
                }
            }
            
            loader.setLocation(fxmlUrl);
            
            Parent root = loader.load();
            
            // 获取控制器
            com.jdkmanager.ui.MainController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            
            // 加载CSS样式
            URL cssUrl = getClass().getResource(CSS_PATH);
            if (cssUrl == null) {
                // 尝试使用当前类的ClassLoader作为备用加载方式
                cssUrl = getClass().getClassLoader().getResource("css/styles.css");
            }
            
            if (cssUrl != null) {
                root.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("警告: 无法加载CSS样式文件: " + CSS_PATH);
            }
            
            // 创建场景
            Scene scene = new Scene(root, 800, 600);
            
            // 设置窗口属性
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);
            
            // 显示窗口
            primaryStage.show();
            
        } catch (IOException e) {
            System.err.println("启动应用程序失败: " + e.getMessage());
            e.printStackTrace();
            
            // 显示错误对话框
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("启动错误");
            alert.setHeaderText("无法启动应用程序");
            alert.setContentText("启动Java版本管理器时发生错误:\n" + e.getMessage());
            alert.showAndWait();
            
            // 退出应用程序
            Platform.exit();
        }
    }
    
    /**
     * 应用程序主方法
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 设置系统编码为UTF-8
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        
        // 添加模块路径检查
        String modulePath = System.getProperty("jdk.module.path");
        if (modulePath == null || modulePath.isEmpty()) {
            System.err.println("警告: 未设置JavaFX模块路径，尝试使用默认配置");
        }
        
        launch(args);
    }
}