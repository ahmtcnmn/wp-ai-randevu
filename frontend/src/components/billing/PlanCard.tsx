"use client";
import { SubscriptionPlanResponse } from "@/types";
import { planLabel } from "@/lib/utils/plan";

interface PlanCardProps {
  plan: SubscriptionPlanResponse;
  isCurrentPlan?: boolean;
  onSelect: (planKey: string) => void;
  loading?: boolean;
}

const PLAN_FEATURES: Record<string, string[]> = {
  STARTER: ["1 şube", "3 çalışan", "50 randevu/ay", "Temel raporlar"],
  GROWTH: ["3 şube", "10 çalışan", "300 randevu/ay", "WhatsApp & AI", "Kampanyalar", "Gelişmiş raporlar"],
  ENTERPRISE: ["Sınırsız şube", "Sınırsız çalışan", "Sınırsız randevu", "WhatsApp & AI", "Kampanyalar", "Tam raporlar"],
};

export default function PlanCard({ plan, isCurrentPlan, onSelect, loading }: PlanCardProps) {
  const features = PLAN_FEATURES[plan.planKey] ?? [];
  const isPopular = plan.planKey === "GROWTH";

  return (
    <div className={`relative rounded-2xl border-2 p-6 flex flex-col gap-4 bg-white shadow-sm transition-all
      ${isCurrentPlan ? "border-blue-500 ring-2 ring-blue-200" : "border-gray-200 hover:border-blue-300"}
      ${isPopular ? "scale-105" : ""}`}>
      {isPopular && (
        <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-blue-600 text-white text-xs font-semibold px-3 py-1 rounded-full">
          Popüler
        </span>
      )}
      {isCurrentPlan && (
        <span className="absolute -top-3 right-4 bg-green-500 text-white text-xs font-semibold px-3 py-1 rounded-full">
          Aktif Plan
        </span>
      )}

      <div>
        <h3 className="text-xl font-bold text-gray-900">{planLabel(plan.planKey)}</h3>
        <p className="text-sm text-gray-500 mt-1">{plan.aciklama}</p>
      </div>

      <div className="flex items-baseline gap-1">
        <span className="text-3xl font-extrabold text-gray-900">₺{plan.aylikFiyat}</span>
        <span className="text-gray-500 text-sm">/ay</span>
      </div>

      <ul className="flex flex-col gap-2 flex-1">
        {features.map((f) => (
          <li key={f} className="flex items-center gap-2 text-sm text-gray-700">
            <span className="text-green-500">✓</span>
            {f}
          </li>
        ))}
      </ul>

      <button
        onClick={() => onSelect(plan.planKey)}
        disabled={isCurrentPlan || loading}
        className={`w-full py-2.5 rounded-xl font-semibold text-sm transition-colors
          ${isCurrentPlan
            ? "bg-gray-100 text-gray-400 cursor-not-allowed"
            : isPopular
              ? "bg-blue-600 hover:bg-blue-700 text-white"
              : "bg-gray-900 hover:bg-gray-800 text-white"
          }`}>
        {isCurrentPlan ? "Mevcut Plan" : loading ? "İşleniyor..." : "Planı Seç"}
      </button>
    </div>
  );
}
