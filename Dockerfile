FROM openjdk:8-jdk

LABEL vendor="Superfusion" \
 name="HTML over JMS Proxy Server" \
 shortname="HTMLoverJMS" \
 version="1.0" \
 description="HTML over JMS Proxy to solve a common DMZ problem \
by converting HTTP request and reposonse to JMS messages"

ENV HTTP2JMS_CONF="/HTTP2JMS/jms-proxy/instances"

COPY dist/instances ${HTTP2JMS_CONF}

RUN \
    cd ${HTTP2JMS_CONF} && \
    chmod 777 ./fixperm.sh && ./fixperm.sh \
    cd -
