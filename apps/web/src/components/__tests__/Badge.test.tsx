import React from 'react';
import { render, screen } from '@testing-library/react';
import { StatusBadge, AIConceptBadge, VerifiedBadge, CategoryBadge } from '../ui/Badge';

describe('Badge Components', () => {
  it('StatusBadge renders default status label', () => {
    render(<StatusBadge status="published" />);
    expect(screen.getByText(/published/i)).toBeDefined();
  });

  it('StatusBadge renders custom label override', () => {
    render(<StatusBadge status="draft" label="Custom Draft Review" />);
    expect(screen.getByText('Custom Draft Review')).toBeDefined();
  });

  it('AIConceptBadge strictly renders the mandatory "AI Concept Visualization" text', () => {
    render(<AIConceptBadge />);
    expect(screen.getByText('AI Concept Visualization')).toBeDefined();
  });

  it('VerifiedBadge renders verification text', () => {
    render(<VerifiedBadge />);
    expect(screen.getByText('Verified Professional')).toBeDefined();
  });

  it('CategoryBadge renders assigned category name', () => {
    render(<CategoryBadge name="Living Room" />);
    expect(screen.getByText('Living Room')).toBeDefined();
  });
});
