import { Metadata } from 'next';
import { PublicProjectDetailDto, PublicStudioDto } from './types';

const SITE_NAME = 'Interior';
const BASE_URL = process.env.NEXT_PUBLIC_APP_URL || 'https://interior.com';

export function buildStudioMetadata(studio: PublicStudioDto): Metadata {
  const title = studio.metaTitle || `${studio.name} | Interior Design & Architecture`;
  const description =
    studio.metaDescription ||
    `${studio.name} - Architectural and interior design portfolio based in ${studio.city || 'India'}. Discover curated spaces and bespoke projects.`;

  const canonical = studio.canonicalUrl.startsWith('http')
    ? studio.canonicalUrl
    : `${BASE_URL}${studio.canonicalUrl}`;

  // Find cover image from projects if available
  const coverImage = studio.projects.find((p) => p.coverImageUrl)?.coverImageUrl;

  return {
    title,
    description,
    alternates: {
      canonical,
    },
    robots: studio.indexingEnabled
      ? { index: true, follow: true }
      : { index: false, follow: false },
    openGraph: {
      title,
      description,
      url: canonical,
      siteName: SITE_NAME,
      type: 'profile',
      images: coverImage ? [{ url: coverImage, alt: studio.name }] : [],
    },
    twitter: {
      card: 'summary_large_image',
      title,
      description,
      images: coverImage ? [coverImage] : [],
    },
  };
}

export function buildProjectMetadata(project: PublicProjectDetailDto): Metadata {
  const title = project.metaTitle || `${project.title} | ${project.studio.name}`;
  const description =
    project.metaDescription ||
    project.shortDescription ||
    `Interior design project ${project.title} by ${project.studio.name} in ${project.city || 'India'}.`;

  const canonical = project.canonicalUrl.startsWith('http')
    ? project.canonicalUrl
    : `${BASE_URL}${project.canonicalUrl}`;

  const coverMedia = project.media.find((m) => m.isCover) || project.media[0];
  const coverImageUrl = coverMedia?.largeUrl || coverMedia?.mediumUrl;

  return {
    title,
    description,
    alternates: {
      canonical,
    },
    robots: {
      index: true,
      follow: true,
    },
    openGraph: {
      title,
      description,
      url: canonical,
      siteName: SITE_NAME,
      type: 'article',
      images: coverImageUrl ? [{ url: coverImageUrl, alt: coverMedia?.altText || project.title }] : [],
    },
    twitter: {
      card: 'summary_large_image',
      title,
      description,
      images: coverImageUrl ? [coverImageUrl] : [],
    },
  };
}

export function buildCategoryMetadata(categoryName: string, categorySlug: string): Metadata {
  const title = `${categoryName} Interior Design Projects & Ideas | ${SITE_NAME}`;
  const description = `Explore curated ${categoryName.toLowerCase()} interior design projects crafted by verified architectural studios.`;
  const canonical = `${BASE_URL}/categories/${categorySlug}`;

  return {
    title,
    description,
    alternates: { canonical },
    robots: { index: true, follow: true },
    openGraph: {
      title,
      description,
      url: canonical,
      siteName: SITE_NAME,
    },
    twitter: {
      card: 'summary',
      title,
      description,
    },
  };
}

export function buildLocationMetadata(city: string): Metadata {
  const title = `Interior Designers & Architects in ${city} | ${SITE_NAME}`;
  const description = `Discover verified interior design studios, architects, and bespoke residential projects based in ${city}.`;
  const canonical = `${BASE_URL}/locations/${city.toLowerCase().replace(/\s+/g, '-')}`;

  return {
    title,
    description,
    alternates: { canonical },
    robots: { index: true, follow: true },
    openGraph: {
      title,
      description,
      url: canonical,
      siteName: SITE_NAME,
    },
    twitter: {
      card: 'summary',
      title,
      description,
    },
  };
}
