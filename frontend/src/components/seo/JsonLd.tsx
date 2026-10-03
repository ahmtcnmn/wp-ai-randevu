/**
 * Inline JSON-LD structured data komponenti.
 * Server component — herhangi bir page.tsx içinde kullanılabilir.
 */
export function JsonLd({ data }: { data: object }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }}
    />
  );
}
