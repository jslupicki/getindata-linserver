#!/usr/bin/env bash

java -cp target/getindata-linserver-0.0.1-SNAPSHOT.jar -Dloader.main=com.slupicki.linserver.PerformanceTest org.springframework.boot.loader.PropertiesLauncher $@
