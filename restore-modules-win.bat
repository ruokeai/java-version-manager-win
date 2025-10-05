@echo off
rem 恢复modules-win目录中的win.jar文件
rem 这个脚本用于恢复被意外删除的Windows特定JavaFX模块

chcp 65001
cd /d %~dp0

echo ==================================================
echo          恢复modules-win目录中的win.jar文件
echo ==================================================

set MODULES_WIN_DIR=%~dp0target\modules-win
set MODULES_DIR=%~dp0target\modules

echo [调试] 检查modules-win目录: %MODULES_WIN_DIR%
echo [调试] 检查modules目录: %MODULES_DIR%

rem 确保modules-win目录存在
if not exist "%MODULES_WIN_DIR%" (
    echo [调试] 创建modules-win目录
    mkdir "%MODULES_WIN_DIR%"
)

echo [调试] 当前modules-win目录内容:
dir "%MODULES_WIN_DIR%" /b 2>nul

echo [调试] 当前modules目录内容:
dir "%MODULES_DIR%" /b 2>nul

rem 首先尝试从modules目录复制win.jar文件
if exist "%MODULES_DIR%\javafx-base-17.0.2-win.jar" (
    echo [调试] 从modules目录复制Windows特定模块...
    copy "%MODULES_DIR%\javafx-base-17.0.2-win.jar" "%MODULES_WIN_DIR%\javafx-base-win.jar" >nul
    copy "%MODULES_DIR%\javafx-controls-17.0.2-win.jar" "%MODULES_WIN_DIR%\javafx-controls-win.jar" >nul
    copy "%MODULES_DIR%\javafx-fxml-17.0.2-win.jar" "%MODULES_WIN_DIR%\javafx-fxml-win.jar" >nul
    copy "%MODULES_DIR%\javafx-graphics-17.0.2-win.jar" "%MODULES_WIN_DIR%\javafx-graphics-win.jar" >nul
    echo [调试] 已复制Windows特定模块到modules-win目录
) else (
    echo [警告] 未找到Windows特定模块文件
)

rem 检查并重命名win.jar文件为标准名称
if exist "%MODULES_WIN_DIR%\javafx-base-win.jar" (
    echo [调试] 重命名win.jar文件为标准名称...
    ren "%MODULES_WIN_DIR%\javafx-base-win.jar" "javafx-base.jar" >nul
    ren "%MODULES_WIN_DIR%\javafx-controls-win.jar" "javafx-controls.jar" >nul
    ren "%MODULES_WIN_DIR%\javafx-fxml-win.jar" "javafx-fxml.jar" >nul
    ren "%MODULES_WIN_DIR%\javafx-graphics-win.jar" "javafx-graphics.jar" >nul
    echo [调试] 已重命名所有win.jar文件
)

rem 如果标准文件不存在，尝试从modules目录复制通用模块
if not exist "%MODULES_WIN_DIR%\javafx-base.jar" (
    if exist "%MODULES_DIR%\javafx-base.jar" (
        echo [调试] 复制通用模块作为备用...
        copy "%MODULES_DIR%\javafx-base.jar" "%MODULES_WIN_DIR%\javafx-base.jar" >nul
        copy "%MODULES_DIR%\javafx-controls.jar" "%MODULES_WIN_DIR%\javafx-controls.jar" >nul
        copy "%MODULES_DIR%\javafx-fxml.jar" "%MODULES_WIN_DIR%\javafx-fxml.jar" >nul
        copy "%MODULES_DIR%\javafx-graphics.jar" "%MODULES_WIN_DIR%\javafx-graphics.jar" >nul
        echo [调试] 已复制通用模块作为备用
    )
)

echo [调试] 恢复后modules-win目录内容:
dir "%MODULES_WIN_DIR%" /b 2>nul

echo ==================================================
echo modules-win目录恢复完成！
echo 现在可以尝试重新启动应用程序
echo ==================================================

pause