#!/bin/bash

./gradlew server:installDist
./server/build/install/server/bin/server kaas debug -mt