# AI studio refinement and live test

## Delivered
- Larger desktop editing canvas, quieter palette, compact controls and clear project/photo/edit steps.
- Precision selection lives on the canvas; source changes clear the previous selection and comparison.
- Brush coordinates follow the displayed canvas dimensions; resizing retains the selection.
- Short, surface-focused inpainting instructions replace verbose automatic preservation instructions.
- Stability binary precision results are composited with the original image outside the normalized mask. Dimension mismatches fail safely rather than silently misaligning the result.
- Authenticated job comparisons use the private preview endpoint for input photos. Draft-project public derivative URLs previously returned 404 and left the before image blank.
- Quick Recolor guidance no longer implies reliable independent upper/lower colour assignment. Each finish needs its own selection.

## Live provider verification
Exactly one new paid Stability request was submitted with explicit user approval, using the repository demo wardrobe photo and a manually painted upper-door mask.

- Prompt: `Matte black cupboard doors`
- Job: `01a19a64-04b9-7e82-8131-e5f5e62ceaaa`
- Result: SUCCEEDED, persisted and displayed after a reload.
- Visual observation: the painted upper region became black; the lower green doors remained green. The manually unpainted border remains visible. This is a successful regional-edit demonstration, not proof of exact colour matching or arbitrary two-tone prompt accuracy.
- Before/after photos both visually confirmed after fixing the input preview route.
- Screenshot: `qa/ai-studio-comparison-2026-11-02.jpg`.

## Validation
- Focused frontend: 14 tests passed.
- Focused backend: 22 tests passed, zero failures/errors/skips (StabilityPromptBuilderTest, PrecisionImageCompositeTest, ConfigurableAiImageProviderTest).
- Frontend lint, typecheck and production build passed.
- Desktop UI reviewed; mobile 390px layout reviewed, document width 375px with no horizontal overflow.
- No database reset, migration change, commit or push in this refinement.

## Limits
The provider remains generative: selected-region geometry and exact finish fidelity require human review. Compositing protects decoded pixels outside the mask before existing watermark/preview processing; it does not validate colours inside the mask. Only one live scenario was tested in this pass. Existing broader QA and isolated PostgreSQL results are documented in QA_AI_WORKSPACE_2026-10-25.md.

## Follow-up: black and white combination
One separately authorized demo used `give the cupboards in white and black combination`. It returned white upper panels and dark-grey lower panels, also recolouring adjacent cabinetry. This is not a strict region/colour fidelity pass. Screenshot: `qa/ai-black-white-test-2026-11-02.jpg`.

The user subsequently supplied a screenshot where both upper and lower kitchen cupboards became white despite requesting upper white/lower black. Their enhanced prompt prefixed the instruction with `in wardrobe space`. Investigation found target selection could select that metadata-derived wardrobe term before the actual instruction. Both server and browser fallback enhancers now omit project category from generation text. Legacy category prefixes are excluded from target extraction. `down` participates in lower-region detection, and detected colour labels retain plain white/black instead of substituting alabaster/charcoal. Regression tests cover the user's exact original wording and legacy improved prompt.

Validation of this follow-up: 29 focused backend tests and 9 prompt-composer tests passed; frontend typecheck and lint passed. No new paid generation was performed for the screenshot case. The user supplied a comparison screenshot, not the original source file. Reliable independent two-region control remains unverified; passing prompt tests does not establish image fidelity.

## UI readiness follow-up — November 2

- Simplified AI form typography, collapsed optional colour presets, shortened preset instructions and removed the crowded voice-help paragraph.
- Fixed intrinsic grid overflow that allowed the form to extend underneath the preview at desktop widths.
- Added accessible generation-readiness feedback: missing project/photo/prompt/mask and unavailable service now have an explicit reason beside Generate.
- Added Edit this original photo to history comparisons so a displayed old source can become the active generation input.
- Live localhost:3001 verification: selected the user's existing kitchen photo through that action, entered their upper-white/lower-black instruction, and confirmed Generate AI Concept enabled. Did not submit a paid generation.
- Validation: 20 focused frontend tests passed; frontend lint and typecheck passed.
- CSS preload inspection: Next-generated CSS link already uses as=style. The unused-preload warning is not a generation validation failure and has not been claimed eliminated. A clean reload rendered the current form successfully; captured earlier development hot-reload errors preceded that reload.
- Proof: docs/qa/ai-studio-ready-2026-11-02.png.
- Exact two-region AI colour adherence remains a provider limitation; this UI verification does not establish image-quality success.
