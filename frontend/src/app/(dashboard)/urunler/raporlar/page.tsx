"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { productApi, ProductSalesSummaryResponse } from "@/lib/api";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

function ymd(d: Date) { return d.toISOString().substring(0, 10); }

export default function UrunRaporlariPage() {
  const { labels } = useSector();
  const today = ymd(new Date());
  const firstOfMonth = ymd(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
  const [from, setFrom] = useState(firstOfMonth);
  const [to, setTo] = useState(today);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<ProductSalesSummaryResponse | null>(null);

  async function load() {
    setLoading(true);
    try {
      setData(await productApi.salesSummary(from, to));
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Ürün Satış Raporu</h1>
        <Link href="/urunler"><Button variant="ghost" size="sm">← Ürünler</Button></Link>
      </div>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3">
          <Input label="Başlangıç" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
          <Input label="Bitiş" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
          <Button onClick={load} loading={loading}>Filtrele</Button>
        </CardContent>
      </Card>

      {loading ? (
        <div className="flex justify-center py-12"><Spinner /></div>
      ) : !data ? null : data.toplamIslem === 0 ? (
        <Card><CardContent><EmptyState icon="📊" title="Bu dönemde ürün satışı yok" /></CardContent></Card>
      ) : (
        <>
          {/* Özet */}
          <div className="grid grid-cols-3 gap-4">
            <Kpi label="Toplam Ciro" value={formatMoney(data.toplamCiro)} />
            <Kpi label="Toplam Adet" value={data.toplamAdet} />
            <Kpi label="İşlem Sayısı" value={data.toplamIslem} />
          </div>

          {/* Ürün bazlı */}
          <Card>
            <CardHeader><CardTitle>Ürün Bazlı</CardTitle></CardHeader>
            <CardContent className="!p-0">
              <table className="w-full">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                  <tr>
                    <th className="px-6 py-3 text-left font-medium">Ürün</th>
                    <th className="px-6 py-3 text-right font-medium">Adet</th>
                    <th className="px-6 py-3 text-right font-medium">Ciro</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.urunBazli.map((u) => (
                    <tr key={u.productId}>
                      <td className="px-6 py-3 text-sm">{u.productAd}</td>
                      <td className="px-6 py-3 text-sm text-right">{u.adet}</td>
                      <td className="px-6 py-3 text-sm text-right font-medium">{formatMoney(u.ciro)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>

          {/* Staff-based */}
          <Card>
            <CardHeader><CardTitle>{labels.staffSingular} Bazlı</CardTitle></CardHeader>
            <CardContent className="!p-0">
              <table className="w-full">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                  <tr>
                    <th className="px-6 py-3 text-left font-medium">{labels.staffSingular}</th>
                    <th className="px-6 py-3 text-right font-medium">İşlem</th>
                    <th className="px-6 py-3 text-right font-medium">Ciro</th>
                    <th className="px-6 py-3 text-right font-medium">Komisyon</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {data.calisanBazli.map((c) => (
                    <tr key={c.staffId}>
                      <td className="px-6 py-3 text-sm">{c.staffAd}</td>
                      <td className="px-6 py-3 text-sm text-right">{c.islem}</td>
                      <td className="px-6 py-3 text-sm text-right">{formatMoney(c.ciro)}</td>
                      <td className="px-6 py-3 text-sm text-right font-medium">{formatMoney(c.toplamKomisyon)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: string | number }) {
  return (
    <Card><CardContent>
      <div className="text-xs text-slate-500">{label}</div>
      <div className="text-2xl font-bold text-slate-900 mt-1">{value}</div>
    </CardContent></Card>
  );
}
