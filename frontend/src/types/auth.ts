export type Role =
  | "OWNER"
  | "ADMIN"
  | "BRANCH_MANAGER"
  | "STAFF"
  | "SUPER_ADMIN"
  | "MUSTERI";

/** Sprint 7 ile genişletilmiş login response */
export interface AuthResponse {
  token: string | null;
  refreshToken: string | null;
  email: string;
  ad?: string;
  soyad?: string;
  rol?: Role;
  tenantId?: number;
  /** 2FA aktif kullanicilar icin login sonrasi dolu gelir */
  requires2fa?: boolean | null;
  /** 2FA dogrulamasi icin 5dk gecerli token */
  tempToken?: string | null;
}

export interface LoginRequest {
  email: string;
  sifre: string;
}

export interface RegisterRequest {
  ad: string;
  soyad: string;
  email: string;
  sifre: string;
  telefon: string;
  businessType?:
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
  isletmeAdi?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  yeniSifre: string;
}

export interface TwoFactorLoginRequest {
  tempToken: string;
  code: string;
}

export interface TwoFactorSetupResponse {
  secret: string;
  qrCodeDataUri: string;
  otpauthUrl: string;
}

export interface TwoFactorEnableResponse {
  recoveryCodes: string[];
}

export interface UserResponse {
  id: number;
  ad: string;
  soyad: string;
  email: string;
  telefon: string;
  rol: Role;
  subeId: number | null;
  subeAd: string | null;
  pozisyon: string | null;
  pozisyonAd: string | null;
  hizmetIds?: number[];
  aktif: boolean;
  emailDogrulandi: boolean;
  sonGirisTarihi: string | null;
  createdAt: string;
}
