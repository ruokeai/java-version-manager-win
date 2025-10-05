@echo off
REM 修复modules-win目录问题的脚本
REM 这个脚本会确保modules-win目录在重新打包后仍然存在

chcp 65001
cd /d %~dp0

echo ==================================================
echo          修复modules-win目录问题
echo ==================================================

REM 检查modules目录是否存在
if not exist "target\modules" (
    echo 错误: modules目录不存在，请先运行构建命令
    echo 正在运行构建命令...
    call build-and-package.bat
    if errorlevel 1 (
        echo 构建失败，无法修复modules-win目录
        pause
        exit /b 1
    )
)

REM 创建modules-win目录
echo 创建modules-win目录...
if not exist "target\modules-win" mkdir "target\modules-win"

REM 清理通用模块，保留Windows特定模块
echo 清理通用模块，保留Windows特定模块...
if exist "target\modules-win\javafx-base.jar" del "target\modules-win\javafx-base.jar" >nul
if exist "target\modules-win\javafx-controls.jar" del "target\modules-win\javafx-controls.jar" >nul
if exist "target\modules-win\javafx-fxml.jar" del "target\modules-win\javafx-fxml.jar" >nul
if exist "target\modules-win\javafx-graphics.jar" del "target\modules-win\javafx-graphics.jar" >nul

REM 检查并使用Windows特定模块
if exist "target\modules-win\javafx-base-win.jar" (
    echo 找到Windows特定模块，正在重命名...
    ren "target\modules-win\javafx-base-win.jar" "javafx-base.jar"
    ren "target\modules-win\javafx-controls-win.jar" "javafx-controls.jar"
    ren "target\modules-win\javafx-fxml-win.jar" "javafx-fxml.jar"
    ren "target\modules-win\javafx-graphics-win.jar" "javafx-graphics.jar"
    echo 已成功重命名Windows特定JavaFX模块
) else if exist "target\modules\javafx-base.jar" (
    echo 未找到Windows特定模块，使用通用模块...
    copy "target\modules\javafx-base.jar" "target\modules-win\javafx-base.jar" >nul
    copy "target\modules\javafx-controls.jar" "target\modules-win\javafx-controls.jar" >nul
    copy "target\modules\javafx-fxml.jar" "target\modules-win\javafx-fxml.jar" >nul
    copy "target\modules\javafx-graphics.jar" "target\modules-win\javafx-graphics.jar" >nul
    echo 已成功复制通用JavaFX模块
) else (
    echo 错误: 未找到任何JavaFX模块
    pause
    exit /b 1
)

REM 验证复制结果
echo 验证modules-win目录内容:
dir "target\modules-win" /b

echo ==================================================
echo modules-win目录修复完成！
echo 现在可以正常启动应用程序了
echo ==================================================

pause