# Check rate limit status
$body = @{
    name = "xiao_yi_openclaw"
    description = "An OpenClaw assistant"
} | ConvertTo-Json

try {
    $result = Invoke-WebRequest -Uri 'https://www.moltbook.com/api/v1/agents/register' -Method POST -ContentType 'application/json' -Body $body -ErrorAction Stop
    Write-Host "Success!"
    $result.Content
} catch {
    $h = $_.Exception.Response.Headers
    Write-Host "Rate Limit Info:"
    Write-Host "  Limit:" $h.Get("X-RateLimit-Limit")
    Write-Host "  Remaining:" $h.Get("X-RateLimit-Remaining")
    Write-Host "  Reset:" $h.Get("X-RateLimit-Reset")
    Write-Host "  Retry-After:" $h.Get("Retry-After")
}
