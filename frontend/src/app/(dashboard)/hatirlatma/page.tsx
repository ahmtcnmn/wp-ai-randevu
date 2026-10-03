"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { reminderApi, AppointmentReminderResponse, ReminderStatsResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateTime } from "@/lib/utils/date";

const STATUS_LABELS: Record<string, { label: string; variant: "success" | "warning" | "danger" | "default" | "info" }> = {
  PENDING: { label: "Bekliyor", variant: "warning" },
  SENT: { label: "Gönderildi", variant: "success" },
  RESPONDED_YES: { label: "Onaylandı", variant: "success" },
  RESPONDED_NO: { label: "Reddedildi", variant: "danger" },
  SNOOZED: { label: "Ertelendi", variant: "info" },
  CANCELLED: { label: "İptal", variant: "default" },
};

export default function HatirlatmalarPage() {
  const toast = useToast();
  const [list, setList] = useState<AppointmentReminderResponse[] | null>(null);
  const [stats, setStats] = useState<ReminderStatsResponse | null>(null);

  async function load() {
    try {
      const [l, s] = await Promise.all([
        reminderApi.list().catch(() => []),
        reminderApi.stats().catch(() => null),
      ]);
      setList(l); setStats(s);
    } catch { setList([]); }
  }
  useEffect(() => { load(); }, []);

  async function resend(id: number) {
    try {
      await reminderApi.resend(id);
      toast.success("Tekrar gönderildi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Hatırlatmalar</h1>
        <Link href="/hatirlatma/sablonlar"><Button variant="secondary">Şablonlar</Button></Link>
      </div>

      {stats && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <Kpi label="Toplam" value={stats.toplam} />
          <Kpi label="Gönderildi" value={stats.gonderildi} />
          <Kpi label="Beklemede" value={stats.beklemede} />
          <Kpi label="Yanıt: Evet" value={stats.yanitlandiEvet} />
        </div>
      )}

      <Card>
        <CardHeader><CardTitle>Hatırlatma Geçmişi</CardTitle></CardHeader>
        <CardContent className="!p-0">
          {!list ? <div className="flex justify-center py-12"><Spinner /></div> :
           list.length === 0 ? <EmptyState icon="🔔" title="Hatırlatma yok" /> : (
            <ul className="divide-y divide-slate-100">
              {list.map((r) => {
                const st = STATUS_LABELS[r.status] || { label: r.status, variant: "default" as const };
                return (
                  <li key={r.id} className="px-6 py-3 flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-medium">{r.musteriAd}</span>
                        <Badge variant="info">{r.kanal}</Badge>
                        <Badge variant={st.variant}>{st.label}</Badge>
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">{r.mesaj}</p>
                      <p className="text-xs text-slate-400 mt-0.5">Plan: {formatDateTime(r.gonderimTarihi)}</p>
                    </div>
                    {(r.status === "PENDING" || r.status === "SENT") && (
                      <Button size="sm" onClick={() => resend(r.id)}>Tekrar Gönder</Button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function Kpi({ label, value }: { label: string; value: number }) {
  return <Card><CardContent>
    <div className="text-xs text-slate-500">{label}</div>
    <div className="text-2xl font-bold mt-1">{value}</div>
  </CardContent></Card>;
}
