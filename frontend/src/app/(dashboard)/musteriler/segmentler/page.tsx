"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Tabs } from "@/components/ui/Tabs";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { customerApi, CustomerSegmentResponse, SegmentSummaryResponse, SegmentType } from "@/lib/api";
import { useSector } from "@/store/SectorContext";

const SEGMENT_INFO: Record<SegmentType, { label: string; color: "primary" | "success" | "warning" | "danger" | "info" | "default" }> = {
  NEW: { label: "Yeni", color: "info" },
  REGULAR: { label: "Düzenli", color: "default" },
  LOYAL: { label: "Sadık", color: "success" },
  VIP: { label: "VIP", color: "primary" },
  OCCASIONAL: { label: "Ara Sıra", color: "warning" },
  DRIFTING: { label: "Uzaklaşan", color: "warning" },
  AT_RISK: { label: "Risk Altında", color: "danger" },
  LOST: { label: "Kaybedilen", color: "danger" },
};

export default function SegmentlerPage() {
  const { labels } = useSector();
  const [summary, setSummary] = useState<SegmentSummaryResponse | null>(null);
  const [active, setActive] = useState<SegmentType>("VIP");
  const [list, setList] = useState<CustomerSegmentResponse[] | null>(null);

  useEffect(() => {
    customerApi.segmentSummary().then(setSummary).catch(() => setSummary(null));
  }, []);

  useEffect(() => {
    setList(null);
    customerApi.segmentList(active).then(setList).catch(() => setList([]));
  }, [active]);

  const tabs = Object.entries(SEGMENT_INFO).map(([key, { label }]) => ({
    id: key,
    label,
    badge: summary && <Badge variant="default">{summary.segmentCounts?.[key as SegmentType] || 0}</Badge>,
  }));

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.customerSingular} Segmentleri</h1>
        <Link href="/musteriler">
          <Button variant="secondary">Tüm {labels.customerPlural}</Button>
        </Link>
      </div>

      {summary && (
        <Card>
          <CardContent>
            <div className="text-sm text-slate-500">
              Toplam <span className="font-bold text-slate-900">{summary.totalCustomers}</span> müşteri
              {summary.lastCalculatedAt && (
                <> · Son hesaplama: {new Date(summary.lastCalculatedAt).toLocaleDateString("tr-TR")}</>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="!pb-0">
          <Tabs
            tabs={tabs}
            active={active}
            onChange={(id) => setActive(id as SegmentType)}
          />
        </CardHeader>
        <CardContent className="!p-0">
          {list === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : list.length === 0 ? (
            <EmptyState
              icon="👥"
              title="Bu segmentte müşteri yok"
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-6 py-3 text-left font-medium">Ad Soyad</th>
                    <th className="px-6 py-3 text-left font-medium">Telefon</th>
                    <th className="px-6 py-3 text-left font-medium hidden md:table-cell">Son Ziyaret</th>
                    <th className="px-6 py-3 text-right font-medium hidden sm:table-cell">Ziyaret</th>
                    <th className="px-6 py-3 text-right font-medium">Toplam</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {list.map((c) => (
                    <tr key={c.customerId} className="hover:bg-slate-50">
                      <td className="px-6 py-3">
                        <Link
                          href={`/musteriler/${c.customerId}`}
                          className="font-medium text-slate-900 hover:text-[var(--color-primary)]"
                        >
                          {c.customerAd} {c.customerSoyad}
                        </Link>
                      </td>
                      <td className="px-6 py-3 text-sm text-slate-600">{c.customerTelefon}</td>
                      <td className="px-6 py-3 text-sm text-slate-600 hidden md:table-cell">
                        {c.lastVisitDays != null ? `${c.lastVisitDays} gün önce` : "—"}
                      </td>
                      <td className="px-6 py-3 text-sm text-right text-slate-700 hidden sm:table-cell">
                        {c.totalVisits ?? "—"}
                      </td>
                      <td className="px-6 py-3 text-sm text-right font-medium text-slate-900">
                        {c.totalSpent != null ? `${c.totalSpent.toLocaleString("tr-TR")} ₺` : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
