@echo off
cd /d "%~dp0"
java -jar target\warehouse-system.jar
if errorlevel 1 pause
