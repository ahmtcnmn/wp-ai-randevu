"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { Alert } from "@/components/ui/Alert";
import { billingApi, QuotaUsageResponse } from "@/lib/api";
import { formatDate } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";
import { planLabel, statusLabel, statusVariant } from "@/lib/utils/plan";

export default function FinansPage() {
  const { labels } = useSector();
  const [sub, setSub] = useState<any>(null);
  const [quota, setQuota] = useState<QuotaUsageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [errors, setErrors] = useState<{ sub?: string; quota?: string }>({});

  useEffect(() => {
    Promise.all([
      billingApi.subscription().catch((e) => {
        setErrors((p) => ({ ...p, sub: e?.message || "Abonelik yüklenemedi" }));
        return null;
      }),
      billingApi.quota().catch((e) => {
        setErrors((p) => ({ ...p, quota: e?.message || "Kullanım yüklenemedi" }));
        return null;
      }),
    ]).then(([s, q]) => {
      setSub(s);
      setQuota(q);
    }).finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="flex justify-center py-12"><Spinner /></div>;

  // sub objesi farklı şekillerde dönebilir — güvenli şekilde alanlara eriş
  const planKey =
    (sub && (sub.planKey || sub?.plan?.planKey || sub?.plan)) || "—";
  const status = sub?.status || null;
  const endDate = sub?.endDate || sub?.sonrakiOdemeTarihi || sub?.denemeBitisTarihi || null;
  const amount = sub?.amount ?? sub?.plan?.aylikFiyat ?? null;

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <h1 className="text-2xl font-bold text-slate-900">Finans</h1>

      {(errors.sub || errors.quota) && (
        <Alert variant="warning">
          Bazı veriler yüklenemedi: {[errors.sub, errors.quota].filter(Boolean).join(" · ")}
        </Alert>
      )}

      <Card>
        <CardHeader><CardTitle>Mevcut Abonelik</CardTitle></CardHeader>
        <CardContent>
          {!sub ? (
            <p className="text-sm text-slate-500">Aktif abonelik bulunamadı.</p>
          ) : (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="text-lg font-bold">{planLabel(String(planKey))}</span>
                {status && (
                  <Badge variant={statusVariant(status)}>{statusLabel(status)}</Badge>
                )}
              </div>
              {endDate && <Stat label="Bitiş tarihi" value={safeFormatDate(endDate)} />}
              {amount != null && <Stat label="Bedel" value={formatMoney(Number(amount))} />}
            </div>
          )}
        </CardContent>
      </Card>

      {quota && (
        <Card>
          <CardHeader><CardTitle>Kullanım</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            <QuotaRow
              label={`${labels.appointmentSingular} (bu ay)`}
              used={quota.usedAppointmentsThisMonth ?? 0}
              max={quota.maxAppointmentsThisMonth ?? -1}
            />
            <QuotaRow
              label="Şube"
              used={quota.usedBranches ?? 0}
              max={quota.maxBranches ?? -1}
            />
            <QuotaRow
              label={labels.staffSingular}
              used={quota.usedStaff ?? 0}
              max={quota.maxStaff ?? -1}
            />
          </CardContent>
        </Card>
      )}

      <Link href="/ayarlar/plan"><Button>Planı Değiştir</Button></Link>
    </div>
  );
}

function safeFormatDate(d: any): string {
  try {
    return formatDate(d);
  } catch {
    return String(d);
  }
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}

function QuotaRow({ label, used, max }: { label: string; used: number; max: number }) {
  const unlimited = max === -1;
  const pct = unlimited ? 0 : max > 0 ? Math.min(100, (used / max) * 100) : 0;
  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span>{label}</span>
        <span className="font-medium">{used} / {unlimited ? "∞" : max}</span>
      </div>
      {!unlimited && (
        <div className="w-full bg-slate-100 rounded-full h-2">
          <div
            className={`h-2 rounded-full ${pct > 80 ? "bg-red-500" : pct > 60 ? "bg-amber-500" : "bg-emerald-500"}`}
            style={{ width: `${pct}%` }}
          />
        </div>
      )}
    </div>
  );
}
