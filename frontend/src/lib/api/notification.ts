import api, { unwrap } from "./axios";
import { Page } from "@/types/api";

export interface NotificationResponse {
  id: number;
  tip: string;
  baslik: string;
  icerik?: string | null;
  link?: string | null;
  okundu: boolean;
  createdAt: string;
  readAt?: string | null;
}

const BASE = "/api/v1/notifications";

export const notificationApi = {
  list: async (page = 0, size = 20): Promise<Page<NotificationResponse>> => {
    const res = await api.get(BASE, { params: { page, size } });
    return unwrap<Page<NotificationResponse>>(res.data);
  },
  unreadCount: async (): Promise<number> => {
    const res = await api.get(`${BASE}/unread-count`);
    const data = unwrap<{ unreadCount: number }>(res.data);
    return data.unreadCount;
  },
  read: async (id: number): Promise<NotificationResponse> => {
    const res = await api.post(`${BASE}/${id}/read`);
    return unwrap<NotificationResponse>(res.data);
  },
  readAll: async (): Promise<number> => {
    const res = await api.post(`${BASE}/read-all`);
    return unwrap<number>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
};
