sudo apt-get update

if [ ! -f /usr/bin/screen ]; then
  sudo apt-get install -y apache2 curl unzip screen tofrodos xsltproc
fi

if ! netstat -nat | grep 5432
then
  	sudo apt-get install -y postgresql
	if [ ! -f /home/vagrant/.pgpass ]
	then
	  sudo -u postgres psql -c "create user keycloak_db with password 'keycloakDb999';"
	  sudo -u postgres psql -c "ALTER USER keycloak_db WITH SUPERUSER;"
	  echo "localhost:5432:*:keycloak_db:keycloakDb999" > /home/vagrant/.pgpass
	  sudo chown vagrant:vagrant /home/vagrant/.pgpass
	  sudo chmod 600 /home/vagrant/.pgpass
	  sudo cp /home/vagrant/.pgpass /root
	  sudo -u postgres createdb keycloak_db
	fi
fi

sudo chown vagrant:vagrant /opt

if ! [ -d /opt/downloads ]
then 
	mkdir /opt/downloads
fi


