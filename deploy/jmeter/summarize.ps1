# Summarize a JMeter CSV result of the P3 enroll test.
# Usage: powershell -ExecutionPolicy Bypass -File summarize.ps1 -Jtl result.jtl

param(
    [string]$Jtl = "result.jtl"
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $Jtl)) {
    throw "file not found: $Jtl"
}

$rows = Import-Csv $Jtl
if (-not $rows) {
    throw "no samples in $Jtl"
}

$ok = @($rows | Where-Object { $_.responseData -match '"code"\s*:\s*0' })
$soldOut = @($rows | Where-Object { $_.responseData -match '"code"\s*:\s*429' })
$duplicate = @($rows | Where-Object { $_.responseData -match '"code"\s*:\s*409' })
$other = $rows.Count - $ok.Count - $soldOut.Count - $duplicate.Count

$elapsed = $rows | ForEach-Object { [int]$_.elapsed } | Sort-Object
function Percentile([int[]]$Values, [double]$P) {
    if ($Values.Count -eq 0) { return 0 }
    $index = [math]::Min($Values.Count - 1, [math]::Ceiling($Values.Count * $P) - 1)
    return $Values[$index]
}
$avg = [math]::Round(($elapsed | Measure-Object -Average).Average, 1)
$p95 = Percentile $elapsed 0.95
$p99 = Percentile $elapsed 0.99

Write-Host "samples      : $($rows.Count)"
Write-Host "success(0)   : $($ok.Count)"
Write-Host "sold out(429): $($soldOut.Count)"
Write-Host "duplicate(409): $($duplicate.Count)"
Write-Host "other        : $other"
Write-Host "elapsed avg  : $avg ms"
Write-Host "elapsed p95  : $p95 ms"
Write-Host "elapsed p99  : $p99 ms"
Write-Host ""
Write-Host "acceptance: success == capacity, sold out == threads - capacity, oversell == 0"
