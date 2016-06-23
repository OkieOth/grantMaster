#!/bin/bash

tomcat8Webapps=/opt/tomcat8/webapps

if ! [ -f $tomcat8Webapps/unprotectedWar.war ]; then
    cd /vagrant/sampleApps/unprotectedWar
    gradle clean war
    rm -rf $tomcat8Webapps/unprotectedWar*
    cp build/libs/unprotectedWar.war $tomcat8Webapps
fi

if ! cat /etc/rc.local | grep tomcat8
then
    sudo chmod 777 /etc/rc.local
    echo 'sudo su vagrant -c /opt/tomcat8/bin/startup.sh' >> /etc/rc.local
    sudo chmod 755 /etc/rc.local
fi
