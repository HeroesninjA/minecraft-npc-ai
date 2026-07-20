# RCON client for Minecraft Paper server
# Usage: Connect-Rcon ; Send-Rcon "list" ; Disconnect-Rcon

$script:rconStream = $null
$script:rconRequestId = 1

function Connect-Rcon {
    param([string]$Hostname = "localhost", [int]$Port = 25575, [string]$Password = "demo")
    
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect($Hostname, $Port)
        $tcp.NoDelay = $true
        $tcp.ReceiveTimeout = 5000
        $tcp.SendTimeout = 5000
        $stream = $tcp.GetStream()
        
        # Authenticate
        Send-RconPacket -Stream $stream -Type 3 -Body $Password
        
        $authResp = Receive-RconPacket -Stream $stream
        if ($authResp.RequestId -eq -1) {
            Write-Error "RCON: Autentificare esuata (parola gresita?)"
            $stream.Close()
            $tcp.Close()
            return $false
        }
        
        $script:rconStream = $stream
        Write-Host "  RCON conectat la $Hostname`:$Port" -ForegroundColor Green
        return $true
    } catch {
        Write-Error "RCON: $($_.Exception.Message)"
        return $false
    }
}

function Disconnect-Rcon {
    if ($script:rconStream) {
        $script:rconStream.Close()
        $script:rconStream = $null
        Write-Host "  RCON deconectat"
    }
}

function Send-RconPacket {
    param($Stream, [int]$Type, [string]$Body)
    
    $id = $script:rconRequestId++
    $bodyBytes = [System.Text.Encoding]::ASCII.GetBytes($Body + "`0")
    $payload = [System.BitConverter]::GetBytes($id) + [System.BitConverter]::GetBytes($Type) + $bodyBytes
    $len = [System.BitConverter]::GetBytes($payload.Length)
    
    $packet = $len + $payload
    $Stream.Write($packet, 0, $packet.Length)
    return $id
}

function Receive-RconPacket {
    param($Stream)
    
    $lenBuf = New-Object byte[] 4
    try {
        $read = 0
        while ($read -lt 4) {
            $r = $Stream.Read($lenBuf, $read, 4 - $read)
            if ($r -le 0) { return $null }
            $read += $r
        }
    } catch {
        return $null
    }
    $len = [System.BitConverter]::ToInt32($lenBuf, 0)
    
    if ($len -le 0 -or $len -gt 16384) { return $null }
    
    $data = New-Object byte[] $len
    $read = 0
    while ($read -lt $len) {
        $r = $Stream.Read($data, $read, $len - $read)
        if ($r -le 0) { break }
        $read += $r
    }
    
    if ($data.Length -lt 8) { return $null }
    
    $reqId = [System.BitConverter]::ToInt32($data, 0)
    $type = [System.BitConverter]::ToInt32($data, 4)
    $bodyEnd = [Array]::IndexOf($data, 0, 8)
    if ($bodyEnd -eq -1) { $bodyEnd = $data.Length }
    $body = if ($bodyEnd -gt 8) { [System.Text.Encoding]::ASCII.GetString($data, 8, $bodyEnd - 8) } else { "" }
    
    return @{ RequestId = $reqId; Type = $type; Body = $body }
}

function Send-Rcon {
    param([string]$Command)
    
    if (-not $script:rconStream) {
        $connected = Connect-Rcon
        if (-not $connected) { return $null }
    }
    
    Send-RconPacket -Stream $script:rconStream -Type 2 -Body $Command
    Start-Sleep -Milliseconds 100
    $response = Receive-RconPacket -Stream $script:rconStream
    if (-not $response) { return "" }
    
    # For long responses, may need multiple reads
    $fullResponse = $response.Body
    $timeout = 10
    while ($timeout -gt 0) {
        Start-Sleep -Milliseconds 100
        $extra = Receive-RconPacket -Stream $script:rconStream
        if ($extra -and $extra.RequestId -eq $response.RequestId) {
            $fullResponse += $extra.Body
        } else {
            break
        }
        $timeout--
    }
    
    return $fullResponse
}

function Invoke-RconCommands {
    param([string[]]$Commands)
    
    $results = @()
    foreach ($cmd in $Commands) {
        Write-Host "  > $cmd" -ForegroundColor Yellow
        $result = Send-Rcon $cmd
        if ($result) {
            $result.Trim() -split "`n" | Where-Object { $_.Trim() } | ForEach-Object {
                Write-Host "    $_" -ForegroundColor Gray
            }
            $results += $result
        }
        Start-Sleep -Milliseconds 200
    }
    return $results
}
