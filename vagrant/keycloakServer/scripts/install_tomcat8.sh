#!/bin/bash

cd /opt

shutdownPort=7003
httpPort=7001
ajpPort=7002
redirectPort=7004

if [ ! -L /opt/tomcat8 ]; then
  curl http://tomcat.apache.org/download-80.cgi 2>/dev/null | grep '.zip\"' | head -n 1 | sed -e 's-<a href=\"--' -e 's-\"--' | awk '{print $1}' | xargs wget
  unzip apache-tomcat*.zip
  rm -f apache-tomcat*.zip
  ls -d apache-tomcat-8* | xargs bash -c 'ln -s $0 tomcat8'
  chmod u+x tomcat8/bin/*.sh
fi

# add change the default tcp ports ...
# 8005 - shutdown
# 8009 - ajp
# 8080 - http
# 8433 - redirect https
if cat /opt/tomcat8/conf/server.xml | grep 8080 > /dev/null
then
    cat /opt/tomcat8/conf/server.xml | sed -e "s-8080-$httpPort-" -e "s-8009-$ajpPort-" -e "s-8009-$shutdownPort-" | sed -e "s-8443-$redirectPort-" > /tmp/server.xml
    cp /tmp/server.xml /opt/tomcat8/conf
fi

# add autostart for tomcat
if ! cat /etc/rc.local | grep tomcat8 > /dev/null
then
    sudo chmod 777 /etc/rc.local
    echo 'sudo su vagrant -c /opt/tomcat8/bin/startup.sh' >> /etc/rc.local
    sudo chmod 755 /etc/rc.local
fi

