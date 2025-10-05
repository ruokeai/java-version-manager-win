package com.jdkmanager.ui;

import com.jdkmanager.scanner.JdkInfo;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * JDK列表项自定义组件
 * 负责在ListView中显示JDK信息的自定义单元格
 */
public class JdkListCell extends ListCell<JdkInfo> {
    private final HBox content;
    private final VBox textBox;
    private final Label versionLabel;
    private final Label pathLabel;
    private final Label archLabel;
    private final Circle statusIndicator;
    
    /**
     * 构造函数
     */
    public JdkListCell() {
        super();
        
        // 状态指示器
        statusIndicator = new Circle(6);
        statusIndicator.setFill(Color.LIGHTGRAY);
        
        // 文本内容
        versionLabel = new Label();
        versionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        pathLabel = new Label();
        pathLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12px;");
        
        archLabel = new Label();
        archLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        
        // 布局
        textBox = new VBox(2, versionLabel, pathLabel, archLabel);
        content = new HBox(10, statusIndicator, textBox);
        content.setAlignment(Pos.CENTER_LEFT);
    }
    
    @Override
    protected void updateItem(JdkInfo jdkInfo, boolean empty) {
        super.updateItem(jdkInfo, empty);
        
        if (empty || jdkInfo == null) {
            setGraphic(null);
            setTooltip(null);
        } else {
            versionLabel.setText(jdkInfo.getVersion());
            pathLabel.setText(jdkInfo.getPath().toString());
            archLabel.setText(jdkInfo.is64Bit() ? "64位" : "32位");
            
            // 设置状态指示器颜色
            if (jdkInfo.isCurrent()) {
                statusIndicator.setFill(Color.web("#4CAF50")); // 绿色
                versionLabel.setText(versionLabel.getText() + " (当前)");
            } else {
                statusIndicator.setFill(Color.LIGHTGRAY);
            }
            
            // 设置工具提示
            String tooltipText = createTooltipText(jdkInfo);
            setTooltip(new javafx.scene.control.Tooltip(tooltipText));
            
            setGraphic(content);
        }
    }
    
    /**
     * 创建工具提示文本
     * @param jdkInfo JDK信息
     * @return 工具提示文本
     */
    private String createTooltipText(JdkInfo jdkInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("版本: ").append(jdkInfo.getVersion()).append("\n");
        sb.append("路径: ").append(jdkInfo.getPath()).append("\n");
        sb.append("架构: ").append(jdkInfo.is64Bit() ? "64位" : "32位").append("\n");
        
        if (jdkInfo.isCurrent()) {
            sb.append("状态: 当前使用的JDK");
        } else {
            sb.append("状态: 可切换到此版本");
        }
        
        return sb.toString();
    }
}