@echo off

set TARGET_HOST=search-service-20000
set TARGET_NETWORK=getindata-linserver_default
set JMETER_VERSION=latest

set T_DIR=docker-jmeter

rem Reporting dir: start fresh
set R_DIR=%T_DIR%/report
rd /s /q %R_DIR%
mkdir %R_DIR%

del %T_DIR%\test-plan.jtl
del %T_DIR%\jmeter.log

.\%T_DIR%\run.cmd -Dlog_level.jmeter=DEBUG ^
	-JTARGET_HOST=%TARGET_HOST% ^
	-n -t %T_DIR%/gettindata-linserver.jmx -l %T_DIR%/test-plan.jtl -j %T_DIR%/jmeter.log ^
	-e -o %R_DIR%

echo "==== HTML Test Report ===="
echo "See HTML test report in %R_DIR%/index.html"