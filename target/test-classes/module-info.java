module com.jdkmanager {
    // 导出我们的包，使JavaFX可以访问
    exports com.jdkmanager;
    exports com.jdkmanager.ui;
    exports com.jdkmanager.scanner;
    exports com.jdkmanager.config;
    exports com.jdkmanager.exception;
    
    // 打开包以便JavaFX可以通过反射访问
    opens com.jdkmanager.ui to javafx.fxml;
    opens com.jdkmanager to javafx.fxml, javafx.base;
    
    // 依赖的JavaFX模块
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    
    // 依赖的其他模块
    requires java.base;
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    
    // 指定主类
    provides javafx.application.Application with com.jdkmanager.JdkManagerApp;
}