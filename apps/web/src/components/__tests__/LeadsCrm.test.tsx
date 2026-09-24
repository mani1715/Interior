import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { EnquirySheet } from '@/components/discovery/EnquirySheet';
import LeadsWorkspacePage from '@/app/workspace/leads/page';
import * as leadsApi from '@/lib/leads/api';
import { Project } from '@/lib/discovery/types';

vi.mock('@/lib/leads/api', () => ({
  submitPublicLead: vi.fn(),
  initiatePublicWhatsAppHandoff: vi.fn(),
  fetchLeads: vi.fn(),
  fetchLeadCounts: vi.fn(),
  fetchLeadDetail: vi.fn(),
  updateLead: vi.fn(),
  addLeadNote: vi.fn(),
  archiveLead: vi.fn(),
  fetchWhatsAppStatus: vi.fn(),
  sendWhatsAppMessage: vi.fn(),
}));

const mockProject: Project = {
  id: 'proj-001',
  slug: 'contemporary-villa',
  title: 'Contemporary Villa',
  category: 'tv-units',
  categoryName: 'TV Units',
  location: 'guntur',
  locationName: 'Guntur, Andhra Pradesh',
  style: 'contemporary',
  styleName: 'Contemporary',
  budgetRange: '3l-5l',
  budgetLabel: '₹4.5 Lakhs',
  propertyType: 'villa',
  propertyTypeName: 'Villa',
  professionalType: 'interior-studio',
  professionalTypeName: 'Interior Studio',
  professionalSlug: 'studio-design',
  professionalName: 'Rajesh Varma',
  studioName: 'Studio Design',
  description: 'A luxurious villa project',
  scope: 'Full interior makeover',
  coverImage: '/images/test.jpg',
  gallery: [],
  materials: ['Italian Marble', 'Fluted Teak'],
  completionYear: 2024,
  duration: '6 weeks',
  services: ['Turnkey Interior'],
  createdAt: '2026-01-01T00:00:00Z',
};

describe('Phase 26 — Public Enquiry & WhatsApp Handoff Flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits a public inquiry successfully and displays generated reference number', async () => {
    vi.mocked(leadsApi.submitPublicLead).mockResolvedValueOnce({
      referenceNumber: 'INQ-AB12CD34',
      message: 'Inquiry received successfully.',
      studioName: 'Studio Design',
    });

    render(<EnquirySheet isOpen={true} onClose={vi.fn()} project={mockProject} />);

    // Fill form
    fireEvent.change(screen.getByLabelText(/your name/i), {
      target: { value: 'Suresh Raina' },
    });
    fireEvent.change(screen.getByLabelText(/phone \/ whatsapp/i), {
      target: { value: '+919876543210' },
    });

    // Submit
    const submitBtn = screen.getByRole('button', { name: /send inquiry/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(leadsApi.submitPublicLead).toHaveBeenCalledWith(
        expect.objectContaining({
          targetStudioSlug: 'studio-design',
          targetProjectSlug: 'contemporary-villa',
          name: 'Suresh Raina',
          phone: '+919876543210',
          consentContact: true,
        })
      );
    });

    // Displays reference number truthfully
    expect(await screen.findByText(/INQ-AB12CD34/i)).toBeDefined();
    expect(screen.getByText(/Inquiry Dispatched Successfully/i)).toBeDefined();
  });

  it('initiates user-initiated WhatsApp handoff without claiming delivery', async () => {
    vi.mocked(leadsApi.initiatePublicWhatsAppHandoff).mockResolvedValueOnce({
      whatsappUrl: 'https://wa.me/919876543210?text=Hello',
      studioName: 'Studio Design',
      studioPhoneMasked: '+91 ••••• •3210',
      prefilledMessage: 'Hello',
    });

    render(<EnquirySheet isOpen={true} onClose={vi.fn()} project={mockProject} />);

    // Switch to WhatsApp tab
    const waTab = screen.getByRole('button', { name: /chat on whatsapp/i });
    fireEvent.click(waTab);

    expect(screen.getByText(/Direct WhatsApp Chat/i)).toBeDefined();

    // Click launch WhatsApp
    const waBtn = screen.getByRole('button', { name: /open whatsapp chat/i });
    fireEvent.click(waBtn);

    await waitFor(() => {
      expect(leadsApi.initiatePublicWhatsAppHandoff).toHaveBeenCalledWith(
        expect.objectContaining({
          targetStudioSlug: 'studio-design',
        })
      );
    });

    expect(await screen.findByText(/WhatsApp Chat Window Opened/i)).toBeDefined();
  });
});

describe('Phase 26 — Workspace Leads CRM Page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders pipeline metrics, search input, and lead list', async () => {
    vi.mocked(leadsApi.fetchLeadCounts).mockResolvedValueOnce({
      total: 12,
      newLeads: 4,
      active: 5,
      won: 2,
      lost: 1,
      archived: 0,
    });

    vi.mocked(leadsApi.fetchLeads).mockResolvedValueOnce({
      items: [
        {
          id: 'lead-1',
          studioId: 'studio-1',
          projectId: 'proj-1',
          projectTitle: 'Modern 3BHK Flat',
          projectSlug: 'modern-3bhk',
          source: 'PUBLIC_PROJECT',
          sourceDisplayName: 'Project Inquiry',
          status: 'NEW',
          statusDisplayName: 'New Inquiry',
          name: 'Anil Ambani',
          phoneMasked: '+91 ••••• •1234',
          phoneNormalized: '+919876541234',
          emailNormalized: 'anil@example.com',
          city: 'Guntur',
          projectCategory: 'Residential',
          budgetRange: '25L-50L',
          preferredContactChannel: 'WHATSAPP',
          hasWhatsappConsent: true,
          assignedUserId: null,
          assignedUserName: null,
          nextFollowUpAt: null,
          possibleDuplicate: false,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          version: 1,
          archivedAt: null,
        },
      ],
      total: 1,
      limit: 50,
      offset: 0,
      hasMore: false,
    });

    render(<LeadsWorkspacePage />);

    // Metric counts rendered
    expect(await screen.findByText('12')).toBeDefined();
    expect(screen.getByText('4')).toBeDefined();

    // Lead item rendered
    expect(await screen.findByText('Anil Ambani')).toBeDefined();
    expect(screen.getByText('+91 ••••• •1234')).toBeDefined();
    expect(screen.getByText(/New Inquiry/i)).toBeDefined();
    expect(screen.getByText(/WhatsApp Opt-in/i)).toBeDefined();
  });
});
