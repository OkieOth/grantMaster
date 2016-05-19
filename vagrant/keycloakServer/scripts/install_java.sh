#!/bin/bash

# Java 8 installieren

# ATTENTION don't call in provision

sudo add-apt-repository ppa:webupd8team/java -y
sudo apt-get update
sudo apt-get install -y oracle-java8-installer
sudo apt-get install -y oracle-java8-set-default

