import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import HomePage from '@/app/page';
import { Hero } from '@/components/home/Hero';
import { TransformationExperience } from '@/components/home/TransformationExperience';
import { AiVisualizerSection } from '@/components/home/AiVisualizerSection';
import { CoreValueStrip } from '@/components/home/CoreValueStrip';

describe('Homepage Production Implementation', () => {
  it('renders exactly one h1 in the Hero section', () => {
    render(<Hero />);
    const headings = screen.getAllByRole('heading', { level: 1 });
    expect(headings.length).toBe(1);
    expect(headings[0].textContent).toContain('Turn beautiful interiors into a business');
  });

  it('renders primary CTA button to create portfolio', () => {
    render(<Hero />);
    const cta = screen.getByRole('button', { name: /create your portfolio/i });
    expect(cta).toBeDefined();
  });

  it('renders secondary CTA button to explore interior projects', () => {
    render(<Hero />);
    const exploreCta = screen.getByRole('button', { name: /explore interior projects/i });
    expect(exploreCta).toBeDefined();
  });

  it('renders core value strip pillars', () => {
    render(<CoreValueStrip />);
    expect(screen.getByText('Premium Portfolio')).toBeDefined();
    expect(screen.getByText('Google Discoverability')).toBeDefined();
    expect(screen.getByText('AI Visualization')).toBeDefined();
    expect(screen.getByText('Direct Client Leads')).toBeDefined();
  });

  it('TransformationExperience renders 7-stage architectural progression and navigates stages', () => {
    render(<TransformationExperience />);

    // Stage 1 indicator initially
    expect(screen.getByText(/Stage 1 of 7/i)).toBeDefined();

    // Click Next button
    const nextButtons = screen.getAllByRole('button', { name: /next/i });
    fireEvent.click(nextButtons[0]);

    // Stage 2 indicator now active
    expect(screen.getByText(/Stage 2 of 7/i)).toBeDefined();
  });

  it('AiVisualizerSection displays mandatory AI Concept Visualization badge and legal disclaimer', () => {
    render(<AiVisualizerSection />);

    // Mandatory badge text
    expect(screen.getByText('AI Concept Visualization')).toBeDefined();

    // Legal and architectural disclosure
    expect(screen.getByText(/Colours and materials shown in AI concepts are visual simulations/i)).toBeDefined();
  });

  it('renders full Homepage with structured data script and semantic main landmark', () => {
    const { container } = render(<HomePage />);

    // Check main landmark
    const main = container.querySelector('main#main-content');
    expect(main).toBeDefined();

    // Check JSON-LD structured data script
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toBeDefined();
    expect(script?.innerHTML).toContain('https://schema.org');
    expect(script?.innerHTML).toContain('Elégance Interior Platform');
  });
});
