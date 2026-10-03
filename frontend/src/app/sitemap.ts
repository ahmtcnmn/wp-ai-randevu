import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/constants";

/**
 * SEO için sitemap.xml — sadece public sayfalar.
 * Dashboard ve auth sayfaları noindex'lendiği için burada yok.
 */
export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();
  const routes: { path: string; priority: number; changeFreq: "daily" | "weekly" | "monthly" | "yearly" }[] = [
    { path: "/", priority: 1.0, changeFreq: "weekly" },
    { path: "/fiyatlandirma", priority: 0.9, changeFreq: "monthly" },
    { path: "/iletisim", priority: 0.7, changeFreq: "monthly" },
    { path: "/gizlilik", priority: 0.3, changeFreq: "yearly" },
    { path: "/sartlar", priority: 0.3, changeFreq: "yearly" },
  ];

  return routes.map((r) => ({
    url: `${SITE_URL}${r.path}`,
    lastModified: now,
    changeFrequency: r.changeFreq,
    priority: r.priority,
  }));
}
