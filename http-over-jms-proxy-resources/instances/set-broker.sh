#!/bin/bash

CURR_DIR=$(dirname "$(readlink -f "$0")")

CUSTOM_URL=$1

if [[ "" == "${CUSTOM_URL}" ]];then
    echo "Missing host address as argument 1"
    echo ""
    echo "This programm configures the url uf the JMS broker for the HTTP2JMS proxy environment"
    echo ""
    echo "Example:"
    echo "        $0 192.168.0.10:61616"
 exit
fi

echo "Update BROKER_DESTINATION to ${CUSTOM_URL}"

CUSTOM_URL="$(echo "${CUSTOM_URL}" | sed s#\\.#\\\\.#g)"

echo "Consumer BROKER_DESTINATION"
cp -f ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties jms-proxy.properties.tmp
sed s#BROKER_DESTINATION=.*#BROKER_DESTINATION=${CUSTOM_URL}#g \
    jms-proxy.properties.tmp > jms-proxy.properties.tmp.1
mv -f jms-proxy.properties.tmp.1 jms-proxy.properties.tmp
mv -f jms-proxy.properties.tmp ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties
cat ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties | grep BROKER_DESTINATION

echo "Producer BROKER_DESTINATION"
cp -f ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties jms-proxy.properties.tmp
sed s#BROKER_DESTINATION=.*#BROKER_DESTINATION=${CUSTOM_URL}#g \
    jms-proxy.properties.tmp > jms-proxy.properties.tmp.1
mv -f jms-proxy.properties.tmp.1 jms-proxy.properties.tmp
mv -f jms-proxy.properties.tmp ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties
cat ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties | grep BROKER_DESTINATION


