import api, { unwrap } from "./axios";

export interface FeedbackResponse {
  id: number;
  randevuId: number;
  uzmanId: number;
  uzmanAd: string;
  puan: number;
  yorum: string | null;
  sikayetVarmi: boolean;
  tarih: string;
}

const BASE = "/api/v1/feedback";

export const feedbackApi = {
  byUzman: async (uzmanId: number): Promise<FeedbackResponse[]> => {
    // NOT: wrapper YOK — bu endpoint istisna
    const res = await api.get(`${BASE}/uzman/${uzmanId}`);
    return res.data as FeedbackResponse[];
  },
  complaints: async (): Promise<FeedbackResponse[]> => {
    const res = await api.get(`${BASE}/sikayetler`);
    return res.data as FeedbackResponse[];
  },
};
