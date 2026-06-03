#!/bin/bash

./mvnw -f ./pom.xml clean package -Dmaven.test.skip -T 1C
