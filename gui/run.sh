#!/bin/bash

cd ../java
./gradlew client:build
mkdir ../gui/src-tauri/java
cp ./client/build/libs/gooselib-client.jar  ../gui/src-tauri/java/gooselib-client.jar
cd ../gui
cargo tauri dev