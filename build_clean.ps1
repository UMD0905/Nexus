# Build script - clean jpackage build
$ErrorActionPreference = "Stop"
$PROJECT = "D:\JetBrains\untitled1"
$JPACKAGE = "$env:USERPROFILE\.jdks\corretto-21.0.6\bin\jpackage.exe"
$LOG = "$PROJECT\build_clean_log.txt"

"=== Build Clean Script ===" | Out-File $LOG
"$(Get-Date)" | Out-File $LOG -Append

# Step 1: Create clean input dir (only win-classifier JavaFX JARs + other deps, no stubs)
$src = "$PROJECT\target\dist-input"
$dst = "$PROJECT\target\clean-input"

"Creating clean-input..." | Out-File $LOG -Append
if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
New-Item -ItemType Directory -Path $dst | Out-Null

# Copy all non-stub JARs
Get-ChildItem $src -Filter "*.jar" | ForEach-Object {
    # Skip stub javafx jars (javafx-xxx-21.0.5.jar but NOT javafx-xxx-21.0.5-win.jar)
    if ($_.Name -match "^javafx-" -and $_.Name -notmatch "-win\.jar$") {
        "  SKIP stub: $($_.Name)" | Out-File $LOG -Append
    } else {
        Copy-Item $_.FullName -Destination $dst
        "  COPY: $($_.Name)" | Out-File $LOG -Append
    }
}

$count = (Get-ChildItem $dst).Count
"Copied $count files to clean-input" | Out-File $LOG -Append

# Step 2: Remove existing dist\Nexus
$distNexus = "$PROJECT\dist\Nexus"
if (Test-Path $distNexus) {
    "Removing existing $distNexus..." | Out-File $LOG -Append
    try { Remove-Item $distNexus -Recurse -Force }
    catch { "WARNING: Could not remove dist\Nexus: $_" | Out-File $LOG -Append }
}

# Step 3: Run jpackage
"Running jpackage..." | Out-File $LOG -Append
$jpArgs = @(
    "--type", "app-image",
    "--input", $dst,
    "--main-jar", "nexus-app.jar",
    "--main-class", "com.nexus.Main",
    "--name", "Nexus",
    "--dest", "$PROJECT\dist",
    "--java-options", "--module-path=`$APPDIR\mods",
    "--java-options", "--add-modules=javafx.controls,javafx.fxml,javafx.swing,javafx.base,javafx.graphics,javafx.media,javafx.web",
    "--java-options", "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--java-options", "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--java-options", "--add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED",
    "--java-options", "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED",
    "--java-options", "--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED",
    "--java-options", "--add-exports=javafx.web/com.sun.webkit=ALL-UNNAMED"
)

$proc = Start-Process -FilePath $JPACKAGE -ArgumentList $jpArgs -Wait -PassThru -RedirectStandardOutput "$PROJECT\jp_stdout.txt" -RedirectStandardError "$PROJECT\jp_stderr.txt"
$exitCode = $proc.ExitCode
"jpackage exit code: $exitCode" | Out-File $LOG -Append
Get-Content "$PROJECT\jp_stdout.txt" | Out-File $LOG -Append
Get-Content "$PROJECT\jp_stderr.txt" | Out-File $LOG -Append

if ($exitCode -ne 0) {
    "JPACKAGE FAILED" | Out-File $LOG -Append
    exit 1
}

# Step 4: Post-process - move win JARs to mods/, remove stubs
"Post-processing..." | Out-File $LOG -Append
$appDir = "$distNexus\app"
$modsDir = "$appDir\mods"
New-Item -ItemType Directory -Path $modsDir -Force | Out-Null

Get-ChildItem "$appDir\javafx-*-win.jar" -ErrorAction SilentlyContinue | ForEach-Object {
    Move-Item $_.FullName -Destination $modsDir -Force
    "  Moved to mods: $($_.Name)" | Out-File $LOG -Append
}
Get-ChildItem "$appDir\javafx-*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "-win\.jar$" } | ForEach-Object {
    Remove-Item $_.FullName -Force
    "  Deleted stub: $($_.Name)" | Out-File $LOG -Append
}

"=== Done! ===" | Out-File $LOG -Append
"App: $distNexus\Nexus.exe" | Out-File $LOG -Append
