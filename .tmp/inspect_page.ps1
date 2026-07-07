Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

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
      return $data
    }
  }
}

$targets = Invoke-RestMethod -Uri http://localhost:9222/json/list
$target = $targets | Where-Object { $_.url -eq 'http://localhost:5500/' } | Select-Object -First 1
if (-not $target) { throw 'Target not found.' }

$ws = [System.Net.WebSockets.ClientWebSocket]::new()
$ws.ConnectAsync([Uri]$target.webSocketDebuggerUrl, [System.Threading.CancellationToken]::None).GetAwaiter().GetResult()
try {
  Invoke-Cdp -WebSocket $ws -Method 'Runtime.enable' | Out-Null
  $values = @(
    'document.readyState',
    'typeof window.validatePayload',
    'typeof window.resolveApiBaseUrl',
    'document.querySelector("[data-order-form]") ? "form-present" : "no-form"',
    'document.querySelector("[data-submit-button]") ? document.querySelector("[data-submit-button]").textContent : ""',
    'document.querySelector("[data-form-status]") ? document.querySelector("[data-form-status]").textContent : ""'
  )

  foreach ($expr in $values) {
    $result = Invoke-Cdp -WebSocket $ws -Method 'Runtime.evaluate' -Params @{
      expression = $expr
      returnByValue = $true
    }
    $val = $result.result.result.value
    Write-Host "$expr => $val"
  }
}
finally {
  try { $ws.Dispose() } catch {}
}
