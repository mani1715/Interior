/** Conservative recognition of explicit upper/lower colour assignments, not image segmentation. */
export function regionalColours(prompt: string): { region: 'upper' | 'lower'; colour: string }[] {
  const instruction = prompt.replace(/\b(?:do not|don't|preserve|leave|keep)\b[^.;]*(?:[.;]|$)/gi, ' ');
  const assignments = new Map<'upper' | 'lower', string>();
  for (const match of instruction.matchAll(/\b(upper|top|lower|bottom|down)\b([^.;]*?)(?=\b(?:upper|top|lower|bottom|down)\b|[.;]|$)/gi)) {
    const colours = [...match[2].matchAll(/\b(white|black|yellow|blue|green|red|orange|pink|purple|grey|gray|beige|cream|brown|navy|sage)\b/gi)].map(m => m[1].toLowerCase());
    if (new Set(colours).size !== 1) continue;
    const region = /upper|top/i.test(match[1]) ? 'upper' : 'lower';
    if (assignments.has(region) && assignments.get(region) !== colours[0]) return [];
    assignments.set(region, colours[0]);
  }
  if (assignments.size !== 2 || assignments.get('upper') === assignments.get('lower')) return [];
  return ['upper', 'lower'].map(region => ({ region: region as 'upper' | 'lower', colour: assignments.get(region as 'upper' | 'lower')! }));
}
