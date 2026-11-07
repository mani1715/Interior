# ==============================================================================
# verify-seo-readiness.ps1
#
# Verification for SEO, Generative Engine Optimization (GEO),
# Schema.org structured data, llms.txt, and Agentic Search readiness.
# ==============================================================================

param(
    [string]$BaseUrl = "http://localhost:3000"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Elegance Platform - SEO and GEO Readiness Verification" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

$passed = 0
$total = 0

function Test-Check {
    param(
        [string]$Name,
        [scriptblock]$Check
    )
    $script:total++
    try {
        $result = & $Check
        if ($result -eq $true) {
            Write-Host " [PASS] $Name" -ForegroundColor Green
            $script:passed++
        } else {
            Write-Host " [FAIL] ${Name}: Test returned false" -ForegroundColor Red
        }
    } catch {
        Write-Host " [FAIL] ${Name}: $_" -ForegroundColor Red
    }
}

# 1. Static Artifacts Verification
Test-Check "robots.txt route definition exists" {
    (Test-Path "apps/web/src/app/robots.ts") -eq $true
}

Test-Check "sitemap.xml route definition exists" {
    (Test-Path "apps/web/src/app/sitemap.ts") -eq $true
}

Test-Check "llms.txt discovery route exists" {
    (Test-Path "apps/web/src/app/llms.txt/route.ts") -eq $true
}

Test-Check "llms-full.txt discovery route exists" {
    (Test-Path "apps/web/src/app/llms-full.txt/route.ts") -eq $true
}

# 2. Source Code Content Audits
Test-Check "robots.ts allows AI crawlers and allows llms.txt" {
    $content = Get-Content "apps/web/src/app/robots.ts" -Raw
    ($content.Contains("GPTBot") -and $content.Contains("ClaudeBot") -and $content.Contains("PerplexityBot") -and $content.Contains("/llms.txt"))
}

Test-Check "structured-data.tsx defines HomeAndConstructionBusiness" {
    $content = Get-Content "apps/web/src/lib/seo/structured-data.tsx" -Raw
    ($content.Contains("ProfessionalService") -and $content.Contains("HomeAndConstructionBusiness"))
}

Test-Check "structured-data.tsx defines ImageObject schema for Google Images" {
    $content = Get-Content "apps/web/src/lib/seo/structured-data.tsx" -Raw
    ($content.Contains("buildProjectImageObjects") -and $content.Contains("ImageObject"))
}

Test-Check "projects/[projectSlug] injects ImageObject schema" {
    $content = Get-Content -LiteralPath "apps/web/src/app/projects/[projectSlug]/page.tsx" -Raw
    ($content.Contains("buildProjectImageObjects"))
}

Test-Check "workspace/seo/page.tsx includes GEO Citability scorecard" {
    $content = Get-Content "apps/web/src/app/workspace/seo/page.tsx" -Raw
    ($content.Contains("AI Search & Citability Optimization (GEO)"))
}

Write-Host ""
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host " Summary: $passed / $total SEO checks passed cleanly." -ForegroundColor $(if ($passed -eq $total) { "Green" } else { "Red" })
Write-Host "--------------------------------------------------------" -ForegroundColor Cyan
Write-Host ""

if ($passed -ne $total) {
    exit 1
}
