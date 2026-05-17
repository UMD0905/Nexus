@echo off
setlocal

REM ── Paths ─────────────────────────────────────────────────────────────────────
set "JPACKAGE=%USERPROFILE%\.jdks\corretto-21.0.6\bin\jpackage.exe"
set "MVN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.14-bin\1cb7fhup6b5n3bed6kckbrnspv\apache-maven-3.9.14\bin\mvn"
set "JAVA_HOME=%USERPROFILE%\.jdks\corretto-21.0.6"
set "PROJECT_DIR=%~dp0"

echo.
echo [1/2] Building dist-input with Maven...
call "%MVN%" package -Ppackage -DskipTests -f "%PROJECT_DIR%pom.xml"
if errorlevel 1 ( echo Maven build FAILED & pause & exit /b 1 )

echo.
echo [2/2] Creating native app-image with jpackage...
if exist "%PROJECT_DIR%dist\Nexus" rmdir /s /q "%PROJECT_DIR%dist\Nexus"

"%JPACKAGE%" ^
  --type app-image ^
  --input "%PROJECT_DIR%target\dist-input" ^
  --main-jar nexus-app.jar ^
  --main-class com.nexus.Main ^
  --name Nexus ^
  --dest "%PROJECT_DIR%dist" ^
  --java-options "--module-path $APPDIR" ^
  --java-options "--add-modules=javafx.controls,javafx.fxml,javafx.swing,javafx.base,javafx.graphics,javafx.media" ^
  --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" ^
  --java-options "--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED"

if errorlevel 1 ( echo jpackage FAILED & pause & exit /b 1 )

REM Remove no-classifier JavaFX stub JARs (win-classifier ones are the real named modules)
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-base-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-controls-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-fxml-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-graphics-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-swing-21.0.5.jar" 2>nul

echo.
echo Done!  Your app is at: %PROJECT_DIR%dist\Nexus\Nexus.exe
echo.
echo To install on your laptop:
echo   1. Copy the dist\Nexus folder anywhere (e.g. C:\Users\%USERNAME%\Apps\Nexus)
echo   2. Right-click Nexus.exe -^> Send to -^> Desktop (create shortcut)
echo   3. Right-click the desktop shortcut -^> Pin to taskbar
echo.
pause
