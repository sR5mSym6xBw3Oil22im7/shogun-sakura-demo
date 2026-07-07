param(
  [string]$ChromePath = 'C:\Program Files\Google\Chrome\Application\chrome.exe',
  [string]$Url = 'http://localhost:5500/',
  [string]$DebuggingPort = '9222',
  [string]$UserDataDir = (Join-Path $PSScriptRoot 'chrome-profile')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Wait-ForDebugPort {
  param([string]$Port, [int]$TimeoutSeconds = 30)
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $response = Invoke-RestMethod -Uri "http://localhost:$Port/json/version" -TimeoutSec 2
      if ($response.webSocketDebuggerUrl) { return }
    } catch {
      Start-Sleep -Milliseconds 500
    }
  }
  throw "Chrome debugging port $Port did not become ready."
}

function Test-DebugPortReady {
  param([string]$Port)
  try {
    $response = Invoke-RestMethod -Uri "http://localhost:$Port/json/version" -TimeoutSec 2
    return [bool]$response.webSocketDebuggerUrl
  } catch {
    return $false
  }
}

function New-JsonTarget {
  param([string]$Port, [string]$TargetUrl)
  Invoke-RestMethod -Method Put -Uri "http://localhost:$Port/json/new?$TargetUrl"
}

function Invoke-Cdp {
  param(
    [System.Net.WebSockets.ClientWebSocket]$WebSocket,
    [string]$Method,
    [hashtable]$Params = @{}
  )

  if (-not (Get-Variable -Scope Script -Name nextId -ErrorAction SilentlyContinue)) { $script:nextId = 1 }
  $id = $script:nextId
  $script:nextId++

  $payload = @{ id = $id; method = $Method; params = $Params } | ConvertTo-Json -Depth 20 -Compress
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
  $segment = [System.ArraySegment[byte]]::new($bytes)
  $null = $WebSocket.SendAsync($segment, [System.Net.WebSockets.WebSocketMessageType]::Text, $true, [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()

  $buffer = New-Object byte[] 65536
  while ($true) {
    $receiveSegment = [System.ArraySegment[byte]]::new($buffer)
    $result = $WebSocket.ReceiveAsync($receiveSegment, [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
    $message = [System.Text.Encoding]::UTF8.GetString($buffer, 0, $result.Count)
    $data = $message | ConvertFrom-Json
    if ($data.PSObject.Properties.Name -contains 'id' -and $data.id -eq $id) {
      if ($data.PSObject.Properties.Name -contains 'error') {
        throw "CDP error calling $Method`: $($data.error.message)"
      }
      return $data
    }
  }
}

function Wait-ForPageReady {
  param([System.Net.WebSockets.ClientWebSocket]$WebSocket)
  for ($i = 0; $i -lt 40; $i++) {
    $result = Invoke-Cdp -WebSocket $WebSocket -Method 'Runtime.evaluate' -Params @{
      expression = 'document.readyState'
      returnByValue = $true
    }
    if ($result.result.result.value -eq 'complete') { return }
    Start-Sleep -Milliseconds 250
  }
  throw 'The browser page did not finish loading.'
}

function Wait-ForStatus {
  param([System.Net.WebSockets.ClientWebSocket]$WebSocket)
  for ($i = 0; $i -lt 40; $i++) {
    $result = Invoke-Cdp -WebSocket $WebSocket -Method 'Runtime.evaluate' -Params @{
      expression = @'
(() => {
  const el = document.querySelector('[data-form-status]');
  return {
    text: el ? el.textContent : '',
    className: el ? el.className : ''
  };
})()
'@
      returnByValue = $true
    }
    $value = $result.result.result.value
    if ($value.text -and $value.text -match '注文番号:') { return $value }
    Start-Sleep -Milliseconds 500
  }
  throw 'Success message did not appear in the browser.'
}

if (-not (Test-Path $ChromePath)) { throw "Chrome not found at $ChromePath" }

if (Test-Path $UserDataDir) { Remove-Item -Recurse -Force $UserDataDir }
New-Item -ItemType Directory -Force -Path $UserDataDir | Out-Null

$chrome = $null
if (-not (Test-DebugPortReady -Port $DebuggingPort)) {
  $chrome = Start-Process -FilePath $ChromePath -ArgumentList @(
    "--remote-debugging-port=$DebuggingPort"
    "--user-data-dir=$UserDataDir"
    '--new-window'
    $Url
  ) -PassThru
}

try {
  if (-not (Test-DebugPortReady -Port $DebuggingPort)) {
    Wait-ForDebugPort -Port $DebuggingPort -TimeoutSeconds 30
  }

  $target = New-JsonTarget -Port $DebuggingPort -TargetUrl $Url
  $ws = [System.Net.WebSockets.ClientWebSocket]::new()
  $ws.ConnectAsync([Uri]$target.webSocketDebuggerUrl, [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()

  try {
    Invoke-Cdp -WebSocket $ws -Method 'Page.enable' | Out-Null
    Invoke-Cdp -WebSocket $ws -Method 'Runtime.enable' | Out-Null
    Start-Sleep -Seconds 2
    Wait-ForPageReady -WebSocket $ws

    $submitResult = Invoke-Cdp -WebSocket $ws -Method 'Runtime.evaluate' -Params @{
      expression = @'
(() => {
  const setValue = (selector, value) => {
    const element = document.querySelector(selector);
    element.value = value;
    element.dispatchEvent(new Event("input", { bubbles: true }));
    element.dispatchEvent(new Event("change", { bubbles: true }));
  };

  setValue("#name", "Taro Yamada");
  setValue("#email", "test@example.com");
  setValue("#postalCode", "100-0001");
  setValue("#address", "Tokyo 1-1-1");
  setValue("#quantity", "1");
  setValue("#note", "Browser order demo");

  document.querySelector("[data-order-form]").requestSubmit();
  return "submitted";
})()
'@
      returnByValue = $true
    }

    Start-Sleep -Seconds 3
    $status = Wait-ForStatus -WebSocket $ws

    Write-Host "Browser submit result: $($submitResult.result.result.value)"
    Write-Host "Status text: $($status.text)"
    Write-Host "Status class: $($status.className)"
  }
  finally {
    try { $ws.Dispose() } catch {}
  }
}
finally {
  if ($chrome) {
    try { Stop-Process -Id $chrome.Id -Force } catch {}
  }
}
