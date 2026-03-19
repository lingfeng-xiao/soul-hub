# Kill zombie chrome-devtools-mcp processes
$count = 0
$procs = Get-Process -Name node -ErrorAction SilentlyContinue | Where-Object { 
    $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($_.Id)" -ErrorAction SilentlyContinue).CommandLine
    $cmd -match "chrome-devtools-mcp" -and $_.WorkingSet64 -gt 100MB
}
if ($procs) {
    Write-Host "Found $($procs.Count) high-memory chrome-devtools-mcp processes:"
    foreach ($p in $procs) {
        $memMB = [math]::Round($p.WorkingSet64/1MB,0)
        Write-Host "  Killing PID $($p.Id) - Memory: $memMB MB"
        Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
        $count++
    }
}
Write-Host "Killed $count processes"
Write-Host "Memory freed: approximately $([math]::Round(($procs | Measure-Object WorkingSet64 -Sum).Sum/1MB,0)) MB"
