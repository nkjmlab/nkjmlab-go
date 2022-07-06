cd `dirname $0`
cd ../
java -cp classes:lib/* -Dfile.encoding=UTF-8  -Dlog4j2.configurationFile=./classes/log4j2-production.xml org.nkjmlab.go.javalin.GoApplication $@

