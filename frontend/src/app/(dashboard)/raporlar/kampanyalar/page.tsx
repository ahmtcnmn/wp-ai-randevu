"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { reportApi, CampaignReportResponse } from "@/lib/api";

export default function KampanyaRaporPage() {
  const [data, setData] = useState<CampaignReportResponse | null>(null);

  useEffect(() => { reportApi.campaigns().then(setData).catch(() => setData(null)); }, []);

  return (
    <div className="p-4 lg:p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Kampanya Raporu</h1>
        <Link href="/raporlar"><Button variant="ghost" size="sm">← Raporlar</Button></Link>
      </div>
      {!data ? <div className="flex justify-center py-12"><Spinner /></div> : (
        <div className="grid grid-cols-2 gap-3">
          <Kpi label="Slot Kampanya Sayısı" value={data.slotCampaignsTotal} />
          <Kpi label="Dolan Slot" value={data.slotCampaignsFilled} />
          <Kpi label="Doluluk Oranı" value={`%${Math.round((data.slotFillRate || 0) * 100)}`} />
          <Kpi label="Segment Kampanya" value={data.segmentCampaignsTotal} />
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
