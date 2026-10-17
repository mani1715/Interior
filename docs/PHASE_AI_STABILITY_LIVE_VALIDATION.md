# Elégance Platform: Stability AI Live Provider Validation Report

**Phase:** 15D — AI Visualizer Real Provider Activation  
**Subsystem:** Cupboard / Wardrobe / Cabinet Recolor & Precise Inpaint  
**Provider:** Stability AI (`https://api.stability.ai`)  
**Date:** 2026-10-17  
**Validation Status:** **LIVE PROVIDER VALIDATION COMPLETE (PASS)**  
**Security Certification:** **0 Credentials Logged, Committed, or Exposed**  

---

## 1. Executive Summary

The Elégance Interior Designer Platform has successfully connected to the live Stability AI production API (`https://api.stability.ai`) and validated end-to-end interior design recolor and inpaint workflows.

All four planned live test cases executed cleanly against real cloud endpoints using sample interior assets (bedroom wardrobe and modern kitchen cabinetry). Every test succeeded on the first attempt with 100% contract compliance, zero HTTP errors, and sub-16s generation latencies.

```
========================================================================================
  LIVE STABILITY AI QUALITY & CONTRACT CERTIFICATION RESULTS
========================================================================================
  Total Test Runs      : 4
  Successful Runs      : 4 (100.0%)
  Failed Runs          : 0 (0.0%)
  Credits Consumed     : 20 credits (5 credits / generation)
  Studio Daily Quota   : 4 / 20 generations used (isolated studio ledger)
  HTTP Latency Range   : 11,423 ms - 15,593 ms (average 13,162 ms)
  Output Artifacts     : 4 High-Resolution PNGs (~2.1 MB - 2.3 MB each)
========================================================================================
```

---

## 2. API Endpoints & Contract Specifications

### 2.1 Quick Recolor Endpoint
- **URL:** `POST https://api.stability.ai/v2beta/stable-image/edit/search-and-recolor`
- **Headers:**
  - `Authorization: Bearer [REDACTED]`
  - `Accept: image/*` (or `application/json`)
  - `Content-Type: multipart/form-data; boundary=...`
- **Form Fields:**
  - `image`: Original interior photo binary (PNG)
  - `select_prompt`: Target furniture identifier (e.g. `"wardrobe"`, `"kitchen cabinets"`)
  - `prompt`: Architectural recolor and room preservation directive
  - `negative_prompt`: Structural preservation and anti-distortion negative prompt
  - `grow_mask`: `3` (edge bleed prevention)
  - `output_format`: `png`
- **Billing:** 5 credits per successful request.

### 2.2 Precise Edit (Inpaint) Endpoint
- **URL:** `POST https://api.stability.ai/v2beta/stable-image/edit/inpaint`
- **Headers:**
  - `Authorization: Bearer [REDACTED]`
  - `Accept: image/*` (or `application/json`)
  - `Content-Type: multipart/form-data; boundary=...`
- **Form Fields:**
  - `image`: Original interior photo binary (PNG)
  - `mask`: 1-channel or 3-channel binary mask matching exact source dimensions
    - Polarity: `0x000000` (Black) = **PRESERVE**
    - Polarity: `0xFFFFFF` (White) = **INPAINT / EDIT**
  - `prompt`: Surface finish and hardware preservation directive
  - `negative_prompt`: Structural preservation and anti-distortion negative prompt
  - `grow_mask`: `3`
  - `output_format`: `png`
- **Billing:** 5 credits per successful request.

---

## 3. Live Test Run Telemetry & Execution Log

| # | Test Name | Endpoint | Furniture Target | Target Finish | Latency | Status | Credits | Output Artifact |
|---|---|---|---|---|---|---|---|---|
| **1** | **Quick Recolor** | `/search-and-recolor` | Bedroom Wardrobe | Matte Walnut Brown | 14,134 ms | `200 OK` | 5 | `01_live_quick_wardrobe_walnut.png` (2,240,195 B) |
| **2** | **Quick Recolor** | `/search-and-recolor` | Kitchen Cabinets | Warm Sage Green Laminate | 15,593 ms | `200 OK` | 5 | `02_live_quick_kitchen_sage.png` (2,286,027 B) |
| **3** | **Precise Inpaint** | `/inpaint` | Wardrobe Door Mask | Natural Oak Wood Finish | 11,423 ms | `200 OK` | 5 | `03_live_precise_wardrobe_oak.png` (2,229,387 B) |
| **4** | **Precise Inpaint** | `/inpaint` | Cabinet Door Mask | Smooth Matte Off-White | 11,500 ms | `200 OK` | 5 | `04_live_precise_cabinet_offwhite.png` (2,128,181 B) |

---

## 4. Architectural & Safety Protections

1. **Strict SSRF Defense:**  
   `SsrfProtectionService` intercepts and verifies all outgoing URIs. Non-HTTPS schemes, private subnets (`10.0.0.0/8`, `192.168.0.0/16`, `172.16.0.0/12`), loopback (`127.0.0.1`), link-local (`169.254.169.254`), and IPv6 equivalents are strictly rejected before connection initiation.

2. **Server-Side API Key Confinement:**  
   The Stability API key is injected exclusively into the Spring Boot backend (`ConfigurableAiImageProvider`) via environment variables (`STABILITY_API_KEY`) or local git-ignored properties. No frontend code (`apps/web`) receives or handles provider tokens.

3. **Binary Mask Normalization:**  
   `StabilityPromptBuilder.normalizeMaskToBinary(byte[] rawMaskBytes)` enforces that canvas paint strokes and translucent antialiased pixels are thresholded into exact binary black (`#000000` / preserve) and white (`#FFFFFF` / inpaint) pixels, preventing Stability API edge-smear rejections.

4. **Studio Quota & Ledger Isolation:**  
   All generations consume atomic studio daily quota (20 generations/day limit per studio). Each generation commits an `ai_cost_ledgers` transaction recording the 5-credit deduction, request latency, and operation metadata.

5. **ClamAV Antivirus Stream Filtering:**  
   Input and output image streams are scanned for malicious macro payloads, polyglots, and embedded exploits before persisting or streaming to the designer workspace.

---

## 5. Next Steps

- Present live cupboard recoloring results to the platform owner for visual sign-off.
- Keep other provider credentials deferred until scheduled activation.
