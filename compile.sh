CP=conf/:classes/:lib/*
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath $SP -classpath $CP -d classes/ src/java/rise/*.java src/java/rise/*/*.java src/java/fr/cryptohash/*.java || exit 1

/bin/rm -f rise.jar 
jar cf rise.jar -C classes . || exit 1
/bin/rm -rf classes

echo "rise.jar generated successfully"
