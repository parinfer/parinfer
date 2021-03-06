#!/bin/bash

# cleans, builds, and publishes the parinfer site to:
# http://shaunlebron.github.io/parinfer

set -ex

cd `dirname $0`

lein clean

vcr_file=src/parinfer_site/vcr.cljs
sed -i .bak 's/(def SHOW_CONTROLS .*)/(def SHOW_CONTROLS false)/' $vcr_file
lein cljsbuild once min
mv ${vcr_file}.bak $vcr_file

cd deploy

git reset HEAD .
git rm -rf *
cp -r ../resources/public/ .
git add -u
git add .
git commit -m "update deploy"
git push origin master:gh-pages

