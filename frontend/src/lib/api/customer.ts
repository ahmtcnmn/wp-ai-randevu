import api, { unwrap } from "./axios";

export type SegmentType =
  | "NEW" | "REGULAR" | "LOYAL" | "VIP"
  | "OCCASIONAL" | "DRIFTING" | "AT_RISK" | "LOST";

export interface CustomerRequest {
  ad: string;
  soyad: string;
  telefon: string;
  email?: string;
  notlar?: string;
}

export interface CustomerResponse {
  id: number;
  tenantId: number;
  ad: string;
  soyad: string;
  telefon: string;
  email?: string | null;
  notlar?: string | null;
  etiketler: string[];
  karaListedeMi: boolean;
  gelmemeSayisi: number;
  sadakatPuani: number;
  sonZiyaret?: string | null;
  createdAt: string;
}

export interface SegmentSummaryResponse {
  tenantId: number;
  totalCustomers: number;
  segmentCounts: Record<SegmentType, number>;
  lastCalculatedAt?: string | null;
}

export interface CustomerSegmentResponse {
  customerId: number;
  customerAd: string;
  customerSoyad: string;
  customerTelefon: string;
  segment: SegmentType;
  lastVisitDays?: number | null;
  totalVisits?: number | null;
  totalSpent?: number | null;
}

export interface CustomerImportResponse {
  toplam: number;
  basarili: number;
  hatalar: { satir: number; mesaj: string }[];
}

const BASE = "/api/v1/customers";

export const customerApi = {
  list: async (): Promise<CustomerResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<CustomerResponse[]>(res.data);
  },
  get: async (id: number): Promise<CustomerResponse> => {
    const res = await api.get(`${BASE}/${id}`);
    return unwrap<CustomerResponse>(res.data);
  },
  create: async (body: CustomerRequest): Promise<CustomerResponse> => {
    const res = await api.post(BASE, body);
    return unwrap<CustomerResponse>(res.data);
  },
  update: async (id: number, body: CustomerRequest): Promise<CustomerResponse> => {
    const res = await api.put(`${BASE}/${id}`, body);
    return unwrap<CustomerResponse>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
  block: async (id: number): Promise<void> => {
    await api.post(`${BASE}/${id}/block`);
  },
  unblock: async (id: number): Promise<void> => {
    await api.post(`${BASE}/${id}/unblock`);
  },
  addTag: async (id: number, etiket: string): Promise<CustomerResponse> => {
    const res = await api.post(`${BASE}/${id}/tags`, { etiket });
    return unwrap<CustomerResponse>(res.data);
  },
  removeTag: async (id: number, tagId: number): Promise<void> => {
    await api.delete(`${BASE}/${id}/tags/${tagId}`);
  },
  segmentSummary: async (): Promise<SegmentSummaryResponse> => {
    const res = await api.get(`${BASE}/segments/summary`);
    return unwrap<SegmentSummaryResponse>(res.data);
  },
  segmentList: async (segmentType: SegmentType): Promise<CustomerSegmentResponse[]> => {
    const res = await api.get(`${BASE}/segments/${segmentType}`);
    return unwrap<CustomerSegmentResponse[]>(res.data);
  },
  importCsv: async (file: File): Promise<CustomerImportResponse> => {
    const form = new FormData();
    form.append("file", file);
    const res = await api.post(`${BASE}/import`, form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return unwrap<CustomerImportResponse>(res.data);
  },
};
