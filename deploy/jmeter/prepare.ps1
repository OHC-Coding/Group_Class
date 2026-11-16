# P3 load test helper.
# 1. Log in as admin.
# 2. Create and publish a clean class with a random future schedule.
# 3. Log in member001..member200 and write tokens.csv for JMeter.
#
# Usage: powershell -ExecutionPolicy Bypass -File prepare.ps1

param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$Members = 200,
    [int]$Capacity = 30,
    [int]$Sessions = 5
)

$ErrorActionPreference = "Stop"

function Login([string]$Username, [string]$Password) {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $resp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
        -ContentType "application/json" -Body $body
    if ($resp.code -ne 0) {
        throw "login failed: $Username -> $($resp.message)"
    }
    return $resp.data.token
}

Write-Host "[1/4] login admin"
$adminToken = Login "admin" "123456"
$headers = @{ Authorization = "Bearer $adminToken" }

Write-Host "[2/4] load template / coach / venue"
$template = (Invoke-RestMethod -Uri "$BaseUrl/api/admin/course-templates" -Headers $headers).data |
    Where-Object { $_.status -eq 1 } | Select-Object -First 1
$coach = (Invoke-RestMethod -Uri "$BaseUrl/api/admin/coaches" -Headers $headers).data |
    Select-Object -First 1
$venue = (Invoke-RestMethod -Uri "$BaseUrl/api/admin/venues" -Headers $headers).data |
    Where-Object { $_.status -eq 1 } | Select-Object -First 1
if (-not $template -or -not $coach -or -not $venue) {
    throw "template / coach / venue not found, run 02-data.sql and 03-dev-accounts.sql first"
}

Write-Host "[3/4] create and publish test class"
$startDay = (Get-Date).Date.AddDays((Get-Random -Minimum 60 -Maximum 200))
$startHour = Get-Random -Minimum 7 -Maximum 20
$items = @()
for ($i = 0; $i -lt $Sessions; $i++) {
    $items += @{
        date      = $startDay.AddDays($i).ToString("yyyy-MM-dd")
        startTime = "{0:00}:00:00" -f $startHour
        endTime   = "{0:00}:30:00" -f ($startHour + 1)
    }
}
$payload = @{
    templateId      = $template.id
    coachId         = $coach.userId
    venueId         = $venue.id
    price           = 300.00
    capacity        = $Capacity
    enrollOpenTime  = (Get-Date).AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ss")
    enrollCloseTime = (Get-Date).AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")
    remark          = "P3 load test"
    sessions        = $items
} | ConvertTo-Json -Depth 5

$created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/classes" `
    -Headers $headers -ContentType "application/json" -Body $payload
if ($created.code -ne 0) {
    throw "create class failed: $($created.message). The random slot may conflict, run again."
}
$classId = $created.data.id
$published = Invoke-RestMethod -Method Put -Uri "$BaseUrl/api/admin/classes/$classId/publish" `
    -Headers $headers
if ($published.code -ne 0) {
    throw "publish failed: $($published.message)"
}
Write-Host "class id = $classId, capacity = $Capacity, status = $($published.data.status)"

Write-Host "[4/4] login $Members members and write tokens.csv"
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("username,token")
for ($i = 1; $i -le $Members; $i++) {
    $name = "member{0:D3}" -f $i
    $token = Login $name "123456"
    $lines.Add("$name,$token")
    if ($i % 50 -eq 0) {
        Write-Host "  $i / $Members"
    }
}
$csv = Join-Path $PSScriptRoot "tokens.csv"
Set-Content -Path $csv -Value $lines -Encoding ASCII

Write-Host ""
Write-Host "done."
Write-Host "class id : $classId"
Write-Host "tokens   : $csv"
Write-Host ""
Write-Host "next:"
Write-Host "  jmeter -n -t p3-enroll.jmx -JCLASS_ID=$classId -JTHREADS=$Members -l result.jtl"
Write-Host "  powershell -ExecutionPolicy Bypass -File summarize.ps1 -Jtl result.jtl"
