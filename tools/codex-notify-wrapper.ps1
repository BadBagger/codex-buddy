$desktopNotifier = "C:\Users\KyleB\AppData\Local\OpenAI\Codex\runtimes\cua_node\1b23c930bdf84ed6\bin\node_modules\@oai\sky\bin\windows\codex-computer-use.exe"
$phoneUrl = "http://192.168.1.223:8787/notify"
$payload = [Console]::In.ReadToEnd()

try {
    if ((Test-Path -LiteralPath $desktopNotifier) -and -not [string]::IsNullOrWhiteSpace($payload)) {
        $payload | & $desktopNotifier turn-ended 2>$null
    }
} catch {
}

try {
    & powershell -NoProfile -ExecutionPolicy Bypass -File "C:\Users\KyleB\Documents\Codex\2026-07-06\can-you-make-a-codex-android\tools\codex-buddy-notify.ps1" -PhoneUrl $phoneUrl -Title "Codex is done" -Message "Codex finished working in the current thread." -Status "done"
} catch {
}
