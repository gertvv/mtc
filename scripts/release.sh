#!/bin/bash

echo '---- Getting version from Maven'
mvn validate

VERSION=`cat version`
rm version
GUIDIR=gemtc-gui-$VERSION
CLIDIR=gemtc-cli-$VERSION

if [ "$VERSION" = '' ]; then
	echo '!!!! Error: could not get version';
	exit;
else
	echo "---- Version: $VERSION"
fi

if [[ "$VERSION" == *-SNAPSHOT ]]; then
	echo '!!!! Not packaging -SNAPSHOT';
	exit;
fi;

# Add license to all files
echo '---- Putting license on all sources'
ant license || exit

# Package
echo '---- Building JAR'
#mvn clean package -q|| exit

# Package the GUI
rm -Rf $GUIDIR
mkdir $GUIDIR
ARTIFACT=$GUIDIR/gemtc-gui-$VERSION.jar
cp mtc-gui/target/mtc-gui-$VERSION-jar-with-dependencies.jar $ARTIFACT
chmod a+x $ARTIFACT 
cp LICENSE.txt $GUIDIR
cp mtc-gui/README.txt $GUIDIR
mkdir $GUIDIR/example
cp example/*.gemtc $GUIDIR/example/
cp r-code/mtc/R/mtc.R $GUIDIR/
zip -r gemtc-gui-$VERSION.zip $GUIDIR

# Package the CLI
rm -Rf $CLIDIR
mkdir $CLIDIR
ARTIFACT=$CLIDIR/gemtc-cli-$VERSION.jar
cp mtc-cli/target/mtc-cli-$VERSION-jar-with-dependencies.jar $ARTIFACT
chmod a+x $ARTIFACT 
cp LICENSE.txt $CLIDIR
cp mtc-cli/README.txt $CLIDIR
mkdir $CLIDIR/example
cp example/*.gemtc $CLIDIR/example/
cp r-code/mtc/R/mtc.R $CLIDIR/
zip -r gemtc-cli-$VERSION.zip $CLIDIR

# Package the R package
cd r-code
make mtc_$VERSION.tar.gz
mv mtc_$VERSION.tar.gz ..
cd ..