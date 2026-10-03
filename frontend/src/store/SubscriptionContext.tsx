"use client";
import { createContext, useContext, useEffect, useState, ReactNode, useCallback } from "react";
import { billingApi, SubscriptionResponse } from "@/lib/api";
import { useAuth } from "./AuthContext";

interface Ctx {
  subscription: SubscriptionResponse | null;
  loading: boolean;
  refresh: () => Promise<void>;
}

const SubCtx = createContext<Ctx>({
  subscription: null,
  loading: false,
  refresh: async () => {},
});

export function SubscriptionProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!user) {
      setSubscription(null);
      return;
    }
    setLoading(true);
    try {
      const s = await billingApi.subscription();
      setSubscription(s);
    } catch {
      setSubscription(null);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <SubCtx.Provider value={{ subscription, loading, refresh }}>
      {children}
    </SubCtx.Provider>
  );
}

export function useSubscription() {
  return useContext(SubCtx);
}
