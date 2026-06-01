@echo off
setlocal

REM ── Paths ─────────────────────────────────────────────────────────────────────
set "JPACKAGE=C:\Program Files\Android\openjdk\jdk-21.0.8\bin\jpackage.exe"
set "MVN=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd"
set "JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8"
set "PROJECT_DIR=%~dp0"

echo.
echo [0/2] Copying platform JARs not managed by Maven package profile...
set "M2=%USERPROFILE%\.m2\repository"
copy /y "%M2%\org\openjfx\javafx-swing\21.0.5\javafx-swing-21.0.5-win.jar" "%PROJECT_DIR%target\dist-input\" >nul 2>&1

echo.
echo [1/2] Building dist-input with Maven...
call "%MVN%" clean package -Ppackage -DskipTests -f "%PROJECT_DIR%pom.xml"
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
  --icon "%PROJECT_DIR%src\main\resources\nexus.ico" ^
  --dest "%PROJECT_DIR%dist" ^
  --java-options "--module-path=$APPDIR\mods" ^
  --java-options "--add-modules=javafx.controls,javafx.fxml,javafx.swing,javafx.base,javafx.graphics,javafx.media,javafx.web" ^
  --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.util=ALL-UNNAMED" ^
  --java-options "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" ^
  --java-options "--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" ^
  --java-options "--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED" ^
  --java-options "--add-exports=javafx.web/com.sun.webkit=ALL-UNNAMED"

if errorlevel 1 ( echo jpackage FAILED & pause & exit /b 1 )

REM Move win-classifier JavaFX JARs to mods/ so they're isolated on the module-path.
REM Remove the no-classifier stub JARs (they have no module-info and would conflict).
mkdir "%PROJECT_DIR%dist\Nexus\app\mods" 2>nul
move /y "%PROJECT_DIR%dist\Nexus\app\javafx-*-win.jar" "%PROJECT_DIR%dist\Nexus\app\mods\" >nul 2>&1
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-base-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-controls-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-fxml-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-graphics-21.0.5.jar" 2>nul
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-swing-21.0.5.jar" 2>nul
REM If jpackage didn't copy the swing win-jar from input, copy it directly from Maven repo
if not exist "%PROJECT_DIR%dist\Nexus\app\mods\javafx-swing-21.0.5-win.jar" (
    copy /y "%M2%\org\openjfx\javafx-swing\21.0.5\javafx-swing-21.0.5-win.jar" "%PROJECT_DIR%dist\Nexus\app\mods\" >nul
)
del /q "%PROJECT_DIR%dist\Nexus\app\javafx-web-21.0.5.jar" 2>nul

echo.
echo Done!  Your app is at: %PROJECT_DIR%dist\Nexus\Nexus.exe
echo.
echo To install on your laptop:
echo   1. Copy the dist\Nexus folder anywhere (e.g. C:\Users\%USERNAME%\Apps\Nexus)
echo   2. Right-click Nexus.exe -^> Send to -^> Desktop (create shortcut)
echo   3. Right-click the desktop shortcut -^> Pin to taskbar
echo.
pause
