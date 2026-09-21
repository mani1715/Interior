import React from 'react';
import { PublicProjectDetailDto, PublicStudioDto } from './types';

export function serializeJsonLd(data: unknown): string {
  return JSON.stringify(data)
    .replace(/</g, '\\u003c')
    .replace(/>/g, '\\u003e')
    .replace(/&/g, '\\u0026');
}

export function SafeJsonLd({ data }: { data: unknown }) {
  const safeJson = serializeJsonLd(data);
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: safeJson }}
    />
  );
}

export function buildStudioJsonLd(studio: PublicStudioDto, origin: string) {
  const phoneContact = studio.contacts.find((c) => c.channelType === 'PHONE');
  const emailContact = studio.contacts.find((c) => c.channelType === 'EMAIL');

  const coverImage = studio.projects.find((p) => p.coverImageUrl)?.coverImageUrl;

  const fullUrl = studio.canonicalUrl.startsWith('http')
    ? studio.canonicalUrl
    : `${origin}${studio.canonicalUrl}`;

  const jsonLd: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'ProfessionalService',
    name: studio.name,
    description: studio.metaDescription,
    url: fullUrl,
  };

  if (coverImage) {
    jsonLd.image = coverImage;
  }

  if (phoneContact) {
    jsonLd.telephone = phoneContact.contactValue;
  }

  if (emailContact) {
    jsonLd.email = emailContact.contactValue;
  }

  if (studio.city || studio.state || studio.country) {
    jsonLd.address = {
      '@type': 'PostalAddress',
      addressLocality: studio.city || undefined,
      addressRegion: studio.state || undefined,
      addressCountry: studio.country || 'IN',
    };
  }

  if (studio.serviceAreas && studio.serviceAreas.length > 0) {
    jsonLd.areaServed = studio.serviceAreas;
  }

  if (studio.specialties && studio.specialties.length > 0) {
    jsonLd.knowsAbout = studio.specialties;
  }

  return jsonLd;
}

export function buildProjectJsonLd(project: PublicProjectDetailDto, origin: string) {
  const fullUrl = project.canonicalUrl.startsWith('http')
    ? project.canonicalUrl
    : `${origin}${project.canonicalUrl}`;

  const studioUrl = project.studio.slug
    ? `${origin}/professionals/${project.studio.slug}`
    : origin;

  const coverMedia = project.media.find((m) => m.isCover) || project.media[0];
  const images = project.media
    .filter((m) => m.largeUrl || m.mediumUrl)
    .map((m) => m.largeUrl || m.mediumUrl);

  const jsonLd: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'CreativeWork',
    name: project.title,
    headline: project.metaTitle,
    description: project.metaDescription,
    url: fullUrl,
    creator: {
      '@type': 'Organization',
      name: project.studio.name,
      url: studioUrl,
    },
  };

  if (images.length > 0) {
    jsonLd.image = images;
  }

  if (project.completionYear) {
    jsonLd.dateCreated = `${project.completionYear}`;
  }

  if (project.city || project.state) {
    jsonLd.contentLocation = {
      '@type': 'Place',
      name: [project.city, project.state].filter(Boolean).join(', '),
    };
  }

  return jsonLd;
}

export function buildBreadcrumbJsonLd(
  items: Array<{ name: string; url: string }>,
  origin: string
) {
  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => {
      const fullUrl = item.url.startsWith('http') ? item.url : `${origin}${item.url}`;
      return {
        '@type': 'ListItem',
        position: index + 1,
        name: item.name,
        item: fullUrl,
      };
    }),
  };
}
