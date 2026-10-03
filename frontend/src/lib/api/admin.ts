import api, { unwrap } from "./axios";
import { Page } from "@/types/api";

export interface AdminTenantResponse {
  id: number;
  ad: string;
  slug: string;
  email: string | null;
  telefon: string | null;
  aktif: boolean;
  createdAt: string | null;
  planKey: string | null;
  subscriptionStatus: string | null;
  subscriptionEnd: string | null;
  businessType?: string;
}

export interface OverridePlanRequest {
  planKey: string;
}

export interface ContactRequestResponse {
  id: number;
  ad: string;
  soyad: string;
  email: string;
  telefon: string;
  mesaj: string;
  ipAddress: string | null;
  userAgent: string | null;
  durum: string;
  superAdminNotu: string | null;
  cevaplayanUserId: number | null;
  cevaplanmaTarihi: string | null;
  createdAt: string;
}

const TENANTS = "/api/v1/admin/tenants";
const CONTACT = "/api/v1/admin/contact-requests";

export const adminApi = {
  listTenants: async (): Promise<AdminTenantResponse[]> => {
    const res = await api.get(TENANTS);
    return unwrap<AdminTenantResponse[]>(res.data);
  },
  getTenant: async (id: number): Promise<AdminTenantResponse> => {
    const res = await api.get(`${TENANTS}/${id}`);
    return unwrap<AdminTenantResponse>(res.data);
  },
  overridePlan: async (id: number, planKey: string): Promise<void> => {
    await api.post(`${TENANTS}/${id}/override-plan`, { planKey });
  },
  setActive: async (id: number, aktif: boolean): Promise<void> => {
    await api.put(`${TENANTS}/${id}/aktif`, { aktif });
  },
  setBusinessType: async (id: number, businessType: string): Promise<void> => {
    await api.put(`${TENANTS}/${id}/business-type`, { businessType });
  },

  // Contact requests
  listContactRequests: async (page = 0, size = 20, durum?: string): Promise<Page<ContactRequestResponse>> => {
    const params: Record<string, string | number> = { page, size };
    if (durum) params.durum = durum;
    const res = await api.get(CONTACT, { params });
    return unwrap<Page<ContactRequestResponse>>(res.data);
  },
  updateContactRequest: async (id: number, durum: string, not?: string): Promise<ContactRequestResponse> => {
    const body: Record<string, string> = { durum };
    if (not !== undefined) body.not = not;
    const res = await api.put(`${CONTACT}/${id}`, body);
    return unwrap<ContactRequestResponse>(res.data);
  },
};
