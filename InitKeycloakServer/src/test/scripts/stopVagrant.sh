#!/bin/bash

scriptPos=`dirname $0`

echo "stop vagrant vm"
cd "$scriptPos/../../../../vagrant/keycloakServer" && vagrant halt