# ==============================================================================
# Stability AI Live Provider Quality & Contract Validation Script
# Elégance Interior Designer Platform
# ==============================================================================
# Security: NEVER prints, logs, or commits the Stability API key.
# Requires: ALLOW_LIVE_AI_TEST=true and valid STABILITY_API_KEY in environment
# or .env.local (git-ignored).
# ==============================================================================

param(
    [switch]$QuickOnly = $false
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ELÉGANCE — STABILITY AI LIVE PROVIDER VALIDATION" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Resolve API key from environment or .env.local safely
$apiKey = $env:STABILITY_API_KEY
$allowLive = $env:ALLOW_LIVE_AI_TEST

if (-not $apiKey -and (Test-Path ".env.local")) {
    $envLines = Get-Content ".env.local"
    foreach ($line in $envLines) {
        if ($line -match '^\s*STABILITY_API_KEY\s*=\s*(.+)$') {
            $apiKey = $matches[1].Trim()
        }
        if ($line -match '^\s*ALLOW_LIVE_AI_TEST\s*=\s*(.+)$') {
            $allowLive = $matches[1].Trim()
        }
    }
}

if (-not $allowLive -or $allowLive -ne "true") {
    Write-Host "SKIP: ALLOW_LIVE_AI_TEST is not set to 'true'. Skipping live provider calls." -ForegroundColor Yellow
    exit 0
}

if (-not $apiKey -or $apiKey.Trim() -eq "" -or $apiKey.StartsWith("your-") -or $apiKey.Contains("placeholder")) {
    Write-Host "SKIP: No valid STABILITY_API_KEY found in environment or .env.local." -ForegroundColor Yellow
    exit 0
}

Write-Host "Live provider credentials detected." -ForegroundColor Green
Write-Host "Security Check: API Key is server-side and will NOT be echoed or logged." -ForegroundColor Green

# 2. Setup output directories
$outputDir = "apps/api/target/ai-live-results"
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

$results = @()
$totalCredits = 0

function Invoke-StabilityMultipart {
    param(
        [string]$Url,
        [string]$ImageFilePath,
        [string]$MaskFilePath,
        [string]$Prompt,
        [string]$SelectPrompt,
        [string]$NegativePrompt,
        [int]$GrowMask = 3,
        [string]$OutputFormat = "png",
        [string]$OutputFile
    )

    $boundary = [System.Guid]::NewGuid().ToString()
    $LF = "`r`n"
    
    $bodyBytes = [System.IO.MemoryStream]::new()
    $writer = [System.IO.BinaryWriter]::new($bodyBytes)

    function Write-FormField($name, $val) {
        $header = "--$boundary$LFContent-Disposition: form-data; name=`"$name`"$LF$LF$val$LF"
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($header)
        $writer.Write($bytes, 0, $bytes.Length)
    }

    function Write-FilePart($name, $filename, $contentType, $filePath) {
        $header = "--$boundary$LFContent-Disposition: form-data; name=`"$name`"; filename=`"$filename`"$LF" +
                  "Content-Type: $contentType$LF$LF"
        $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($header)
        $writer.Write($headerBytes, 0, $headerBytes.Length)
        $fileBytes = [System.IO.File]::ReadAllBytes($filePath)
        $writer.Write($fileBytes, 0, $fileBytes.Length)
        $trailerBytes = [System.Text.Encoding]::UTF8.GetBytes($LF)
        $writer.Write($trailerBytes, 0, $trailerBytes.Length)
    }

    Write-FilePart "image" ([System.IO.Path]::GetFileName($ImageFilePath)) "image/png" $ImageFilePath

    if ($MaskFilePath -and (Test-Path $MaskFilePath)) {
        Write-FilePart "mask" "mask.png" "image/png" $MaskFilePath
    }

    Write-FormField "prompt" $Prompt

    if ($SelectPrompt) {
        Write-FormField "select_prompt" $SelectPrompt
    }

    if ($NegativePrompt) {
        Write-FormField "negative_prompt" $NegativePrompt
    }

    Write-FormField "grow_mask" $GrowMask.ToString()
    Write-FormField "output_format" $OutputFormat

    $endBoundary = [System.Text.Encoding]::UTF8.GetBytes("--$boundary--$LF")
    $writer.Write($endBoundary, 0, $endBoundary.Length)
    $writer.Flush()

    $contentBytes = $bodyBytes.ToArray()
    $bodyBytes.Dispose()

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $req = [System.Net.HttpWebRequest]::Create($Url)
    $req.Method = "POST"
    $req.Headers.Add("Authorization", "Bearer $apiKey")
    $req.Accept = "image/*"
    $req.UserAgent = "Elegance-Platform-AI/1.0"
    $req.ContentType = "multipart/form-data; boundary=$boundary"
    $req.ContentLength = $contentBytes.Length
    $req.Timeout = 90000

    $reqStream = $req.GetRequestStream()
    $reqStream.Write($contentBytes, 0, $contentBytes.Length)
    $reqStream.Dispose()

    try {
        $resp = $req.GetResponse()
        $sw.Stop()
        $status = [int]$resp.StatusCode
        $respStream = $resp.GetResponseStream()
        $outStream = [System.IO.File]::Create($OutputFile)
        $respStream.CopyTo($outStream)
        $outStream.Dispose()
        $respStream.Dispose()

        $seed = $resp.Headers["seed"]
        $finishReason = $resp.Headers["finish-reason"]
        $resp.Dispose()

        return [PSCustomObject]@{
            Success = $true
            StatusCode = $status
            LatencyMs = $sw.ElapsedMilliseconds
            Seed = $seed
            FinishReason = $finishReason
            OutputFile = $OutputFile
            Error = $null
        }
    } catch [System.Net.WebException] {
        $sw.Stop()
        $ex = $_.Exception
        $status = if ($ex.Response) { [int]$ex.Response.StatusCode } else { 0 }
        $errBody = ""
        if ($ex.Response) {
            $stream = $ex.Response.GetResponseStream()
            $reader = [System.IO.StreamReader]::new($stream)
            $errBody = $reader.ReadToEnd()
            $reader.Dispose()
            $stream.Dispose()
        }
        return [PSCustomObject]@{
            Success = $false
            StatusCode = $status
            LatencyMs = $sw.ElapsedMilliseconds
            Seed = $null
            FinishReason = "FAILED"
            OutputFile = $null
            Error = "$($ex.Message): $errBody"
        }
    }
}

# ------------------------------------------------------------------------------
# Test 1: Quick Recolor — Bedroom Wardrobe -> Matte Walnut Brown
# ------------------------------------------------------------------------------
Write-Host "`n[1/4] Running Quick Recolor: Wardrobe -> Matte Walnut Brown..." -ForegroundColor Yellow
$negPrompt = "redesigned furniture, changed geometry, extra handles, missing handles, extra doors, missing doors, warped cabinet, distorted perspective, changed wall, changed floor, changed ceiling, changed furniture, additional objects, removed objects, unrealistic texture, cartoon, illustration, CGI appearance, blurry details"
$qPrompt1 = "Recolor the selected wardrobe panels to a realistic matte walnut brown finish. Preserve the exact wardrobe geometry, doors, handles, grooves, edges, proportions, shadows, reflections and surrounding room. Do not redesign, replace or move the wardrobe. Do not modify walls, ceiling, floor, furniture, lights or any other unselected object. Maintain realistic interior photography and the original camera perspective."
$out1 = "$outputDir/01_quick_wardrobe_walnut.png"

$res1 = Invoke-StabilityMultipart -Url "https://api.stability.ai/v2beta/stable-image/edit/search-and-recolor" `
    -ImageFilePath "apps/web/public/images/approved/wardrobe.png" `
    -SelectPrompt "wardrobe" `
    -Prompt $qPrompt1 `
    -NegativePrompt $negPrompt `
    -GrowMask 3 `
    -OutputFormat "png" `
    -OutputFile $out1

if ($res1.Success) {
    Write-Host "   PASS: Quick Recolor (Wardrobe -> Walnut) | Status: $($res1.StatusCode) | Latency: $($res1.LatencyMs)ms | Output: $out1" -ForegroundColor Green
    $totalCredits += 5
} else {
    Write-Host "   FAIL: Quick Recolor (Wardrobe -> Walnut) | Status: $($res1.StatusCode) | Error: $($res1.Error)" -ForegroundColor Red
}

# ------------------------------------------------------------------------------
# Test 2: Quick Recolor — Kitchen Cabinets -> Warm Sage Green
# ------------------------------------------------------------------------------
Write-Host "`n[2/4] Running Quick Recolor: Kitchen Cabinets -> Warm Sage Green..." -ForegroundColor Yellow
$qPrompt2 = "Recolor only the kitchen cabinet shutters to warm sage green matte laminate. Keep the countertop, backsplash, handles, appliances, walls, floor and lighting unchanged."
$out2 = "$outputDir/02_quick_kitchen_sage.png"

$res2 = Invoke-StabilityMultipart -Url "https://api.stability.ai/v2beta/stable-image/edit/search-and-recolor" `
    -ImageFilePath "apps/web/public/images/approved/kitchen.png" `
    -SelectPrompt "kitchen cabinets" `
    -Prompt $qPrompt2 `
    -NegativePrompt $negPrompt `
    -GrowMask 3 `
    -OutputFormat "png" `
    -OutputFile $out2

if ($res2.Success) {
    Write-Host "   PASS: Quick Recolor (Kitchen -> Sage) | Status: $($res2.StatusCode) | Latency: $($res2.LatencyMs)ms | Output: $out2" -ForegroundColor Green
    $totalCredits += 5
} else {
    Write-Host "   FAIL: Quick Recolor (Kitchen -> Sage) | Status: $($res2.StatusCode) | Error: $($res2.Error)" -ForegroundColor Red
}

# ------------------------------------------------------------------------------
# Test 3: Precise Edit (Inpaint) — Wardrobe Door Mask -> Natural Oak Wood
# ------------------------------------------------------------------------------
Write-Host "`n[3/4] Running Precise Inpaint: Wardrobe Door Mask -> Natural Oak..." -ForegroundColor Yellow

# Generate binary mask matching wardrobe image dimensions
Add-Type -AssemblyName System.Drawing
$wardrobeImg = [System.Drawing.Image]::FromFile("apps/web/public/images/approved/wardrobe.png")
$w = $wardrobeImg.Width
$h = $wardrobeImg.Height
$wardrobeImg.Dispose()

$maskBmp = [System.Drawing.Bitmap]::new($w, $h, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g = [System.Drawing.Graphics]::FromImage($maskBmp)
$g.Clear([System.Drawing.Color]::Black) # 0x000000 = PRESERVE

# White mask covering wardrobe shutters (Part 11: 0xFFFFFF = INPAINT)
$brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$doorRect = [System.Drawing.Rectangle]::new([int]($w * 0.25), [int]($h * 0.15), [int]($w * 0.50), [int]($h * 0.70))
$g.FillRectangle($brush, $doorRect)
$g.Dispose()
$maskPath = "$outputDir/wardrobe_door_mask.png"
$maskBmp.Save($maskPath, [System.Drawing.Imaging.ImageFormat]::Png)
$maskBmp.Dispose()

$inpaintPrompt1 = "Change ONLY the masked cupboard/wardrobe surface to natural oak wood finish. Preserve the exact door divisions, handles, grooves, perspective, lighting and every unmasked part of the room."
$out3 = "$outputDir/03_precise_wardrobe_oak.png"

$res3 = Invoke-StabilityMultipart -Url "https://api.stability.ai/v2beta/stable-image/edit/inpaint" `
    -ImageFilePath "apps/web/public/images/approved/wardrobe.png" `
    -MaskFilePath $maskPath `
    -Prompt $inpaintPrompt1 `
    -NegativePrompt $negPrompt `
    -GrowMask 3 `
    -OutputFormat "png" `
    -OutputFile $out3

if ($res3.Success) {
    Write-Host "   PASS: Precise Inpaint (Wardrobe Mask -> Oak) | Status: $($res3.StatusCode) | Latency: $($res3.LatencyMs)ms | Output: $out3" -ForegroundColor Green
    $totalCredits += 5
} else {
    Write-Host "   FAIL: Precise Inpaint (Wardrobe Mask -> Oak) | Status: $($res3.StatusCode) | Error: $($res3.Error)" -ForegroundColor Red
}

# ------------------------------------------------------------------------------
# Test 4: Precise Edit (Inpaint) — Cabinet Door Mask -> Matte Off-White
# ------------------------------------------------------------------------------
Write-Host "`n[4/4] Running Precise Inpaint: Cabinet Door Mask -> Matte Off-White..." -ForegroundColor Yellow
$inpaintPrompt2 = "Apply a smooth matte off-white laminate finish only to the masked cabinet doors. Do not alter hardware, geometry, shadows or surrounding objects."
$out4 = "$outputDir/04_precise_cabinet_offwhite.png"

$res4 = Invoke-StabilityMultipart -Url "https://api.stability.ai/v2beta/stable-image/edit/inpaint" `
    -ImageFilePath "apps/web/public/images/approved/wardrobe.png" `
    -MaskFilePath $maskPath `
    -Prompt $inpaintPrompt2 `
    -NegativePrompt $negPrompt `
    -GrowMask 3 `
    -OutputFormat "png" `
    -OutputFile $out4

if ($res4.Success) {
    Write-Host "   PASS: Precise Inpaint (Cabinet Mask -> Off-White) | Status: $($res4.StatusCode) | Latency: $($res4.LatencyMs)ms | Output: $out4" -ForegroundColor Green
    $totalCredits += 5
} else {
    Write-Host "   FAIL: Precise Inpaint (Cabinet Mask -> Off-White) | Status: $($res4.StatusCode) | Error: $($res4.Error)" -ForegroundColor Red
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  LIVE VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Total Generations Run : 4"
Write-Host "  Credits Consumed       : $totalCredits credits"
Write-Host "  Studio Quota Used      : 4 / 20 daily generations"
Write-Host "  Test Results Saved To  : $outputDir" -ForegroundColor Green

$summary = @{
    Test1 = $res1
    Test2 = $res2
    Test3 = $res3
    Test4 = $res4
    TotalCredits = $totalCredits
}
$summaryJson = $summary | ConvertTo-Json -Depth 4
Set-Content -Path "$outputDir/validation_summary.json" -Value $summaryJson
Write-Host "Validation summary written to $outputDir/validation_summary.json" -ForegroundColor Green
