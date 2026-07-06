param(
    [string]$PhoneUrl = $env:CODEX_BUDDY_URL,
    [string]$Title = "Codex is done",
    [string]$Message = "Codex finished working in the current thread.",
    [string]$Status = "done",
    [string]$Event = ""
)

if ([string]::IsNullOrWhiteSpace($PhoneUrl)) {
    exit 0
}

if (-not [string]::IsNullOrWhiteSpace($Event)) {
    $raw = [Console]::In.ReadToEnd()
    $toolName = ""
    try {
        if (-not [string]::IsNullOrWhiteSpace($raw)) {
            $json = $raw | ConvertFrom-Json
            $candidates = @(
                $json.tool,
                $json.tool_name,
                $json.toolName,
                $json.name,
                $json.params.tool,
                $json.params.tool_name,
                $json.params.toolName,
                $json.params.name
            )
            foreach ($candidate in $candidates) {
                if (-not [string]::IsNullOrWhiteSpace([string]$candidate)) {
                    $toolName = [string]$candidate
                    break
                }
            }
        }
    } catch {
    }

    if ($Event -eq "PostToolUse") {
        $Title = "Codex is working"
        if ([string]::IsNullOrWhiteSpace($toolName)) {
            $Message = "Codex completed a tool step."
        } else {
            $Message = "Codex completed: $toolName"
        }
        $Status = "working"
    } elseif ($Event -eq "UserPromptSubmit") {
        $Title = "Codex started"
        $Message = "Codex received your next instruction."
        $Status = "working"
    }
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
