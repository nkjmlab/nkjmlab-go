setlocal
cd /d %~dp0
cd ../
java -cp classes;lib/* -Dfile.encoding=UTF-8 org.nkjmlab.go.javalin.GoApplication
