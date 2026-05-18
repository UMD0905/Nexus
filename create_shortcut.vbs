Set WshShell = CreateObject("WScript.Shell")
Set oShortcut = WshShell.CreateShortcut(WshShell.SpecialFolders("Desktop") & "\Nexus.lnk")
oShortcut.TargetPath = "D:\JetBrains\untitled1\dist\Nexus\Nexus.exe"
oShortcut.WorkingDirectory = "D:\JetBrains\untitled1\dist\Nexus"
oShortcut.Description = "Nexus"
oShortcut.Save
