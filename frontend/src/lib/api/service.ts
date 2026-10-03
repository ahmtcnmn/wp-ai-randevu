import api, { unwrap } from "./axios";

export interface ServiceCategoryRequest {
  ad: string;
  aciklama?: string;
  takvimRengi?: string;
}

export interface ServiceCategoryResponse {
  id: number;
  ad: string;
  aciklama?: string | null;
  sira: number;
  takvimRengi?: string | null;
  aktif: boolean;
}

export interface ServiceRequestBody {
  ad: string;
  aciklama?: string;
  sureDakika: number;
  fiyat: number;
  kategoriId?: number | null;
  staffIds?: number[];
  bufferOnceDk?: number;
  bufferSonraDk?: number;
  takvimRengi?: string;
}

export interface ServiceResponseBody {
  id: number;
  ad: string;
  aciklama: string | null;
  sureDakika: number;
  fiyat: number;
  kategoriId: number | null;
  kategoriAd: string | null;
  bufferOnceDk: number;
  bufferSonraDk: number;
  takvimRengi: string | null;
  aktif: boolean;
  staffIds: number[];
}

export const serviceApi = {
  list: async (): Promise<ServiceResponseBody[]> => {
    const res = await api.get("/api/v1/services");
    return unwrap<ServiceResponseBody[]>(res.data);
  },
  get: async (id: number): Promise<ServiceResponseBody> => {
    const res = await api.get(`/api/v1/services/${id}`);
    return unwrap<ServiceResponseBody>(res.data);
  },
  create: async (body: ServiceRequestBody): Promise<ServiceResponseBody> => {
    const res = await api.post("/api/v1/services", body);
    return unwrap<ServiceResponseBody>(res.data);
  },
  update: async (id: number, body: ServiceRequestBody): Promise<ServiceResponseBody> => {
    const res = await api.put(`/api/v1/services/${id}`, body);
    return unwrap<ServiceResponseBody>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`/api/v1/services/${id}`);
  },
  activate: async (id: number): Promise<void> => {
    await api.put(`/api/v1/services/${id}/activate`);
  },
};

export const serviceCategoryApi = {
  list: async (): Promise<ServiceCategoryResponse[]> => {
    const res = await api.get("/api/v1/service-categories");
    return unwrap<ServiceCategoryResponse[]>(res.data);
  },
  create: async (body: ServiceCategoryRequest): Promise<ServiceCategoryResponse> => {
    const res = await api.post("/api/v1/service-categories", body);
    return unwrap<ServiceCategoryResponse>(res.data);
  },
  update: async (id: number, body: ServiceCategoryRequest): Promise<ServiceCategoryResponse> => {
    const res = await api.put(`/api/v1/service-categories/${id}`, body);
    return unwrap<ServiceCategoryResponse>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`/api/v1/service-categories/${id}`);
  },
  reorder: async (ids: number[]): Promise<void> => {
    await api.put("/api/v1/service-categories/reorder", ids);
  },
};
