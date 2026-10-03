/** Para formatı — Türk Lirası */

export function formatMoney(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined) return "-";
  const n = typeof amount === "string" ? parseFloat(amount) : amount;
  if (isNaN(n)) return "-";
  return new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    maximumFractionDigits: 2,
  }).format(n);
}

/** "0,15" → "%15" (komisyon oranı) */
export function formatPercent(rate: number | null | undefined): string {
  if (rate === null || rate === undefined) return "-";
  return `%${Math.round(rate * 100)}`;
}

/** "-1" → "Sınırsız", normal sayı → toLocaleString */
export function formatLimit(limit: number): string {
  if (limit === -1) return "Sınırsız";
  return limit.toLocaleString("tr-TR");
}
