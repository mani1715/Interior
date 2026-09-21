/* eslint-disable @next/next/no-img-element -- render only supplied media; no invented optimization service. */
export function ComparisonMedia({ pairs, ai, mediaUrl, text }: {
  pairs: Record<string, unknown>[]; ai: boolean;
  mediaUrl: (value: unknown) => string | undefined;
  text: (value: unknown) => string;
}) {
  return <div data-comparisons>{pairs.filter(pair => ai
    ? mediaUrl(pair.conceptImageUrl) && mediaUrl(pair.realityImageUrl)
    : mediaUrl(pair.beforeImageUrl) && mediaUrl(pair.afterImageUrl)).map((pair, index) => (
    <article key={index}>
      {text(pair.title) && <h3>{text(pair.title)}</h3>}
      <div data-media-sequence>{(ai ? [
        ...(mediaUrl(pair.beforeImageUrl) ? [{ src: pair.beforeImageUrl, label: 'Before' }] : []),
        { src: pair.conceptImageUrl, label: 'AI Concept Visualization' },
        { src: pair.realityImageUrl, label: 'Real Result' },
      ] : [{ src: pair.beforeImageUrl, label: 'Before' }, { src: pair.afterImageUrl, label: 'After' }]).map(item => (
        <figure key={item.label}>
          <img src={mediaUrl(item.src)} alt={`${item.label}${text(pair.title) ? ` — ${text(pair.title)}` : ''}`} width={960} height={720} loading="lazy" decoding="async" />
          <figcaption>{item.label}</figcaption>
        </figure>
      ))}</div>
    </article>
  ))}</div>;
}
