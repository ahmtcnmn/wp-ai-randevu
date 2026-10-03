"use client";
import { useEffect, useState, useMemo } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { appointmentApi, AppointmentResponse, RandevuDurumu } from "@/lib/api";
import { formatDate, formatTime } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { cn } from "@/lib/utils/cn";
import { useSector } from "@/store/SectorContext";

const FILTERS: { id: "ALL" | RandevuDurumu; label: string }[] = [
  { id: "ALL", label: "Tümü" },
  { id: "BEKLIYOR", label: "Bekleyen" },
  { id: "ONAYLANDI", label: "Onaylı" },
  { id: "TAMAMLANDI", label: "Tamamlanan" },
  { id: "GELMEDI", label: "Gelmeyen" },
  { id: "IPTAL_EDILDI", label: "İptal" },
];

export default function RandevularPage() {
  const { labels } = useSector();
  const [list, setList] = useState<AppointmentResponse[] | null>(null);
  const [filter, setFilter] = useState<"ALL" | RandevuDurumu>("ALL");

  useEffect(() => {
    appointmentApi.list().then(setList).catch(() => setList([]));
  }, []);

  const filtered = useMemo(() => {
    if (!list) return [];
    return list
      .filter((a) => filter === "ALL" || a.durum === filter)
      .sort((a, b) => b.tarihSaat.localeCompare(a.tarihSaat));
  }, [list, filter]);

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.appointmentPlural}</h1>
        <div className="flex gap-2">
          <Link href="/takvim">
            <Button variant="secondary">📅 Takvim Görünümü</Button>
          </Link>
          <Link href="/randevular/yeni">
            <Button>+ Yeni {labels.appointmentSingular}</Button>
          </Link>
        </div>
      </div>

      {/* Filter pills */}
      <div className="flex gap-2 flex-wrap">
        {FILTERS.map((f) => (
          <button
            key={f.id}
            onClick={() => setFilter(f.id)}
            className={cn(
              "px-3 py-1.5 text-sm rounded-full font-medium transition-colors",
              filter === f.id
                ? "bg-[var(--color-primary)] text-white"
                : "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50"
            )}
          >
            {f.label}
          </button>
        ))}
      </div>

      <Card>
        <CardContent className="!p-0">
          {list === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon="📋"
              title={`${labels.appointmentSingular} yok`}
              description={filter === "ALL" ? "Henüz randevu oluşturulmamış" : "Bu durumda randevu yok"}
              action={
                filter === "ALL" ? (
                  <Link href="/randevular/yeni"><Button>+ Yeni {labels.appointmentSingular}</Button></Link>
                ) : undefined
              }
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-6 py-3 text-left font-medium">Tarih</th>
                    <th className="px-6 py-3 text-left font-medium">Saat</th>
                    <th className="px-6 py-3 text-left font-medium">{labels.customerSingular}</th>
                    <th className="px-6 py-3 text-left font-medium hidden md:table-cell">Uzman</th>
                    <th className="px-6 py-3 text-left font-medium hidden lg:table-cell">{labels.serviceSingular}</th>
                    <th className="px-6 py-3 text-right font-medium">Tutar</th>
                    <th className="px-6 py-3 text-left font-medium">Durum</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filtered.map((a) => (
                    <tr key={a.id} className="hover:bg-slate-50">
                      <td className="px-6 py-3 text-sm text-slate-700">
                        <Link href={`/randevular/${a.id}`} className="hover:text-[var(--color-primary)]">
                          {formatDate(a.tarihSaat)}
                        </Link>
                      </td>
                      <td className="px-6 py-3 text-sm font-medium text-slate-900">{formatTime(a.tarihSaat)}</td>
                      <td className="px-6 py-3 text-sm">
                        <Link href={`/randevular/${a.id}`} className="font-medium text-slate-900 hover:text-[var(--color-primary)]">
                          {a.musteriAd}
                        </Link>
                      </td>
                      <td className="px-6 py-3 text-sm text-slate-700 hidden md:table-cell">{a.uzmanAd}</td>
                      <td className="px-6 py-3 text-sm text-slate-600 hidden lg:table-cell">
                        {a.hizmetler.map((h) => h.hizmetAd).join(", ")}
                      </td>
                      <td className="px-6 py-3 text-sm font-medium text-right">{formatMoney(a.genelToplam ?? a.toplamFiyat)}</td>
                      <td className="px-6 py-3"><StatusBadge durum={a.durum} /></td>
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

function StatusBadge({ durum }: { durum: RandevuDurumu }) {
  const map: Record<RandevuDurumu, { v: "success" | "warning" | "danger" | "info" | "default"; label: string }> = {
    BEKLIYOR: { v: "warning", label: "Bekliyor" },
    ONAYLANDI: { v: "info", label: "Onaylı" },
    TAMAMLANDI: { v: "success", label: "Tamamlandı" },
    GELMEDI: { v: "danger", label: "Gelmedi" },
    IPTAL_EDILDI: { v: "default", label: "İptal" },
  };
  return <Badge variant={map[durum].v}>{map[durum].label}</Badge>;
}
