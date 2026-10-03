import api, { unwrap } from "./axios";

export type RandevuDurumu =
  | "BEKLIYOR" | "ONAYLANDI" | "TAMAMLANDI" | "GELMEDI" | "IPTAL_EDILDI";
export type RandevuKaynak = "MANUAL" | "WEB" | "WHATSAPP";

export interface AppointmentServiceItem {
  hizmetId: number;
  hizmetAd: string;
  fiyat: number;
  sureDk: number;
}

export interface AppointmentRequest {
  customerId: number;
  uzmanId: number;
  hizmetIds: number[];
  tarihSaat: string;        // ISO date-time
  kaynak?: RandevuKaynak;
  not?: string;
}

export interface AppointmentUpdateRequest {
  hizmetIds: number[];
  tarihSaat: string;
}

export interface AppointmentResponse {
  id: number;
  customerId: number | null;
  musteriAd: string;
  uzmanId: number;
  uzmanAd: string;
  hizmetler: AppointmentServiceItem[];
  tarihSaat: string;
  bitisTarihi: string | null;
  durum: RandevuDurumu;
  kaynak: RandevuKaynak;
  toplamFiyat: number;
  /** Bu randevuda satılan ürünlerin toplam tutarı (varsa). */
  urunToplami?: number;
  /** Hizmet + ürün genel toplamı. */
  genelToplam?: number;
  odenenTutar: number | null;
  toplamSureDk: number;
  not?: string | null;
  iptalNedeni?: string | null;
  createdAt: string;
}

export interface ProductSaleRequest {
  productId: number;
  adet: number;
}

export interface ProductSaleResponse {
  id: number;
  randevuId: number | null;
  productId: number;
  productAd: string;
  staffId: number | null;
  staffAd: string | null;
  customerId: number | null;
  adet: number;
  birimFiyatSnapshot: number;
  toplamTutar: number;
  commissionRateSnapshot: number | null;
  commissionAmount: number | null;
  createdAt: string;
}

const BASE = "/api/v1/appointments";

export const appointmentApi = {
  list: async (): Promise<AppointmentResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<AppointmentResponse[]>(res.data);
  },
  get: async (id: number): Promise<AppointmentResponse> => {
    const res = await api.get(`${BASE}/${id}`);
    return unwrap<AppointmentResponse>(res.data);
  },
  create: async (body: AppointmentRequest): Promise<AppointmentResponse> => {
    const res = await api.post(BASE, body);
    return unwrap<AppointmentResponse>(res.data);
  },
  update: async (id: number, body: AppointmentUpdateRequest): Promise<AppointmentResponse> => {
    const res = await api.put(`${BASE}/${id}`, body);
    return unwrap<AppointmentResponse>(res.data);
  },
  updateStatus: async (
    id: number,
    body: { durum: RandevuDurumu; toplamFiyat?: number }
  ): Promise<AppointmentResponse> => {
    const res = await api.put(`${BASE}/${id}/status`, body);
    return unwrap<AppointmentResponse>(res.data);
  },
  cancel: async (id: number, neden?: string): Promise<AppointmentResponse> => {
    const res = await api.post(`${BASE}/${id}/cancel`, { neden });
    return unwrap<AppointmentResponse>(res.data);
  },
  addNote: async (id: number, body: { icerik: string; tur?: "INTERNAL" | "MUSTERI" | "REMINDER" }): Promise<void> => {
    await api.post(`${BASE}/${id}/notes`, body);
  },
  availability: async (uzmanId: number, hizmetIds: number[], tarih: string): Promise<string[]> => {
    const res = await api.get(`${BASE}/availability`, {
      params: { uzmanId, hizmetIds: hizmetIds.join(","), tarih },
    });
    return unwrap<string[]>(res.data);
  },

  // Sprint 6 — Urun satisi
  addProduct: async (id: number, body: ProductSaleRequest): Promise<ProductSaleResponse> => {
    const res = await api.post(`${BASE}/${id}/products`, body);
    return unwrap<ProductSaleResponse>(res.data);
  },
  listProducts: async (id: number): Promise<ProductSaleResponse[]> => {
    const res = await api.get(`${BASE}/${id}/products`);
    return unwrap<ProductSaleResponse[]>(res.data);
  },
  removeProduct: async (id: number, saleId: number): Promise<void> => {
    await api.delete(`${BASE}/${id}/products/${saleId}`);
  },
};
