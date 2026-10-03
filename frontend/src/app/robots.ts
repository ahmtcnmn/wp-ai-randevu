import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/constants";

/**
 * SEO için robots.txt
 * - Public sayfalar indexlenir
 * - Auth + dashboard + onboarding sayfaları kapalı
 * - API + Next.js internal kapalı
 */
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: "*",
        allow: ["/"],
        disallow: [
          "/login",
          "/register",
          "/forgot-password",
          "/reset-password",
          "/verify-email",
          "/2fa-verify",
          "/dashboard",
          "/takvim",
          "/randevular",
          "/musteriler",
          "/whatsapp",
          "/kampanyalar",
          "/hatirlatma",
          "/calisanlar",
          "/hizmetler",
          "/urunler",
          "/finans",
          "/raporlar",
          "/bildirimler",
          "/hesap",
          "/ayarlar",
          "/audit-log",
          "/sistem",
          "/onboarding",
          "/billing",
          "/book",
          "/api/",
          "/_next/",
        ],
      },
      // AI botlar — opsiyonel, training data için bazı kullanıcılar kapatmak isteyebilir
      // Şimdilik açık bırakıyoruz
    ],
    sitemap: `${SITE_URL}/sitemap.xml`,
    host: SITE_URL,
  };
}
