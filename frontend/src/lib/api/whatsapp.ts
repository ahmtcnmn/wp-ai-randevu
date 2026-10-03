import api, { unwrap } from "./axios";

export type ConversationDurum = "ACTIVE" | "WAITING" | "HUMAN_ACTIVE" | "CLOSED";
export type SenderType = "CUSTOMER" | "AI" | "STAFF" | "SYSTEM";
export type ActiveHandler = "BOOKING" | "CANCELLATION" | null;

export interface ConversationResponse {
  id: number;
  customerId: number | null;
  customerPhone: string;
  customerName: string | null;
  durum: ConversationDurum;
  assignedUserId: number | null;
  kanal: string | null;
  aktifHandler?: ActiveHandler;
  sonMesajZamani: string | null;
  olusturmaTarihi: string;
  kapatmaTarihi: string | null;
}

export interface MessageResponse {
  id: number;
  senderType: SenderType;
  senderId: number | null;
  senderName: string | null;
  mesajTipi: string;
  icerik: string;
  olusturmaTarihi: string;
}

export interface AssignConversationRequest {
  userId: number;
  notlar?: string;
}

export interface SendMessageRequest {
  icerik: string;
  tur?: "OUTBOUND";
}

// WhatsApp config — backend field names: verifyToken, appSecret, phoneNumberId, wabaId,
// accessToken, aktif, webhookUrl, displayPhone
export interface WhatsappConfigResponse {
  tokenConfigured: boolean;
  appSecretConfigured: boolean;
  phoneNumberId: string | null;
  wabaId: string | null;
  verifyToken: string | null;
  webhookUrl: string | null;
  displayPhone: string | null;
  aktif: boolean;
}

export interface WhatsappConfigUpdateRequest {
  accessToken?: string;
  appSecret?: string;
  phoneNumberId?: string;
  wabaId?: string;
  verifyToken?: string;
  webhookUrl?: string;
  displayPhone?: string;
  aktif?: boolean;
}

// WhatsApp template (Sprint 2 B11 — yeni alanlar)
export interface WhatsappTemplateRequest {
  ad: string;
  templateKey: string;
  kategori?: string;
  dil?: string;
  govde: string;
  baslik?: string;
  footer?: string;
}

export interface WhatsappTemplateResponse {
  id: number;
  ad: string;
  templateKey: string;
  kategori: string;
  dil: string;
  govde: string;
  baslik: string | null;
  footer: string | null;
  status: string;
  metaTemplateId: string | null;
  redSebebi: string | null;
}

const CONV = "/api/v1/conversations";
const CONFIG = "/api/v1/whatsapp/config";
const TEMPLATES = "/api/v1/whatsapp/templates";

export const conversationApi = {
  active: async (): Promise<ConversationResponse[]> => {
    const res = await api.get(CONV);
    return unwrap<ConversationResponse[]>(res.data);
  },
  history: async (): Promise<ConversationResponse[]> => {
    const res = await api.get(`${CONV}/history`);
    return unwrap<ConversationResponse[]>(res.data);
  },
  get: async (id: number): Promise<ConversationResponse> => {
    const res = await api.get(`${CONV}/${id}`);
    return unwrap<ConversationResponse>(res.data);
  },
  messages: async (id: number): Promise<MessageResponse[]> => {
    const res = await api.get(`${CONV}/${id}/messages`);
    return unwrap<MessageResponse[]>(res.data);
  },
  sendMessage: async (id: number, body: SendMessageRequest): Promise<MessageResponse> => {
    const res = await api.post(`${CONV}/${id}/messages`, body);
    return unwrap<MessageResponse>(res.data);
  },
  takeover: async (id: number): Promise<ConversationResponse> => {
    const res = await api.post(`${CONV}/${id}/takeover`);
    return unwrap<ConversationResponse>(res.data);
  },
  release: async (id: number): Promise<ConversationResponse> => {
    const res = await api.post(`${CONV}/${id}/release`);
    return unwrap<ConversationResponse>(res.data);
  },
  close: async (id: number): Promise<ConversationResponse> => {
    const res = await api.post(`${CONV}/${id}/close`);
    return unwrap<ConversationResponse>(res.data);
  },
  assign: async (id: number, body: AssignConversationRequest): Promise<ConversationResponse> => {
    const res = await api.post(`${CONV}/${id}/assign`, body);
    return unwrap<ConversationResponse>(res.data);
  },
};

export const whatsappConfigApi = {
  get: async (): Promise<WhatsappConfigResponse> => {
    const res = await api.get(CONFIG);
    return unwrap<WhatsappConfigResponse>(res.data);
  },
  update: async (body: WhatsappConfigUpdateRequest): Promise<WhatsappConfigResponse> => {
    const res = await api.put(CONFIG, body);
    return unwrap<WhatsappConfigResponse>(res.data);
  },
  test: async (telefon: string): Promise<{ ok: boolean; message?: string }> => {
    const res = await api.post(`${CONFIG}/test`, { telefon });
    return unwrap(res.data);
  },
};

export const whatsappTemplateApi = {
  list: async (): Promise<WhatsappTemplateResponse[]> => {
    const res = await api.get(TEMPLATES);
    return unwrap<WhatsappTemplateResponse[]>(res.data);
  },
  create: async (body: WhatsappTemplateRequest): Promise<WhatsappTemplateResponse> => {
    const res = await api.post(TEMPLATES, body);
    return unwrap<WhatsappTemplateResponse>(res.data);
  },
  update: async (id: number, body: WhatsappTemplateRequest): Promise<WhatsappTemplateResponse> => {
    const res = await api.put(`${TEMPLATES}/${id}`, body);
    return unwrap<WhatsappTemplateResponse>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${TEMPLATES}/${id}`);
  },
  submitToMeta: async (id: number): Promise<WhatsappTemplateResponse> => {
    const res = await api.post(`${TEMPLATES}/${id}/submit`);
    return unwrap<WhatsappTemplateResponse>(res.data);
  },
};
