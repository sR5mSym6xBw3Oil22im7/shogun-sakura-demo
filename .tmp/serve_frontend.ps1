param(
  [string]$Root = (Join-Path $PSScriptRoot '..\frontend'),
  [int]$Port = 5500
)

$listener = [System.Net.HttpListener]::new()
$prefix = "http://localhost:$Port/"
$listener.Prefixes.Add($prefix)
$listener.Start()

function Get-ContentType([string]$path) {
  switch ([System.IO.Path]::GetExtension($path).ToLowerInvariant()) {
    '.html' { 'text/html; charset=utf-8' }
    '.css' { 'text/css; charset=utf-8' }
    '.js' { 'application/javascript; charset=utf-8' }
    '.png' { 'image/png' }
    '.jpg' { 'image/jpeg' }
    '.jpeg' { 'image/jpeg' }
    '.svg' { 'image/svg+xml' }
    default { 'application/octet-stream' }
  }
}

try {
  while ($listener.IsListening) {
    $context = $listener.GetContext()
    $requestPath = $context.Request.Url.AbsolutePath.TrimStart('/')
    if ([string]::IsNullOrWhiteSpace($requestPath)) {
      $requestPath = 'index.html'
    }

    $filePath = Join-Path $Root $requestPath
    if (-not (Test-Path $filePath -PathType Leaf)) {
      $context.Response.StatusCode = 404
      $buffer = [System.Text.Encoding]::UTF8.GetBytes('Not Found')
      $context.Response.ContentType = 'text/plain; charset=utf-8'
      $context.Response.OutputStream.Write($buffer, 0, $buffer.Length)
      $context.Response.Close()
      continue
    }

    $bytes = [System.IO.File]::ReadAllBytes($filePath)
    $context.Response.ContentType = Get-ContentType $filePath
    $context.Response.ContentLength64 = $bytes.Length
    $context.Response.OutputStream.Write($bytes, 0, $bytes.Length)
    $context.Response.Close()
  }
}
finally {
  if ($listener.IsListening) {
    $listener.Stop()
  }
  $listener.Close()
}
