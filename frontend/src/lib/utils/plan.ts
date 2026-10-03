/**
 * Plan ve abonelik durumu için Türkçe etiketler.
 * Backend "STARTER/GROWTH/ENTERPRISE" gibi enum'ları döner, UI'da bu sözlükten okunur.
 */

export const PLAN_LABELS: Record<string, string> = {
  STARTER: "Başlangıç",
  GROWTH: "Büyüme",
  ENTERPRISE: "Kurumsal",
};

export function planLabel(planKey?: string | null): string {
  if (!planKey) return "—";
  return PLAN_LABELS[planKey.toUpperCase()] ?? planKey;
}

export const STATUS_LABELS: Record<string, string> = {
  TRIALING: "Deneme",
  ACTIVE: "Aktif",
  PAST_DUE: "Gecikmeli",
  SUSPENDED: "Askıya Alındı",
  CANCELLED: "İptal Edildi",
  EXPIRED: "Süresi Doldu",
};

export function statusLabel(status?: string | null): string {
  if (!status) return "—";
  return STATUS_LABELS[status.toUpperCase()] ?? status;
}

/** Status için badge variant'ı seç. */
export function statusVariant(status?: string | null): "success" | "warning" | "danger" | "info" | "default" {
  switch ((status || "").toUpperCase()) {
    case "ACTIVE":
      return "success";
    case "TRIALING":
      return "info";
    case "PAST_DUE":
      return "warning";
    case "EXPIRED":
    case "SUSPENDED":
    case "CANCELLED":
      return "danger";
    default:
      return "default";
  }
}
