"use client";
import { SubscriptionPlanResponse } from "@/types";
import PlanCard from "./PlanCard";

interface PlanGridProps {
  plans: SubscriptionPlanResponse[];
  currentPlanKey?: string;
  onSelectPlan: (planKey: string) => void;
  loading?: boolean;
}

export default function PlanGrid({ plans, currentPlanKey, onSelectPlan, loading }: PlanGridProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6 py-4">
      {plans.map((plan) => (
        <PlanCard
          key={plan.id}
          plan={plan}
          isCurrentPlan={plan.planKey === currentPlanKey}
          onSelect={onSelectPlan}
          loading={loading}
        />
      ))}
    </div>
  );
}
