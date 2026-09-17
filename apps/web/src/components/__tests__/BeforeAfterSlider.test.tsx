import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { BeforeAfterSlider } from '../media/BeforeAfterSlider';

describe('BeforeAfterSlider Component', () => {
  const beforeUrl = 'data:image/svg+xml;utf8,<svg></svg>';
  const afterUrl = 'data:image/svg+xml;utf8,<svg></svg>';

  it('renders slider element with correct ARIA attributes', () => {
    render(
      <BeforeAfterSlider
        beforeImage={beforeUrl}
        afterImage={afterUrl}
        initialPosition={50}
      />
    );

    const slider = screen.getByRole('slider');
    expect(slider).toBeDefined();
    expect(slider.getAttribute('aria-valuenow')).toBe('50');
    expect(slider.getAttribute('aria-valuemin')).toBe('0');
    expect(slider.getAttribute('aria-valuemax')).toBe('100');
  });

  it('updates position upon keyboard ArrowLeft and ArrowRight navigation', () => {
    render(
      <BeforeAfterSlider
        beforeImage={beforeUrl}
        afterImage={afterUrl}
        initialPosition={50}
      />
    );

    const slider = screen.getByRole('slider');

    // ArrowLeft decreases by 5%
    fireEvent.keyDown(slider, { key: 'ArrowLeft' });
    expect(slider.getAttribute('aria-valuenow')).toBe('45');

    // ArrowRight increases by 5%
    fireEvent.keyDown(slider, { key: 'ArrowRight' });
    expect(slider.getAttribute('aria-valuenow')).toBe('50');

    // Home jumps to 0%
    fireEvent.keyDown(slider, { key: 'Home' });
    expect(slider.getAttribute('aria-valuenow')).toBe('0');

    // End jumps to 100%
    fireEvent.keyDown(slider, { key: 'End' });
    expect(slider.getAttribute('aria-valuenow')).toBe('100');
  });

  it('renders custom before and after label badges', () => {
    render(
      <BeforeAfterSlider
        beforeImage={beforeUrl}
        afterImage={afterUrl}
        beforeLabel="Raw Site"
        afterLabel="Handcrafted Living"
      />
    );

    expect(screen.getByText('Raw Site')).toBeDefined();
    expect(screen.getByText('Handcrafted Living')).toBeDefined();
  });
});
