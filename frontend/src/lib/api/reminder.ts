import api, { unwrap } from "./axios";

export type ReminderKanal = "WHATSAPP" | "SMS" | "EMAIL";
export type ReminderStatus =
  | "PENDING" | "SENT" | "RESPONDED_YES" | "RESPONDED_NO" | "SNOOZED" | "CANCELLED";

export interface AppointmentReminderResponse {
  id: number;
  randevuId: number;
  customerId: number;
  musteriAd: string;
  musteriTelefon: string;
  templateId?: number | null;
  mesaj: string;
  kanal: ReminderKanal;
  gonderimTarihi: string;
  status: ReminderStatus;
  sentAt: string | null;
  respondedAt: string | null;
  snoozedUntil: string | null;
  snoozeCount: number;
  createdAt: string;
}

export interface AppointmentReminderRequest {
  gonderimTarihi: string;
  mesaj: string;
  templateId?: number;
  kanal?: ReminderKanal;
}

export interface ReminderStatsResponse {
  toplam: number;
  beklemede: number;
  gonderildi: number;
  yanitlandiEvet: number;
  yanitlandiHayir: number;
  ertelendi: number;
  iptal: number;
}

export type ReminderBirim = "GUN" | "SAAT";

export interface ReminderTemplateRequest {
  ad: string;
  mesaj: string;
  gunSonra: number;
  oncesi?: boolean;
  birim?: ReminderBirim;
  hizmetId?: number | null;
  kanal?: ReminderKanal;
  aktif?: boolean;
}

export interface ReminderTemplateResponse {
  id: number;
  ad: string;
  mesaj: string;
  gunSonra: number;
  oncesi: boolean;
  birim: ReminderBirim;
  hizmetId: number | null;
  kanal: ReminderKanal;
  aktif: boolean;
}

const BASE = "/api/v1/reminders";
const TPL = "/api/v1/reminder-templates";

export const reminderApi = {
  list: async (): Promise<AppointmentReminderResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<AppointmentReminderResponse[]>(res.data);
  },
  pending: async (): Promise<AppointmentReminderResponse[]> => {
    const res = await api.get(`${BASE}/pending`);
    return unwrap<AppointmentReminderResponse[]>(res.data);
  },
  byRandevu: async (randevuId: number): Promise<AppointmentReminderResponse[]> => {
    const res = await api.get(`${BASE}/randevu/${randevuId}`);
    return unwrap<AppointmentReminderResponse[]>(res.data);
  },
  create: async (randevuId: number, body: AppointmentReminderRequest): Promise<AppointmentReminderResponse> => {
    const res = await api.post(`${BASE}/randevu/${randevuId}`, body);
    return unwrap<AppointmentReminderResponse>(res.data);
  },
  snooze: async (id: number, yeniTarih: string): Promise<AppointmentReminderResponse> => {
    const res = await api.post(`${BASE}/${id}/snooze`, { yeniTarih });
    return unwrap<AppointmentReminderResponse>(res.data);
  },
  cancel: async (id: number): Promise<AppointmentReminderResponse> => {
    const res = await api.post(`${BASE}/${id}/cancel`);
    return unwrap<AppointmentReminderResponse>(res.data);
  },
  stats: async (): Promise<ReminderStatsResponse> => {
    const res = await api.get(`${BASE}/stats`);
    return unwrap<ReminderStatsResponse>(res.data);
  },
  resend: async (id: number): Promise<AppointmentReminderResponse> => {
    const res = await api.post(`${BASE}/${id}/resend`);
    return unwrap<AppointmentReminderResponse>(res.data);
  },
};

export const reminderTemplateApi = {
  list: async (): Promise<ReminderTemplateResponse[]> => {
    const res = await api.get(TPL);
    return unwrap<ReminderTemplateResponse[]>(res.data);
  },
  create: async (body: ReminderTemplateRequest): Promise<ReminderTemplateResponse> => {
    const res = await api.post(TPL, body);
    return unwrap<ReminderTemplateResponse>(res.data);
  },
  update: async (id: number, body: ReminderTemplateRequest): Promise<ReminderTemplateResponse> => {
    const res = await api.put(`${TPL}/${id}`, body);
    return unwrap<ReminderTemplateResponse>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${TPL}/${id}`);
  },
};
