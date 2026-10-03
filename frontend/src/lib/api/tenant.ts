import api, { unwrap } from "./axios";

export interface TenantResponse {
  id: number;
  ad: string;
  slug: string;
  email: string | null;
  telefon: string | null;
  adres: string | null;
  sehir: string | null;
  ulke: string | null;
  tckn: string | null;
  logoUrl: string | null;
  aktif: boolean;
  onboardingCompleted: boolean;
}

export interface TenantUpdateRequest {
  ad: string;
  email?: string;
  telefon?: string;
  adres?: string;
  sehir?: string;
  ulke?: string;
  tckn?: string;
  logoUrl?: string;
}

export interface CancellationPolicy {
  saatOnce: number | null;
  mesaj: string | null;
}

const BASE = "/api/v1/tenant";

export const tenantApi = {
  get: async (): Promise<TenantResponse> => {
    const res = await api.get(BASE);
    return unwrap<TenantResponse>(res.data);
  },
  update: async (body: TenantUpdateRequest): Promise<TenantResponse> => {
    const res = await api.put(BASE, body);
    return unwrap<TenantResponse>(res.data);
  },
  completeOnboarding: async (): Promise<TenantResponse> => {
    const res = await api.put(`${BASE}/onboarding-complete`);
    return unwrap<TenantResponse>(res.data);
  },
  getCancellationPolicy: async (): Promise<CancellationPolicy> => {
    const res = await api.get(`${BASE}/cancellation-policy`);
    return unwrap<CancellationPolicy>(res.data);
  },
  updateCancellationPolicy: async (body: CancellationPolicy): Promise<CancellationPolicy> => {
    const res = await api.put(`${BASE}/cancellation-policy`, body);
    return unwrap<CancellationPolicy>(res.data);
  },
};
