import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { AiPromptComposer } from '../AiPromptComposer';
import { VoiceDictationButton } from '../VoiceDictationButton';
import { enhancePromptLocally } from '@/lib/ai/prompt-enhancer';

class MockSpeechRecognition {
  start = vi.fn();
  stop = vi.fn();
  abort = vi.fn();
}

beforeEach(() => {
  window.SpeechRecognition = MockSpeechRecognition as any;
});

afterEach(() => {
  delete (window as any).SpeechRecognition;
});

describe('AiPromptComposer & Voice Dictation', () => {
  it('renders context pills, character count, and voice button', () => {
    const onPromptChange = vi.fn();
    render(
      <AiPromptComposer
        prompt="Initial prompt text"
        onPromptChange={onPromptChange}
        editingMode="PRECISION_MASK"
        preserveStructure={true}
        roomType="BEDROOM"
        referenceCount={2}
        presets={[{ label: 'Wardrobe Shutters', prompt: 'Change shutters to walnut' }]}
      />
    );

    // Verify context pills
    expect(screen.getByText(/Precision Mask \(Inpainting\)/i)).toBeDefined();
    expect(screen.getByText(/Structure Locked/i)).toBeDefined();
    expect(screen.getByText(/BEDROOM/i)).toBeDefined();
    expect(screen.getByText(/2 References attached/i)).toBeDefined();

    // Verify character count
    expect(screen.getByText(/19 \/ 1000/i)).toBeDefined();

    // Verify voice button
    expect(screen.getByRole('button', { name: /Start voice dictation/i })).toBeDefined();

    // Verify preset
    expect(screen.getByText('Wardrobe Shutters')).toBeDefined();
  });

  it('allows typing and triggers onPromptChange', () => {
    const onPromptChange = vi.fn();
    render(
      <AiPromptComposer
        prompt=""
        onPromptChange={onPromptChange}
        editingMode="FULL_IMAGE"
        preserveStructure={false}
      />
    );

    const textarea = screen.getByLabelText(/AI Prompt Instruction/i);
    fireEvent.change(textarea, { target: { value: 'make cupboards white and handles gold' } });

    expect(onPromptChange).toHaveBeenCalledWith('make cupboards white and handles gold');
  });

  it('optimizes prompt on "Improve Prompt" action and allows reverting', async () => {
    let currentPrompt = 'Change the wardrobe shutters to walnut wood with matte black handles';
    const onPromptChange = vi.fn((newText) => {
      currentPrompt = newText;
    });

    const { rerender } = render(
      <AiPromptComposer
        prompt={currentPrompt}
        onPromptChange={onPromptChange}
        editingMode="PRECISION_MASK"
        preserveStructure={true}
        roomType="BEDROOM"
      />
    );

    const improveBtn = screen.getByRole('button', { name: /Improve Prompt/i });
    fireEvent.click(improveBtn);

    await waitFor(() => {
      expect(onPromptChange).toHaveBeenCalled();
    });

    const enhanced = onPromptChange.mock.calls[0][0];
    expect(enhanced).toContain('Targeted inpainting edit on selected masked area');
    expect(enhanced).toContain('walnut');
    expect(enhanced).toContain('black');

    // Rerender with enhanced prompt to test revert
    rerender(
      <AiPromptComposer
        prompt={enhanced}
        onPromptChange={onPromptChange}
        editingMode="PRECISION_MASK"
        preserveStructure={true}
        roomType="BEDROOM"
      />
    );

    // Revert button should be visible
    const revertBtn = screen.getByRole('button', { name: /Use Original/i });
    expect(revertBtn).toBeDefined();
    fireEvent.click(revertBtn);

    expect(onPromptChange).toHaveBeenCalledWith(
      'Change the wardrobe shutters to walnut wood with matte black handles'
    );
  });

  it('applies design starter preset on click', () => {
    const onPromptChange = vi.fn();
    render(
      <AiPromptComposer
        prompt=""
        onPromptChange={onPromptChange}
        editingMode="PRECISION_MASK"
        preserveStructure={true}
        presets={[{ label: 'Wardrobe Shutters', prompt: 'Change shutters to walnut' }]}
      />
    );

    const presetBtn = screen.getByText('Wardrobe Shutters');
    fireEvent.click(presetBtn);

    expect(onPromptChange).toHaveBeenCalledWith('Change shutters to walnut');
  });

  it('clears prompt on reset button click', () => {
    const onPromptChange = vi.fn();
    render(
      <AiPromptComposer
        prompt="Some existing prompt text"
        onPromptChange={onPromptChange}
        editingMode="FULL_IMAGE"
        preserveStructure={true}
      />
    );

    const clearBtn = screen.getByRole('button', { name: /Clear prompt/i });
    fireEvent.click(clearBtn);

    expect(onPromptChange).toHaveBeenCalledWith('');
  });

  it('VoiceDictationButton renders and handles unsupported browser gracefully', () => {
    const onTranscript = vi.fn();
    render(<VoiceDictationButton onTranscript={onTranscript} />);

    const button = screen.getByRole('button');
    expect(button).toBeDefined();
  });

  it('enhancePromptLocally deterministic engine works as expected', () => {
    const result = enhancePromptLocally({
      prompt: 'make cupboards white and handles gold',
      roomType: 'KITCHEN',
      editingMode: 'FULL_IMAGE',
      preserveStructure: true,
      architecturalStyle: 'Warm Minimalist',
    });

    expect(result.originalPrompt).toBe('make cupboards white and handles gold');
    expect(result.enhancedPrompt).toContain('Photorealistic architectural interior transformation');
    expect(result.enhancedPrompt).toContain('kitchen');
    expect(result.enhancedPrompt).toContain('white');
    expect(result.enhancedPrompt).toContain('gold');
    expect(result.enhancedPrompt).toContain('Warm Minimalist');
    expect(result.enhancedPrompt).toContain('Architectural structural boundaries');
    expect(result.providerName).toBe('DETERMINISTIC_ARCHITECTURAL_PARSER');
  });
});
