"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { reportApi, RevenueReportResponse } from "@/lib/api";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

// Yerel saat dilimine göre YYYY-MM-DD (toISOString UTC döner, gün kayar)
function ymd(d: Date) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

// Bu haftanın Pazartesi'si (TR — hafta Pzt başlar)
function startOfWeek(d: Date): Date {
  const x = new Date(d);
  const day = x.getDay(); // 0=Paz, 1=Pzt...
  const diff = day === 0 ? -6 : 1 - day;
  x.setDate(x.getDate() + diff);
  x.setHours(0, 0, 0, 0);
  return x;
}

// Bu haftanın Pazar'ı
function endOfWeek(d: Date): Date {
  const x = startOfWeek(d);
  x.setDate(x.getDate() + 6);
  return x;
}

export default function CiroRaporPage() {
  const { labels } = useSector();
  const now = new Date();
  const [from, setFrom] = useState(ymd(startOfWeek(now)));
  const [to, setTo] = useState(ymd(endOfWeek(now)));
  const [data, setData] = useState<RevenueReportResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try { setData(await reportApi.revenue(from, to)); }
    catch { setData(null); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, []);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Ciro Raporu</h1>
        <Link href="/raporlar"><Button variant="ghost" size="sm">← Raporlar</Button></Link>
      </div>
      <Card><CardContent className="flex flex-wrap items-end gap-3">
        <Input label="Başlangıç" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        <Input label="Bitiş" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        <Button onClick={load} loading={loading}>Filtrele</Button>
      </CardContent></Card>

      {loading ? <div className="flex justify-center py-12"><Spinner /></div> :
       !data ? null : (
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Card><CardContent>
              <div className="text-xs text-slate-500">Toplam Ciro</div>
              <div className="text-3xl font-bold text-emerald-600 mt-1">{formatMoney(data.totalRevenue)}</div>
            </CardContent></Card>
            <Card><CardContent>
              <div className="text-xs text-slate-500">Ortalama / {labels.appointmentSingular}</div>
              <div className="text-3xl font-bold mt-1">{formatMoney(data.avgRevenuePerAppointment || 0)}</div>
            </CardContent></Card>
          </div>
          <Card><CardHeader><CardTitle>{labels.serviceSingular} Bazlı</CardTitle></CardHeader>
            <CardContent className="!p-0"><RecMoneyTable rows={data.byService} /></CardContent>
          </Card>
          <Card><CardHeader><CardTitle>{labels.staffSingular} Bazlı</CardTitle></CardHeader>
            <CardContent className="!p-0"><RecMoneyTable rows={data.byStaff} /></CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}

function RecMoneyTable({ rows }: { rows: Record<string, number> }) {
  const entries = Object.entries(rows);
  if (entries.length === 0) return <div className="text-sm text-slate-500 py-8 text-center">Veri yok</div>;
  return (
    <table className="w-full">
      <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
        <tr><th className="px-6 py-3 text-left">Etiket</th><th className="px-6 py-3 text-right">Ciro</th></tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {entries.map(([k, v]) => (
          <tr key={k}><td className="px-6 py-2 text-sm">{k}</td><td className="px-6 py-2 text-sm text-right font-medium">{formatMoney(v)}</td></tr>
        ))}
      </tbody>
    </table>
  );
}
