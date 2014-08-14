#!/bin/bash

manual_location="../../TarsosTex/sound_and_java.pdf"
manual_location="/home/joren/Dropbox/UGent/LaTeX/sound_and_java.pdf"

#Remove old releases
rm -R TarsosDSP-*

#link the source files at the expected locations
cd ..
#test -f src || ln -s TarsosDSP/src/main/java/ src
#test -f examples || ln -s TarsosDSPExamples/src/main/java/ examples
#test -f tests || ln -s TarsosDSP/src/test/java tests 
cd build

#Build the new release
ant release

#Find the current version
filename=$(basename TarsosDSP-*.jar)
version=${filename:10:3}

#build the android library
ant tarsos_dsp_android_library

deploy_dir="/var/www/be.0110/current/public/releases/TarsosDSP/"
deploy_location=$deploy_dir"TarsosDSP-$version/"

#Build the readme file
textile2html ../README.textile TarsosDSP-$version-Readme.html

#Copy the manual
cp $manual_location TarsosDSP-$version-Manual.pdf


#Remove old version from the server:
ssh joren@0110.be rm -R $deploy_location

ssh joren@0110.be mkdir $deploy_location
#Deploy to the server 
scp -r TarsosDSP-* joren@0110.be:$deploy_location

deploy_dir="/var/www/be.0110/current/public/releases/TarsosDSP"
deploy_location=$deploy_dir/TarsosDSP-$version

ssh joren@0110.be rm -R $deploy_dir/TarsosDSP-latest
ssh joren@0110.be mkdir $deploy_dir/TarsosDSP-latest
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-$version.jar $deploy_dir/TarsosDSP-latest/TarsosDSP-latest.jar
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-$version-bin.jar $deploy_dir/TarsosDSP-latest/TarsosDSP-latest-bin.jar
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-Android-$version.jar $deploy_dir/TarsosDSP-latest/TarsosDSP-Android-latest.jar
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-Android-$version-bin.jar $deploy_dir/TarsosDSP-latest/TarsosDSP-Android-latest-bin.jar
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-$version-Manual.pdf $deploy_dir/TarsosDSP-latest/TarsosDSP-latest-Manual.pdf
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-$version-Documentation $deploy_dir/TarsosDSP-latest/TarsosDSP-latest-Documentation
ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-$version-Readme.html $deploy_dir/TarsosDSP-latest/TarsosDSP-latest-Readme.html

ssh joren@0110.be mkdir $deploy_dir/TarsosDSP-latest/TarsosDSP-latest-Examples 

for f in TarsosDSP-$version-Examples/*.jar
do 
	name=`basename $f`
	new_name=${name/$version/latest}
	ssh joren@0110.be ln -s -f $deploy_location/TarsosDSP-$version-Examples/$name  $deploy_dir/TarsosDSP-latest/TarsosDSP-latest-Examples/$new_name
done
