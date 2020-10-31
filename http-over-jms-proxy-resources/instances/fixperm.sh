#!/bin/bash

chmod 755 broker.sh
chmod 755 dmz.sh
chmod 755 internet.sh
chmod 755 set-url.sh
chmod 755 set-broker.sh

chmod -R 755 dmz/apache-tomcat-8.0.53-consumer/bin
chmod -R 755 internet/apache-activemq-5.15.7-broker/bin
chmod -R 755 internet/apache-tomcat-8.0.53-producer/bin
