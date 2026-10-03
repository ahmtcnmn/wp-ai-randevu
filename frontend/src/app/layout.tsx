import type { Metadata, Viewport } from "next";
import { Geist } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/store/AuthContext";
import { ToastProvider } from "@/store/ToastContext";
import { SectorProvider } from "@/store/SectorContext";
import { SubscriptionProvider } from "@/store/SubscriptionContext";
import QuotaExceededBanner from "@/components/common/QuotaExceededBanner";
import { BRAND_DESCRIPTION, BRAND_NAME, BRAND_TAGLINE, SITE_URL } from "@/lib/constants";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#ffffff" },
    { media: "(prefers-color-scheme: dark)", color: "#0b1220" },
  ],
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
};

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: {
    default: `${BRAND_NAME} — ${BRAND_TAGLINE}`,
    template: `%s · ${BRAND_NAME}`,
  },
  description: BRAND_DESCRIPTION,
  applicationName: BRAND_NAME,
  authors: [{ name: BRAND_NAME, url: SITE_URL }],
  creator: BRAND_NAME,
  publisher: BRAND_NAME,
  generator: "Next.js",
  keywords: [
    "berber randevu", "kuaför randevu", "online randevu", "randevu yönetim sistemi",
    "salon yönetim yazılımı", "WhatsApp randevu", "AI asistan", "uzman randevu",
    "berber programı", "berber yazılımı", "randevu uygulaması", "müşteri yönetimi",
    "appointflow", "tıraş randevu", "saç kesim randevu",
  ],
  category: "business",
  classification: "Business Software",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  // icons / manifest / opengraph-image: Next.js otomatik olarak app/ klasöründeki
  // icon.svg, apple-icon.svg, manifest.ts, opengraph-image.svg dosyalarını bulur ve servisler.
  alternates: {
    canonical: "/",
    languages: { "tr-TR": "/" },
  },
  openGraph: {
    type: "website",
    locale: "tr_TR",
    url: SITE_URL,
    siteName: BRAND_NAME,
    title: `${BRAND_NAME} — ${BRAND_TAGLINE}`,
    description: BRAND_DESCRIPTION,
  },
  twitter: {
    card: "summary_large_image",
    title: `${BRAND_NAME} — ${BRAND_TAGLINE}`,
    description: BRAND_DESCRIPTION,
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-snippet": -1,
      "max-image-preview": "large",
      "max-video-preview": -1,
    },
  },
  verification: {
    // Eklendiğinde aktif et:
    // google: "google-site-verification-token",
    // yandex: "yandex-verification-token",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  // Organization + SoftwareApplication JSON-LD
  const jsonLd = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "Organization",
        "@id": `${SITE_URL}/#org`,
        name: BRAND_NAME,
        url: SITE_URL,
        logo: `${SITE_URL}/icon.svg`,
        sameAs: [],
        contactPoint: {
          "@type": "ContactPoint",
          email: "info@ehasoftware.com",
          contactType: "customer support",
          availableLanguage: ["Turkish"],
        },
      },
      {
        "@type": "SoftwareApplication",
        name: BRAND_NAME,
        operatingSystem: "Web, iOS, Android",
        applicationCategory: "BusinessApplication",
        offers: {
          "@type": "Offer",
          price: "199",
          priceCurrency: "TRY",
        },
        aggregateRating: {
          "@type": "AggregateRating",
          ratingValue: "4.8",
          ratingCount: "200",
        },
        description: BRAND_DESCRIPTION,
      },
      {
        "@type": "WebSite",
        "@id": `${SITE_URL}/#website`,
        url: SITE_URL,
        name: BRAND_NAME,
        publisher: { "@id": `${SITE_URL}/#org` },
        inLanguage: "tr-TR",
      },
    ],
  };

  return (
    <html lang="tr" className={`${geistSans.variable} h-full antialiased`}>
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className="min-h-full">
        <ToastProvider>
          <AuthProvider>
            <SectorProvider>
              <SubscriptionProvider>
                {children}
                <QuotaExceededBanner />
              </SubscriptionProvider>
            </SectorProvider>
          </AuthProvider>
        </ToastProvider>
      </body>
    </html>
  );
}
