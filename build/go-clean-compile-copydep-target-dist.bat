setlocal
cd /d %~dp0
call mvn-caller.bat "mvn clean compile dependency:copy-dependencies -DoutputDirectory=target-dist/lib -Pdist"
endlocal
