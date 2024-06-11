@echo off
setlocal
chcp 65001
set _B=%~dp0
set BAT_DIR=%_B:~0,-1%

powershell -ExecutionPolicy Bypass -File %BAT_DIR%\kill-process.ps1 org.nkjmlab.go.javalin.

cd /d %BAT_DIR%/../nkjmlab-go-webapp

:loop
if "%~1"=="" goto end
call %~1
shift
goto loop
:end

@if errorlevel 1 pause
endlocal
