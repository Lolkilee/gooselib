#!/bin/bash

cd ../java
./gradlew client:build
cp ./client/build/libs/client.jar  ../gui/java/client.jar
cd ../gui
cargo tauri dev