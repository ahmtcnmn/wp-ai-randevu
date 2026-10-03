"use client";

const PLAN_COLORS: Record<string, string> = {
  STARTER:    "bg-gray-100 text-gray-700",
  GROWTH:     "bg-blue-100 text-blue-700",
  ENTERPRISE: "bg-purple-100 text-purple-700",
};

interface PlanBadgeProps {
  planKey: string;
  planName: string;
}

export default function PlanBadge({ planKey, planName }: PlanBadgeProps) {
  return (
    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${PLAN_COLORS[planKey] ?? "bg-gray-100 text-gray-700"}`}>
      {planName}
    </span>
  );
}
