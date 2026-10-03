import api, { unwrap } from "./axios";

export interface UserDeviceResponse {
  id: number;
  expoPushToken: string;
  platform: string | null;
  sonKullanim?: string | null;
  createdAt: string;
}

const BASE = "/api/v1/user-devices";

export const userDeviceApi = {
  register: async (expoPushToken: string, platform?: string): Promise<UserDeviceResponse> => {
    const res = await api.post(BASE, { expoPushToken, platform });
    return unwrap<UserDeviceResponse>(res.data);
  },
  list: async (): Promise<UserDeviceResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<UserDeviceResponse[]>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
};
