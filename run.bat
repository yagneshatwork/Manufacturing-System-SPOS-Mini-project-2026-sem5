@echo off
title Smart Manufacturing Production Scheduler
cd /d "%~dp0"
echo Starting Smart Manufacturing Production Scheduler...
"C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot\bin\java.exe" -jar "target\manufacturing-sjf-runnable.jar"
if errorlevel 1 pause
