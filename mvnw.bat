@echo off
chcp 65001 >nul

REM 设置Maven命令使用自定义settings.xml
REM 用法: mvnw 命令名 [参数]

REM 检查settings.xml是否存在
if not exist "%~dp0\settings.xml" (
    echo 错误: 找不到settings.xml文件
    pause
    exit /b 1
)

REM 执行Maven命令，使用自定义settings.xml
mvn %* -s "%~dp0\settings.xml"

REM 检查命令执行结果
if %ERRORLEVEL% neq 0 (
    echo Maven命令执行失败
    pause
)