"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/store/AuthContext";
import { useToast } from "@/store/ToastContext";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";
import {
  appointmentApi, AppointmentResponse,
  billingApi, QuotaUsageResponse,
  customerApi, SegmentSummaryResponse,
} from "@/lib/api";
import { formatTime } from "@/lib/utils/date";
import { formatMoney, formatLimit } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

function ymd(d: Date) {
  return d.toISOString().substring(0, 10);
}

export default function DashboardPage() {
  const { user, fullUser, refreshMe } = useAuth();
  const toast = useToast();
  const { labels } = useSector();

  const [todayAppointments, setTodayAppointments] = useState<AppointmentResponse[] | null>(null);
  const [quota, setQuota] = useState<QuotaUsageResponse | null>(null);
  const [segments, setSegments] = useState<SegmentSummaryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const today = ymd(new Date());
    Promise.all([
      appointmentApi.list().catch(() => [] as AppointmentResponse[]),
      billingApi.quota().catch(() => null),
      customerApi.segmentSummary().catch(() => null),
    ])
      .then(([apps, q, s]) => {
        setTodayAppointments(
          apps.filter((a) => a.tarihSaat?.startsWith(today)).sort((a, b) =>
            a.tarihSaat.localeCompare(b.tarihSaat)
          )
        );
        setQuota(q);
        setSegments(s);
      })
      .catch((e) => setError(String(e)));
  }, []);

  async function resendVerification() {
    try {
      const { authApi } = await import("@/lib/api");
      await authApi.resendVerification();
      toast.success("Doğrulama maili tekrar gönderildi");
    } catch (e) {
      toast.error("Mail gönderilemedi");
    }
  }

  const todayCiro = (todayAppointments || [])
    .filter((a) => a.durum === "TAMAMLANDI")
    .reduce((s, a) => s + (a.toplamFiyat || 0), 0);

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-slate-900">
          Hoş geldin, {user?.ad || "Kullanıcı"} 👋
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          İşletmenin bugünkü özeti aşağıda.
        </p>
      </header>

      {fullUser && !fullUser.emailDogrulandi && (
        <Alert variant="warning" title="E-postanızı doğrulayın">
          <span>
            <b>{fullUser.email}</b> adresine gönderdiğimiz bağlantıyı tıklayarak
            hesabınızı doğrulayın.
          </span>
          <div className="mt-2 flex gap-2">
            <Button size="sm" variant="secondary" onClick={resendVerification}>
              Maili Tekrar Gönder
            </Button>
            <Button size="sm" variant="ghost" onClick={refreshMe}>
              Durumu Yenile
            </Button>
          </div>
        </Alert>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          label={`Bugünkü ${labels.appointmentPlural}`}
          value={todayAppointments?.length ?? "—"}
          icon="📅"
          color="primary"
        />
        <KpiCard
          label="Bugün Ciro"
          value={formatMoney(todayCiro)}
          icon="💰"
          color="success"
        />
        <KpiCard
          label={`Toplam ${labels.customerSingular}`}
          value={segments?.totalCustomers ?? "—"}
          icon="👥"
          color="info"
        />
        <KpiCard
          label="Aylık Kota"
          value={
            quota
              ? `${quota.usedAppointmentsThisMonth} / ${formatLimit(quota.maxAppointmentsThisMonth)}`
              : "—"
          }
          icon="📊"
          color="warning"
        />
      </div>

      {/* Hızlı Aksiyonlar */}
      <div className="flex flex-wrap gap-2">
        <Link href="/randevular/yeni">
          <Button variant="secondary" size="sm">+ Yeni {labels.appointmentSingular}</Button>
        </Link>
        <Link href="/musteriler/yeni">
          <Button variant="secondary" size="sm">+ Yeni {labels.customerSingular}</Button>
        </Link>
        <Link href="/urunler/hizli-satis">
          <Button variant="primary" size="sm">🛒 Hızlı Ürün Satışı</Button>
        </Link>
      </div>

      {/* Sıradaki Randevu */}
      {(() => {
        if (!todayAppointments || todayAppointments.length === 0) return null;
        const now = new Date();
        const next = todayAppointments.find((a) => {
          const t = new Date(a.tarihSaat);
          return t > now;
        });
        if (!next) return null;
        return (
          <Card className="border-l-4 border-l-[var(--color-primary)]">
            <CardContent className="flex items-center gap-4">
              <div className="text-3xl">⏭️</div>
              <div className="flex-1 min-w-0">
                <div className="text-xs uppercase text-slate-500 font-medium">Sıradaki {labels.appointmentSingular.toLowerCase()}</div>
                <div className="text-lg font-bold text-slate-900">{formatTime(next.tarihSaat)} — {next.musteriAd}</div>
                <div className="text-xs text-slate-500">
                  {next.hizmetler.map((h) => h.hizmetAd).join(", ")} · {next.uzmanAd}
                </div>
              </div>
              <Link href={`/randevular/${next.id}`}>
                <Button size="sm">Detay</Button>
              </Link>
            </CardContent>
          </Card>
        );
      })()}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Bugünkü Randevular */}
        <Card className="lg:col-span-2">
          <CardHeader className="flex items-center justify-between flex-row">
            <CardTitle>{`Bugünkü ${labels.appointmentPlural}`}</CardTitle>
            <Link href="/randevular">
              <Button variant="ghost" size="sm">Tümünü Gör</Button>
            </Link>
          </CardHeader>
          <CardContent className="!p-0">
            {todayAppointments === null ? (
              <div className="flex justify-center py-12">
                <Spinner />
              </div>
            ) : todayAppointments.length === 0 ? (
              <EmptyState
                icon="📅"
                title={`Bugün ${labels.appointmentSingular.toLowerCase()} yok`}
                description={`Yeni bir ${labels.appointmentSingular.toLowerCase()} oluşturmak için aşağıdaki butonu kullanın.`}
                action={
                  <Link href="/randevular/yeni">
                    <Button>{`Yeni ${labels.appointmentSingular}`}</Button>
                  </Link>
                }
              />
            ) : (
              <div className="divide-y divide-slate-100">
                {todayAppointments.map((a) => (
                  <Link
                    key={a.id}
                    href={`/randevular/${a.id}`}
                    className="flex items-center justify-between px-6 py-3 hover:bg-slate-50"
                  >
                    <div className="flex items-center gap-4">
                      <div className="text-sm font-semibold text-slate-900 w-14">
                        {formatTime(a.tarihSaat)}
                      </div>
                      <div>
                        <div className="text-sm font-medium text-slate-900">
                          {a.musteriAd}
                        </div>
                        <div className="text-xs text-slate-500">
                          {a.hizmetler.map((h) => h.hizmetAd).join(", ")} · {a.uzmanAd}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-medium text-slate-900 hidden sm:inline">
                        {formatMoney(a.toplamFiyat)}
                      </span>
                      <StatusBadge durum={a.durum} />
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Müşteri segmentleri */}
        <Card>
          <CardHeader>
            <CardTitle>{`${labels.customerSingular} Segmentleri`}</CardTitle>
          </CardHeader>
          <CardContent className="!p-0">
            {segments ? (
              <div className="divide-y divide-slate-100">
                {Object.entries(segments.segmentCounts || {})
                  .sort((a, b) => b[1] - a[1])
                  .map(([seg, count]) => (
                    <div key={seg} className="flex items-center justify-between px-6 py-2.5">
                      <span className="text-sm text-slate-700 capitalize">
                        {SEGMENT_LABEL[seg] || seg}
                      </span>
                      <span className="text-sm font-semibold text-slate-900">{count}</span>
                    </div>
                  ))}
              </div>
            ) : (
              <div className="flex justify-center py-12">
                <Spinner />
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {error && <Alert variant="error">{error}</Alert>}
    </div>
  );
}

const SEGMENT_LABEL: Record<string, string> = {
  NEW: "Yeni",
  REGULAR: "Düzenli",
  LOYAL: "Sadık",
  VIP: "VIP",
  OCCASIONAL: "Ara sıra gelen",
  DRIFTING: "Uzaklaşan",
  AT_RISK: "Risk altında",
  LOST: "Kaybedilen",
};

function StatusBadge({ durum }: { durum: string }) {
  const map: Record<string, { v: "success" | "warning" | "danger" | "info" | "default"; label: string }> = {
    BEKLIYOR: { v: "warning", label: "Bekliyor" },
    ONAYLANDI: { v: "info", label: "Onaylı" },
    TAMAMLANDI: { v: "success", label: "Tamamlandı" },
    GELMEDI: { v: "danger", label: "Gelmedi" },
    IPTAL_EDILDI: { v: "default", label: "İptal" },
  };
  const conf = map[durum] || { v: "default" as const, label: durum };
  return <Badge variant={conf.v}>{conf.label}</Badge>;
}

interface KpiCardProps {
  label: string;
  value: string | number;
  icon: string;
  color: "primary" | "success" | "warning" | "info";
}

function KpiCard({ label, value, icon, color }: KpiCardProps) {
  const colorMap = {
    primary: "bg-blue-50 text-blue-700",
    success: "bg-green-50 text-green-700",
    warning: "bg-amber-50 text-amber-700",
    info: "bg-purple-50 text-purple-700",
  };
  return (
    <Card>
      <CardContent className="flex items-center gap-4">
        <div className={`w-12 h-12 rounded-lg flex items-center justify-center text-2xl ${colorMap[color]}`}>
          {icon}
        </div>
        <div className="min-w-0">
          <div className="text-xs text-slate-500">{label}</div>
          <div className="text-xl font-bold text-slate-900 truncate">{value}</div>
        </div>
      </CardContent>
    </Card>
  );
}
