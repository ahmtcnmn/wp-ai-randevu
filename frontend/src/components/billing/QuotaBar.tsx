"use client";

interface QuotaBarProps {
  label: string;
  used: number;
  limit: number | null;
}

export default function QuotaBar({ label, used, limit }: QuotaBarProps) {
  const pct = limit !== null ? Math.min(100, (used / limit) * 100) : 0;
  const isWarning = limit !== null && pct >= 80;
  const isCritical = limit !== null && pct >= 95;

  return (
    <div className="flex flex-col gap-1">
      <div className="flex justify-between text-sm">
        <span className="font-medium text-gray-700">{label}</span>
        <span className={`font-mono ${isCritical ? "text-red-600" : isWarning ? "text-yellow-600" : "text-gray-600"}`}>
          {used} / {limit !== null ? limit : "∞"}
        </span>
      </div>
      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
        {limit !== null && (
          <div
            className={`h-full rounded-full transition-all ${
              isCritical ? "bg-red-500" : isWarning ? "bg-yellow-500" : "bg-blue-500"
            }`}
            style={{ width: `${pct}%` }}
          />
        )}
        {limit === null && (
          <div className="h-full rounded-full bg-green-400 w-full opacity-40" />
        )}
      </div>
    </div>
  );
}
