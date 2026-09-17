// High quality architectural SVG data URIs for self-contained, zero-network, CSP-compliant rendering

export const rawSiteSvg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600">
  <rect width="800" height="600" fill="%23262626"/>
  <!-- Exposed concrete ceiling -->
  <rect width="800" height="160" fill="%233a3a3a"/>
  <!-- Conduit pipes -->
  <line x1="50" y1="60" x2="750" y2="60" stroke="%23555555" stroke-width="6"/>
  <line x1="120" y1="60" x2="120" y2="160" stroke="%23555555" stroke-width="4"/>
  <line x1="450" y1="60" x2="450" y2="220" stroke="%23555555" stroke-width="4"/>
  <!-- Raw Brick Wall Left -->
  <rect x="0" y="160" width="220" height="440" fill="%2359382e"/>
  <line x1="0" y1="210" x2="220" y2="210" stroke="%233a251e" stroke-width="3"/>
  <line x1="0" y1="260" x2="220" y2="260" stroke="%233a251e" stroke-width="3"/>
  <line x1="0" y1="310" x2="220" y2="310" stroke="%233a251e" stroke-width="3"/>
  <line x1="0" y1="360" x2="220" y2="360" stroke="%233a251e" stroke-width="3"/>
  <!-- Unfinished Floor with construction marks -->
  <polygon points="0,480 800,480 800,600 0,600" fill="%232d2d2d"/>
  <line x1="100" y1="520" x2="300" y2="520" stroke="%23d97706" stroke-width="4" stroke-dasharray="10,10"/>
  <!-- Text mark -->
  <text x="400" y="320" fill="%23666666" font-family="sans-serif" font-size="28" font-weight="bold" text-anchor="middle">RAW SITE CONDITION</text>
  <text x="400" y="360" fill="%23888888" font-family="sans-serif" font-size="16" text-anchor="middle">Unfinished screed floor • Exposed electrical rough-in</text>
</svg>`;

export const aiConceptSvg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600">
  <defs>
    <linearGradient id="wallGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="%231a1a24"/>
      <stop offset="100%" stop-color="%232b2b3d"/>
    </linearGradient>
    <linearGradient id="coveGlow" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="%23B88A5A" stop-opacity="0.8"/>
      <stop offset="100%" stop-color="%23B88A5A" stop-opacity="0"/>
    </linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(%23wallGrad)"/>
  <!-- Cove lighting ceiling -->
  <rect x="60" y="40" width="680" height="40" fill="%2314141c" rx="4"/>
  <rect x="60" y="80" width="680" height="120" fill="url(%23coveGlow)"/>
  <!-- Fluted TV backpanel AI Wireframe -->
  <rect x="180" y="140" width="440" height="280" fill="%2338271d" rx="6"/>
  <!-- Wireframe lines -->
  <line x1="220" y1="140" x2="220" y2="420" stroke="%23B88A5A" stroke-width="1.5" stroke-dasharray="4,4"/>
  <line x1="260" y1="140" x2="260" y2="420" stroke="%23B88A5A" stroke-width="1.5" stroke-dasharray="4,4"/>
  <line x1="300" y1="140" x2="300" y2="420" stroke="%23B88A5A" stroke-width="1.5" stroke-dasharray="4,4"/>
  <line x1="500" y1="140" x2="500" y2="420" stroke="%23B88A5A" stroke-width="1.5" stroke-dasharray="4,4"/>
  <line x1="540" y1="140" x2="540" y2="420" stroke="%23B88A5A" stroke-width="1.5" stroke-dasharray="4,4"/>
  <line x1="580" y1="140" x2="580" y2="420" stroke="%23B88A5A" stroke-width="1.5" stroke-dasharray="4,4"/>
  <!-- Floating Media Unit -->
  <rect x="140" y="420" width="520" height="60" fill="%231c1714" stroke="%23B88A5A" stroke-width="2" rx="4"/>
  <!-- AI Mesh Grid overlay -->
  <circle cx="400" cy="280" r="160" fill="none" stroke="%2360a5fa" stroke-width="1.5" stroke-dasharray="5,5" opacity="0.4"/>
  <text x="400" y="270" fill="%23e2e8f0" font-family="serif" font-size="24" font-weight="bold" text-anchor="middle">AI CONCEPT VISUALIZATION</text>
  <text x="400" y="300" fill="%2394a3b8" font-family="sans-serif" font-size="14" text-anchor="middle">Walnut fluting + 3000K warm cove ambient simulation</text>
</svg>`;

