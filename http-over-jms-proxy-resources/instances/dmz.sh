#!/bin/bash

CURR_DIR=$(dirname "$(readlink -f "$0")")

if [[ "${JMS_BROKER_DESTINATION}" != "" ]];then
    ${CURR_DIR}/set-broker.sh "${JMS_BROKER_DESTINATION}"
fi

if [[ "${JMS_TARGET_URI}" != "" ]];then
    ${CURR_DIR}/set-url.sh "${JMS_TARGET_URI}"
fi

export JAVA_OPTS="-DHTTP2JMS_CONF=${CURR_DIR}/dmz/HTTP2JMS_CONF"; \
     ${CURR_DIR}/dmz/apache-tomcat-8.0.53-consumer/bin/catalina.sh $@
