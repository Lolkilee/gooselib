@echo off
REM Build and install the server using Gradle
gradlew.bat server:installDist

REM Run the server with arguments
server\build\install\server\bin\server.bat kaas debug -mt