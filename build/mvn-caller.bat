setlocal
chcp 65001
@echo off
set CURRENT_DIR=%~dp0
set PATH=%PATH%;%CURRENT_DIR%\tools

cd /d %CURRENT_DIR%/../nkjmlab-go-webapp

jps -lm|grep org.nkjmlab.go | gawk "{print $1}" | xargs -r -n1 taskkill /F /T /PID

:loop
if "%~1"=="" goto end
call %~1
shift
goto loop
:end

@if errorlevel 1 pause
endlocal