# Rebuild labelled demo data through the normal development API. Never restores lost records.
# Idempotent by demo project title; does not publish projects or overwrite existing data.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$api = 'http://localhost:8080/api/v1'
$providers = Invoke-RestMethod "$api/auth/providers"
if (-not $providers.devAuthEnabled) { throw 'Local development personas must already be enabled.' }
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$headers = @{ Origin = 'http://localhost:3001' }
$csrf = Invoke-RestMethod "$api/auth/csrf" -WebSession $session
$headers['X-CSRF-Token'] = $csrf.csrfToken
$owner = Invoke-RestMethod "$api/auth/dev-login" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body '{"persona":"designer_owner"}'
# Browsers permit Secure cookies on localhost. PowerShell's cookie container does
# not; explicitly forward this session's cookies only to the fixed localhost API.
$headers['Cookie'] = $session.Cookies.GetCookieHeader([Uri]'https://localhost:8080/')
$headers['X-Studio-Id'] = $owner.activeStudioId
$csrf = Invoke-RestMethod "$api/auth/csrf" -WebSession $session -Headers $headers
$headers['X-CSRF-Token'] = $csrf.csrfToken
function Invoke-DemoApi($path, $body) {
    Invoke-RestMethod "$api$path" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body ($body | ConvertTo-Json -Depth 8)
}
$existingResponse = Invoke-RestMethod "$api/projects" -WebSession $session -Headers $headers
$existing = @($existingResponse)
$manifest = @()
$fixtures = @(
    @{ title='DEMO - Cupboard colour study'; category='WARDROBE'; photo='wardrobe.png' },
    @{ title='DEMO - Contemporary kitchen'; category='MODULAR_KITCHEN'; photo='kitchen.png' },
    @{ title='DEMO - Warm living room'; category='LIVING_ROOM'; photo='hall.png' }
)
foreach ($fixture in $fixtures) {
    $project = $existing | Where-Object title -eq $fixture.title | Select-Object -First 1
    if (-not $project) {
        $project = Invoke-DemoApi '/projects' @{
            title=$fixture.title; categoryCode=$fixture.category;
            shortDescription='Development demo created after the October 23 database reset. Not a real client project.';
            fullDescription='Labelled demo fixture for testing photo uploads, AI editing and portfolio previews. No client identity or completed work is implied.';
            visibilityStatus='PRIVATE'; city='Bengaluru'; state='Karnataka'; country='IN';
            propertyType='APARTMENT'; styleCodes=@('WARM_CONTEMPORARY'); completionYear=2026;
            internalNotes='REPLACEMENT DEMO DATA. Original records could not be recovered.'
        }
    }
    $mediaResponse = Invoke-RestMethod "$api/projects/$($project.id)/media" -WebSession $session -Headers $headers
    $media = @($mediaResponse)
    if ($media.Count -eq 0) {
        $image = Get-Item (Join-Path $root "apps/web/public/images/approved/$($fixture.photo)")
        $intent = Invoke-DemoApi '/media/upload-intent' @{
            projectId=$project.id; mediaType='BEFORE'; expectedContentType='image/png';
            expectedSizeBytes=$image.Length; clientFileName=$image.Name
        }
        $uploadUrl = $intent.uploadUrl
        if ($uploadUrl.StartsWith('/api/v1')) { $uploadUrl = "http://localhost:8080$uploadUrl" }
        elseif ($uploadUrl.StartsWith('/')) { $uploadUrl = "$api$uploadUrl" }
        $uploadTarget = [Uri]$uploadUrl
        if ($uploadTarget.Host -ne 'localhost' -or $uploadTarget.Port -ne 8080) {
            throw 'This fixture script only uploads to local development storage.'
        }
        Invoke-RestMethod $uploadUrl -Method Put -WebSession $session -Headers $headers -ContentType 'image/png' -InFile $image.FullName | Out-Null
        $media = @(Invoke-DemoApi '/media/commit' @{
            uploadIntentId=$intent.uploadIntentId; altText=$fixture.title;
            caption='Repository demo photograph for development testing'; visibility='PORTFOLIO'; isCover=$true
        })
    }
    $manifest += @{ title=$fixture.title; projectId=$project.id; mediaIds=@($media | ForEach-Object id) }
}
$outputDir = Join-Path $root 'storage/backups'
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
@{ kind='replacement-demo-data'; studioId=$owner.activeStudioId; createdAt=(Get-Date).ToUniversalTime().ToString('o'); projects=$manifest } |
    ConvertTo-Json -Depth 8 | Set-Content (Join-Path $outputDir 'demo-rebuild-manifest.json')
Write-Output "Demo studio rebuilt: $($manifest.Count) private projects with uploaded photos. No original records restored."
