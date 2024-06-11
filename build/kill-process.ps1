# powershell -ExecutionPolicy Bypass -File %BAT_DIR%kill-process.ps1 org.nkjmlab.

Get-CimInstance Win32_Process -Filter "name like '%$($args[0])%'" |
    Select-Object ProcessId, CommandLine |
    Where-Object { $_.CommandLine -like "*$($args[1])*" } |
    ForEach-Object {
        $processId = $_.ProcessId
        Stop-Process -Id $processId
        Write-Output "Process killed. $processId, $_.CommandLine"
    }
