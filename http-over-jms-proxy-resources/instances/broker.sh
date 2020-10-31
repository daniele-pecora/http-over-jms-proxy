#!/bin/bash

CURR_DIR=$(dirname "$(readlink -f "$0")")

${CURR_DIR}/internet/apache-activemq-5.15.7-broker/bin/activemq $@
