/** Basit alan validation yardımcıları */

export function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

/**
 * TR telefon — kullanıcı 0555..., 555..., +90555..., 0090555... yazabilir.
 * Backend pattern: ^[0-9+\s\-()]{7,20}$ (Sprint dokümanı)
 * Frontend daha sıkı tutuyoruz.
 */
export function isTrPhone(value: string): boolean {
  const digits = value.replace(/[^0-9]/g, "");
  // 10 (5xxxxxxxxx), 11 (05xxxxxxxxx), 12 (905xxxxxxxxx), 14 (0090...) kabul
  return digits.length >= 10 && digits.length <= 14;
}

/** 05XX → 905XX (E.164 Backend'in sevdiği form) */
export function normalizeTrPhone(value: string): string {
  const digits = value.replace(/[^0-9]/g, "");
  if (digits.length === 10 && digits.startsWith("5")) {
    return `90${digits}`;
  }
  if (digits.length === 11 && digits.startsWith("05")) {
    return `9${digits}`;
  }
  if (digits.length === 12 && digits.startsWith("905")) {
    return digits;
  }
  if (digits.length === 14 && digits.startsWith("0090")) {
    return digits.substring(2);
  }
  return digits;
}

/** Şifre — min 8, max 72 (bcrypt sınırı), en az bir harf + bir rakam */
export interface PasswordStrength {
  valid: boolean;
  score: 0 | 1 | 2 | 3 | 4; // 0=invalid, 4=strong
  message?: string;
}

export function checkPassword(value: string): PasswordStrength {
  if (!value) return { valid: false, score: 0, message: "Şifre boş olamaz" };
  if (value.length < 8) return { valid: false, score: 0, message: "En az 8 karakter olmalı" };
  if (value.length > 72) return { valid: false, score: 0, message: "En fazla 72 karakter olabilir" };

  let score: 0 | 1 | 2 | 3 | 4 = 1;
  if (/[a-z]/.test(value) && /[A-Z]/.test(value)) score = 2;
  if (/[0-9]/.test(value)) score = 3;
  if (/[^a-zA-Z0-9]/.test(value) && value.length >= 10) score = 4;

  return { valid: true, score };
}

export function isRequired(value: string | undefined | null): boolean {
  return typeof value === "string" && value.trim().length > 0;
}
