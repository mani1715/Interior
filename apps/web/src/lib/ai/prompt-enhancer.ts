import { apiFetch } from '../api-client';

export interface PromptEnhanceContext {
  prompt: string;
  roomType?: string;
  editingMode?: 'FULL_IMAGE' | 'PRECISION_MASK';
  preserveStructure?: boolean;
  architecturalStyle?: string;
  referencePurposes?: string[];
}

export interface PromptEnhanceResult {
  originalPrompt: string;
  enhancedPrompt: string;
  detectedElements: string[];
  detectedMaterials: string[];
  detectedColors: string[];
  detectedHardware: string[];
  preservationDirectives: string;
  providerName: string;
}

const VOCAB_ELEMENTS: Record<string, string> = {
  wardrobe: 'modular wardrobe unit',
  shutter: 'cabinet shutters',
  cupboard: 'modular cupboards',
  cabinet: 'cabinetry frontages',
  countertop: 'countertop surface',
  counter: 'countertop surface',
  backsplash: 'kitchen backsplash',
  wall: 'architectural feature wall',
  floor: 'flooring surface',
  ceiling: 'ceiling soffit and cove',
  pooja: 'pooja unit mandir niche',
  'tv unit': 'living media console wall',
  'media wall': 'living media feature wall',
  island: 'kitchen island unit',
};

const VOCAB_MATERIALS: Record<string, string> = {
  walnut: 'natural walnut wood veneer with vertical grain',
  oak: 'fluted warm oak millwork',
  fluted: 'architectural vertical fluted profiles',
  marble: 'honed Italian Calacatta marble slab',
  granite: 'leathered granite finish',
  quartz: 'seamless engineered quartz',
  matte: 'soft-touch ultra-matte anti-fingerprint finish',
  lacquer: 'satin lacquer cabinetry finish',
  veneer: 'architectural natural timber veneer',
  laminate: 'high-pressure architectural laminate',
  cane: 'natural woven cane webbing insert',
  glass: 'fluted bronze-tinted tempered glass',
  linen: 'textured Belgian linen upholstery',
  terracotta: 'matte lime-washed terracotta plaster',
  acoustic: 'vertical slatted acoustic timber panels',
};

const VOCAB_COLORS: Record<string, string> = {
  white: 'warm alabaster white',
  black: 'deep matte charcoal black',
  grey: 'soft architectural grey',
  gray: 'soft architectural grey',
  beige: 'warm oat beige',
  cream: 'rich cream ivory',
  gold: 'brushed champagne gold',
  brass: 'brushed warm brass',
  bronze: 'aged antique bronze',
  burgundy: 'muted deep burgundy',
  sage: 'muted sage olive',
  terracotta: 'warm earthy terracotta',
};

const VOCAB_HARDWARE: Record<string, string> = {
  handle: 'architectural handle pulls',
  pull: 'pull handles',
  knob: 'minimal knurled knobs',
  profile: 'integrated J-pull recessed profile',
  'j-pull': 'concealed J-pull seamless edge',
  knurled: 'precision knurled cylindrical pulls',
};

function matchVocab(text: string, vocab: Record<string, string>): string[] {
  const matches = new Set<string>();
  const lower = text.toLowerCase();
  for (const [key, val] of Object.entries(vocab)) {
    const regex = new RegExp(`\\b${key}(?:s|es)?\\b`, 'i');
    if (regex.test(lower)) {
      matches.add(val);
    }
  }
  return Array.from(matches);
}

/**
 * Local deterministic prompt enhancement fallback
 */
export function enhancePromptLocally(ctx: PromptEnhanceContext): PromptEnhanceResult {
  const raw = (ctx.prompt || '').trim();
  const lower = raw.toLowerCase();

  const detectedElements = matchVocab(lower, VOCAB_ELEMENTS);
  const detectedMaterials = matchVocab(lower, VOCAB_MATERIALS);
  const detectedColors = matchVocab(lower, VOCAB_COLORS);
  const detectedHardware = matchVocab(lower, VOCAB_HARDWARE);

  const isPrecisionMask = ctx.editingMode === 'PRECISION_MASK';
  const preserveStructure = ctx.preserveStructure ?? true;

  const parts: string[] = [];

  if (isPrecisionMask) {
    parts.push('Targeted inpainting edit on selected masked area:');
  } else {
    parts.push('Photorealistic architectural interior transformation:');
  }

  if (ctx.roomType) {
    parts.push(`in ${ctx.roomType.toLowerCase()} space.`);
  }

  if (detectedElements.length > 0 || detectedMaterials.length > 0 || detectedColors.length > 0) {
    const elementStr = detectedElements.length > 0 ? detectedElements.join(' and ') : 'designated cabinetry surfaces';
    const finishComponents: string[] = [];
    if (detectedColors.length > 0) finishComponents.push(detectedColors.join(', '));
    if (detectedMaterials.length > 0) finishComponents.push(detectedMaterials.join(', '));
    if (detectedHardware.length > 0) finishComponents.push(`accented with ${detectedHardware.join(' and ')}`);

    const finishStr = finishComponents.length > 0 ? finishComponents.join(' paired with ') : 'refined architectural finishes';
    parts.push(`Update ${elementStr} to feature ${finishStr}.`);
  } else {
    parts.push(`${raw}${raw.endsWith('.') ? '' : '.'}`);
  }

  if (ctx.architecturalStyle) {
    parts.push(`Aesthetic direction adheres strictly to ${ctx.architecturalStyle} style principles.`);
  }

  const preservationClauses: string[] = [];
  if (isPrecisionMask) {
    preservationClauses.push('Surrounding unmasked walls, ceiling, flooring, and adjacent fixtures remain 100% untouched and geometrically identical');
  }
  if (preserveStructure) {
    preservationClauses.push('Architectural structural boundaries, door/window openings, electrical switchboards, and room perspective must remain strictly preserved without warping');
  }
  preservationClauses.push('Ensure authentic material texture, diffused ambient illumination, and photorealistic joinery reveals');

  const preservationDirectives = `${preservationClauses.join('; ')}.`;
  parts.push(preservationDirectives);

  return {
    originalPrompt: raw,
    enhancedPrompt: parts.join(' ').replace(/\s+/g, ' ').trim(),
    detectedElements,
    detectedMaterials,
    detectedColors,
    detectedHardware,
    preservationDirectives,
    providerName: 'DETERMINISTIC_ARCHITECTURAL_PARSER',
  };
}

/**
 * Call backend prompt enhancement API with automatic local fallback
 */
export async function enhancePrompt(ctx: PromptEnhanceContext): Promise<PromptEnhanceResult> {
  try {
    const res = await apiFetch<PromptEnhanceResult>('/ai/prompt/enhance', {
      method: 'POST',
      body: JSON.stringify(ctx),
    });
    if (res && res.enhancedPrompt) {
      return res;
    }
  } catch {
    // API offline or error -> seamless local fallback
  }
  return enhancePromptLocally(ctx);
}
