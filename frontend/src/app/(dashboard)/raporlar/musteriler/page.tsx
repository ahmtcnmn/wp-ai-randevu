"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { reportApi, customerApi, CustomerReportResponse, SegmentType } from "@/lib/api";
import { useSector } from "@/store/SectorContext";

const SEGMENT_LABELS: Record<SegmentType, string> = {
  NEW: "Yeni", REGULAR: "Düzenli", LOYAL: "Sadık", VIP: "VIP",
  OCCASIONAL: "Ara sıra", DRIFTING: "Uzaklaşıyor", AT_RISK: "Riskli", LOST: "Kaybedilmiş",
};

const SEGMENT_COLORS: Record<string, string> = {
  NEW: "bg-blue-100 text-blue-700",
  REGULAR: "bg-emerald-100 text-emerald-700",
  LOYAL: "bg-emerald-200 text-emerald-800",
  VIP: "bg-amber-100 text-amber-700",
  OCCASIONAL: "bg-slate-100 text-slate-700",
  DRIFTING: "bg-orange-100 text-orange-700",
  AT_RISK: "bg-red-100 text-red-700",
  LOST: "bg-slate-200 text-slate-700",
};

export default function MusteriRaporPage() {
  const { labels } = useSector();
  const [data, setData] = useState<CustomerReportResponse | null>(null);
  const [segCounts, setSegCounts] = useState<Record<string, number>>({});

  useEffect(() => {
    reportApi.customers().then(setData).catch(() => setData(null));
    customerApi.segmentSummary().then((s) => setSegCounts(s.segmentCounts as any)).catch(() => {});
  }, []);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.customerSingular} Raporu</h1>
        <Link href="/raporlar"><Button variant="ghost" size="sm">← Raporlar</Button></Link>
      </div>

      <Card>
        <CardHeader><CardTitle>Segment Dağılımı</CardTitle></CardHeader>
        <CardContent className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {Object.entries(segCounts).map(([k, v]) => (
            <div key={k} className={`p-3 rounded-lg ${SEGMENT_COLORS[k] || "bg-slate-100 text-slate-700"}`}>
              <div className="text-xs uppercase">{SEGMENT_LABELS[k as SegmentType] || k}</div>
              <div className="text-2xl font-bold mt-1">{v}</div>
            </div>
          ))}
        </CardContent>
      </Card>

      {!data ? <div className="flex justify-center py-12"><Spinner /></div> : (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <Kpi label={`Toplam ${labels.customerSingular}`} value={data.total} />
          <Kpi label={`Yeni ${labels.customerSingular}`} value={data.newCustomers} />
          <Kpi label="Risk Altında" value={data.churnRiskCount} />
          <Kpi label="Kara Listede" value={data.blacklistedCount} />
        </div>
      )}
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: number | string }) {
  return (
    <Card><CardContent>
      <div className="text-xs text-slate-500">{label}</div>
      <div className="text-2xl font-bold mt-1">{value}</div>
    </CardContent></Card>
  );
}
