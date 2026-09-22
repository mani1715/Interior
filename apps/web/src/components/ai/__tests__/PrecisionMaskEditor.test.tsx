import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { PrecisionMaskEditor } from '../PrecisionMaskEditor';

describe('PrecisionMaskEditor', () => {
  const sampleUrl = 'https://example.com/unfinished-room.jpg';

  it('renders toolbar controls, canvas, and truthful disclaimer', () => {
    render(<PrecisionMaskEditor sourceImageUrl={sampleUrl} />);

    // Truthful disclaimer must be present
    expect(
      screen.getByText(/AI will attempt to modify only the selected area\. Minor changes outside the selection may occur\./i)
    ).toBeDefined();

    // Tools
    expect(screen.getByRole('button', { name: /Brush tool/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Eraser tool/i })).toBeDefined();

    // Sizes
    expect(screen.getByRole('button', { name: /Small brush size/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Medium brush size/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Large brush size/i })).toBeDefined();

    // Action buttons
    expect((screen.getByRole('button', { name: /Undo/i }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: /Clear/i }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('toggles tool modes between brush and eraser', () => {
    render(<PrecisionMaskEditor sourceImageUrl={sampleUrl} />);

    const brushBtn = screen.getByRole('button', { name: /Brush tool/i });
    const eraserBtn = screen.getByRole('button', { name: /Eraser tool/i });

    expect(brushBtn.getAttribute('aria-pressed')).toBe('true');
    expect(eraserBtn.getAttribute('aria-pressed')).toBe('false');

    fireEvent.click(eraserBtn);
    expect(brushBtn.getAttribute('aria-pressed')).toBe('false');
    expect(eraserBtn.getAttribute('aria-pressed')).toBe('true');

    fireEvent.click(brushBtn);
    expect(brushBtn.getAttribute('aria-pressed')).toBe('true');
    expect(eraserBtn.getAttribute('aria-pressed')).toBe('false');
  });

  it('changes brush sizes', () => {
    render(<PrecisionMaskEditor sourceImageUrl={sampleUrl} />);

    const smallBtn = screen.getByRole('button', { name: /Small brush size/i });
    const mediumBtn = screen.getByRole('button', { name: /Medium brush size/i });
    const largeBtn = screen.getByRole('button', { name: /Large brush size/i });

    expect(mediumBtn.getAttribute('aria-pressed')).toBe('true');

    fireEvent.click(smallBtn);
    expect(smallBtn.getAttribute('aria-pressed')).toBe('true');
    expect(mediumBtn.getAttribute('aria-pressed')).toBe('false');

    fireEvent.click(largeBtn);
    expect(largeBtn.getAttribute('aria-pressed')).toBe('true');
    expect(smallBtn.getAttribute('aria-pressed')).toBe('false');
  });

  it('provides export ready callback', () => {
    const onExportReady = vi.fn();
    render(<PrecisionMaskEditor sourceImageUrl={sampleUrl} onExportReady={onExportReady} />);

    expect(onExportReady).toHaveBeenCalled();
    expect(typeof onExportReady.mock.calls[0][0]).toBe('function');
  });
});
