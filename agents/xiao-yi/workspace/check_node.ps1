# Get node processes
$nodeProcs = Get-Process -Name node -ErrorAction SilentlyContinue
if ($nodeProcs) {
    Write-Host "Node processes found:"
    foreach ($p in $nodeProcs) {
        $cmdline = (Get-CimInstance Win32_Process -Filter "ProcessId=$($p.Id)").CommandLine
        Write-Host "PID: $($p.Id) - Memory: $([math]::Round($p.WorkingSet64/1MB,0)) MB - Cmd: $cmdline"
    }
} else {
    Write-Host "No node processes"
}
