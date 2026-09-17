import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Dialog } from '../overlay/Dialog';

describe('Dialog Component', () => {
  it('does not render when isOpen is false', () => {
    render(
      <Dialog isOpen={false} onClose={() => {}} title="Test Dialog">
        <p>Dialog Content</p>
      </Dialog>
    );

    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('renders modal with role="dialog" and aria-modal="true" when isOpen is true', () => {
    render(
      <Dialog isOpen={true} onClose={() => {}} title="Test Dialog" description="Test Description">
        <p>Dialog Content</p>
      </Dialog>
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toBeDefined();
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(screen.getByText('Test Dialog')).toBeDefined();
    expect(screen.getByText('Dialog Content')).toBeDefined();
  });

  it('triggers onClose when close button is clicked', () => {
    const handleClose = vi.fn();
    render(
      <Dialog isOpen={true} onClose={handleClose} title="Test Dialog">
        <p>Dialog Content</p>
      </Dialog>
    );

    const closeBtn = screen.getByRole('button', { name: /close dialog/i });
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('triggers onClose when Escape key is pressed', () => {
    const handleClose = vi.fn();
    render(
      <Dialog isOpen={true} onClose={handleClose} title="Test Dialog">
        <p>Dialog Content</p>
      </Dialog>
    );

    fireEvent.keyDown(window, { key: 'Escape' });
    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});
