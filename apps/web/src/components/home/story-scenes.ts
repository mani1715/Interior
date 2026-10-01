/** Approved user-supplied concept images, kept separate from live project listings. */
export const STORY_SCENES = [
  {
    key: 'wardrobe', category: 'Wardrobes', image: '/images/approved/wardrobe.png',
    title: 'Storage designed around the way you live.',
    body: 'Elegant wardrobes, thoughtful shelving, and refined finishes that bring order, warmth, and everyday ease into the bedroom.',
    cta: 'Explore wardrobes', href: '/categories/wardrobes', detail: '01 — BEDROOM STORAGE',
    alt: 'Full-height olive and cream bedroom cupboards beside illuminated shelving and a timber dressing niche',
    material: 'Full-height cupboards / Warm timber', focal: '64% 50%', mobileFocal: '70% 50%',
  },
  {
    key: 'kitchen', category: 'Kitchens', image: '/images/approved/kitchen.png',
    title: 'Crafted kitchens for modern living.',
    body: 'Beautifully planned cabinetry, seamless storage, and functional layouts designed to make daily life more effortless and refined.',
    cta: 'Explore kitchens', href: '/categories/modular-kitchens', detail: '02 — MODULAR KITCHENS',
    alt: 'Modular kitchen with cream upper cupboards, deep blue base cabinets, integrated ovens and illuminated stone worktops',
    material: 'Seamless cabinetry / Everyday function', focal: '53% 50%', mobileFocal: '52% 50%',
  },
  {
    key: 'hall', category: 'Hall interiors', image: '/images/approved/hall.png',
    title: 'Storage that becomes part of the design.',
    body: 'From display shelving to built-in cabinetry, create living spaces that feel open, organized, and beautifully composed.',
    cta: 'Explore hall interiors', href: '/categories/living-room', detail: '03 — HALL INTERIORS',
    alt: 'Hall display cabinetry with glass-front shelves, warm lighting and full-height storage beside a timber partition',
    material: 'Display shelving / Considered storage', focal: '67% 50%', mobileFocal: '74% 50%',
  },
  {
    key: 'tv-unit', category: 'TV units', image: '/images/approved/tv-unit.png',
    title: 'Living spaces crafted with balance and purpose.',
    body: 'Custom TV units, display details, and integrated storage solutions designed to bring calm, function, and character to the heart of the home.',
    cta: 'Explore TV units', href: '/categories/tv-units', detail: '04 — TV UNITS',
    alt: 'Floating charcoal TV cabinetry with stone wall panels, timber detailing and illuminated display niches',
    material: 'Floating storage / Architectural detail', focal: '65% 50%', mobileFocal: '73% 50%',
  },
  {
    key: 'feature', category: 'Custom interiors', image: '/images/approved/feature.png',
    title: 'Every corner can be crafted with intention.',
    body: 'Feature units, compact custom solutions, and thoughtful finishes that bring beauty, warmth, and identity into every part of the home.',
    cta: 'Explore custom interiors', href: '/categories/custom-furniture', detail: '05 — CUSTOM INTERIORS',
    alt: 'Warmly lit feature niche with fluted timber, built-in drawers and an integrated entry bench',
    material: 'Feature niches / Crafted corners', focal: '65% 50%', mobileFocal: '71% 50%',
  },
] as const;

export const STORY_DISTANCE = STORY_SCENES.length - 0.35;
export const clamp = (value: number, min = 0, max = 1) => Math.min(max, Math.max(min, value));
export const activeScene = (progress: number) => Math.min(STORY_SCENES.length - 1, Math.floor(clamp(progress) * STORY_DISTANCE + 0.175));

export function sceneMotion(progress: number, index: number) {
  const local = clamp(progress) * STORY_DISTANCE - index;
  const smooth = (value: number) => { const t = clamp(value); return t * t * (3 - 2 * t); };
  const enter = smooth((local + 0.35) / 0.35);
  const leave = index === STORY_SCENES.length - 1 ? 0 : smooth((local - 0.65) / 0.35);
  // Finish the old copy before the next headline enters, avoiding doubled type mid-scroll.
  const textEnter = smooth((local + 0.17) / 0.17);
  const textLeave = index === STORY_SCENES.length - 1 ? 0 : smooth((local - 0.65) / 0.16);
  return {
    local, opacity: enter * (1 - leave),
    textOpacity: textEnter * (1 - textLeave),
    // Incoming room masks over the outgoing plane rather than fading to cream.
    reveal: (1 - enter) * 100, visible: local >= -0.35 && local < 1 - 1e-6,
    x: (1 - enter) * 7 - leave * 4,
    rotation: (1 - enter) * -4 + leave * 3,
    depth: -65 * (1 - enter) - leave * 85,
    scale: 1.025 + clamp(local) * 0.035,
    foreground: clamp(local) * -16,
  };
}
