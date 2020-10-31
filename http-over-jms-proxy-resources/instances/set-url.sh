#!/bin/bash

CURR_DIR=$(dirname "$(readlink -f "$0")")

CUSTOM_URL=$1

if [[ "" == "${CUSTOM_URL}" ]];then
    echo "Missing url as argument 1"
    echo ""
    echo "Example:"
    echo "        $0 https://angular.io"
 exit
fi

echo "Consumer"

cp -f ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties jms-proxy.properties.tmp
sed s#targetUri=.*#targetUri=${CUSTOM_URL}#g \
    jms-proxy.properties.tmp > jms-proxy.properties.tmp.1
mv -f jms-proxy.properties.tmp.1 jms-proxy.properties.tmp
mv -f jms-proxy.properties.tmp ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties
cat ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties | grep "targetUri="

cp -f ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties jms-proxy.properties.tmp
sed s#TARGET_URI=.*#TARGET_URI=${CUSTOM_URL}#g \
    jms-proxy.properties.tmp > jms-proxy.properties.tmp.1
mv -f jms-proxy.properties.tmp.1 jms-proxy.properties.tmp
mv -f jms-proxy.properties.tmp ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties
cat ${CURR_DIR}/dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties | grep "TARGET_URI="


echo "Producer"

cp -f ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties jms-proxy.properties.tmp
sed s#targetUri=.*#targetUri=${CUSTOM_URL}#g \
    jms-proxy.properties.tmp > jms-proxy.properties.tmp.1
mv -f jms-proxy.properties.tmp.1 jms-proxy.properties.tmp
mv -f jms-proxy.properties.tmp ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties
cat ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties | grep "targetUri="

cp -f ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties jms-proxy.properties.tmp
sed s#TARGET_URI=.*#TARGET_URI=${CUSTOM_URL}#g \
    jms-proxy.properties.tmp > jms-proxy.properties.tmp.1
mv -f jms-proxy.properties.tmp.1 jms-proxy.properties.tmp
mv -f jms-proxy.properties.tmp ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties
cat ${CURR_DIR}/internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties | grep "TARGET_URI="


