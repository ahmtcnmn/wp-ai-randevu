import api, { unwrap } from "./axios";

export interface BranchRequest {
  ad: string;
  adres?: string;
  telefon?: string;
  aktif?: boolean;
}

export interface BranchResponse {
  id: number;
  ad: string;
  adres: string | null;
  telefon: string | null;
  aktif: boolean;
}

const BASE = "/api/v1/branches";

export const branchApi = {
  list: async (): Promise<BranchResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<BranchResponse[]>(res.data);
  },
  create: async (body: BranchRequest): Promise<BranchResponse> => {
    const res = await api.post(BASE, body);
    return unwrap<BranchResponse>(res.data);
  },
  update: async (id: number, body: BranchRequest): Promise<BranchResponse> => {
    const res = await api.put(`${BASE}/${id}`, body);
    return unwrap<BranchResponse>(res.data);
  },
  /** Şube pasifleştirme (eskiden delete — backend artık soft delete yapıyor, veri korunur). */
  remove: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
  activate: async (id: number): Promise<void> => {
    await api.put(`${BASE}/${id}/activate`);
  },
};
