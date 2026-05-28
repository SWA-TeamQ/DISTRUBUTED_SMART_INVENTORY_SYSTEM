param(
    [Parameter(Position = 0)]
    [int]$Port = 8900
)

$ErrorActionPreference = 'Stop'

Push-Location $PSScriptRoot
try {
    & mvn.cmd exec:java "-Dexec.args=$Port"
}
finally {
    Pop-Location
}
