import api, { unwrap } from "./axios";
import { SegmentType } from "./customer";

export interface SlotCampaignResponse {
  id: number;
  randevuId: number | null;
  randevuTarihi: string;
  hizmetAd: string;
  uzmanAd: string;
  durum: "ACTIVE" | "FILLED" | "EXPIRED";
  gonderimSayisi: number;
  basariliSayisi: number;
  createdAt: string;
}

export interface SegmentCampaignRequest {
  baslik: string;
  hedefSegment: SegmentType;
  mesaj: string;
}

export interface SegmentCampaignResponse {
  id: number;
  baslik: string;
  hedefSegment: SegmentType;
  mesaj: string;
  hedefSayisi: number;
  gonderimSayisi: number;
  basariliSayisi: number;
  durum: string;
  createdAt: string;
}

const BASE = "/api/v1/campaigns";

export const campaignApi = {
  listSlot: async (): Promise<SlotCampaignResponse[]> => {
    const res = await api.get(`${BASE}/slot`);
    return unwrap<SlotCampaignResponse[]>(res.data);
  },
  cancelSlot: async (id: number): Promise<void> => {
    await api.post(`${BASE}/slot/${id}/cancel`);
  },
  listSegment: async (): Promise<SegmentCampaignResponse[]> => {
    const res = await api.get(`${BASE}/segment`);
    return unwrap<SegmentCampaignResponse[]>(res.data);
  },
  createSegment: async (body: SegmentCampaignRequest): Promise<SegmentCampaignResponse> => {
    const res = await api.post(`${BASE}/segment`, body);
    return unwrap<SegmentCampaignResponse>(res.data);
  },
};
