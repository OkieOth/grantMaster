#!/bin/bash

# install gradle to build test java stuff

cd /opt

if [ ! -L /opt/gradle ]; then
  curl http://gradle.org/gradle-download/ 2>/dev/null | grep 'bin.zip\"' | head -n 1 | sed -e 's-<a href=\"--' -e 's-<ul>--' -e 's-<li>--' -e 's-\".*--' | xargs wget
  unzip gradle*.zip
  rm -f gradle*.zip
  ls -d gradle* | xargs bash -c 'ln -s $0 gradle'
  sudo ln -s /opt/gradle/bin/gradle /usr/bin
fi

