import type { Metadata } from "next";
import { ReactNode } from "react";
import { JsonLd } from "@/components/seo/JsonLd";
import { BRAND_NAME, SITE_URL } from "@/lib/constants";

export const metadata: Metadata = {
  title: "Fiyatlandırma — Aylık Planlar",
  description:
    "AppointFlow planları — Starter, Growth, Enterprise. Her plan 14 gün ücretsiz denenir. Berberler için en uygun fiyat.",
  alternates: { canonical: "/fiyatlandirma" },
  openGraph: {
    title: "AppointFlow Fiyatlandırma",
    description: "Berber/kuaför işletmeniz için aylık 199 TL'den başlayan planlar. 14 gün ücretsiz dene.",
    url: "/fiyatlandirma",
    type: "website",
  },
};

const productJsonLd = {
  "@context": "https://schema.org",
  "@type": "Product",
  name: BRAND_NAME,
  description: "Berberler ve uzmanlar için randevu yönetim platformu",
  url: `${SITE_URL}/fiyatlandirma`,
  brand: { "@type": "Brand", name: BRAND_NAME },
  offers: [
    {
      "@type": "Offer",
      name: "Starter",
      price: "199",
      priceCurrency: "TRY",
      availability: "https://schema.org/InStock",
      url: `${SITE_URL}/register`,
      priceSpecification: {
        "@type": "UnitPriceSpecification",
        price: "199",
        priceCurrency: "TRY",
        billingDuration: "P1M",
      },
    },
    {
      "@type": "Offer",
      name: "Growth",
      price: "499",
      priceCurrency: "TRY",
      availability: "https://schema.org/InStock",
      url: `${SITE_URL}/register`,
    },
    {
      "@type": "Offer",
      name: "Enterprise",
      price: "999",
      priceCurrency: "TRY",
      availability: "https://schema.org/InStock",
      url: `${SITE_URL}/register`,
    },
  ],
};

export default function FiyatlandirmaLayout({ children }: { children: ReactNode }) {
  return (
    <>
      <JsonLd data={productJsonLd} />
      {children}
    </>
  );
}
