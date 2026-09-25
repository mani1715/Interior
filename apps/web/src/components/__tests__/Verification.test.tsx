import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { VerificationBadge } from '../verification/VerificationBadge';

describe('Phase 27 — VerificationBadge Component', () => {
  it('renders nothing when verified is false', () => {
    const { container } = render(<VerificationBadge verified={false} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders verified badge with truthful wording when verified is true', () => {
    render(<VerificationBadge verified={true} businessName="Acme Studios" />);
    expect(screen.getByText('Verified Business')).toBeDefined();
    expect(screen.getByRole('status')).toBeDefined();
  });

  it('displays tooltip on hover with business verification details and disclaimer', () => {
    render(<VerificationBadge verified={true} businessName="Acme Studios" />);
    const badge = screen.getByRole('status');
    fireEvent.mouseEnter(badge);

    expect(screen.getByText('Verified Business Entity')).toBeDefined();
    expect(screen.getByText(/Acme Studios/i)).toBeDefined();
    expect(
      screen.getByText(/official registration documents were verified by the platform team/i)
    ).toBeDefined();
    expect(
      screen.getByText(/It does not guarantee service quality or project outcomes/i)
    ).toBeDefined();
  });

  it('hides tooltip on mouse leave', () => {
    render(<VerificationBadge verified={true} businessName="Acme Studios" />);
    const badge = screen.getByRole('status');
    fireEvent.mouseEnter(badge);
    expect(screen.getByText('Verified Business Entity')).toBeDefined();

    fireEvent.mouseLeave(badge);
    expect(screen.queryByText('Verified Business Entity')).toBeNull();
  });
});
