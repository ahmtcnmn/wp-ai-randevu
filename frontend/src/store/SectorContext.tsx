"use client";
import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from "react";
import {
  sectorApi,
  SectorLabelDictionary,
  SectorLabelsResponse,
  BusinessType,
  DEFAULT_SECTOR_LABELS,
} from "@/lib/api";
import { useAuth } from "./AuthContext";

interface SectorContextValue {
  businessType: BusinessType;
  businessTypeName: string;
  labels: SectorLabelDictionary;
  loading: boolean;
  refresh: () => Promise<void>;
}

const DEFAULT_CTX: SectorContextValue = {
  businessType: "OTHER",
  businessTypeName: "İşletme",
  labels: DEFAULT_SECTOR_LABELS,
  loading: true,
  refresh: async () => {},
};

const SectorCtx = createContext<SectorContextValue>(DEFAULT_CTX);

const STORAGE_KEY = "appointflow.sector";

export function SectorProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [state, setState] = useState<SectorContextValue>(() => {
    if (typeof window === "undefined") return DEFAULT_CTX;
    try {
      const cached = localStorage.getItem(STORAGE_KEY);
      if (cached) {
        const parsed = JSON.parse(cached) as SectorLabelsResponse;
        return {
          businessType: parsed.businessType,
          businessTypeName: parsed.businessTypeName,
          labels: parsed.labels,
          loading: false,
          refresh: async () => {},
        };
      }
    } catch {}
    return DEFAULT_CTX;
  });

  const refresh = useCallback(async () => {
    try {
      const resp = await sectorApi.labels();
      if (typeof window !== "undefined") {
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(resp));
        } catch {}
      }
      setState({
        businessType: resp.businessType,
        businessTypeName: resp.businessTypeName,
        labels: resp.labels,
        loading: false,
        refresh,
      });
    } catch {
      setState((prev) => ({ ...prev, loading: false }));
    }
  }, []);

  useEffect(() => {
    if (user) {
      refresh();
    } else if (typeof window !== "undefined") {
      // Logout → cache temizle
      try {
        localStorage.removeItem(STORAGE_KEY);
      } catch {}
      setState({ ...DEFAULT_CTX, loading: false, refresh });
    }
  }, [user, refresh]);

  return <SectorCtx.Provider value={{ ...state, refresh }}>{children}</SectorCtx.Provider>;
}

/** Etiket sözlüğüne kolay erişim. */
export function useSector() {
  return useContext(SectorCtx);
}

/** UI'da etiket basmak için kısayol — örn: `<h1>{t("staffPlural")}</h1>`. */
export function useSectorLabel() {
  const ctx = useContext(SectorCtx);
  return (key: keyof SectorLabelDictionary) => ctx.labels[key];
}
