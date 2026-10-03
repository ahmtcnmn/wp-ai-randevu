import api, { unwrap } from "./axios";

export interface PlanResponse {
  id: number;
  planKey: string;
  ad: string;
  aciklama: string;
  aylikFiyat: number;
  maxSube: number | null;
  maxCalisan: number | null;
  maxAylikRandevu: number | null;
  aktif: boolean;
}

export interface QuotaUsageResponse {
  usedAppointmentsThisMonth: number;
  maxAppointmentsThisMonth: number; // -1 = sınırsız
  usedBranches: number;
  maxBranches: number;
  usedStaff: number;
  maxStaff: number;
}

export type SubscriptionStatus =
  | "TRIALING"
  | "ACTIVE"
  | "PAST_DUE"
  | "SUSPENDED"
  | "CANCELLED"
  | "EXPIRED";

export interface SubscriptionResponse {
  id: number;
  plan: PlanResponse;
  status: SubscriptionStatus;
  baslangicTarihi: string;
  denemeBitisTarihi: string | null;
  sonrakiOdemeTarihi: string | null;
  gracePeriodBitis: string | null;
  /** Trial sırasında kalan gün sayısı (sadece TRIALING için). */
  trialDaysRemaining?: number | null;
  isTrialing?: boolean;
  /** Hesap kullanılabilir mi (TRIALING/ACTIVE = true, EXPIRED/SUSPENDED = false). */
  isUsable?: boolean;
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

const BASE = "/api/v1/billing";

export const billingApi = {
  plans: async (): Promise<PlanResponse[]> => {
    const res = await api.get(`${BASE}/plans`);
    return unwrap<PlanResponse[]>(res.data);
  },
  subscription: async (): Promise<SubscriptionResponse | null> => {
    const res = await api.get(`${BASE}/subscription`);
    return unwrap<SubscriptionResponse | null>(res.data);
  },
  quota: async (): Promise<QuotaUsageResponse> => {
    const res = await api.get(`${BASE}/quota`);
    return unwrap<QuotaUsageResponse>(res.data);
  },
  invoices: async (): Promise<InvoiceResponse[]> => {
    const res = await api.get(`${BASE}/invoices`);
    return unwrap<InvoiceResponse[]>(res.data);
  },
  checkout: async (planKey: string): Promise<CheckoutInitResponse> => {
    const res = await api.post(`${BASE}/checkout`, { planKey });
    return unwrap<CheckoutInitResponse>(res.data);
  },
  upgrade: async (planKey: string): Promise<CheckoutInitResponse> => {
    const res = await api.post(`${BASE}/upgrade`, { planKey });
    return unwrap<CheckoutInitResponse>(res.data);
  },
  cancel: async (): Promise<void> => {
    await api.delete(`${BASE}/subscription`);
  },
};
