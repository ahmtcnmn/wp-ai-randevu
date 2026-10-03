"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { productApi, ProductResponse } from "@/lib/api";
import { formatMoney } from "@/lib/utils/money";

const LOW_STOCK = 5;

export default function UrunlerPage() {
  const [list, setList] = useState<ProductResponse[] | null>(null);

  useEffect(() => {
    productApi.list().then(setList).catch(() => setList([]));
  }, []);

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Ürünler</h1>
        <div className="flex gap-2">
          <Link href="/urunler/raporlar"><Button variant="secondary">Satış Raporları</Button></Link>
          <Link href="/urunler/yeni"><Button>+ Yeni Ürün</Button></Link>
        </div>
      </div>

      <Card>
        <CardContent className="!p-0">
          {list === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : list.length === 0 ? (
            <EmptyState
              icon="🛒"
              title="Ürün yok"
              description="İlk ürününüzü ekleyin"
              action={<Link href="/urunler/yeni"><Button>+ Yeni Ürün</Button></Link>}
            />
          ) : (
            <table className="w-full">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-6 py-3 text-left font-medium">Ürün</th>
                  <th className="px-6 py-3 text-left font-medium hidden md:table-cell">Kategori</th>
                  <th className="px-6 py-3 text-right font-medium">Fiyat</th>
                  <th className="px-6 py-3 text-right font-medium">Stok</th>
                  <th className="px-6 py-3 text-center font-medium">AI Önerisi</th>
                  <th className="px-6 py-3 text-right font-medium">İşlem</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {list.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50">
                    <td className="px-6 py-3">
                      <Link href={`/urunler/${p.id}`} className="font-medium text-slate-900 hover:text-[var(--color-primary)]">
                        {p.ad}
                      </Link>
                      {!p.aktif && <Badge variant="default" className="ml-2">Pasif</Badge>}
                    </td>
                    <td className="px-6 py-3 text-sm text-slate-600 hidden md:table-cell">{p.kategori || "—"}</td>
                    <td className="px-6 py-3 text-right font-medium">{formatMoney(p.fiyat)}</td>
                    <td className="px-6 py-3 text-right">
                      <span className={p.stok === 0 ? "text-red-600 font-bold" : p.stok < LOW_STOCK ? "text-amber-600 font-semibold" : "text-slate-700"}>
                        {p.stok}
                      </span>
                      {p.stok === 0 && <Badge variant="danger" className="ml-1">Tükendi</Badge>}
                      {p.stok > 0 && p.stok < LOW_STOCK && <Badge variant="warning" className="ml-1">Az</Badge>}
                    </td>
                    <td className="px-6 py-3 text-center">{p.aiOneriAktif ? "✓" : "—"}</td>
                    <td className="px-6 py-3 text-right">
                      <Link href={`/urunler/${p.id}`}><Button size="sm" variant="secondary">Detay</Button></Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
