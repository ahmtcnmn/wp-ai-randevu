import api, { unwrap } from "./axios";

export interface CalismaSaatiEntry {
  gunOfWeek: number; // 1=Pzt, 7=Paz
  baslangic: string; // "09:00"
  bitis: string;     // "18:00"
  aktif: boolean;
}

// NOT: /api/calisma-saatleri (v1 YOK)
const BASE = "/api/calisma-saatleri";

export const calismaSaatiApi = {
  get: async (uzmanId: number): Promise<CalismaSaatiEntry[]> => {
    const res = await api.get(`${BASE}/${uzmanId}`);
    return unwrap<CalismaSaatiEntry[]>(res.data);
  },
  save: async (uzmanId: number, entries: CalismaSaatiEntry[]): Promise<CalismaSaatiEntry[]> => {
    const res = await api.post(BASE, { uzmanId, entries });
    return unwrap<CalismaSaatiEntry[]>(res.data);
  },
};
