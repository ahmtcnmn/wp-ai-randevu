import api, { unwrap } from "./axios";

export interface AiConfigResponse {
  aktif: boolean;
  model: string;
  sistemPromptu: string;
  maxToken: number;
  personaAdi: string;
  /** virgülle ayrılmış kelimeler (Sprint dokümanı: String) */
  handoffKelimeleri: string;
  dil: string;
  fiyatBilgisiGoster?: boolean;
  otomatikOnay?: boolean;
  /** Onboarding'de alınan işletme tanıtım metni. */
  isletmeAciklamasi?: string;
}

export type AiConfigUpdateRequest = Partial<AiConfigResponse>;

const BASE = "/api/v1/ai/config";

export const aiConfigApi = {
  get: async (): Promise<AiConfigResponse> => {
    const res = await api.get(BASE);
    return unwrap<AiConfigResponse>(res.data);
  },
  update: async (body: AiConfigUpdateRequest): Promise<AiConfigResponse> => {
    const res = await api.put(BASE, body);
    return unwrap<AiConfigResponse>(res.data);
  },
};
