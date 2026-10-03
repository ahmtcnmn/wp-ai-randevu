import api, { unwrap } from "./axios";

export interface ProductRequest {
  ad: string;
  aciklama?: string;
  fiyat: number;
  stok?: number;
  kategori?: string;
  aiOneriAktif?: boolean;
  hizmetIds?: number[];
}

export interface ProductResponse {
  id: number;
  ad: string;
  aciklama: string | null;
  fiyat: number;
  stok: number;
  kategori: string | null;
  aiOneriAktif: boolean;
  aktif: boolean;
  hizmetIds: number[];
}

export interface ProductSalesSummaryResponse {
  toplamCiro: number;
  toplamAdet: number;
  toplamIslem: number;
  urunBazli: { productId: number; productAd: string; adet: number; ciro: number }[];
  calisanBazli: { staffId: number; staffAd: string; islem: number; ciro: number; toplamKomisyon: number }[];
}

const BASE = "/api/v1/products";

export const productApi = {
  list: async (): Promise<ProductResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<ProductResponse[]>(res.data);
  },
  get: async (id: number): Promise<ProductResponse> => {
    const res = await api.get(`${BASE}/${id}`);
    return unwrap<ProductResponse>(res.data);
  },
  create: async (body: ProductRequest): Promise<ProductResponse> => {
    const res = await api.post(BASE, body);
    return unwrap<ProductResponse>(res.data);
  },
  update: async (id: number, body: ProductRequest): Promise<ProductResponse> => {
    const res = await api.put(`${BASE}/${id}`, body);
    return unwrap<ProductResponse>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
  /** Bir hizmet için önerilen ürünler (aiOneriAktif=true). Randevu detayında kullanılır. */
  recommendedForHizmet: async (hizmetId: number): Promise<ProductResponse[]> => {
    const res = await api.get(`${BASE}/recommended/hizmet/${hizmetId}`);
    return unwrap<ProductResponse[]>(res.data);
  },
  /** Randevu dışı (standalone) satış — dashboard hızlı satış kullanır. */
  sellStandalone: async (body: { productId: number; adet: number; customerId?: number | null; staffId?: number | null }) => {
    const res = await api.post(`${BASE}/sales/standalone`, body);
    return unwrap(res.data);
  },
  salesSummary: async (dateFrom: string, dateTo: string): Promise<ProductSalesSummaryResponse> => {
    const res = await api.get(`${BASE}/sales/summary`, { params: { dateFrom, dateTo } });
    return unwrap<ProductSalesSummaryResponse>(res.data);
  },
  productHistory: async (id: number) => {
    const res = await api.get(`${BASE}/${id}/sales`);
    return unwrap(res.data);
  },
};
