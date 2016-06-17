#!/bin/bash

cd /opt

if [ ! -L /opt/tomcat8 ]; then
  curl http://tomcat.apache.org/download-80.cgi 2>/dev/null | grep '.zip\"' | head -n 1 | sed -e 's-<a href=\"--' -e 's-\"--' | awk '{print $1}' | xargs wget
  unzip apache-tomcat*.zip
  rm -f apache-tomcat*.zip
  ls -d apache-tomcat-8* | xargs bash -c 'ln -s $0 tomcat8'
fi
