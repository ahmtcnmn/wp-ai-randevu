"use client";
import { createContext, useContext, useState, useEffect, useCallback, ReactNode } from "react";
import api from "@/lib/api";
import { SubscriptionResponse, QuotaUsageResponse, SubscriptionPlanResponse } from "@/types";

interface BillingContextType {
  subscription: SubscriptionResponse | null;
  quota: QuotaUsageResponse | null;
  plans: SubscriptionPlanResponse[];
  isLoading: boolean;
  refetch: () => void;
}

const BillingContext = createContext<BillingContextType | null>(null);

export function BillingProvider({ children }: { children: ReactNode }) {
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [quota, setQuota] = useState<QuotaUsageResponse | null>(null);
  const [plans, setPlans] = useState<SubscriptionPlanResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchAll = useCallback(async () => {
    setIsLoading(true);
    try {
      const [plansRes, subRes, quotaRes] = await Promise.allSettled([
        api.get("/v1/billing/plans"),
        api.get("/v1/billing/subscription"),
        api.get("/v1/billing/quota"),
      ]);

      if (plansRes.status === "fulfilled") setPlans(plansRes.value.data.data);
      if (subRes.status === "fulfilled") setSubscription(subRes.value.data.data);
      if (quotaRes.status === "fulfilled") setQuota(quotaRes.value.data.data);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  return (
    <BillingContext.Provider value={{ subscription, quota, plans, isLoading, refetch: fetchAll }}>
      {children}
    </BillingContext.Provider>
  );
}

export const useBilling = () => {
  const ctx = useContext(BillingContext);
  if (!ctx) throw new Error("useBilling must be used within BillingProvider");
  return ctx;
};
