"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { reportApi, AppointmentReportResponse } from "@/lib/api";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

function ymd(d: Date) { return d.toISOString().substring(0, 10); }

export default function RandevuRaporPage() {
  const { labels } = useSector();
  const today = ymd(new Date());
  const monthAgo = ymd(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));
  const [from, setFrom] = useState(monthAgo);
  const [to, setTo] = useState(today);
  const [data, setData] = useState<AppointmentReportResponse | null>(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try { setData(await reportApi.appointments(from, to)); }
    catch { setData(null); }
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.appointmentSingular} Raporu</h1>
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
          <Card><CardContent>
            <div className="text-xs text-slate-500">Toplam {labels.appointmentSingular}</div>
            <div className="text-3xl font-bold mt-1">{data.total}</div>
          </CardContent></Card>

          <Card><CardHeader><CardTitle>Durum Dağılımı</CardTitle></CardHeader>
            <CardContent className="!p-0">
              <RecordTable rows={data.byStatus} valueLabel="Sayı" />
            </CardContent>
          </Card>

          <Card><CardHeader><CardTitle>{labels.staffSingular} Ciro</CardTitle></CardHeader>
            <CardContent className="!p-0">
              <RecordTable rows={data.revenueByStaff} valueLabel="Ciro" money />
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}

function RecordTable({ rows, valueLabel, money }: { rows: Record<string, number>; valueLabel: string; money?: boolean }) {
  const entries = Object.entries(rows);
  if (entries.length === 0) return <div className="text-sm text-slate-500 py-8 text-center">Veri yok</div>;
  return (
    <table className="w-full">
      <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
        <tr><th className="px-6 py-3 text-left">Etiket</th><th className="px-6 py-3 text-right">{valueLabel}</th></tr>
      </thead>
      <tbody className="divide-y divide-slate-100">
        {entries.map(([k, v]) => (
          <tr key={k}>
            <td className="px-6 py-2 text-sm">{k}</td>
            <td className="px-6 py-2 text-sm text-right font-medium">{money ? formatMoney(v) : v}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
