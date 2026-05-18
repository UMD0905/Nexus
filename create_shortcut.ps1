$WshShell = New-Object -comObject WScript.Shell

# Start Menu shortcut (makes it searchable and pinnable)
$StartMenu = [System.Environment]::GetFolderPath("StartMenu") + "\Programs"
$Shortcut = $WshShell.CreateShortcut("$StartMenu\Nexus.lnk")
$Shortcut.TargetPath = "D:\JetBrains\untitled1\dist\Nexus\Nexus.exe"
$Shortcut.WorkingDirectory = "D:\JetBrains\untitled1\dist\Nexus"
$Shortcut.Description = "Nexus"
$Shortcut.Save()
Write-Host "Start Menu shortcut created: $StartMenu\Nexus.lnk"

# Desktop shortcut too
$Desktop = [System.Environment]::GetFolderPath("Desktop")
$Shortcut2 = $WshShell.CreateShortcut("$Desktop\Nexus.lnk")
$Shortcut2.TargetPath = "D:\JetBrains\untitled1\dist\Nexus\Nexus.exe"
$Shortcut2.WorkingDirectory = "D:\JetBrains\untitled1\dist\Nexus"
$Shortcut2.Description = "Nexus"
$Shortcut2.Save()
Write-Host "Desktop shortcut created: $Desktop\Nexus.lnk"
