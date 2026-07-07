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
  $js = @'
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
  $submit = Invoke-Cdp -WebSocket $ws -Method 'Runtime.evaluate' -Params @{ expression = $js; returnByValue = $true }
  Write-Host ($submit | ConvertTo-Json -Depth 6)
  Start-Sleep -Seconds 5

  $checks = @(
    'document.querySelector("[data-form-status]").textContent',
    'document.querySelector("[data-form-status]").className',
    'document.querySelector("#name").getAttribute("aria-invalid")',
    'document.querySelector("#email").getAttribute("aria-invalid")',
    'document.querySelector("#postalCode").getAttribute("aria-invalid")',
    'document.querySelector("#address").getAttribute("aria-invalid")',
    'document.querySelector("#quantity").getAttribute("aria-invalid")',
    'document.querySelector("#note").getAttribute("aria-invalid")'
  )

  foreach ($expr in $checks) {
    $result = Invoke-Cdp -WebSocket $ws -Method 'Runtime.evaluate' -Params @{ expression = $expr; returnByValue = $true }
    Write-Host "$expr => $($result.result.result.value)"
  }
}
finally {
  try { $ws.Dispose() } catch {}
}
