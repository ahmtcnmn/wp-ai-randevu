"use client";
import Link from "next/link";
import { useSubscription } from "@/store/SubscriptionContext";
import { planLabel } from "@/lib/utils/plan";

/**
 * Trial sürerken üst banner — kalan gün sayısı + planı seçme CTA.
 * EXPIRED durumda kırmızı uyarı, ACTIVE'de banner gösterilmez.
 */
export function TrialBanner() {
  const { subscription } = useSubscription();
  if (!subscription) return null;

  // ACTIVE → banner yok
  if (subscription.status === "ACTIVE") return null;

  // EXPIRED / SUSPENDED → kırmızı, bloklu mesaj
  if (subscription.status === "EXPIRED" || subscription.status === "SUSPENDED" || subscription.status === "CANCELLED") {
    return (
      <div className="bg-red-600 text-white px-4 py-2.5 text-sm text-center">
        <span className="font-semibold">⛔ Deneme süreniz doldu.</span>{" "}
        Devam etmek için bir plan seçin.{" "}
        <Link href="/finans" className="underline font-semibold ml-1">
          Plan seç →
        </Link>
      </div>
    );
  }

  // TRIALING — gün sayısı uyarı
  if (subscription.status === "TRIALING") {
    const days = subscription.trialDaysRemaining ?? 0;
    const isLastDays = days <= 3;
    const bg = isLastDays ? "bg-amber-500 text-white" : "bg-blue-50 text-blue-900 border-b border-blue-200";

    return (
      <div className={`${bg} px-4 py-2.5 text-sm text-center`}>
        <span className="font-semibold">
          🎁 {planLabel(subscription.plan?.planKey)} planı ücretsiz deneme
        </span>{" "}
        — <span className="font-bold">{days} gün</span> kaldı.{" "}
        <Link href="/finans" className="underline font-semibold ml-1">
          Plan seç →
        </Link>
      </div>
    );
  }

  return null;
}
