@echo off
setlocal
chcp 65001
set _B=%~dp0
set BAT_DIR=%_B:~0,-1%

set wmicKillNameFilter=java
set wmicKillCommandLineFilter=org.nkjmlab.go.javalin.
set wmicCommand=wmic process where "name like '%%%wmicKillNameFilter%%%' and commandline like '%%%wmicKillCommandLineFilter%%%' " get processid /format:value

cd /d %BAT_DIR%/../nkjmlab-go-webapp


setlocal enabledelayedexpansion
echo %wmicCommand%
for /f "tokens=2 delims==; skip=1" %%i in ('%wmicCommand%') do (
    call echo [CALL] call taskkill /PID %%i /F
    call taskkill /PID %%i /F
)
endlocal

:loop
if "%~1"=="" goto end
echo [CALL] call %~1
call %~1
shift
goto loop
:end
@if errorlevel 1 pause
exit /b 0

endlocal
