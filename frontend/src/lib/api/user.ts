import api, { unwrap } from "./axios";
import { Role, UserResponse } from "@/types/auth";

export interface UserCreateRequest {
  ad: string;
  soyad: string;
  email: string;
  sifre: string;
  telefon: string;
  rol: Role;
  subeId?: number | null;
  pozisyon?: string | null;
  hizmetIds?: number[];
}

export interface UserUpdateRequest {
  ad: string;
  soyad: string;
  telefon: string;
  rol?: Role;
  subeId?: number | null;
  pozisyon?: string | null;
  /** Çalışanın yapabileceği hizmetler. undefined → değişiklik yok. */
  hizmetIds?: number[];
}

const BASE = "/api/v1/users";

export const userApi = {
  list: async (): Promise<UserResponse[]> => {
    const res = await api.get(BASE);
    return unwrap<UserResponse[]>(res.data);
  },
  get: async (id: number): Promise<UserResponse> => {
    const res = await api.get(`${BASE}/${id}`);
    return unwrap<UserResponse>(res.data);
  },
  create: async (body: UserCreateRequest): Promise<UserResponse> => {
    const res = await api.post(BASE, body);
    return unwrap<UserResponse>(res.data);
  },
  update: async (id: number, body: UserUpdateRequest): Promise<UserResponse> => {
    const res = await api.put(`${BASE}/${id}`, body);
    return unwrap<UserResponse>(res.data);
  },
  remove: async (id: number): Promise<void> => {
    await api.delete(`${BASE}/${id}`);
  },
  activate: async (id: number): Promise<UserResponse> => {
    const res = await api.put(`${BASE}/${id}/activate`);
    return unwrap<UserResponse>(res.data);
  },
  resendVerification: async (id: number): Promise<void> => {
    await api.post(`${BASE}/${id}/resend-verification`);
  },
  markEmailVerified: async (id: number): Promise<UserResponse> => {
    const res = await api.put(`${BASE}/${id}/mark-email-verified`);
    return unwrap<UserResponse>(res.data);
  },
};
