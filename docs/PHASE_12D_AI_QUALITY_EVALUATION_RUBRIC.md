# Elégance Platform: Phase 12D AI Quality Evaluation Rubric & Benchmark Suite

## 1. Executive Summary

This rubric establishes the formal quality standards and evaluation methodology for the Elégance Interior Designer Platform AI visualizer.

Because interior design requires strict structural accuracy, generic generative criteria (aesthetic novelty or creativity) are insufficient. The Elégance evaluation framework emphasizes **room geometry preservation**, **material fidelity**, **mask boundary containment**, and **photorealistic lighting consistency**.

---

## 2. Standard Fixture Benchmark Catalog (10 Categories)

| # | Benchmark Category | Fixture Target | Representative Source Description | Primary Modification Task |
| :-: | :--- | :--- | :--- | :--- |
| **01** | **Master Bedroom Wardrobe** | 4-door floor-to-ceiling wardrobe | Modern bedroom with natural morning light from right window | Recolor laminate doors to matte sage green / charcoal gray |
| **02** | **Kitchen Cabinetry** | L-shaped lower and overhead cabinets | Open-plan kitchen with white quartz countertops | Change lower cabinets from gloss white to natural American walnut veneer |
| **03** | **Living Room TV Unit** | Media console + vertical wall paneling | Contemporary lounge with hardwood floors and recessed LEDs | Apply vertical fluted oak paneling behind wall-mounted screen |
| **04** | **Bathroom Vanity** | Floating dual-sink vanity cabinet | Tiled master bath with circular backlit mirrors | Change vanity drawer fronts from dark grey to fluted teak wood |
| **05** | **Dining Room Credenza** | 3-bay low credenza | Dining area with brass pendant chandelier | Modify cabinet front panels to cane webbing with black timber frames |
| **06** | **Home Office Joinery** | Built-in desk and ceiling-height bookshelf | Study with large workstation and open shelving bays | Recolor open bookshelf cubbies while preserving books and accessories |
| **07** | **Entryway Foyer Unit** | Slatted shoe console and partition | Narrow entryway with overhead spot illumination | Apply warm smoked oak lacquer finish to slatted partition |
| **08** | **Bed Accent Wall** | Master bedroom bedhead feature wall | Bedroom with upholstered king bed and bedside lamps | Apply textured lime wash plaster finish (warm greige) behind headboard |
| **09** | **Walk-In Closet Island** | Central jewelry island with glass display top | Dedicated dressing room with neutral downlighting | Change island drawer carcasses to brushed brass-trimmed ebony wood |
| **10** | **Ceiling & Cove Treatment** | False ceiling tray with cove perimeter | Living room ceiling junction with perimeter molding | Recolor recessed tray ceiling to midnight navy with warm cove glow |

---

## 3. Evaluation Dimensions & 5-Point Scoring Rubric

Each generated output is evaluated across 5 core dimensions on a 1.0 to 5.0 scale:

### 1. Room Geometry Preservation (RGP)
- **5 (Flawless)**: Structural lines (wall-ceiling intersections, floor planes, door frames, windows) are 100% rectilinear and undistorted. Zero perspective drift.
- **4 (Minor Imperfection)**: Microscopic curvature (< 1–2 pixels) detectable only under 400% zoom; no visual distortion.
- **3 (Noticeable Warp)**: Slight waviness along straight structural borders; perceptible but usable for rough concepting.
- **2 (Significant Distortion)**: Wall edges angle unnaturally; window or door proportions skewed.
- **1 (Unacceptable Hallucination)**: Room perspective collapsed; walls, floors, or ceiling fundamentally warped.

### 2. Material Fidelity & Texture Realism (MTR)
- **5 (Photorealistic)**: Wood grain direction, pore depth, marble veining, fabric weave, and reflection specular highlights are indistinguishable from high-resolution professional photography.
- **4 (High Quality)**: Convincing texture with realistic diffuse reflection; slight repetitive grain pattern on large areas.
- **3 (Acceptable)**: Looks like a clean 3D render; lacks fine tactile micro-details.
- **2 (Sub-Par)**: Muddy, blurry, or low-resolution texture; plasticky or wax-like appearance.
- **1 (Unusable)**: Unidentifiable visual artifacts, smear patterns, or AI noise.

