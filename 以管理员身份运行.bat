@echo off
chcp 65001 >nul
rem Java版本管理器 - 以管理员身份运行
rem 这个批处理文件会请求管理员权限并启动JAR应用程序

rem 检查是否已经以管理员身份运行
net session >nul 2>&1
if %errorLevel% == 0 (
    echo 已经以管理员身份运行，正在启动应用程序...
    goto run_app
)

rem 请求管理员权限
echo 正在请求管理员权限...
echo.
rem 使用PowerShell请求管理员权限并重新运行此批处理
powershell -Command "Start-Process '%~f0' -Verb RunAs"
exit /b

:run_app
rem 获取脚本所在目录
set APP_DIR=%~dp0

rem 设置JavaFX模块路径
set JAVAFX_MODULES=%APP_DIR%target\modules-win

rem 检查modules-win目录中的模块状态，避免删除已有文件
echo [调试] 检查modules-win目录中的模块状态...
echo [调试] 当前modules-win目录内容:
dir "%JAVAFX_MODULES%" /b 2>nul

rem 检查是否存在win.jar文件，如果存在则重命名为标准名称
if exist "%JAVAFX_MODULES%\javafx-base-win.jar" (
    echo [调试] 发现javafx-base-win.jar，重命名为javafx-base.jar
    ren "%JAVAFX_MODULES%\javafx-base-win.jar" "javafx-base.jar" >nul
)
if exist "%JAVAFX_MODULES%\javafx-controls-win.jar" (
    echo [调试] 发现javafx-controls-win.jar，重命名为javafx-controls.jar
    ren "%JAVAFX_MODULES%\javafx-controls-win.jar" "javafx-controls.jar" >nul
)
if exist "%JAVAFX_MODULES%\javafx-fxml-win.jar" (
    echo [调试] 发现javafx-fxml-win.jar，重命名为javafx-fxml.jar
    ren "%JAVAFX_MODULES%\javafx-fxml-win.jar" "javafx-fxml.jar" >nul
)
if exist "%JAVAFX_MODULES%\javafx-graphics-win.jar" (
    echo [调试] 发现javafx-graphics-win.jar，重命名为javafx-graphics.jar
    ren "%JAVAFX_MODULES%\javafx-graphics-win.jar" "javafx-graphics.jar" >nul
)
echo [调试] 已处理Windows特定模块文件
echo [调试] 处理后modules-win目录内容:
dir "%JAVAFX_MODULES%" /b 2>nul

rem 检查JavaFX模块是否存在
echo 检查JavaFX模块目录: %JAVAFX_MODULES%
if not exist "%JAVAFX_MODULES%" (
    echo 警告: modules-win目录不存在，正在创建...
    mkdir "%JAVAFX_MODULES%"
)

if not exist "%JAVAFX_MODULES%\javafx-base.jar" (
    echo 警告: 未找到JavaFX模块，正在准备JavaFX模块...
    echo 检查源目录: %APP_DIR%target\modules
    
    rem 列出源目录内容用于调试
    echo 源目录内容:
    dir "%APP_DIR%target\modules" /b 2>nul
    if errorlevel 1 (
        echo 错误: 源目录不存在或无法访问
    )
    
    rem 复制Windows特定的JavaFX JAR文件
    echo [调试] 检查源模块目录: %APP_DIR%target\modules
    dir "%APP_DIR%target\modules" /b 2>nul
    
    if exist "%APP_DIR%target\modules\javafx-base-17.0.2-win.jar" (
        echo [调试] 正在复制Windows特定的JavaFX模块...
        copy "%APP_DIR%target\modules\javafx-base-17.0.2-win.jar" "%JAVAFX_MODULES%\javafx-base.jar" >nul && echo [调试] 复制javafx-base.jar成功 || echo [错误] 复制javafx-base.jar失败
        copy "%APP_DIR%target\modules\javafx-controls-17.0.2-win.jar" "%JAVAFX_MODULES%\javafx-controls.jar" >nul && echo [调试] 复制javafx-controls.jar成功 || echo [错误] 复制javafx-controls.jar失败
        copy "%APP_DIR%target\modules\javafx-fxml-17.0.2-win.jar" "%JAVAFX_MODULES%\javafx-fxml.jar" >nul && echo [调试] 复制javafx-fxml.jar成功 || echo [错误] 复制javafx-fxml.jar失败
        copy "%APP_DIR%target\modules\javafx-graphics-17.0.2-win.jar" "%JAVAFX_MODULES%\javafx-graphics.jar" >nul && echo [调试] 复制javafx-graphics.jar成功 || echo [错误] 复制javafx-graphics.jar失败
        echo [调试] 已成功复制Windows特定的JavaFX模块
    ) else if exist "%APP_DIR%target\modules\javafx-base.jar" (
        echo [调试] 警告: 未找到Windows特定的JavaFX模块，使用通用模块...
        copy "%APP_DIR%target\modules\javafx-base.jar" "%JAVAFX_MODULES%\javafx-base.jar" >nul && echo [调试] 复制通用javafx-base.jar成功 || echo [错误] 复制通用javafx-base.jar失败
        copy "%APP_DIR%target\modules\javafx-controls.jar" "%JAVAFX_MODULES%\javafx-controls.jar" >nul && echo [调试] 复制通用javafx-controls.jar成功 || echo [错误] 复制通用javafx-controls.jar失败
        copy "%APP_DIR%target\modules\javafx-fxml.jar" "%JAVAFX_MODULES%\javafx-fxml.jar" >nul && echo [调试] 复制通用javafx-fxml.jar成功 || echo [错误] 复制通用javafx-fxml.jar失败
        copy "%APP_DIR%target\modules\javafx-graphics.jar" "%JAVAFX_MODULES%\javafx-graphics.jar" >nul && echo [调试] 复制通用javafx-graphics.jar成功 || echo [错误] 复制通用javafx-graphics.jar失败
        echo [调试] 已成功复制通用JavaFX模块
    ) else (
        echo [错误] 未找到任何JavaFX模块，请先运行构建命令
        echo [调试] 请运行 build-and-package.bat 来重新构建项目
        pause
        exit /b 1
    )
) else (
    echo JavaFX模块已存在，跳过复制步骤
)

rem 验证modules-win目录内容
echo [调试] 验证modules-win目录内容:
dir "%JAVAFX_MODULES%" /b
echo [调试] 最终modules-win目录详细内容:
dir "%JAVAFX_MODULES%" /s 2>nul

rem 启动应用程序
echo 正在以管理员身份启动Java版本管理器...
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 --module-path "%JAVAFX_MODULES%" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base -jar "%APP_DIR%target\jdk-manager-1.0.0-executable.jar"

rem 如果应用程序出错，显示错误信息
if errorlevel 1 (
    echo 应用程序启动失败，请检查Java环境是否正确安装
    pause
)