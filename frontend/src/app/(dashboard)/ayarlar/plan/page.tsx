"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import {
  billingApi,
  PlanResponse,
  SubscriptionResponse,
  QuotaUsageResponse,
  InvoiceResponse,
  CheckoutInitResponse,
} from "@/lib/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Alert } from "@/components/ui/Alert";
import PlanGrid from "@/components/billing/PlanGrid";
import CurrentPlanBanner from "@/components/billing/CurrentPlanBanner";
import QuotaBars from "@/components/billing/QuotaBars";
import InvoiceTable from "@/components/billing/InvoiceTable";
import IyzicoCheckoutModal from "@/components/billing/IyzicoCheckoutModal";
import CancelModal from "@/components/billing/CancelModal";
import { extractApiError } from "@/hooks/useApiError";
import { useToast } from "@/store/ToastContext";

export default function PlanPage() {
  const toast = useToast();
  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [quota, setQuota] = useState<QuotaUsageResponse | null>(null);
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [checkout, setCheckout] = useState<CheckoutInitResponse | null>(null);
  const [showCancel, setShowCancel] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function fetchData() {
    setLoading(true);
    setError(null);
    const [plansRes, subRes, quotaRes, invRes] = await Promise.allSettled([
      billingApi.plans(),
      billingApi.subscription(),
      billingApi.quota(),
      billingApi.invoices(),
    ]);
    if (plansRes.status === "fulfilled") setPlans(plansRes.value);
    else setError(extractApiError(plansRes.reason, "Plan listesi yüklenemedi"));
    if (subRes.status === "fulfilled") setSubscription(subRes.value);
    if (quotaRes.status === "fulfilled") setQuota(quotaRes.value);
    if (invRes.status === "fulfilled") setInvoices(invRes.value);
    setLoading(false);
  }

  useEffect(() => {
    fetchData();
  }, []);

  async function handleSelectPlan(planKey: string) {
    setActionLoading(true);
    setError(null);
    try {
      const res = await billingApi.checkout(planKey);
      setCheckout(res);
    } catch (err) {
      setError(extractApiError(err, "Ödeme formu oluşturulamadı"));
    } finally {
      setActionLoading(false);
    }
  }

  async function handleCancel() {
    setActionLoading(true);
    try {
      await billingApi.cancel();
      setShowCancel(false);
      toast.success("Abonelik iptal edildi");
      await fetchData();
    } catch (err) {
      setError(extractApiError(err, "İptal işlemi başarısız"));
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64 pt-20">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Plan & Abonelik</h1>
          <p className="text-sm text-slate-500 mt-1">
            Aboneliğinizi yönetin ve kota kullanımınızı görün.
          </p>
        </div>
        <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {subscription && (
        <div className="space-y-4">
          <CurrentPlanBanner subscription={subscription as any} />
          {quota && <QuotaBars quota={mapQuotaToLegacy(quota)} />}
          {(subscription.status === "ACTIVE" || subscription.status === "PAST_DUE") && (
            <div className="flex justify-end">
              <button
                onClick={() => setShowCancel(true)}
                className="text-sm text-red-600 hover:text-red-800 underline"
              >
                Aboneliği İptal Et
              </button>
            </div>
          )}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Planlar</CardTitle>
        </CardHeader>
        <CardContent>
          {plans.length === 0 ? (
            <p className="text-sm text-slate-500">Plan listesi yüklenemedi.</p>
          ) : (
            <PlanGrid
              plans={plans as any}
              currentPlanKey={subscription?.plan?.planKey}
              onSelectPlan={handleSelectPlan}
              loading={actionLoading}
            />
          )}
        </CardContent>
      </Card>

      {invoices.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Fatura Geçmişi</CardTitle>
          </CardHeader>
          <CardContent>
            <InvoiceTable invoices={invoices} />
          </CardContent>
        </Card>
      )}

      {checkout && (
        <IyzicoCheckoutModal checkout={checkout} onClose={() => setCheckout(null)} />
      )}

      {showCancel && (
        <CancelModal
          onConfirm={handleCancel}
          onClose={() => setShowCancel(false)}
          loading={actionLoading}
        />
      )}
    </div>
  );
}

// Yeni quota DTO'sunu eski QuotaBars component'inin beklediği formata çevir
function mapQuotaToLegacy(q: QuotaUsageResponse) {
  return {
    appointmentsUsed: q.usedAppointmentsThisMonth,
    appointmentsLimit: q.maxAppointmentsThisMonth === -1 ? null : q.maxAppointmentsThisMonth,
    branchCount: q.usedBranches,
    branchLimit: q.maxBranches === -1 ? null : q.maxBranches,
    staffCount: q.usedStaff,
    staffLimit: q.maxStaff === -1 ? null : q.maxStaff,
  };
}
