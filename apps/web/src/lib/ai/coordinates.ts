/**
 * Coordinate mapping and ratio normalization for precision editing canvas.
 * Normalizes screen/pointer coordinates against display dimensions and maps
 * to authoritative source image dimensions to prevent distortion across responsive displays.
 */

export interface Point {
  x: number;
  y: number;
}

export interface Dimensions {
  width: number;
  height: number;
}

/**
 * Normalizes screen/pointer coordinates into unit [0, 1] relative coordinates.
 * Clamps coordinates to keep them strictly within bounds [0, 1].
 */
export function toNormalizedCoords(
  screenX: number,
  screenY: number,
  displayWidth: number,
  displayHeight: number
): Point {
  if (displayWidth <= 0 || displayHeight <= 0) {
    return { x: 0, y: 0 };
  }
  const x = Math.max(0, Math.min(1, screenX / displayWidth));
  const y = Math.max(0, Math.min(1, screenY / displayHeight));
  return { x, y };
}

/**
 * Maps normalized [0, 1] coordinates to original source image pixel coordinates.
 */
export function toSourceCoords(
  normalized: Point,
  sourceWidth: number,
  sourceHeight: number
): Point {
  const x = Math.round(normalized.x * sourceWidth);
  const y = Math.round(normalized.y * sourceHeight);
  return {
    x: Math.max(0, Math.min(sourceWidth, x)),
    y: Math.max(0, Math.min(sourceHeight, y)),
  };
}

/**
 * Scales a display canvas containing the user's painted mask to a new offscreen canvas
 * matching the intrinsic source image dimensions.
 */
export function scaleCanvasToSource(
  displayCanvas: HTMLCanvasElement,
  sourceWidth: number,
  sourceHeight: number
): HTMLCanvasElement {
  const targetCanvas = document.createElement('canvas');
  targetCanvas.width = sourceWidth;
  targetCanvas.height = sourceHeight;

  const ctx = targetCanvas.getContext('2d');
  if (ctx) {
    ctx.imageSmoothingEnabled = false; // Keep mask edges crisp
    ctx.drawImage(
      displayCanvas,
      0,
      0,
      displayCanvas.width,
      displayCanvas.height,
      0,
      0,
      sourceWidth,
      sourceHeight
    );
  }

  return targetCanvas;
}

/**
 * Calculates the proportion of selected/drawn pixels on a canvas (0.0 to 1.0).
 */
export function calculateCoverageRatio(canvas: HTMLCanvasElement): number {
  const ctx = canvas.getContext('2d');
  if (!ctx || canvas.width === 0 || canvas.height === 0) return 0;

  const imgData = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const data = imgData.data;
  let selected = 0;
  const total = canvas.width * canvas.height;

  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3];
    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    if (alpha > 20 && (r > 20 || g > 20 || b > 20)) {
      selected++;
    }
  }

  return total > 0 ? selected / total : 0;
}
