cd "%CD%\..\java"
CALL ./gradlew.bat "client:build"
mkdir "%CD%\..\gui\src-tauri\java"
COPY  "%CD%\client\build\libs\gooselib-client.jar" "%CD%\..\gui\src-tauri\java\gooselib-client.jar"
cd "%CD%\..\gui"
cargo tauri dev