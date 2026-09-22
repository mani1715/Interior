import { describe, it, expect } from 'vitest';
import {
  toNormalizedCoords,
  toSourceCoords,
  calculateCoverageRatio,
  scaleCanvasToSource,
} from '../coordinates';

describe('AI Visualizer Coordinates Math', () => {
  describe('toNormalizedCoords', () => {
    it('normalizes center point correctly', () => {
      const norm = toNormalizedCoords(200, 150, 400, 300);
      expect(norm.x).toBeCloseTo(0.5);
      expect(norm.y).toBeCloseTo(0.5);
    });

    it('clamps negative values to 0', () => {
      const norm = toNormalizedCoords(-50, -10, 500, 500);
      expect(norm.x).toBe(0);
      expect(norm.y).toBe(0);
    });

    it('clamps values exceeding display dimensions to 1', () => {
      const norm = toNormalizedCoords(600, 400, 500, 300);
      expect(norm.x).toBe(1);
      expect(norm.y).toBe(1);
    });

    it('handles zero or negative display dimensions gracefully', () => {
      const norm = toNormalizedCoords(100, 100, 0, 0);
      expect(norm.x).toBe(0);
      expect(norm.y).toBe(0);
    });
  });

  describe('toSourceCoords', () => {
    it('maps normalized coords to landscape source dimensions (1920x1080)', () => {
      const source = toSourceCoords({ x: 0.25, y: 0.75 }, 1920, 1080);
      expect(source.x).toBe(480);
      expect(source.y).toBe(810);
    });

    it('maps normalized coords to portrait source dimensions (1080x1920)', () => {
      const source = toSourceCoords({ x: 0.5, y: 0.2 }, 1080, 1920);
      expect(source.x).toBe(540);
      expect(source.y).toBe(384);
    });

    it('preserves boundary corners (0,0) and (1,1)', () => {
      const origin = toSourceCoords({ x: 0, y: 0 }, 1200, 800);
      expect(origin.x).toBe(0);
      expect(origin.y).toBe(0);

      const corner = toSourceCoords({ x: 1, y: 1 }, 1200, 800);
      expect(corner.x).toBe(1200);
      expect(corner.y).toBe(800);
    });
  });

  describe('scaleCanvasToSource & calculateCoverageRatio', () => {
    it('scales display canvas to target source dimensions', () => {
      const displayCanvas = document.createElement('canvas');
      displayCanvas.width = 400;
      displayCanvas.height = 300;

      const target = scaleCanvasToSource(displayCanvas, 1200, 900);
      expect(target.width).toBe(1200);
      expect(target.height).toBe(900);
    });

    it('calculates coverage ratio on empty canvas', () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      expect(calculateCoverageRatio(canvas)).toBe(0);
    });

    it('calculates coverage ratio on partially painted canvas', () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;

      const dummyData = new Uint8ClampedArray(100 * 100 * 4);
      for (let i = 0; i < 5000 * 4; i += 4) {
        dummyData[i] = 184;
        dummyData[i + 1] = 138;
        dummyData[i + 2] = 90;
        dummyData[i + 3] = 200;
      }

      vi.spyOn(canvas, 'getContext').mockReturnValue({
        getImageData: () =>
          ({
            data: dummyData,
            width: 100,
            height: 100,
          } as unknown as ImageData),
      } as unknown as CanvasRenderingContext2D);

      const ratio = calculateCoverageRatio(canvas);
      expect(ratio).toBeCloseTo(0.5, 1);
    });
  });
});
