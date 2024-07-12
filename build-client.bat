cd .\java
call .\gradlew.bat client:build
cd ..
move .\java\client\build\libs\gooselib-client.jar .\gui\src-tauri\java\gooselib-client.jar
cd .\gui
cargo tauri build