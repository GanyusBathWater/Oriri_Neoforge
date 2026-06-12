$json = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/sodium/version"
$json | Where-Object { $_.game_versions -contains "1.21.1" } | Select-Object -ExpandProperty version_number -First 5
