#!/usr/bin/env pwsh
# Helper script to start server and optionally run a stress test for recording.

param(
    [switch] $RunStressTest
)

$root = "d:\Real Time Distributed Auction System\Real-Time-Distributed-Auction-System"
Set-Location $root

Write-Host "Starting server (logs will stream)..."
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "-DskipTests","compile","exec:java" -WorkingDirectory $root

if ($RunStressTest) {
    Write-Host "Waiting 2s then running stress test (will print results)..."
    Start-Sleep -Seconds 2
    mvn -Dtest=com.auction.stress.ConcurrentBiddingHighStressTest test
}

Write-Host "Server started. Use your client to demo flows. Ctrl+C in this console will not stop the started process (find java process to stop)."
