@echo off
rem Java Version Manager - Run as Administrator
rem This batch file requests admin privileges and starts the JAR application

rem Set console encoding to UTF-8
chcp 65001 >nul

rem Check if already running as Administrator
NET SESSION >nul 2>&1
if %errorLevel% == 0 (
    echo Already running as Administrator, starting application...
    goto run_app
)

rem Request Administrator privileges
echo Requesting Administrator privileges...
echo.
rem Use PowerShell to request admin privileges and rerun this batch
powershell -Command "Start-Process '%~f0' -Verb RunAs"
exit /b

:run_app
rem Get script directory
set APP_DIR=%~dp0

rem Verify JAR file exists
if not exist "%APP_DIR%target\jdk-manager-1.0.0-executable.jar" (
    echo [ERROR] Cannot find executable JAR file
    echo [DEBUG] Please ensure the project has been built successfully
    pause
    exit /b 1
)

rem Start application
setlocal EnableDelayedExpansion
set JAVA_BIN=java

rem Find Java executable in system path
for %%I in (java.exe) do set "JAVA_BIN=%%~$PATH:I"
if not defined JAVA_BIN (
    echo [ERROR] Java is not found in system PATH
    echo [DEBUG] Please install Java and add it to PATH
    pause
    exit /b 1
)

echo Starting Java Version Manager as Administrator...
echo [DEBUG] Starting command: !JAVA_BIN! -jar "%APP_DIR%target\jdk-manager-1.0.0-executable.jar"
"!JAVA_BIN!" -jar "%APP_DIR%target\jdk-manager-1.0.0-executable.jar"

rem Show error message if application fails
if errorlevel 1 (
    echo Application failed to start. Please check if Java is properly installed.
    pause
)
endlocal