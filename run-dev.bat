@echo off
REM Windows version of the offline launcher

REM Set working directory
set WORK_DIR=%USERPROFILE%\.minecraft
if not exist "%WORK_DIR%" mkdir "%WORK_DIR%"

REM Set Java arguments for memory
set JAVA_ARGS=-Xmx2G -Xms1G

REM Create launcher_profiles.json with offline mode enabled
echo { > "%WORK_DIR%\launcher_profiles.json"
echo   "profiles": { >> "%WORK_DIR%\launcher_profiles.json"
echo     "MinModDev": { >> "%WORK_DIR%\launcher_profiles.json"
echo       "name": "MinModDev", >> "%WORK_DIR%\launcher_profiles.json"
echo       "type": "custom", >> "%WORK_DIR%\launcher_profiles.json"
echo       "created": "2023-01-01T00:00:00.000Z", >> "%WORK_DIR%\launcher_profiles.json"
echo       "lastUsed": "2023-01-01T00:00:00.000Z", >> "%WORK_DIR%\launcher_profiles.json"
echo       "icon": "Grass", >> "%WORK_DIR%\launcher_profiles.json"
echo       "lastVersionId": "1.20.2" >> "%WORK_DIR%\launcher_profiles.json"
echo     } >> "%WORK_DIR%\launcher_profiles.json"
echo   }, >> "%WORK_DIR%\launcher_profiles.json"
echo   "settings": { >> "%WORK_DIR%\launcher_profiles.json"
echo     "enableSnapshots": true, >> "%WORK_DIR%\launcher_profiles.json"
echo     "enableAdvanced": true, >> "%WORK_DIR%\launcher_profiles.json"
echo     "keepLauncherOpen": false, >> "%WORK_DIR%\launcher_profiles.json"
echo     "showGameLog": false >> "%WORK_DIR%\launcher_profiles.json"
echo   }, >> "%WORK_DIR%\launcher_profiles.json"
echo   "offline": true >> "%WORK_DIR%\launcher_profiles.json"
echo } >> "%WORK_DIR%\launcher_profiles.json"

REM Stop any running gradle daemons
call gradlew.bat --stop

REM Clean and build the project
call gradlew.bat clean build

REM Create mods directory and copy jars
if not exist "%WORK_DIR%\mods" mkdir "%WORK_DIR%\mods"
copy "build\libs\minmod-1.0.0.jar" "%WORK_DIR%\mods\"

REM Launch Minecraft directly without authentication
echo Launching Minecraft in offline mode with MinMod...
set MINECRAFT_CLIENT_JAR=%USERPROFILE%\.gradle\caches\fabric-loom\minecraft-client-1.20.2-mapped.jar
set FABRIC_LOADER_JAR=%USERPROFILE%\.gradle\caches\modules-2\files-2.1\net.fabricmc\fabric-loader\0.14.23\fabric-loader-0.14.23.jar

java %JAVA_ARGS% ^
  -Dfabric.skipAuth=true ^
  -Dfabric.development=true ^
  -Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true ^
  -Dfabric.skipAuthLib=true ^
  -Dfml.ignoreInvalidMinecraftCertificates=true ^
  -Dfml.ignorePatchDiscrepancies=true ^
  -Dminecraft.applet.TargetDirectory="%WORK_DIR%" ^
  -cp "%MINECRAFT_CLIENT_JAR%;%FABRIC_LOADER_JAR%" ^
  net.fabricmc.loader.impl.launch.knot.KnotClient ^
  --username DevUser ^
  --version 1.20.2 ^
  --gameDir "%WORK_DIR%" ^
  --assetsDir "%USERPROFILE%\.gradle\caches\fabric-loom\assets" ^
  --assetIndex "5"
