package com.jdkmanager.ui;

import com.jdkmanager.config.AppConfig;
import com.jdkmanager.env.EnvironmentManager;
import com.jdkmanager.env.EnvVariableScope;
import com.jdkmanager.exception.EnvironmentVariableException;
import com.jdkmanager.exception.InsufficientPrivilegeException;
import com.jdkmanager.scanner.DefaultJdkScanner;
import com.jdkmanager.scanner.JdkInfo;
import com.jdkmanager.scanner.JdkScanner;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 主窗口控制器
 * 负责处理用户界面的交互逻辑
 */
public class MainController {
    
    // UI组件
    @FXML private ListView<JdkInfo> jdkListView;
    @FXML private Label currentJdkLabel;
    @FXML private Button refreshButton;
    @FXML private Button addPathButton;
    @FXML private Button switchButton;
    @FXML private CheckBox userScopeCheck;
    @FXML private CheckBox systemScopeCheck;
    @FXML private Label permissionWarning;
    
    // 业务组件
    private JdkScanner jdkScanner;
    private EnvironmentManager environmentManager;
    private AppConfig appConfig;
    private Stage primaryStage;
    
    // 状态变量
    private boolean isRefreshing = false;
    
    /**
     * 初始化方法
     */
    @FXML
    public void initialize() {
        // 初始化业务组件
        jdkScanner = new DefaultJdkScanner();
        environmentManager = new EnvironmentManager();
        appConfig = new AppConfig();
        
        // 初始化扫描器的自定义路径
        List<Path> customPaths = appConfig.loadCustomPaths();
        if (jdkScanner instanceof DefaultJdkScanner) {
            ((DefaultJdkScanner) jdkScanner).initializeCustomPaths(customPaths);
        }
        
        // 设置列表单元格工厂
        jdkListView.setCellFactory(listView -> new JdkListCell());
        
        // 设置列表选择监听器
        jdkListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> onJdkSelected(newValue));
        
        // 设置复选框监听器
        userScopeCheck.selectedProperty().addListener(
            (observable, oldValue, newValue) -> onScopeChanged());
        
        systemScopeCheck.selectedProperty().addListener(
            (observable, oldValue, newValue) -> onScopeChanged());
        
        // 加载配置
        loadConfiguration();
        
