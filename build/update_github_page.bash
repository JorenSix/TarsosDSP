#!/bin/bash

#This Script Creates the TarsosDSP github page
#make sure everything on master banch is checked in !

cd ..

filename=$(basename build/TarsosDSP-*-bin.jar)
version=${filename:10:3}
git fetch origin
git checkout gh-pages

cat build/header.html > index.html
cat build/TarsosDSP-$version-Readme.html >> index.html
cat build/footer.html >> index.html

git add index.html
git commit -m "Updated index html file."
git push origin gh-pages
git checkout master
