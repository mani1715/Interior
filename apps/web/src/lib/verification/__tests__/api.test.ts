import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from '../../api-client';
import { getStudioVerification, submitVerification } from '../api';

vi.mock('../../api-client', () => ({ apiFetch: vi.fn() }));

describe('verification response contract', () => {
  beforeEach(() => vi.clearAllMocks());
  it('loads the actual route and maps document metadata and decision fields', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ gstNumber: 'GST', decisionReason: 'More evidence needed',
      documents: [{ originalFilename: 'certificate.pdf', createdAt: '2026-10-23T00:00:00Z' }] });
    const result = await getStudioVerification();
    expect(apiFetch).toHaveBeenCalledWith('/verification/studio');
    expect(result.taxIdentifier).toBe('GST');
    expect(result.rejectionReason).toBe('More evidence needed');
    expect(result.documents[0].fileName).toBe('certificate.pdf');
    expect(result.documents[0].uploadedAt).toBe('2026-10-23T00:00:00Z');
  });
  it('submits required professional type and backend field names', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ documents: [] });
    await submitVerification({ businessName: 'Test', professionalType: 'INTERIOR_DESIGNER',
      taxIdentifier: 'GST', registeredAddress: 'Review notes', documentIds: [] });
    const [path, options] = vi.mocked(apiFetch).mock.calls[0];
    expect(path).toBe('/verification/studio/submit');
    expect(JSON.parse(options!.body as string)).toEqual({ businessName: 'Test',
      professionalType: 'INTERIOR_DESIGNER', gstNumber: 'GST', notes: 'Review notes' });
  });
});
