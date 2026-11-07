# Regional cupboard recolouring investigation — 2026-11-06

## Observed failure
The user requested yellow upper cupboards and black lower cupboards. Their screenshot shows predominantly pale cupboards, so the output fails the requested colour assignment.

## Source findings and changes
- Stability search-and-recolor receives one automatic object selection. The existing builder deliberately removes position qualifiers when both upper and lower occur, so all cupboards are selected together. A longer preservation prompt does not supply two independent masks.
- Fixed missing yellow and other primary colour labels in backend and frontend prompt enhancement summaries. This metadata fix alone does not solve generated-image quality.
- Fixed single-region target extraction when a colour occurs between position and object, such as `upper yellow cupboards`.
- Removed legacy enhancement/category prefixes from the finish instruction sent to search-and-recolor.
- Flagged unsupported historical perfect-score and geometry-guarantee claims in the old quality report.

## Proposed visual experiment
`scripts/test-ai-regional-recolor.py --live` sends only the repository demo wardrobe image to Stability, first selecting upper overhead doors with a yellow finish, then selecting lower tall doors with a black finish. It writes the actual returned images and request summaries under `apps/api/target/regional-recolor-2026-11-06`. No database mutations, retries or production routing changes are made by this experiment.

The first sandbox attempt failed DNS before connecting. Automatic approval review initially blocked escalation; the user then explicitly approved the two calls (10 credits). Both calls completed with HTTP 200, with no retries or additional calls. Both returned images were visually inspected.

## Actual visual results — FAIL
- `upper overhead wardrobe cabinet doors` with yellow finish: the LOWER tall doors became yellow while the upper doors stayed pale. This is a region-selection failure, not an HTTP or upload failure.
- Using that output, `lower tall wardrobe cabinet doors below the overhead cabinets` with black finish: lower doors became charcoal/dark grey; upper doors remained pale. Final image does not satisfy yellow upper / black lower.
- Stable evidence: `docs/qa/regional-upper-yellow-failed-2026-11-06.png` and `docs/qa/regional-two-call-failed-2026-11-06.png`.
- The tested automatic two-call approach is NOT enabled in production code.

## Corrective workflow
- Added conservative recognition of explicit distinct upper/lower colour assignments in frontend and backend. Matching Stability requests are rejected before job creation in both create and variation paths, avoiding a known unsuitable generation. This is not a general natural-language or image understanding model.
- The UI offers each detected region/colour as a separate precise edit. Clicking one selects mask editing, clears any prior selection, and sets one-region instructions. The user must paint the actual doors.
- Added `Edit this result` to load an authorized completed image as the next input, so a subsequent precise edit can preserve the previous colour outside its mask. Existing media authorization and preview endpoints are used; no visibility or ownership changes are made.
- No new paid test of the two-step masked workflow was performed. These controls do not guarantee perfect colour/material output inside a selected region.

## Status
Frontend focused tests: 23 passed. Backend focused prompt/regional tests: 20 passed. Frontend typecheck and lint passed. The earlier broader provider unit suite stalled while Mockito attached and was stopped; it is not a passing result. Browser verification could not proceed because localhost:3001 refused the connection. No runtime restart, database mutation, commit or push. Automatic prompt-only two-colour accuracy remains unsolved with the tested provider endpoint.

