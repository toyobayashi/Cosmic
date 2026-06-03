#!/bin/bash

cd "$(dirname "$0")"
java -Xmx2048m -Dwz-path=wz -jar ./target/Cosmic.jar
