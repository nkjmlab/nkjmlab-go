setlocal
cd /d %~dp0
mvn-caller.bat "mvn clean" "mvn compile" "mvn dependency:copy-dependencies -DoutputDirectory=target/lib"
