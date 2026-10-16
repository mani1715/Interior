# Elégance Platform: Phase 12D AI Provider Capability Matrix

## 1. Executive Summary

This document evaluates candidate AI providers for the Elégance Interior Designer Platform image generation and editing subsystem. Live account activation and paid API calls are deferred to **Phase 15 (Pre-Launch Activation)**. 

The evaluation criteria reflect the domain requirements of interior designers: precision inpainting (masking), preservation of perspective lines and architectural geometry, accurate material and color rendering, enterprise data privacy, and predictable cost controls.

---

## 2. Comprehensive Provider Comparison Matrix

| Evaluation Dimension | Google Cloud Vertex AI (Imagen 3) | Stability AI (SD 3.5 / Inpaint) | Replicate (Flux.1 / SDXL Inpaint) | Custom / Self-Hosted Endpoint |
| :--- | :--- | :--- | :--- | :--- |
| **Primary Model Family** | `imagen-3.0-generate-002` / Inpaint | `stable-diffusion-xl-1024-v1-0` / SD 3.5 | `black-forest-labs/flux-fill-pro` | ComfyUI / vLLM / Diffusers API |
| **Precision Inpainting / Masking** | **EXCELLENT**: Native mask + prompt editing; seamless seam blending | **VERY GOOD**: Highly capable inpainting endpoint; requires prompt tuning | **EXCELLENT**: State-of-the-art mask fill fidelity on furniture boundaries | **HIGH DEPENDENCY**: Depends on backend model pipeline |
| **Room Geometry Preservation** | **SUPERIOR**: Preserves wall angles, floor planes, and structural lines | **MODERATE**: Can hallucinate slight perspective shifts if prompt is loose | **VERY GOOD**: High structural fidelity on architectural boundaries | **VARIABLE**: Requires ControlNet (Depth / Canny / MLSD) integration |
| **Material & Texture Rendering** | **SUPERIOR**: Photorealistic wood grains, matte cabinetry, marble veining | **GOOD**: Good textural detail; occasional smoothing on fabrics | **SUPERIOR**: Outstanding fine textures and natural lighting reflection | **VARIABLE**: Highly tuned LoRAs required for production consistency |
| **Reference Image Conditioning** | Supported (Style / Subject reference guidance) | Supported via Image-to-Image / multi-prompt weights | Supported via multi-input pipeline | Supported via IP-Adapter / ControlNet |
| **Typical Latency (p50 / p95)** | ~4.5s / ~9.0s | ~5.0s / ~12.0s | ~6.5s / ~15.0s | ~2.5s / ~5.0s (Dedicated GPU cluster) |
| **Estimated Cost / Image** | ~\$0.030 – \$0.040 (300–400 minor INR/USD) | ~\$0.035 – \$0.050 | ~\$0.025 – \$0.055 | Fixed GPU instance cost + egress |
| **Data Privacy & Training Policy** | **Zero Data Training**: Customer data excluded from foundation training | **Opt-out via Enterprise**: Standard terms require review | **Opt-out via Enterprise**: Depends on model creator license | **100% On-Premise / Sovereign**: Complete data isolation |
| **Regional Residency** | Global regions (US, EU, Asia-South1 / Mumbai) | Primarily US / Global CDN | Cloud regions (US-East / West) | Cloud VPC of choice |
| **Safety & Moderation** | Built-in Google Cloud Safety Filter with configurable thresholds | Built-in NSFW / Safety content filter | Provider-level moderation checks | Must implement custom safety filters |
| **Elégance Compatibility Rating** | **PRIMARY RECOMMENDATION (Score: 9.6/10)** | **SECONDARY CANDIDATE (Score: 8.5/10)** | **ALTERNATIVE (Score: 8.1/10)** | **PHASE 16+ ROADMAP (Score: 7.8/10)** |

---

## 3. Deep-Dive Provider Analysis

### 3.1 Primary Candidate: Google Cloud Vertex AI (Imagen 3)
- **Strengths**:
  - Unrivaled spatial coherence and photorealism on architectural interiors.
  - Native inpainting model preserves unmasked regions with zero color shift or boundary fringing.
  - Stringent enterprise data privacy commitments (no customer data used for model retraining).
  - Regional data processing in `asia-south1` (Mumbai) and `europe-west3` (Frankfurt) meets global data sovereignty requirements.
- **Integration Profile**:
  - Header: `Authorization: Bearer <GCP_ACCESS_TOKEN>` or API Key.
  - Payload format: JSON containing base64 encoded source image, mask image, and structured prompt parameters.

### 3.2 Secondary Candidate: Stability AI
- **Strengths**:
  - Mature inpainting REST APIs with granular control over denoising strength and guidance scale.
  - Extensive community research and documentation.
- **Considerations**:
  - Occasional perspective drift unless prompt explicitly instructs preservation of vanishing points.
  - Requires enterprise agreement for contractual zero-training guarantee.

### 3.3 Alternative: Replicate (Flux.1 Fill Pro)
- **Strengths**:
  - Flux.1 Fill provides bleeding-edge lighting coherence across mask seams.
  - Pay-per-second or pay-per-generation pricing model.
- **Considerations**:
  - Cold-start latency spikes (up to 20-30s during cold boots).
  - Webhook callback model requires public ingress or polling.

### 3.4 Future Roadmap: Self-Hosted GPU Microservice
- **Target**: Dedicated Kubernetes cluster with NVIDIA L4/A100 instances running ComfyUI / Diffusers.
- **Timing**: Deferred to Phase 16+ after platform volume exceeds 10,000 generations/day to justify fixed infrastructure overhead.

---

## 4. Phase 15 Activation Recommendation

1. **Target**: Activate **Google Cloud Vertex AI (Imagen 3)** as default production provider.
2. **Fallback**: Maintain **Stability AI** configuration as hot standby fallback provider via `application-production.properties`.
3. **Budget Allocation**: Initial monthly sandbox budget of \$100.00 USD for Phase 15 live acceptance verification drills.
