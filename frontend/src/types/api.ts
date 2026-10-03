/**
 * Backend ApiResponse wrapper — Sprint 1+ standardı
 * Tüm endpoint'ler (istisnalar haric — bkz. EXCEPTION_PATHS) bu sarmalayıcıyı kullanır.
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message?: string | null;
  errorCode?: string | null;
}

/**
 * Spring Data Page<T> — sayfalı endpoint'ler döndürür.
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // mevcut sayfa (0-based)
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ApiError {
  success: false;
  message: string;
  errorCode?: string;
}
