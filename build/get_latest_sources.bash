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
bash deploy_nightly.bash
cd ..
cd ..
rm -rf TarsosDSP.zip JorenSix-TarsosDSP*