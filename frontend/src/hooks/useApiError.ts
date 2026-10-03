import { AxiosError } from "axios";

/** Axios hatasından kullanıcıya gösterilecek mesaj çıkarır. */
export function extractApiError(err: unknown, fallback = "Bir hata oluştu"): string {
  if (err instanceof AxiosError) {
    const data = err.response?.data as { message?: string; success?: boolean } | undefined;
    if (data?.message) return data.message;
    if (err.message) return err.message;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}
