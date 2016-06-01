#!/bin/bash

scriptPos=${0%/*}

packageBase=/opt/downloads

defUser=vagrant

# create a helper script to later modify the keycloak configuration
if ! [ -d /home/vagrant/scripts ]; then
	mkdir /home/vagrant/scripts
	cat << EOFX > /home/vagrant/scripts/setDbParams.xsl
<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="urn:jboss:domain:4.0"
	xmlns:ds="urn:jboss:domain:datasources:4.0">
	<xsl:output method="xml" indent="yes"/> 
	<xsl:template match="ds:datasources">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<ds:datasource jndi-name="java:jboss/datasources/MyKeycloakDS" pool-name="MyKeycloakDS" enabled="true" use-java-context="true">
			    <ds:connection-url>jdbc:postgresql:keycloak_db</ds:connection-url>
			    <ds:driver>psql</ds:driver>
			    <ds:security>
				<ds:user-name>keycloak_db</ds:user-name>
				<ds:password>keycloakDb999</ds:password>
			    </ds:security>
			</ds:datasource>
			<xsl:apply-templates select="*"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="ds:drivers">
		<xsl:apply-templates select="@*"/>
                    <ds:driver name="psql" module="org.postgresql">
                        <ds:xa-datasource-class>org.postgresql.Driver</ds:xa-datasource-class>
                    </ds:driver>
		<xsl:apply-templates select="*"/>
	</xsl:template>
	<xsl:template match="text()">
		<xsl:value-of select="normalize-space()"/>
	</xsl:template>
	<!-- umkopieren der restlichen Zeilen -->
	<xsl:template match="*|@*|comment()|processing-instruction()">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|text()|comment()|processing-instruction()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
EOFX
fi

# step 1: Installation des Servers
if ! [ -L /opt/keycloak ]; then
	sudo chown -R $defUser /opt
	instArchiv='keycloak-1.9.5.Final.tar.gz'
	if ! cp "$packageBase/$instArchiv" /opt; then exit 1; fi
	cd /opt
	if ! tar -xzf "$instArchiv"; then exit 1; fi
	if ! rm -f "$instArchiv"; then exit 1; fi
	if ! ls -d keycloak* | xargs bash -c 'ln -s $0 keycloak'; then exit 1; fi
	sudo chown -R vagrant:vagrant /opt/keycloak*
fi


if ! [ -d /opt/bin ]; then
	mkdir /opt/bin	
	cat << EOF > /opt/bin/runKeycloakServer.sh
#!/bin/bash
/opt/keycloak/bin/standalone.sh
EOF
	chmod a+x /opt/bin/*.sh
fi

if ! cat /etc/rc.local | grep runKeycloakServer
then
	sudo chmod 777 /etc/rc.local
	echo '#!/bin/sh' > /etc/rc.local
	echo '' >> /etc/rc.local 
	echo 'sudo iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 8080 -j DNAT --to 127.0.0.1:8080' >> /etc/rc.local
	echo 'sudo iptables -I FORWARD 1 -m state -p tcp -d 127.0.0.1 --dport 8080 --state NEW,ESTABLISHED,RELATED -j ACCEPT' >> /etc/rc.local
	echo 'sudo sysctl -w net.ipv4.conf.eth0.route_localnet=1' >> /etc/rc.local
	echo "sudo su $defUser -c /opt/bin/runKeycloakServer.sh" >> /etc/rc.local
	sudo chmod 755 /etc/rc.local
fi

if ! [ -f /opt/conf/keycloak.conf ]; then
	if ! [ -d /opt/conf ]; then
		mkdir -p /opt/conf
	fi
	cat << EOF2 > /opt/conf/keycloak.conf 
ProxyPass /auth/ http://localhost:8080/auth/
ProxyPassReverse /auth/ http://localhost:8080/auth/
<Location /auth/>
        Header Set Cache-Control "max-age=0, no-store"
</Location>
EOF2
	sudo ln -s /opt/conf/keycloak.conf /etc/apache2/conf-enabled
	sudo a2enmod proxy	
	sudo a2enmod proxy_http	
	sudo a2enmod proxy_ajp
	sudo a2enmod headers
	sudo a2enmod proxy_html
#	sudo a2enmod substitute
	sudo /etc/init.d/apache2 restart
fi

# Anpassen der Konfiguration des SSO-Servers
# Postgresql-Treiber Wildfly verfügbar machen
PSQL_JARDIR=/opt/keycloak/modules/system/layers/keycloak/org/postgresql/main
if ! [ -d "$PSQL_JARDIR" ]; then
	mkdir -p "$PSQL_JARDIR"
	pushd "$PSQL_JARDIR" > /dev/null
	wget https://jdbc.postgresql.org/download/postgresql-9.3-1104.jdbc4.jar
	cat << EOF3 > module.xml
<?xml version="1.0" encoding="UTF-8"?>  
    <module xmlns="urn:jboss:module:1.0" name="org.postgresql">  
     <resources>  
	<resource-root path="postgresql-9.3-1104.jdbc4.jar"/>  
     </resources>  
     <dependencies>  
     	<module name="javax.api"/>  
     	<module name="javax.transaction.api"/>  
     </dependencies>  
</module>  
EOF3
	popd > /dev/null
fi

BACKUP_EXT=oth.bak

# Postgresql datasource vereinbaren
CONFIG_FILE="/opt/keycloak/standalone/configuration/standalone.xml"
if ! [ -f "$CONFIG_FILE.$BACKUP_EXT" ]; then
	mv "$CONFIG_FILE" "$CONFIG_FILE.$BACKUP_EXT"
	xsltproc /home/vagrant/scripts/setDbParams.xsl "$CONFIG_FILE.$BACKUP_EXT" > "$CONFIG_FILE"
fi

# Die Postgresql-Connection für Keycloak aktivieren
CONFIG_FILE="/opt/keycloak/standalone/configuration/keycloak-server.json"
if ! [ -f "$CONFIG_FILE.$BACKUP_EXT" ]; then
	mv "$CONFIG_FILE" "$CONFIG_FILE.$BACKUP_EXT"
	cat "$CONFIG_FILE.$BACKUP_EXT" | sed -e 's-KeycloakDS-MyKeycloakDS-g' > "$CONFIG_FILE"
fi

SSO_ADMIN=keycloak_admin
SSO_ADMIN_PWD=k6ycloakAdmin
if ! [ -f /opt/keycloak/standalone/configuration/keycloak-add-user.json ]; then
	/opt/keycloak/bin/add-user-keycloak.sh -r master -u $SSO_ADMIN -p $SSO_ADMIN_PWD
else
	if ! cat /opt/keycloak/standalone/configuration/keycloak-add-user.json | grep $SSO_ADMIN
	then
		/opt/keycloak/bin/add-user-keycloak.sh -r master -u $SSO_ADMIN -p $SSO_ADMIN_PWD
		pgrep java | xargs kill -9
	fi
fi

sudo reboot


