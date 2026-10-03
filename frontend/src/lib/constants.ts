// API base URL
// Prod: https://apiguzelim.ehasoftware.com
// Dev: http://localhost:8081
export const API_URL =
  process.env.NEXT_PUBLIC_API_URL || "https://apiguzelim.ehasoftware.com";

// Public site URL (SEO için — sitemap, OG, canonical)
export const SITE_URL =
  process.env.NEXT_PUBLIC_SITE_URL || "https://guzelim.ehasoftware.com";

// Cookie names (web — js-cookie)
export const COOKIE_TOKEN = "token";
export const COOKIE_REFRESH = "refreshToken";

// Token expiry (days)
export const TOKEN_EXPIRES_DAYS = 1;
export const REFRESH_EXPIRES_DAYS = 7;

// API exception paths (Sprint dokumantasyonu — bu endpoint'ler wrapper YOK veya v1 YOK)
export const EXCEPTION_PATHS = {
  CHAT: "/api/chat",
  CALISMA_SAATLERI: "/api/calisma-saatleri",
  FEEDBACK_V1: "/api/v1/feedback",
};

// Brand
export const BRAND_NAME = "AppointFlow";
export const BRAND_TAGLINE = "Berberler ve uzmanlar için modern randevu yönetim platformu";
export const BRAND_DESCRIPTION =
  "AppointFlow — WhatsApp entegrasyonu, AI asistan ve otomatik hatırlatma ile randevu yönetimi. 14 gün ücretsiz dene.";

// Auth callback path (login sonrasi yonlendirme)
export const AFTER_LOGIN_PATH = "/dashboard";
