#!/bin/bash

./gradlew installDist
./app/build/install/app/bin/app kaas debug -mt