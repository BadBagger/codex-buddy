param(
    [string]$PhoneUrl = $env:CODEX_BUDDY_URL,
    [string]$Title = "Codex is done",
    [string]$Message = "Codex finished working in the current thread.",
    [string]$Status = "done"
)

if ([string]::IsNullOrWhiteSpace($PhoneUrl)) {
    exit 0
}

$payload = @{
    title = $Title
    message = $Message
    status = $Status
} | ConvertTo-Json -Compress

try {
    Invoke-RestMethod -Uri $PhoneUrl -Method Post -ContentType "application/json" -Body $payload -TimeoutSec 4 | Out-Null
} catch {
    exit 0
}