        // 初始刷新JDK列表
        refreshJdkListAsync();
    }
    
    /**
     * 设置主窗口
     * @param primaryStage 主窗口
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // 设置窗口大小和位置
        if (appConfig.hasConfiguration()) {
            double width = appConfig.loadWindowWidth();
            double height = appConfig.loadWindowHeight();
            double x = appConfig.loadWindowX();
            double y = appConfig.loadWindowY();
            
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            
            if (x >= 0 && y >= 0) {
                primaryStage.setX(x);
                primaryStage.setY(y);
            }
        }
        
        // 窗口关闭时保存配置
        primaryStage.setOnHidden(event -> saveConfiguration());
    }
    
    /**
     * 刷新JDK列表
     */
    @FXML
    private void refreshJdkList() {
        refreshJdkListAsync();
    }
    
    /**
     * 异步刷新JDK列表
     */
    private void refreshJdkListAsync() {
        if (isRefreshing) {
            return;
        }
        
        isRefreshing = true;
        refreshButton.setDisable(true);
        refreshButton.setText("刷新中...");
        
        Task<List<JdkInfo>> refreshTask = new Task<>() {
            @Override
            protected List<JdkInfo> call() throws Exception {
                // 加载自定义路径并初始化扫描器
                List<Path> customPaths = appConfig.loadCustomPaths();
                if (jdkScanner instanceof DefaultJdkScanner) {
                    ((DefaultJdkScanner) jdkScanner).initializeCustomPaths(customPaths);
                } else {
                    // 兼容性处理
                    for (Path customPath : customPaths) {
                        jdkScanner.addCustomPath(customPath);
                    }
                }
                
                // 扫描JDK
                return jdkScanner.scanForJdks();
            }
            
            @Override
            protected void succeeded() {
                List<JdkInfo> jdks = getValue();
                updateJdkList(jdks);
                updateCurrentJdkLabel(jdks);
                isRefreshing = false;
                refreshButton.setDisable(false);
                refreshButton.setText("刷新");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                showError("刷新失败", "扫描JDK时发生错误: " + exception.getMessage());
                isRefreshing = false;
                refreshButton.setDisable(false);
                refreshButton.setText("刷新");
            }
        };
        
        new Thread(refreshTask).start();
    }
    
    /**
     * 添加自定义路径
     */
    @FXML
    private void addCustomPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择JDK安装目录");
        
        // 设置初始目录
        File initialDir = new File("C:\\");
        if (initialDir.exists()) {
            directoryChooser.setInitialDirectory(initialDir);
        }
        
        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            Path selectedPath = selectedDirectory.toPath();
            
            // 验证是否为有效JDK
            if (!jdkScanner.validateJdk(selectedPath)) {
                showWarning("无效路径", "选择的目录不是有效的JDK安装目录");
                return;
            }
            
            // 添加到扫描器
            if (jdkScanner.addCustomPath(selectedPath)) {
                // 保存配置
                List<Path> customPaths = appConfig.loadCustomPaths();
                customPaths.add(selectedPath);
                appConfig.saveCustomPaths(customPaths);
                
                // 刷新列表
                refreshJdkListAsync();
                
                showInfo("添加成功", "已添加自定义路径: " + selectedPath);
            } else {
                showWarning("添加失败", "该路径已经存在于扫描列表中");
            }
        }
    }
    
    /**
     * 切换JDK
     */
    @FXML
    private void switchJdk() {
        JdkInfo selectedJdk = jdkListView.getSelectionModel().getSelectedItem();
        if (selectedJdk == null) {
            return;
        }
        
        // 如果已经是当前JDK，不需要切换
        if (selectedJdk.isCurrent()) {
            showInfo("无需切换", "当前已经是此JDK版本");
            return;
        }
        
        // 获取选择的作用域
        List<EnvVariableScope> selectedScopes = getSelectedScopes();
        if (selectedScopes.isEmpty()) {
            showError("选择错误", "请至少选择一个环境变量作用域");
            return;
        }
        
        // 确认对话框
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认切换");
        confirmDialog.setHeaderText("切换JDK版本");
        
        StringBuilder scopeText = new StringBuilder();
        for (EnvVariableScope scope : selectedScopes) {
            if (scopeText.length() > 0) {
                scopeText.append(", ");
            }
            scopeText.append(scope.getDisplayName());
        }
        
        confirmDialog.setContentText(String.format(
            "确定要将默认JDK切换到 %s 吗？\n\n路径: %s\n作用域: %s",
            selectedJdk.getVersion(),
            selectedJdk.getPath(),
            scopeText.toString()
        ));
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            switchJdkAsync(selectedJdk, selectedScopes);
        }
    }
    
    /**
     * 异步切换JDK
     * @param jdkInfo 要切换到的JDK
     * @param scopes 作用域列表
     */
    private void switchJdkAsync(JdkInfo jdkInfo, List<EnvVariableScope> scopes) {
        switchButton.setDisable(true);
        switchButton.setText("切换中...");
        
        Task<Boolean> switchTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                boolean allSuccess = true;
                for (EnvVariableScope scope : scopes) {
                    boolean success = environmentManager.switchJdk(jdkInfo.getPath(), scope);
                    if (!success) {
                        allSuccess = false;
                        break;
                    }
                }
                return allSuccess;
            }
            
            @Override
            protected void succeeded() {
                boolean success = getValue();
                switchButton.setDisable(false);
                switchButton.setText("切换到此JDK");
                
                if (success) {
                    showInfo("切换成功", "JDK版本已成功切换到 " + jdkInfo.getVersion());
                    // 强制刷新环境变量缓存并刷新列表显示
                    forceRefreshJdkDetection();
                } else {
                    showError("切换失败", "无法切换到指定的JDK版本");
                }
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                switchButton.setDisable(false);
                switchButton.setText("切换到此JDK");
                
                if (exception instanceof InsufficientPrivilegeException) {
                    showError("权限不足", exception.getMessage() + "\n\n请以管理员身份运行程序或选择用户级环境变量。");
                } else {
                    showError("切换失败", "切换JDK时发生错误: " + exception.getMessage());
                }
            }
        };
        
        new Thread(switchTask).start();
    }
    
    /**
     * JDK选择事件处理
     * @param selectedJdk 选中的JDK
     */
    private void onJdkSelected(JdkInfo selectedJdk) {
        boolean enabled = selectedJdk != null && !selectedJdk.isCurrent();
        switchButton.setDisable(!enabled);
        
        if (selectedJdk != null) {
            switchButton.setText(selectedJdk.isCurrent() ? "当前JDK" : "切换到此JDK");
        } else {
            switchButton.setText("切换到此JDK");
        }
    }
    
    /**
     * 作用域改变事件处理
     */
    private void onScopeChanged() {
        boolean userSelected = userScopeCheck.isSelected();
        boolean systemSelected = systemScopeCheck.isSelected();
        
        System.out.println("作用域变更 - 用户级: " + userSelected + ", 系统级: " + systemSelected);
        
        if (systemSelected) {
            // 检查管理员权限
            if (!environmentManager.hasAdminRights()) {
                permissionWarning.setVisible(true);
                switchButton.setDisable(true);
                System.out.println("系统级被选中但缺少管理员权限");
            } else {
                permissionWarning.setVisible(false);
                // 重新检查按钮状态
                JdkInfo selectedJdk = jdkListView.getSelectionModel().getSelectedItem();
                onJdkSelected(selectedJdk);
                System.out.println("系统级被选中且有管理员权限");
            }
        } else {
            permissionWarning.setVisible(false);
            // 重新检查按钮状态
            JdkInfo selectedJdk = jdkListView.getSelectionModel().getSelectedItem();
            onJdkSelected(selectedJdk);
            System.out.println("系统级未选中");
        }
    }
    
    /**
     * 获取选择的作用域列表
     * @return 选择的作用域列表
     */
    private List<EnvVariableScope> getSelectedScopes() {
        List<EnvVariableScope> scopes = new ArrayList<>();
        if (userScopeCheck.isSelected()) {
            scopes.add(EnvVariableScope.USER);
        }
        if (systemScopeCheck.isSelected()) {
            scopes.add(EnvVariableScope.SYSTEM);
        }
        
        System.out.println("选中的作用域: " + scopes.stream()
            .map(EnvVariableScope::getDisplayName)
            .collect(java.util.stream.Collectors.joining(", ")));
        
        return scopes;
    }
    
    /**
     * 更新JDK列表
     * @param jdks JDK列表
     */
    private void updateJdkList(List<JdkInfo> jdks) {
        Platform.runLater(() -> {
            jdkListView.getItems().clear();
            jdkListView.getItems().addAll(jdks);
            
            // 如果有当前JDK，选中它
            jdks.stream()
                .filter(JdkInfo::isCurrent)
                .findFirst()
                .ifPresent(currentJdk -> 
                    jdkListView.getSelectionModel().select(currentJdk));
        });
    }
    
    /**
     * 更新当前JDK标签
     * @param jdks JDK列表
     */
    private void updateCurrentJdkLabel(List<JdkInfo> jdks) {
        Platform.runLater(() -> {
            Optional<JdkInfo> currentJdk = jdks.stream()
                .filter(JdkInfo::isCurrent)
                .findFirst();
            
            if (currentJdk.isPresent()) {
                String version = currentJdk.get().getVersion();
                String path = currentJdk.get().getPath().toString();
                currentJdkLabel.setText("当前JDK: " + version);
                System.out.println("UI更新 - 当前JDK: " + version + " at " + path);
            } else {
                currentJdkLabel.setText("当前JDK: 未检测到");
                System.out.println("UI更新 - 未检测到当前JDK");
            }
        });
    }
    
    /**
     * 加载配置
     */
    private void loadConfiguration() {
        // 加载作用域选择
        EnvVariableScope lastScope = appConfig.loadLastSelectedScope();
        // 默认选择用户级，如果上次选择的是系统级，则同时选择用户级和系统级
        userScopeCheck.setSelected(true);
        systemScopeCheck.setSelected(lastScope == EnvVariableScope.SYSTEM);
        
        // 初始化作用域状态
        onScopeChanged();
    }
    
    /**
     * 保存配置
     */
    private void saveConfiguration() {
        // 保存窗口状态
        if (primaryStage != null) {
            appConfig.saveWindowState(
                primaryStage.getWidth(),
                primaryStage.getHeight(),
                primaryStage.getX(),
                primaryStage.getY()
            );
        }
        
        // 保存作用域选择 - 优先保存系统级选择，如果系统级被选中则保存系统级，否则保存用户级
        if (systemScopeCheck.isSelected()) {
            appConfig.saveLastSelectedScope(EnvVariableScope.SYSTEM);
        } else if (userScopeCheck.isSelected()) {
            appConfig.saveLastSelectedScope(EnvVariableScope.USER);
        }
        
        // 保存自定义路径（已经在添加时保存）
    }
    
    /**
     * 显示错误对话框
     * @param title 标题
     * @param message 消息
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 显示警告对话框
     * @param title 标题
     * @param message 消息
     */
    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 显示信息对话框
     * @param title 标题
     * @param message 消息
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    /**
     * 强制刷新JDK检测
     * 在JDK切换后调用此方法，确保能够检测到最新的JDK状态
     */
    private void forceRefreshJdkDetection() {
        // 创建一个新的任务来强制刷新JDK检测
        Task<Void> forceRefreshTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 等待更长时间，确保环境变量更改生效
                System.out.println("开始等待环境变量更改生效...");
                for (int i = 1; i <= 3; i++) {
                    System.out.println("等待 " + i + "/3 秒...");
                    Thread.sleep(1000);
                }
                
                // 检查当前系统环境变量
                String currentJavaHome = System.getenv("JAVA_HOME");
                System.out.println("系统JAVA_HOME (缓存): " + currentJavaHome);
                
                // 通过新的CMD进程检查实际的java版本，确保使用最新的环境变量
                try {
                    System.out.println("通过新CMD进程检查java版本...");
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "java -version");
                    pb.redirectErrorStream(true);
                    // 清空环境变量，确保从注册表重新读取
                    pb.environment().clear();
                    Process process = pb.start();
                    
                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    }
                    
                    process.waitFor();
                    System.out.println("新CMD进程中的java版本:\n" + output.toString());
                } catch (Exception e) {
                    System.err.println("检查新CMD进程中的java版本失败: " + e.getMessage());
                }
                
                // 直接通过完整路径检查目标JDK版本
                JdkInfo selectedJdk = jdkListView.getSelectionModel().getSelectedItem();
                if (selectedJdk != null) {
                    try {
                        System.out.println("直接检查目标JDK版本...");
                        Path targetJavaExe = selectedJdk.getPath().resolve("bin").resolve("java.exe");
                        ProcessBuilder pb = new ProcessBuilder(targetJavaExe.toString(), "-version");
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        
                        StringBuilder output = new StringBuilder();
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                        }
                        
                        process.waitFor();
                        System.out.println("目标JDK直接版本检查:\n" + output.toString());
                    } catch (Exception e) {
                        System.err.println("直接检查目标JDK版本失败: " + e.getMessage());
                    }
                }
                
                // 强制刷新JDK列表
                System.out.println("开始刷新JDK列表...");
                Platform.runLater(() -> refreshJdkListAsync());
                
                return null;
            }
        };
        
        new Thread(forceRefreshTask).start();
    }
}