#!/bin/bash
########################
#get the latest sources
#unzip
#build
#deploy
#cleanup
########################
wget https://github.com/JorenSix/TarsosDSP/zipball/master/ -O TarsosDSP.zip
unzip TarsosDSP.zip
cd JorenSix-TarsosDSP*/build/
ln -s TarsosDSPExamples/src/main/java/ examples
ln -s TarsosDSP/src/main/java/ src
ln -s TarsosDSP/src/test/java tests
bash deploy_nightly.bash
cd ..
cd ..
rm -rf TarsosDSP.zip JorenSix-TarsosDSP*