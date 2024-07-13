#!/bin/bash

./gradlew client:installDist
./client/build/install/client/bin/client -notimeout
