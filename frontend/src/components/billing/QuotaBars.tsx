"use client";
import { QuotaUsageResponse } from "@/types";
import QuotaBar from "./QuotaBar";

interface QuotaBarsProps {
  quota: QuotaUsageResponse;
}

export default function QuotaBars({ quota }: QuotaBarsProps) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 flex flex-col gap-4">
      <h3 className="font-semibold text-gray-900">Kota Kullanımı</h3>
      <QuotaBar label="Aylık Randevu" used={quota.appointmentsUsed} limit={quota.appointmentsLimit} />
      <QuotaBar label="Şube" used={quota.branchCount} limit={quota.branchLimit} />
      <QuotaBar label="Çalışan" used={quota.staffCount} limit={quota.staffLimit} />
    </div>
  );
}
