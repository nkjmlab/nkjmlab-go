@echo off
cd /d %~dp0
jps -lm|grep org.nkjmlab.go. | gawk "{print $1}" | xargs -r -n1 taskkill /F /T /PID
call mvn clean install dependency:copy-dependencies -DoutputDirectory=target/lib -f nkjmlab-go-webapp