### 3. Mask Boundary Adherence (MBA)
- **5 (Razor Sharp)**: Modification is strictly bounded within the masked polygon; zero color bleed onto adjacent walls, floors, or hardware. Antialiased seam blending is invisible.
- **4 (Clean)**: Border is sharp; sub-pixel fringe on high-contrast edges easily blended.
- **3 (Minor Bleed)**: Trace color spill (1–3 pixels) onto door jambs, countertops, or ceiling molding.
- **2 (Excessive Bleed)**: Noticeable color spill or haloing extending beyond masked region.
- **1 (Boundary Breach)**: Model disregarded mask boundary; modified unmasked walls or adjacent furniture.

### 4. Color & Swatch Accuracy (CSA)
- **5 (Calibrated Match)**: Visual hue, saturation, and lightness match reference color hex or sample swatch across shadows and highlights.
- **4 (Close Match)**: Perceptible shift of less than 5 Delta-E units; natural under ambient scene lighting.
- **3 (Moderate Shift)**: Shift in hue undertone (e.g., olive undertone appearing slightly khaki).
- **2 (Poor Match)**: Distinctly different shade or saturation level.
- **1 (Completely Erroneous)**: Wrong primary color rendered.

### 5. Lighting & Shadow Consistency (LSC)
- **5 (Physically Consistent)**: Directional shadows, cast shadows from handles, ambient occlusion in cabinet crevices, and bounced floor light match original scene physics perfectly.
- **4 (Consistent)**: Accurate directional light; minor softness in contact shadows.
- **3 (Plausible)**: General scene lighting matches, but micro-shadows under pulls or trims are flattened.
- **2 (Inconsistent)**: Contradictory light direction or missing shadows causing objects to appear "floating".
- **1 (Unrealistic)**: Flat or inverted lighting; highlights appear on shadowed planes.

---

## 4. Minimum Quality Acceptance Thresholds

For an AI model/provider to be certified for production deployment in Phase 15:
- **Composite Benchmark Score**: **>= 4.2 / 5.0** across all 10 fixture categories.
- **Room Geometry Preservation (RGP)**: **Minimum 4.0** on every individual test (Zero tolerance for warped architecture).
- **Mask Boundary Adherence (MBA)**: **Minimum 4.0** on all precision inpainting tasks.
- **Critical Failure Disqualification**: Any single score of 1.0 or 2.0 on RGP or MBA disqualifies the candidate model.

---

## 5. Human Review Worksheet Template

```markdown
### Phase 15 AI Evaluation Worksheet
- **Evaluator**: [Designer / QA Engineer]
- **Date / Time**: [YYYY-MM-DD HH:mm UTC]
- **Provider Tested**: [Google Vertex AI Imagen 3 / Stability AI]
- **Model Version**: [e.g. imagen-3.0-generate-002]
- **Benchmark Fixture**: [01 - Master Bedroom Wardrobe]

| Metric | Score (1.0 - 5.0) | Evaluator Observations / Notes |
| :--- | :---: | :--- |
| Room Geometry Preservation (RGP) | 5.0 | Window frame and vertical wardrobe edges remained perfectly plumb. |
| Material Fidelity & Texture Realism (MTR) | 4.5 | Matte lacquer surface looks authentic; subtle reflection of window. |
| Mask Boundary Adherence (MBA) | 4.8 | Crisp cut against crown molding; clean seam with mirror panel. |
| Color & Swatch Accuracy (CSA) | 4.5 | Sage green matched target hex #7A8B7B under warm lighting. |
| Lighting & Shadow Consistency (LSC) | 4.7 | Ambient shadow under plinth preserved seamlessly. |
| **Composite Score** | **4.7 / 5.0** | **STATUS: ACCEPTED** |
```
