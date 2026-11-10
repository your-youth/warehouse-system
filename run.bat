@echo off
cd /d "%~dp0"

if not exist "target\warehouse-system.jar" (
    echo File target\warehouse-system.jar not found.
    echo Build it first: IntelliJ - Maven - Lifecycle - package
    pause
    exit /b 1
)

java -jar target\warehouse-system.jar
pause