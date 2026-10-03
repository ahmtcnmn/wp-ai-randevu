/** Tarih/saat format yardımcıları (Türkçe) */

const monthNames = [
  "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
  "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
];

const dayNames = ["Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi"];

function pad(n: number): string {
  return n < 10 ? `0${n}` : `${n}`;
}

/** "15 Tem 2026" */
export function formatDate(d: Date | string): string {
  const date = typeof d === "string" ? new Date(d) : d;
  if (isNaN(date.getTime())) return "-";
  return `${date.getDate()} ${monthNames[date.getMonth()].substring(0, 3)} ${date.getFullYear()}`;
}

/** "15 Temmuz 2026 Çarşamba" */
export function formatDateLong(d: Date | string): string {
  const date = typeof d === "string" ? new Date(d) : d;
  if (isNaN(date.getTime())) return "-";
  return `${date.getDate()} ${monthNames[date.getMonth()]} ${date.getFullYear()} ${dayNames[date.getDay()]}`;
}

/** "14:30" */
export function formatTime(d: Date | string): string {
  const date = typeof d === "string" ? new Date(d) : d;
  if (isNaN(date.getTime())) return "-";
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** "15 Tem 14:30" */
export function formatDateTime(d: Date | string): string {
  return `${formatDate(d)} ${formatTime(d)}`;
}

/** "2 dakika önce", "3 saat önce", "5 gün önce" */
export function formatRelative(d: Date | string): string {
  const date = typeof d === "string" ? new Date(d) : d;
  if (isNaN(date.getTime())) return "-";
  const diff = Date.now() - date.getTime();
  const sec = Math.floor(diff / 1000);
  const min = Math.floor(sec / 60);
  const hour = Math.floor(min / 60);
  const day = Math.floor(hour / 24);
  if (sec < 60) return "az önce";
  if (min < 60) return `${min} dakika önce`;
  if (hour < 24) return `${hour} saat önce`;
  if (day < 7) return `${day} gün önce`;
  return formatDate(date);
}
