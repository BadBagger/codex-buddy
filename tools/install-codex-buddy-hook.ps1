param(
    [Parameter(Mandatory = $true)]
    [string]$PhoneUrl,

    [string]$CodexHome = "$env:USERPROFILE\.codex",
    [switch]$SkipConfigUpdate
)

$ErrorActionPreference = "Stop"

function Assert-PhoneUrl {
    param([string]$Value)

    $parsed = $null
    if (-not [System.Uri]::TryCreate($Value, [System.UriKind]::Absolute, [ref]$parsed)) {
        throw "PhoneUrl must be an absolute URL, for example http://192.168.1.223:8787/notify"
    }
    if ($parsed.Scheme -ne "http" -and $parsed.Scheme -ne "https") {
        throw "PhoneUrl must start with http:// or https://"
    }
    if (-not $parsed.AbsolutePath.EndsWith("/notify")) {
        throw "PhoneUrl should point to the Codex Buddy /notify endpoint."
    }
}

function Convert-ToTomlSingleQuotedString {
    param([string]$Value)
    return "'" + ($Value -replace "'", "''") + "'"
}

Assert-PhoneUrl -Value $PhoneUrl

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceNotifier = Join-Path $scriptRoot "codex-buddy-notify.ps1"
if (-not (Test-Path -LiteralPath $sourceNotifier)) {
    throw "Missing notifier script: $sourceNotifier"
}

$installDir = Join-Path $CodexHome "tools\codex-buddy"
New-Item -ItemType Directory -Force -Path $installDir | Out-Null

$installedNotifier = Join-Path $installDir "codex-buddy-notify.ps1"
$installedWrapper = Join-Path $installDir "codex-notify-wrapper.ps1"
Copy-Item -LiteralPath $sourceNotifier -Destination $installedNotifier -Force

$wrapperContent = @"
`$phoneUrl = "$PhoneUrl"
`$payload = [Console]::In.ReadToEnd()

try {
    if (-not [string]::IsNullOrWhiteSpace(`$payload)) {
        `$configPath = Join-Path `$env:USERPROFILE ".codex\config.toml"
        `$desktopNotifier = Select-String -LiteralPath `$configPath -Pattern "codex-computer-use\.exe" -ErrorAction SilentlyContinue |
            Select-Object -First 1 |
            ForEach-Object {
                if (`$_.Line -match "'([^']*codex-computer-use\.exe)'|`"([^`"]*codex-computer-use\.exe)`"") {
                    if (`$Matches[1]) { `$Matches[1] } else { `$Matches[2] }
                }
            }
        if (`$desktopNotifier -and (Test-Path -LiteralPath `$desktopNotifier)) {
            `$payload | & `$desktopNotifier turn-ended 2>`$null
        }
    }
} catch {
}

try {
    & powershell -NoProfile -ExecutionPolicy Bypass -File "$installedNotifier" -PhoneUrl `$phoneUrl -Title "Codex is done" -Message "Codex finished working in the current thread." -Status "done"
} catch {
}
"@

Set-Content -LiteralPath $installedWrapper -Value $wrapperContent -Encoding UTF8

if (-not $SkipConfigUpdate) {
    $configPath = Join-Path $CodexHome "config.toml"
    if (-not (Test-Path -LiteralPath $configPath)) {
        New-Item -ItemType File -Force -Path $configPath | Out-Null
    }

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backupPath = "$configPath.codex-buddy-$timestamp.bak"
    Copy-Item -LiteralPath $configPath -Destination $backupPath -Force

    $notifyLine = "notify = [ 'powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', " + (Convert-ToTomlSingleQuotedString -Value $installedWrapper) + " ]"
    $configText = Get-Content -LiteralPath $configPath -Raw
    if ($configText -match "(?m)^notify\s*=") {
        $configText = [regex]::Replace($configText, "(?m)^notify\s*=.*$", $notifyLine, 1)
    } elseif ([string]::IsNullOrWhiteSpace($configText)) {
        $configText = $notifyLine + [Environment]::NewLine
    } else {
        $configText = $notifyLine + [Environment]::NewLine + $configText
    }
    Set-Content -LiteralPath $configPath -Value $configText -Encoding UTF8

    Write-Host "Updated Codex notify command in $configPath"
    Write-Host "Backup written to $backupPath"
}

Write-Host "Installed Codex Buddy notifier to $installDir"
Write-Host "Test it with:"
Write-Host "  powershell -NoProfile -ExecutionPolicy Bypass -File `"$installedNotifier`" -PhoneUrl `"$PhoneUrl`" -Title `"Codex Buddy test`" -Message `"PC hook installed.`" -Status `"done`""
