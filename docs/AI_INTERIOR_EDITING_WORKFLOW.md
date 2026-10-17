# Elégance Platform: Interior Designer AI Recolor & Inpaint Workflow

This document provides step-by-step guidance for interior designers using the Elégance AI Studio to recolor cupboards, wardrobes, millwork, and cabinetry without altering architectural geometry or surrounding room features.

---

## 1. Mode Selection: Quick Recolor vs. Precise Edit

The Elégance AI Workspace (`/workspace/ai`) offers two specialized editing modes tailored to interior millwork:

```
+---------------------------------------------------------------------------------------+
|                                    MODE SELECTOR                                      |
+---------------------------------------------------+-----------------------------------+
|  [⚡ Quick Recolor]                                |  [🎯 Precise Edit]                |
|  Automatic AI segmentation                        |  Designer mask painting           |
|  Ideal for: Entire wardrobes, kitchen cabinets,   |  Ideal for: Specific doors,       |
|  credenzas, vanities, or complete closet suites.  |  accent panels, hardware masking, |
|  No manual painting required.                     |  or dual-tone color schemes.      |
+---------------------------------------------------+-----------------------------------+
```

---

## 2. Quick Recolor Workflow

1. **Upload or Select Image:**
   - Navigate to `/workspace/ai`.
   - Select an existing project render or upload a clean, high-resolution interior photograph (PNG or JPEG, up to 10 MB, minimum 1024x768).
2. **Select Mode:**
   - Choose **Quick Recolor**.
3. **Specify the Furniture Item & Target Finish:**
   - Enter your prompt describing the furniture item and desired finish.
   - *Example 1:* `"Change the wardrobe to a premium matte walnut brown finish"`
   - *Example 2:* `"Recolor kitchen cabinets to warm sage green matte laminate"`
   - *Example 3:* `"Update the closet doors to dark charcoal ash wood grain"`
4. **Automatic Prompt Enhancement:**
   - The platform automatically extracts the target furniture category (e.g. `wardrobe`, `kitchen cabinets`) for semantic segmentation (`select_prompt`).
   - The platform injects structural preservation directives (handles, door gaps, walls, floor, lighting, camera angle) and anti-hallucination negative prompts.
5. **Generate:**
   - Click **Generate Visualization**.
   - Processing time is typically 11 - 15 seconds.
   - Inspect the before/after preview using the interactive comparison slider.

---

## 3. Precise Edit (Mask Inpaint) Workflow

1. **Select Mode:**
   - Choose **Precise Edit**.
2. **Adjust Brush Size & Paint Mask:**
   - Use the slider to select your brush size (10px - 100px).
   - Paint directly over the cupboard panels or cabinet doors you wish to recolor.
   - **Tip:** You do not need to avoid handles—the AI prompt will preserve them, but masking only the door face provides the cleanest boundary.
   - Use **Clear Mask** if you need to start over.
3. **Enter Desired Finish:**
   - *Example 1:* `"Natural white oak veneer with subtle vertical grain"`
   - *Example 2:* `"Smooth matte off-white laminate (#FAF9F6)"`
   - *Example 3:* `"Deep navy blue lacquer finish with soft satin sheen"`
4. **Automatic Mask Normalization:**
   - The platform maps your viewport strokes back to native source image coordinates.
   - Normalizes the mask into high-contrast binary pixels (`0x000000` = preserve, `0xFFFFFF` = inpaint).
   - Applies automated mask expansion (`grow_mask = 3`) to ensure seamless seam transitions without haloing.
5. **Generate & Save:**
   - Click **Generate Visualization**.
   - Save the approved visualization directly to your studio project portfolio or mood board.

---

## 4. Prompt Engineering Best Practices for Interior Finishes

| Finish Category | Recommended Keywords | What to Avoid |
|---|---|---|
| **Natural Wood** | `"natural white oak"`, `"matte walnut wood grain"`, `"fine vertical grain"`, `"warm teak veneer"` | Avoid `"wooden"`, `"brown"` (too vague, leads to flat muddy colors). |
| **Laminate** | `"matte laminate"`, `"satin finish"`, `"ultra-matte anti-fingerprint finish"`, `"soft-touch surface"` | Avoid `"plastic"`, `"shiny"` (leads to artificial specular glare). |
| **Colors & Tones** | `"warm sage green"`, `"soft taupe"`, `"charcoal anthracite"`, `"warm off-white (#F5F5F0)"` | Avoid specifying colors without finish terms. Always pair color with finish. |
| **Preservation** | Auto-injected by Elégance platform. | Do NOT ask the AI to change handles or add moldings in recolor mode. |

---

## 5. Daily Quotas & Studio Billing

- **Studio Allowance:** Each studio workspace receives **20 AI generations per day** included in standard subscriptions.
- **Credit Value:** 1 generation = 5 provider credits.
- **Cost Transparency:** Every generation logs timestamp, latency, prompt tokens, and credit deduction to the studio's audit ledger at `/workspace/billing`.
- **Concurrency Safety:** Generation requests are throttled per studio to prevent accidental multi-click credit depletion.