export const executedSpaceSvg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="800" height="600">
  <defs>
    <linearGradient id="realWall" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="%23242220"/>
      <stop offset="100%" stop-color="%23141312"/>
    </linearGradient>
    <linearGradient id="realWood" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="%236e472a"/>
      <stop offset="50%" stop-color="%23855734"/>
      <stop offset="100%" stop-color="%23573820"/>
    </linearGradient>
    <linearGradient id="warmSpot" x1="50%" y1="0%" x2="50%" y2="100%">
      <stop offset="0%" stop-color="%23ffd699" stop-opacity="0.9"/>
      <stop offset="100%" stop-color="%23ffd699" stop-opacity="0"/>
    </linearGradient>
  </defs>
  <rect width="800" height="600" fill="url(%23realWall)"/>
  <!-- False ceiling with warm spot cones -->
  <rect width="800" height="60" fill="%231a1918"/>
  <polygon points="200,60 120,400 280,400" fill="url(%23warmSpot)" opacity="0.3"/>
  <polygon points="600,60 520,400 680,400" fill="url(%23warmSpot)" opacity="0.3"/>
  <!-- Italian Marble TV Accent Wall -->
  <rect x="160" y="100" width="480" height="340" fill="%23e8e4df" rx="8"/>
  <!-- Marble Veins -->
  <path d="M 180 120 Q 300 220 380 180 T 520 320" fill="none" stroke="%23b8b0a5" stroke-width="3" opacity="0.7"/>
  <path d="M 240 380 Q 400 280 600 360" fill="none" stroke="%23c4bcb0" stroke-width="2" opacity="0.6"/>
  <!-- Fluted Walnut Panels flanking marble -->
  <rect x="80" y="100" width="80" height="340" fill="url(%23realWood)"/>
  <rect x="640" y="100" width="80" height="340" fill="url(%23realWood)"/>
  <!-- OLED Display on Marble -->
  <rect x="220" y="140" width="360" height="200" fill="%230a0a0a" rx="4" stroke="%23333333" stroke-width="4"/>
  <!-- Floating Walnut Console -->
  <rect x="120" y="440" width="560" height="50" fill="url(%23realWood)" rx="4" filter="drop-shadow(0 10px 15px rgba(0,0,0,0.5))"/>
  <!-- Marble Floor with reflections -->
  <polygon points="0,520 800,520 800,600 0,600" fill="%231e1c1b"/>
  <text x="400" y="565" fill="%23B88A5A" font-family="serif" font-size="20" font-weight="bold" text-anchor="middle">HANDCRAFTED BUILT REALITY</text>
  <text x="400" y="585" fill="%23a8a29e" font-family="sans-serif" font-size="12" text-anchor="middle">Completed On-Site • Statuario Marble & Natural Walnut</text>
</svg>`;

export const projectImage1Svg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
  <rect width="600" height="450" fill="%231a1816"/>
  <rect x="40" y="40" width="520" height="370" fill="%232b2622" rx="12"/>
  <circle cx="300" cy="180" r="100" fill="%23B88A5A" opacity="0.35"/>
  <text x="300" y="240" fill="%23FAF9F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Modern TV Unit & Lounge</text>
  <text x="300" y="270" fill="%23B88A5A" font-family="sans-serif" font-size="14" text-anchor="middle">Guntur, Andhra Pradesh</text>
</svg>`;

export const projectImage2Svg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
  <rect width="600" height="450" fill="%23161a18"/>
  <rect x="40" y="40" width="520" height="370" fill="%23222b26" rx="12"/>
  <circle cx="300" cy="180" r="100" fill="%234d7c67" opacity="0.4"/>
  <text x="300" y="240" fill="%23FAF9F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Warm Oak Modular Kitchen</text>
  <text x="300" y="270" fill="%237cb398" font-family="sans-serif" font-size="14" text-anchor="middle">Amaravati Capital District</text>
</svg>`;

export const projectImage3Svg = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="600" height="450">
  <rect width="600" height="450" fill="%231d1618"/>
  <rect x="40" y="40" width="520" height="370" fill="%232f2227" rx="12"/>
  <circle cx="300" cy="180" r="100" fill="%23c2614b" opacity="0.35"/>
  <text x="300" y="240" fill="%23FAF9F5" font-family="serif" font-size="22" font-weight="bold" text-anchor="middle">Fluted Master Suite & Walk-in</text>
  <text x="300" y="270" fill="%23d68370" font-family="sans-serif" font-size="14" text-anchor="middle">Banjara Hills, Hyderabad</text>
</svg>`;
