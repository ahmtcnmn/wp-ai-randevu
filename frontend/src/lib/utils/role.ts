import { Role } from "@/types/auth";

/** Rol bazlı yetki kontrol yardımcıları */

export const ROLE_LABELS: Record<Role, string> = {
  OWNER: "İşletme Sahibi",
  ADMIN: "Yönetici",
  BRANCH_MANAGER: "Şube Müdürü",
  STAFF: "Çalışan",
  SUPER_ADMIN: "Süper Admin",
  MUSTERI: "Müşteri",
};

export const ROLE_BADGE_COLOR: Record<Role, string> = {
  OWNER: "bg-red-100 text-red-700",
  ADMIN: "bg-orange-100 text-orange-700",
  BRANCH_MANAGER: "bg-purple-100 text-purple-700",
  STAFF: "bg-blue-100 text-blue-700",
  SUPER_ADMIN: "bg-black text-white",
  MUSTERI: "bg-slate-100 text-slate-700",
};

export function hasRole(userRole: Role | undefined, ...allowed: Role[]): boolean {
  if (!userRole) return false;
  return allowed.includes(userRole);
}

export function isOwnerOrAdmin(role: Role | undefined): boolean {
  return role === "OWNER" || role === "ADMIN";
}

export function isStaffOnly(role: Role | undefined): boolean {
  return role === "STAFF";
}

export function isSuperAdmin(role: Role | undefined): boolean {
  return role === "SUPER_ADMIN";
}
