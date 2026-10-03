import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from "axios";
import Cookies from "js-cookie";
import {
  API_URL,
  COOKIE_REFRESH,
  COOKIE_TOKEN,
  REFRESH_EXPIRES_DAYS,
  TOKEN_EXPIRES_DAYS,
} from "../constants";

/**
 * Tek-uçuş refresh — eşzamanlı 401'ler tek refresh çağrısı paylaşır.
 */
let refreshPromise: Promise<string | null> | null = null;

const api = axios.create({
  baseURL: API_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 30000,
});

/** İstek interceptor — JWT ekle */
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = Cookies.get(COOKIE_TOKEN);
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Refresh denemesi — tek instance */
async function tryRefresh(): Promise<string | null> {
  const refreshToken = Cookies.get(COOKIE_REFRESH);
  if (!refreshToken) return null;

  try {
    const resp = await axios.post(
      `${API_URL}/api/v1/auth/refresh`,
      { refreshToken },
      { headers: { "Content-Type": "application/json" } }
    );
    const data = resp.data?.data;
    if (!data?.token) return null;
    Cookies.set(COOKIE_TOKEN, data.token, { expires: TOKEN_EXPIRES_DAYS });
    if (data.refreshToken) {
      Cookies.set(COOKIE_REFRESH, data.refreshToken, { expires: REFRESH_EXPIRES_DAYS });
    }
    return data.token as string;
  } catch {
    return null;
  }
}

/** Refresh'i dışarıdan tetiklenecek path'ler */
const NO_REFRESH_PATHS = [
  "/api/v1/auth/login",
  "/api/v1/auth/login-2fa",
  "/api/v1/auth/register",
  "/api/v1/auth/refresh",
  "/api/v1/auth/forgot-password",
  "/api/v1/auth/reset-password",
];

function shouldAttemptRefresh(config: AxiosRequestConfig | undefined): boolean {
  const url = config?.url || "";
  return !NO_REFRESH_PATHS.some((p) => url.includes(p));
}

/** Yanıt interceptor — 401 → refresh → 1 kere tekrar dene */
api.interceptors.response.use(
  (res) => res,
  async (err: AxiosError) => {
    const config = err.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    // 402 quota: global event, banner'a haber ver
    if (err.response?.status === 402) {
      if (typeof window !== "undefined") {
        window.dispatchEvent(
          new CustomEvent("quotaExceeded", { detail: err.response.data })
        );
      }
      return Promise.reject(err);
    }

    if (
      err.response?.status === 401 &&
      config &&
      !config._retry &&
      shouldAttemptRefresh(config)
    ) {
      config._retry = true;

      // Tek-uçuş — eşzamanlı 401'ler tek refresh paylaşır
      if (!refreshPromise) {
        refreshPromise = tryRefresh().finally(() => {
          // Refresh tamamlandıktan sonra promise'i sıfırla (sonraki 401'ler için)
          setTimeout(() => {
            refreshPromise = null;
          }, 0);
        });
      }

      const newToken = await refreshPromise;
      if (newToken) {
        if (config.headers) {
          config.headers.Authorization = `Bearer ${newToken}`;
        }
        return api(config);
      }

      // Refresh başarısız → cookie temizle, login'e at
      Cookies.remove(COOKIE_TOKEN);
      Cookies.remove(COOKIE_REFRESH);
      if (typeof window !== "undefined") {
        // localStorage user bilgisi de temizlenir
        localStorage.removeItem("user");
        window.location.href = "/login";
      }
    }

    return Promise.reject(err);
  }
);

/** Yardımcı — istek body wrap'ından unwrap (ApiResponse<T> → T) */
export function unwrap<T>(data: unknown): T {
  if (data && typeof data === "object" && "data" in data) {
    return (data as { data: T }).data;
  }
  return data as T;
}

export default api;
