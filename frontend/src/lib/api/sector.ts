import api, { unwrap } from "./axios";

export type BusinessType =
  | "BARBER"
  | "HAIR_SALON"
  | "DENTAL_CLINIC"
  | "BEAUTY_SALON"
  | "SPA"
  | "GYM"
  | "VETERINARY"
  | "PET_GROOMING"
  | "NAIL_SALON"
  | "TATTOO"
  | "OTHER";

export interface SectorLabelDictionary {
  staffSingular: string;
  staffPlural: string;
  customerSingular: string;
  customerPlural: string;
  serviceSingular: string;
  servicePlural: string;
  appointmentSingular: string;
  appointmentPlural: string;
}

export interface SectorLabelsResponse {
  businessType: BusinessType;
  businessTypeName: string;
  labels: SectorLabelDictionary;
}

export interface SectorTypeInfo {
  code: BusinessType;
  name: string;
}

export interface PositionInfo {
  code: string;
  name: string;
}

const BASE = "/api/v1/sector";

export const sectorApi = {
  /** Giriş yapmış kullanıcının tenant'ının etiket sözlüğü. */
  labels: async (): Promise<SectorLabelsResponse> => {
    const res = await api.get(`${BASE}/labels`);
    return unwrap<SectorLabelsResponse>(res.data);
  },
  /** Tüm sektörlerin listesi (register sayfası için, auth gerekmez). */
  types: async (): Promise<SectorTypeInfo[]> => {
    const res = await api.get(`${BASE}/types`);
    return unwrap<SectorTypeInfo[]>(res.data);
  },
  /** Tenant'ın sektörü için geçerli çalışan pozisyonları. */
  positions: async (): Promise<PositionInfo[]> => {
    const res = await api.get(`${BASE}/positions`);
    return unwrap<PositionInfo[]>(res.data);
  },
};

/** Default etiketler — SectorContext yüklenene kadar kullanılır. */
export const DEFAULT_SECTOR_LABELS: SectorLabelDictionary = {
  staffSingular: "Çalışan",
  staffPlural: "Çalışanlar",
  customerSingular: "Müşteri",
  customerPlural: "Müşteriler",
  serviceSingular: "Hizmet",
  servicePlural: "Hizmetler",
  appointmentSingular: "Randevu",
  appointmentPlural: "Randevular",
};
