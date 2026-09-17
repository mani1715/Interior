import React, { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { TextInput, Checkbox, Switch } from '../forms/FormControls';

describe('Form Controls Components', () => {
  it('TextInput renders with accessible label and responds to changes', () => {
    const handleChange = vi.fn();
    render(
      <TextInput
        label="Project Name"
        placeholder="Enter project name"
        onChange={handleChange}
      />
    );

    const input = screen.getByLabelText(/project name/i);
    expect(input).toBeDefined();

    fireEvent.change(input, { target: { value: 'Penthouse 501' } });
    expect(handleChange).toHaveBeenCalledTimes(1);
  });

  it('Checkbox toggles state and maintains accessible association', () => {
    function CheckboxWrapper() {
      const [checked, setChecked] = useState(false);
      return (
        <Checkbox
          label="Watermark Enabled"
          checked={checked}
          onChange={(e) => setChecked(e.target.checked)}
        />
      );
    }

    render(<CheckboxWrapper />);
    const checkbox = screen.getByRole('checkbox', { name: /watermark enabled/i });
    expect((checkbox as HTMLInputElement).checked).toBe(false);

    fireEvent.click(checkbox);
    expect((checkbox as HTMLInputElement).checked).toBe(true);
  });

  it('Switch component toggles correctly upon click', () => {
    const handleToggle = vi.fn();
    render(
      <Switch
        label="AI Diffusion"
        checked={false}
        onChange={handleToggle}
      />
    );

    const switchBtn = screen.getByRole('switch', { name: /ai diffusion/i });
    expect(switchBtn.getAttribute('aria-checked')).toBe('false');

    fireEvent.click(switchBtn);
    expect(handleToggle).toHaveBeenCalledWith(true);
  });
});
