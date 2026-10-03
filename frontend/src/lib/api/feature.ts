import api, { unwrap } from "./axios";

export interface FeatureResponse {
  featureKey: string;
  ad: string;
  aciklama: string;
  enabled: boolean;
}

const BASE = "/api/v1/features";

export const featureApi = {
  list: async (): Promise<FeatureResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<FeatureResponse[]>(res.data);
  },
  toggle: async (featureKey: string, enabled: boolean): Promise<FeatureResponse> => {
    const res = await api.put(`${BASE}/${featureKey}`, { enabled });
    return unwrap<FeatureResponse>(res.data);
  },
};
