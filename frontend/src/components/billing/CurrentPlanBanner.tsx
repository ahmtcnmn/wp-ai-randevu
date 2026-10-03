"use client";
import { SubscriptionResponse } from "@/types";
import { planLabel } from "@/lib/utils/plan";

interface CurrentPlanBannerProps {
  subscription: SubscriptionResponse;
}

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  TRIALING:  { label: "Deneme Süreci", color: "bg-blue-100 text-blue-800" },
  ACTIVE:    { label: "Aktif", color: "bg-green-100 text-green-800" },
  PAST_DUE:  { label: "Ödeme Bekliyor", color: "bg-yellow-100 text-yellow-800" },
  SUSPENDED: { label: "Askıya Alındı", color: "bg-red-100 text-red-800" },
  CANCELLED: { label: "İptal Edildi", color: "bg-gray-100 text-gray-800" },
  EXPIRED:   { label: "Süresi Doldu", color: "bg-gray-100 text-gray-800" },
};

function daysLeft(dateStr: string | null): number | null {
  if (!dateStr) return null;
  const diff = new Date(dateStr).getTime() - Date.now();
  return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
}

export default function CurrentPlanBanner({ subscription }: CurrentPlanBannerProps) {
  const statusInfo = STATUS_LABELS[subscription.status] ?? { label: subscription.status, color: "bg-gray-100 text-gray-800" };
  const trialDays = subscription.status === "TRIALING" ? daysLeft(subscription.denemeBitisTarihi) : null;
  const nextPayment = subscription.sonrakiOdemeTarihi
    ? new Date(subscription.sonrakiOdemeTarihi).toLocaleDateString("tr-TR")
    : null;

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 flex items-center justify-between gap-4">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-blue-600 flex items-center justify-center text-white font-bold text-sm">
          {subscription.plan.planKey[0]}
        </div>
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-semibold text-gray-900">{planLabel(subscription.plan.planKey)}</h3>
            <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${statusInfo.color}`}>
              {statusInfo.label}
            </span>
          </div>
          <p className="text-sm text-gray-500">
            {trialDays !== null
              ? `Deneme süresi: ${trialDays} gün kaldı`
              : nextPayment
                ? `Sonraki ödeme: ${nextPayment}`
                : subscription.plan.aciklama}
          </p>
        </div>
      </div>
      <div className="text-right">
        <span className="text-xl font-bold text-gray-900">₺{subscription.plan.aylikFiyat}</span>
        <span className="text-gray-500 text-sm">/ay</span>
      </div>
    </div>
  );
}
