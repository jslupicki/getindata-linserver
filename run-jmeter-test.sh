#!/usr/bin/env bash

export TARGET_HOST="search-service-20000"
export TARGET_NETWORK="getindata-linserver_default"
#Just for git bash on Windows
export MSYS_NO_PATHCONV=1
export JMETER_VERSION=latest

T_DIR=docker-jmeter

# Reporting dir: start fresh
R_DIR=${T_DIR}/report
rm -rf ${R_DIR} > /dev/null 2>&1
mkdir -p ${R_DIR}

/bin/rm -f ${T_DIR}/test-plan.jtl ${T_DIR}/jmeter.log  > /dev/null 2>&1

./${T_DIR}/run.sh -Dlog_level.jmeter=DEBUG \
	-JTARGET_HOST=${TARGET_HOST} \
	-n -t ${T_DIR}/gettindata-linserver.jmx -l ${T_DIR}/test-plan.jtl -j ${T_DIR}/jmeter.log \
	-e -o ${R_DIR}

#echo "==== jmeter.log ===="
#cat ${T_DIR}/jmeter.log

#echo "==== Raw Test Report ===="
#cat ${T_DIR}/test-plan.jtl

echo "==== HTML Test Report ===="
echo "See HTML test report in ${R_DIR}/index.html"