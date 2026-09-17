// Centralized mock data module for Phase 05 Homepage
// Contains zero remote dependencies, full CSP compliance with inline vector SVGs

export interface ProjectInspirationItem {
  id: string;
  title: string;
  category: string;
  location: string;
  designerName: string;
  studioName: string;
  image: string;
  viewCount: string;
  tags: string[];
}

export interface TransformationStage {
  id: number;
  name: string;
  title: string;
  shortDesc: string;
  fullDesc: string;
  specs: string[];
  svgContent: string;
}

// 7-Stage Architectural Progression SVGs
export const transformationStages: TransformationStage[] = [
  {
    id: 1,
    name: '01. Raw Site',
    title: 'Empty Masonry & Rough-in',
    shortDesc: 'Unfinished brickwork and conduit marks prior to carpentry intervention.',
    fullDesc: 'Raw concrete ceiling with exposed electrical conduits, unfinished plaster wall, and marked screed datum points.',
    specs: ['9ft 6in ceiling height', 'Exposed 25mm PVC conduits', 'Laser-leveled datum line'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <rect width="800" height="500" fill="%23242220"/>
      <rect x="60" y="40" width="680" height="420" fill="%23302c29" rx="4"/>
      <!-- Raw Brick Patterns -->
      <line x1="60" y1="120" x2="740" y2="120" stroke="%233d3733" stroke-width="2"/>
      <line x1="60" y1="200" x2="740" y2="200" stroke="%233d3733" stroke-width="2"/>
      <line x1="60" y1="280" x2="740" y2="280" stroke="%233d3733" stroke-width="2"/>
      <line x1="60" y1="360" x2="740" y2="360" stroke="%233d3733" stroke-width="2"/>
      <!-- Electrical conduit runs -->
      <path d="M 120 40 L 120 180 L 400 180" stroke="%23B88A5A" stroke-width="3" fill="none" opacity="0.6"/>
      <circle cx="400" cy="180" r="10" fill="%23B88A5A" opacity="0.6"/>
      <!-- Laser chalk line -->
      <line x1="60" y1="320" x2="740" y2="320" stroke="%23C76F4A" stroke-width="2" stroke-dasharray="6,6"/>
      <text x="400" y="240" fill="%238E653B" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">STAGE 1: RAW SITE CONDITION</text>
      <text x="400" y="270" fill="%236B6B6B" font-family="sans-serif" font-size="12" text-anchor="middle">Unfinished screed floor • Rough conduit layout</text>
    </svg>`,
  },
  {
    id: 2,
    name: '02. Carcass',
    title: 'Marine Ply Framework & Studs',
    shortDesc: 'Precision IS:710 boiling water resistant plywood carcass erection.',
    fullDesc: 'Structural framing anchored to masonry using chemical fasteners, pre-treated against termites with calcinated acoustic dampening.',
    specs: ['18mm BWP Marine Plywood', 'Calibrated aluminum grid', 'Internal wiring channels'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <rect width="800" height="500" fill="%23242220"/>
      <rect x="60" y="40" width="680" height="420" fill="%232b2724" rx="4"/>
      <!-- Wooden Framework Grid -->
      <rect x="140" y="90" width="520" height="320" fill="none" stroke="%23B88A5A" stroke-width="4" stroke-dasharray="8,8"/>
      <rect x="140" y="330" width="520" height="80" fill="%233a322b" stroke="%23B88A5A" stroke-width="2"/>
      <!-- Vertical carcass ribs -->
      <line x1="270" y1="90" x2="270" y2="410" stroke="%23B88A5A" stroke-width="2"/>
      <line x1="400" y1="90" x2="400" y2="410" stroke="%23B88A5A" stroke-width="2"/>
      <line x1="530" y1="90" x2="530" y2="410" stroke="%23B88A5A" stroke-width="2"/>
      <text x="400" y="220" fill="%23B88A5A" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">STAGE 2: BWP STRUCTURAL CARCASS</text>
      <text x="400" y="250" fill="%23FAF8F5" font-family="sans-serif" font-size="12" text-anchor="middle">18mm Marine Ply Box • Chemical Anchor Fasteners</text>
    </svg>`,
  },
  {
    id: 3,
    name: '03. Cabinetry',
    title: 'Precision Shutters & Paneling',
    shortDesc: 'Mounting of floating media credenza and base paneling.',
    fullDesc: 'Soft-close tandem drawers installed with 45kg load capacity. Wall panels aligned with 3mm shadow reveals.',
    specs: ['Heavy-duty concealed brackets', 'Push-to-open hardware', 'Shadow line reveals'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <rect width="800" height="500" fill="%23242220"/>
      <!-- Back Wall Paneling Substrate -->
      <rect x="120" y="70" width="560" height="320" fill="%233d342d" stroke="%2354473e" stroke-width="2" rx="4"/>
      <!-- Floating credenza boxes -->
      <rect x="100" y="340" width="600" height="70" fill="%234f4136" stroke="%23B88A5A" stroke-width="1.5" rx="3"/>
      <!-- Drawer Divisions -->
      <line x1="300" y1="340" x2="300" y2="410" stroke="%23242220" stroke-width="2"/>
      <line x1="500" y1="340" x2="500" y2="410" stroke="%23242220" stroke-width="2"/>
      <text x="400" y="210" fill="%23FAF8F5" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">STAGE 3: CABINETRY & SHUTTERS</text>
      <text x="400" y="240" fill="%23E7E1D8" font-family="sans-serif" font-size="12" text-anchor="middle">Floating console mounted • Soft-close runners calibrated</text>
    </svg>`,
  },
  {
    id: 4,
    name: '04. Materials',
    title: 'Statuario Marble & Walnut Veneer',
    shortDesc: 'Application of natural bookmatched stone and smoked walnut veneers.',
    fullDesc: 'Seamless bookmatched Italian Statuario composite with warm natural walnut wood grain panels flanking both sides.',
    specs: ['Italian Statuario Slab', 'Smoked American Walnut', 'Polyurethane seal'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <rect width="800" height="500" fill="%231a1918"/>
      <!-- Marble Center -->
      <rect x="230" y="70" width="340" height="270" fill="%23e8e4df" rx="4"/>
      <!-- Marble Veins -->
      <path d="M 250 90 Q 340 180 400 130 T 540 260" stroke="%23b8b0a5" stroke-width="3" fill="none" opacity="0.8"/>
      <path d="M 300 280 Q 420 220 530 300" stroke="%23c4bcb0" stroke-width="2" fill="none" opacity="0.6"/>
      <!-- Walnut Sides -->
      <rect x="100" y="70" width="130" height="270" fill="%235c3b24"/>
      <rect x="570" y="70" width="130" height="270" fill="%235c3b24"/>
      <!-- Floating Console in Walnut -->
      <rect x="100" y="340" width="600" height="70" fill="%23482d1b" rx="4"/>
      <text x="400" y="200" fill="%231F1F1F" font-family="serif" font-size="18" font-weight="bold" text-anchor="middle">STAGE 4: NATURAL MATERIALS</text>
      <text x="400" y="225" fill="%234A4A4A" font-family="sans-serif" font-size="12" text-anchor="middle">Bookmatched Statuario • Smoked Walnut Veneer</text>
    </svg>`,
  },
  {
    id: 5,
    name: '05. Fluting & Details',
    title: 'CNC Fluted Grooves & Champagne Brass',
    shortDesc: 'Precision micro-fluting and PVD coated brass trims.',
    fullDesc: 'Vertical CNC fluted slats with 12mm scallop rhythm, champagne brass profile inserts, and custom wire grommet concealment.',
    specs: ['12mm CNC Fluting', 'Champagne Brass PVD', 'Concealed cable raceways'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <rect width="800" height="500" fill="%231a1918"/>
      <!-- Marble Center -->
      <rect x="230" y="70" width="340" height="270" fill="%23e8e4df" rx="4"/>
      <path d="M 250 90 Q 340 180 400 130 T 540 260" stroke="%23b8b0a5" stroke-width="3" fill="none" opacity="0.8"/>
      <!-- Fluted Walnut Sides with vertical lines -->
      <rect x="100" y="70" width="130" height="270" fill="%235c3b24"/>
      <line x1="120" y1="70" x2="120" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="140" y1="70" x2="140" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="160" y1="70" x2="160" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="180" y1="70" x2="180" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="200" y1="70" x2="200" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <rect x="570" y="70" width="130" height="270" fill="%235c3b24"/>
      <line x1="590" y1="70" x2="590" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="610" y1="70" x2="610" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="630" y1="70" x2="630" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="650" y1="70" x2="650" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <line x1="670" y1="70" x2="670" y2="340" stroke="%23B88A5A" stroke-width="1.5"/>
      <!-- Brass Inlay Strip on Console -->
      <rect x="100" y="340" width="600" height="70" fill="%23482d1b" rx="4"/>
      <line x1="100" y1="342" x2="700" y2="342" stroke="%23B88A5A" stroke-width="3"/>
      <text x="400" y="190" fill="%231F1F1F" font-family="serif" font-size="16" font-weight="bold" text-anchor="middle">STAGE 5: FLUTING & BRASS DETAILS</text>
      <text x="400" y="215" fill="%234A4A4A" font-family="sans-serif" font-size="12" text-anchor="middle">12mm CNC Flutes • Champagne Brass PVD Trims</text>
    </svg>`,
  },
  {
    id: 6,
    name: '06. Lighting',
    title: '3000K Indirect Cove & Spot Cones',
    shortDesc: 'Ambient cove illumination and high-CRI accent downlights.',
    fullDesc: 'Indirect warm glow illuminating stone texture from behind the paneling with 3000K 95+ CRI architectural linear LED strips.',
    specs: ['3000K Warm White LED', 'CRI > 95 Architectural Grade', 'Flicker-free dimming'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <defs>
        <radialGradient id="coveGlow" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="%23ffd699" stop-opacity="0.8"/>
          <stop offset="100%" stop-color="%23ffd699" stop-opacity="0"/>
        </radialGradient>
      </defs>
      <rect width="800" height="500" fill="%23141312"/>
      <!-- Glow behind marble panel -->
      <rect x="210" y="50" width="380" height="310" fill="url(%23coveGlow)" opacity="0.6"/>
      <rect x="230" y="70" width="340" height="270" fill="%23e8e4df" rx="4"/>
      <!-- Fluted sides -->
      <rect x="100" y="70" width="130" height="270" fill="%235c3b24"/>
      <rect x="570" y="70" width="130" height="270" fill="%235c3b24"/>
      <!-- Spotlights on Ceiling -->
      <polygon points="180,0 120,280 240,280" fill="%23ffd699" opacity="0.25"/>
      <polygon points="620,0 560,280 680,280" fill="%23ffd699" opacity="0.25"/>
      <!-- Console -->
      <rect x="100" y="340" width="600" height="70" fill="%23482d1b" rx="4"/>
      <line x1="100" y1="342" x2="700" y2="342" stroke="%23ffd699" stroke-width="2" opacity="0.8"/>
      <text x="400" y="190" fill="%231F1F1F" font-family="serif" font-size="16" font-weight="bold" text-anchor="middle">STAGE 6: 3000K ARCHITECTURAL LIGHTING</text>
      <text x="400" y="215" fill="%234A4A4A" font-family="sans-serif" font-size="12" text-anchor="middle">High-CRI Warm Cove Strips • Indirect Perimeter Glow</text>
    </svg>`,
  },
  {
    id: 7,
    name: '07. Built Reality',
    title: 'Completed Handcrafted Interior Living',
    shortDesc: 'The finished space ready for handover with client styling.',
    fullDesc: 'Seamlessly executed TV lounge featuring 75-inch flush OLED display, integrated acoustic soundbar channel, and tailored decor accents.',
    specs: ['Turnkey On-Site Handover', 'Clean wire management', '10-Year structural guarantee'],
    svgContent: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 500" width="800" height="500">
      <defs>
        <linearGradient id="finWall" x1="0" y1="0" x2="0" y2="100%">
          <stop offset="0%" stop-color="%231a1918"/>
          <stop offset="100%" stop-color="%23100f0e"/>
        </linearGradient>
      </defs>
      <rect width="800" height="500" fill="url(%23finWall)"/>
      <!-- Cove Glow -->
      <rect x="210" y="50" width="380" height="310" fill="%23ffd699" opacity="0.15" rx="12"/>
      <!-- Marble Panel -->
      <rect x="230" y="70" width="340" height="270" fill="%23e8e4df" rx="4"/>
      <!-- OLED Display Mounted -->
      <rect x="270" y="100" width="260" height="150" fill="%23050505" stroke="%232b2b2b" stroke-width="3" rx="4"/>
      <circle cx="400" cy="175" r="30" fill="%23B88A5A" opacity="0.4"/>
      <!-- Fluted Walnut Sides -->
      <rect x="100" y="70" width="130" height="270" fill="%235c3b24"/>
      <rect x="570" y="70" width="130" height="270" fill="%235c3b24"/>
      <!-- Floating Console with Brass line -->
      <rect x="100" y="340" width="600" height="70" fill="%23482d1b" rx="4"/>
      <line x1="100" y1="342" x2="700" y2="342" stroke="%23B88A5A" stroke-width="3"/>
      <!-- Floor reflections -->
      <polygon points="0,440 800,440 800,500 0,500" fill="%23161413"/>
      <text x="400" y="475" fill="%23B88A5A" font-family="serif" font-size="18" font-weight="bold" text-anchor="middle">STAGE 7: COMPLETED BUILT REALITY</text>
    </svg>`,
  },
];

// Project Inspiration Gallery Items (India-specific context: Guntur, Amaravati, Hyderabad, Vijayawada, Bangalore)
export const inspirationProjects: ProjectInspirationItem[] = [
  {
    id: 'proj-1',
    title: 'Warm Walnut TV Unit & Fluted Paneling',
    category: 'TV Unit',
    location: 'Guntur, Andhra Pradesh',
    designerName: 'Vikram Sharma',
    studioName: 'Studio Elégance',
    image: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
      <rect width="600" height="450" fill="%231a1816"/>
      <rect x="40" y="40" width="520" height="370" fill="%232b2622" rx="12"/>
      <circle cx="300" cy="180" r="100" fill="%23B88A5A" opacity="0.35"/>
      <text x="300" y="240" fill="%23FAF8F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Modern TV Unit & Lounge</text>
      <text x="300" y="270" fill="%23B88A5A" font-family="sans-serif" font-size="14" text-anchor="middle">Guntur, Andhra Pradesh</text>
    </svg>`,
    viewCount: '2.8k',
    tags: ['Walnut Veneer', 'Statuario Marble', 'Cove Lighting'],
  },
  {
    id: 'proj-2',
    title: 'Minimalist Japandi Modular Kitchen with Island',
    category: 'Modular Kitchen',
    location: 'Amaravati Capital Region',
    designerName: 'Priya Rao',
    studioName: 'Atelier Earth',
    image: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
      <rect width="600" height="450" fill="%23161a18"/>
      <rect x="40" y="40" width="520" height="370" fill="%23222b26" rx="12"/>
      <circle cx="300" cy="180" r="100" fill="%232E5D4B" opacity="0.4"/>
      <text x="300" y="240" fill="%23FAF8F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Warm Oak Modular Kitchen</text>
      <text x="300" y="270" fill="%232E5D4B" font-family="sans-serif" font-size="14" text-anchor="middle">Amaravati Capital District</text>
    </svg>`,
    viewCount: '3.4k',
    tags: ['Quartz Countertop', 'Anti-Fingerprint Acrylic', 'Pantry Pull-out'],
  },
  {
    id: 'proj-3',
    title: 'Fluted Master Suite & Walk-in Wardrobe',
    category: 'Bedroom',
    location: 'Banjara Hills, Hyderabad',
    designerName: 'Arun Varma',
    studioName: 'Varma Design Studio',
    image: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
      <rect width="600" height="450" fill="%231d1618"/>
      <rect x="40" y="40" width="520" height="370" fill="%232f2227" rx="12"/>
      <circle cx="300" cy="180" r="100" fill="%23C76F4A" opacity="0.35"/>
      <text x="300" y="240" fill="%23FAF8F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Fluted Master Suite & Walk-in</text>
      <text x="300" y="270" fill="%23C76F4A" font-family="sans-serif" font-size="14" text-anchor="middle">Banjara Hills, Hyderabad</text>
    </svg>`,
    viewCount: '4.1k',
    tags: ['Fluted Glass', 'Internal LED Sensor', 'Linen Upholstery'],
  },
  {
    id: 'proj-4',
    title: 'Traditional Teak Wood Pooja Mandir Unit',
    category: 'Pooja Unit',
    location: 'Vijayawada, Andhra Pradesh',
    designerName: 'Suresh Babu',
    studioName: 'Kala Kuteer Woodworks',
    image: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
      <rect width="600" height="450" fill="%23241b14"/>
      <rect x="40" y="40" width="520" height="370" fill="%233b2a1c" rx="12"/>
      <circle cx="300" cy="180" r="90" fill="%23B88A5A" opacity="0.4"/>
      <text x="300" y="240" fill="%23FAF8F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Teak CNC Pooja Mandir</text>
      <text x="300" y="270" fill="%23B88A5A" font-family="sans-serif" font-size="14" text-anchor="middle">Vijayawada, Andhra Pradesh</text>
    </svg>`,
    viewCount: '1.9k',
    tags: ['Solid Burma Teak', 'Backlit Corian Jali', 'Brass Bells'],
  },
  {
    id: 'proj-5',
    title: 'Floor-to-Ceiling Lacquered Glass Wardrobe',
    category: 'Wardrobe',
    location: 'Hitec City, Hyderabad',
    designerName: 'Meera Nambiar',
    studioName: 'Spatial Architects',
    image: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
      <rect width="600" height="450" fill="%23171c21"/>
      <rect x="40" y="40" width="520" height="370" fill="%23222a33" rx="12"/>
      <circle cx="300" cy="180" r="90" fill="%233E6D8C" opacity="0.4"/>
      <text x="300" y="240" fill="%23FAF8F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Lacquered Glass Wardrobe</text>
      <text x="300" y="270" fill="%233E6D8C" font-family="sans-serif" font-size="14" text-anchor="middle">Hitec City, Hyderabad</text>
    </svg>`,
    viewCount: '3.1k',
    tags: ['Aluminum Profile', 'Tinted Mirror', 'Integrated Vanity'],
  },
  {
    id: 'proj-6',
    title: 'Executive Residence 4BHK Turnkey Living',
    category: 'Living Room',
    location: 'Indiranagar, Bangalore',
    designerName: 'Kavitha Hegde',
    studioName: 'Hegde & Associates',
    image: `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
      <rect width="600" height="450" fill="%231e1921"/>
      <rect x="40" y="40" width="520" height="370" fill="%232c2430" rx="12"/>
      <circle cx="300" cy="180" r="90" fill="%23E6C9C3" opacity="0.35"/>
      <text x="300" y="240" fill="%23FAF8F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Turnkey 4BHK Villa Living</text>
      <text x="300" y="270" fill="%23E6C9C3" font-family="sans-serif" font-size="14" text-anchor="middle">Indiranagar, Bangalore</text>
    </svg>`,
    viewCount: '5.2k',
    tags: ['Italian Marble Flooring', 'Curved Ceiling', 'Custom Settee'],
  },
];

export const professionalTypes = [
  {
    title: 'Interior Designers',
    description: 'Transform client lifestyle into bespoke spatial plans, palette specifications, and photorealistic concept studies.',
    tag: 'Design & Spatial Planning',
  },
  {
    title: 'Interior Design Studios',
    description: 'Multi-member studios managing commercial and residential pipelines with centralized project archives and team branding.',
    tag: 'Studio Teams',
  },
  {
    title: 'Architects',
    description: 'Structure-first practitioners presenting seamless transitions between exterior form, structural layout, and interior joinery.',
    tag: 'Architecture & Spaces',
  },
  {
    title: 'Custom Furniture Studios',
    description: 'Artisans building custom sofas, dining suites, accent chairs, and bespoke millwork with material transparency.',
    tag: 'Custom Millwork',
  },
  {
    title: 'Woodwork & Cabinetry',
    description: 'Carpentry experts and modular factories delivering flawless carcasses, shutter alignments, and hardware fittings.',
    tag: 'Modular & Carpentry',
  },
  {
    title: 'Turnkey Contractors',
    description: 'Single-window execution teams handling electrical, plumbing, masonry, false ceilings, and final interior handover.',
    tag: 'Full Execution',
  },
];

export const howItWorksSteps = [
  {
    step: '01',
    title: 'Create Your Studio Profile',
    desc: 'Claim your studio handle, upload your emblem, and establish your professional presence in your city.',
  },
  {
    step: '02',
    title: 'Publish Real Project Case Studies',
    desc: 'Document completed work with high-resolution photography, material specifications, and site location context.',
  },
  {
    step: '03',
    title: 'Build Your Search-Optimized Portfolio',
    desc: 'Each project automatically receives clean, search-friendly URLs, mobile schema, and watermark protection.',
  },
  {
    step: '04',
    title: 'Visualize Client Ideas with AI',
    desc: 'Upload on-site smartphone photos and explore design directions, finishes, and palette alternatives in seconds.',
  },
  {
    step: '05',
    title: 'Get Discovered by Homeowners',
    desc: 'Homeowners searching for specific rooms and styles in your locality discover your real finished work.',
  },
  {
    step: '06',
    title: 'Convert Inquiries into Projects',
    desc: 'Direct WhatsApp and structured enquiry links connect high-intent clients directly to your studio pipeline.',
  },
];
