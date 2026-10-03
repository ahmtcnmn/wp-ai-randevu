import api, { unwrap } from "./axios";
import {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  TwoFactorEnableResponse,
  TwoFactorLoginRequest,
  TwoFactorSetupResponse,
  UserResponse,
} from "@/types/auth";

const BASE = "/api/v1/auth";

export const authApi = {
  login: async (body: LoginRequest): Promise<AuthResponse> => {
    const res = await api.post(`${BASE}/login`, body);
    return unwrap<AuthResponse>(res.data);
  },

  loginTwoFactor: async (body: TwoFactorLoginRequest): Promise<AuthResponse> => {
    const res = await api.post(`${BASE}/login-2fa`, body);
    return unwrap<AuthResponse>(res.data);
  },

  register: async (body: RegisterRequest): Promise<AuthResponse> => {
    const res = await api.post(`${BASE}/register`, body);
    return unwrap<AuthResponse>(res.data);
  },

  refresh: async (refreshToken: string): Promise<AuthResponse> => {
    const res = await api.post(`${BASE}/refresh`, { refreshToken });
    return unwrap<AuthResponse>(res.data);
  },

  logout: async (): Promise<void> => {
    await api.post(`${BASE}/logout`);
  },

  /** Hesabı sil — soft delete, 30 gün geri alma penceresi var. Sadece OWNER. */
  deleteAccount: async (): Promise<void> => {
    await api.delete(`${BASE}/account`);
  },

  me: async (): Promise<UserResponse> => {
    const res = await api.get(`${BASE}/me`);
    return unwrap<UserResponse>(res.data);
  },

  forgotPassword: async (body: ForgotPasswordRequest): Promise<void> => {
    await api.post(`${BASE}/forgot-password`, body);
  },

  resetPassword: async (body: ResetPasswordRequest): Promise<void> => {
    await api.post(`${BASE}/reset-password`, body);
  },

  resendVerification: async (): Promise<void> => {
    await api.post(`${BASE}/resend-verification`);
  },

  verifyEmail: async (token: string): Promise<void> => {
    await api.get(`${BASE}/verify-email`, { params: { token } });
  },

  // 2FA endpoint'leri (Sprint 7)
  twoFactor: {
    setup: async (): Promise<TwoFactorSetupResponse> => {
      const res = await api.post(`${BASE}/2fa/setup`);
      return unwrap<TwoFactorSetupResponse>(res.data);
    },
    verify: async (code: string): Promise<TwoFactorEnableResponse> => {
      const res = await api.post(`${BASE}/2fa/verify`, { code });
      return unwrap<TwoFactorEnableResponse>(res.data);
    },
    disable: async (sifre: string): Promise<void> => {
      await api.post(`${BASE}/2fa/disable`, { sifre });
    },
    recoveryCodesCount: async (): Promise<number> => {
      const res = await api.get(`${BASE}/2fa/recovery-codes/count`);
      const data = unwrap<{ unusedCount: number }>(res.data);
      return data.unusedCount;
    },
    regenerateRecoveryCodes: async (): Promise<string[]> => {
      const res = await api.post(`${BASE}/2fa/recovery-codes/regenerate`);
      return unwrap<string[]>(res.data);
    },
  },
};
