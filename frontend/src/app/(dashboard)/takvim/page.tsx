"use client";
import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { appointmentApi, AppointmentResponse } from "@/lib/api";
import { formatTime } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { cn } from "@/lib/utils/cn";
import { useSector } from "@/store/SectorContext";

function startOfWeek(d: Date): Date {
  const r = new Date(d);
  const day = r.getDay(); // 0=Pazar
  const diff = day === 0 ? -6 : 1 - day; // Pazartesi başla
  r.setDate(r.getDate() + diff);
  r.setHours(0, 0, 0, 0);
  return r;
}

function addDays(d: Date, n: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + n);
  return r;
}

const DAY_NAMES = ["Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"];
const MONTH_NAMES = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];

export default function TakvimPage() {
  const { labels } = useSector();
  const [appointments, setAppointments] = useState<AppointmentResponse[] | null>(null);
  const [weekStart, setWeekStart] = useState(() => startOfWeek(new Date()));

  useEffect(() => {
    appointmentApi.list().then(setAppointments).catch(() => setAppointments([]));
  }, []);

  const weekDays = useMemo(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)), [weekStart]);

  function appointmentsForDay(d: Date) {
    if (!appointments) return [];
    const ymd = d.toISOString().substring(0, 10);
    return appointments
      .filter((a) => a.tarihSaat?.startsWith(ymd))
      .sort((a, b) => a.tarihSaat.localeCompare(b.tarihSaat));
  }

  const today = new Date();
  const isToday = (d: Date) => d.toDateString() === today.toDateString();

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <h1 className="text-2xl font-bold text-slate-900">Takvim</h1>
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={() => setWeekStart(addDays(weekStart, -7))}>← Önceki</Button>
          <Button variant="secondary" size="sm" onClick={() => setWeekStart(startOfWeek(new Date()))}>Bugün</Button>
          <Button variant="secondary" size="sm" onClick={() => setWeekStart(addDays(weekStart, 7))}>Sonraki →</Button>
          <Link href="/randevular/yeni"><Button size="sm">+ Yeni {labels.appointmentSingular}</Button></Link>
        </div>
      </div>

      <div className="text-sm text-slate-500">
        {weekDays[0].getDate()} {MONTH_NAMES[weekDays[0].getMonth()]} – {weekDays[6].getDate()} {MONTH_NAMES[weekDays[6].getMonth()]} {weekDays[6].getFullYear()}
      </div>

      {!appointments ? (
        <div className="flex justify-center py-12"><Spinner /></div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-7 gap-3">
          {weekDays.map((d, i) => {
            const list = appointmentsForDay(d);
            return (
              <Card
                key={d.toISOString()}
                className={cn("min-h-[200px]", isToday(d) && "ring-2 ring-[var(--color-primary)]")}
              >
                <CardHeader className="!p-3">
                  <div className="text-xs text-slate-500">{DAY_NAMES[i]}</div>
                  <div className="text-lg font-semibold text-slate-900">{d.getDate()}</div>
                  <div className="text-xs text-slate-500">{MONTH_NAMES[d.getMonth()]}</div>
                </CardHeader>
                <CardContent className="!p-3 !pt-0 space-y-1">
                  {list.length === 0 ? (
                    <div className="text-xs text-slate-400 text-center py-2">Boş</div>
                  ) : (
                    list.map((a) => (
                      <Link
                        key={a.id}
                        href={`/randevular/${a.id}`}
                        className={cn(
                          "block rounded-md p-2 text-xs hover:opacity-80",
                          a.durum === "TAMAMLANDI" && "bg-green-50 border-l-2 border-l-green-500",
                          a.durum === "ONAYLANDI" && "bg-blue-50 border-l-2 border-l-blue-500",
                          a.durum === "BEKLIYOR" && "bg-amber-50 border-l-2 border-l-amber-500",
                          a.durum === "IPTAL_EDILDI" && "bg-slate-100 border-l-2 border-l-slate-400 line-through opacity-60",
                          a.durum === "GELMEDI" && "bg-red-50 border-l-2 border-l-red-500"
                        )}
                      >
                        <div className="font-semibold">{formatTime(a.tarihSaat)}</div>
                        <div className="truncate">{a.musteriAd}</div>
                        <div className="text-slate-500 truncate">{a.uzmanAd}</div>
                      </Link>
                    ))
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
