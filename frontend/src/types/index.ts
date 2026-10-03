// Yeniden ihracat — yeni tipler
export type { ApiResponse, Page, ApiError } from "./api";
export type {
  Role,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  TwoFactorLoginRequest,
  TwoFactorSetupResponse,
  TwoFactorEnableResponse,
  UserResponse,
} from "./auth";

// Geriye uyumluluk için bazı eski tip alias'ları
export type KayitRequest = import("./auth").RegisterRequest;
export type GirisRequest = import("./auth").LoginRequest;

// Mevcut booking sayfasının kullandığı tipler (book/page.tsx için)
export interface Hizmet {
  id: number;
  ad: string;
  aciklama: string;
  sureDakika: number;
  fiyat: number;
}

export interface Sube {
  id: number;
  ad: string;
  adres: string;
}

export interface Kullanici {
  id: number;
  ad: string;
  soyad: string;
  email: string;
  telefon: string;
  rol: import("./auth").Role;
  sube?: Sube;
  karaListedeMi?: boolean;
}

export interface RandevuRequest {
  uzmanId: number;
  hizmetId: number;
  tarihSaat: string;
  not?: string;
}

export interface RandevuResponse {
  id: number;
  musteriAd: string;
  uzmanAd: string;
  hizmetAd: string;
  fiyat: number;
  sureDakika: number;
  tarihSaat: string;
  durum: "BEKLIYOR" | "ONAYLANDI" | "IPTAL_EDILDI" | "TAMAMLANDI";
  not?: string;
}

// Subscription & Billing types

export interface SubscriptionPlanResponse {
  id: number;
  planKey: "STARTER" | "GROWTH" | "ENTERPRISE" | string;
  ad: string;
  aciklama: string;
  aylikFiyat: number;
  maxSube: number | null;
  maxCalisan: number | null;
  maxAylikRandevu: number | null;
  aktif: boolean;
}

export interface SubscriptionResponse {
  id: number;
  plan: SubscriptionPlanResponse;
  status:
    | "TRIALING"
    | "ACTIVE"
    | "PAST_DUE"
    | "SUSPENDED"
    | "CANCELLED"
    | "EXPIRED";
  baslangicTarihi: string;
  denemeBitisTarihi: string | null;
  sonrakiOdemeTarihi: string | null;
  gracePeriodBitis: string | null;
}

/**
 * @deprecated Sprint 1 rename: yeni adlar `usedAppointmentsThisMonth/maxAppointmentsThisMonth/usedBranches/...`
 * Mevcut billing sayfası bu eski formatı bekliyor olabilir, billing context içinde mapping yapılır.
 */
export interface QuotaUsageResponse {
  appointmentsUsed: number;
  appointmentsLimit: number | null;
  branchCount: number;
  branchLimit: number | null;
  staffCount: number;
  staffLimit: number | null;
}

export interface InvoiceResponse {
  id: number;
  tutar: number;
  paraBirimi: string;
  durum: "PENDING" | "PAID" | "FAILED" | "REFUNDED";
  donemBaslangic: string;
  donemBitis: string;
  odemeTarihi: string | null;
}

export interface CheckoutInitResponse {
  checkoutFormContent: string;
  token: string;
  planKey: string;
}
