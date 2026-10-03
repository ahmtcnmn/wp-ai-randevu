"use client";
import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Spinner } from "@/components/ui/Spinner";
import { Alert } from "@/components/ui/Alert";
import { adminApi, AdminTenantResponse, sectorApi, SectorTypeInfo } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { planLabel, statusLabel, statusVariant } from "@/lib/utils/plan";
import { formatDate } from "@/lib/utils/date";

const PLAN_OPTIONS = ["STARTER", "GROWTH", "ENTERPRISE"];

export default function TenantDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const tid = Number(id);
  const router = useRouter();
  const toast = useToast();

  const [tenant, setTenant] = useState<AdminTenantResponse | null>(null);
  const [sectors, setSectors] = useState<SectorTypeInfo[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<string | null>(null);

  async function load() {
    try {
      const t = await adminApi.getTenant(tid);
      setTenant(t);
    } catch (err) {
      setError(extractApiError(err));
    }
  }

  useEffect(() => {
    load();
    sectorApi.types().then(setSectors).catch(() => setSectors([]));
  }, [tid]);

  async function changePlan(planKey: string) {
    if (!confirm(`Bu işletmenin planı ${planLabel(planKey)} olarak değiştirilsin mi?`)) return;
    setLoading("plan");
    try {
      await adminApi.overridePlan(tid, planKey);
      toast.success("Plan güncellendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
    finally { setLoading(null); }
  }

  async function changeSector(code: string) {
    setLoading("sector");
    try {
      await adminApi.setBusinessType(tid, code);
      toast.success("Sektör güncellendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
    finally { setLoading(null); }
  }

  async function toggleActive() {
    if (!tenant) return;
    const action = tenant.aktif ? "DEVRE DIŞI BIRAK" : "AKTİVE ET";
    if (!confirm(`Bu işletme ${action}sın mı?`)) return;
    setLoading("active");
    try {
      await adminApi.setActive(tid, !tenant.aktif);
      toast.success("Durum güncellendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
    finally { setLoading(null); }
  }

  if (error) return <div className="p-8"><Alert variant="error">{error}</Alert></div>;
  if (!tenant) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={() => router.push("/sistem/tenants")}>← Tenant Listesi</Button>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-start justify-between gap-3 flex-wrap">
            <div>
              <CardTitle>{tenant.ad}</CardTitle>
              <p className="text-sm text-slate-500 mt-1">slug: <code>{tenant.slug}</code> · ID: {tenant.id}</p>
            </div>
            <div className="flex gap-2 flex-wrap">
              {tenant.subscriptionStatus && (
                <Badge variant={statusVariant(tenant.subscriptionStatus)}>{statusLabel(tenant.subscriptionStatus)}</Badge>
              )}
              <Badge variant={tenant.aktif ? "success" : "danger"}>{tenant.aktif ? "Aktif" : "Pasif"}</Badge>
            </div>
          </div>
        </CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-3 text-sm">
          <Field label="E-posta" value={tenant.email || "—"} />
          <Field label="Telefon" value={tenant.telefon || "—"} />
          <Field label="Plan" value={planLabel(tenant.planKey)} />
          <Field label="Abonelik Bitiş" value={tenant.subscriptionEnd ? formatDate(tenant.subscriptionEnd) : "—"} />
          <Field label="Sektör" value={tenant.businessType || "—"} />
          <Field label="Kayıt Tarihi" value={tenant.createdAt ? formatDate(tenant.createdAt) : "—"} />
        </CardContent>
      </Card>

      <div className="grid lg:grid-cols-3 gap-4">
        <Card>
          <CardHeader><CardTitle>Plan Override</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {PLAN_OPTIONS.map((p) => (
              <Button
                key={p}
                variant={tenant.planKey === p ? "primary" : "secondary"}
                fullWidth
                disabled={tenant.planKey === p || loading === "plan"}
                onClick={() => changePlan(p)}
              >
                {planLabel(p)}
              </Button>
            ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Sektör Değiştir</CardTitle></CardHeader>
          <CardContent>
            <select
              value={tenant.businessType || "OTHER"}
              onChange={(e) => changeSector(e.target.value)}
              disabled={loading === "sector"}
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
            >
              {sectors.map((s) => (
                <option key={s.code} value={s.code}>{s.name}</option>
              ))}
            </select>
            <p className="text-xs text-slate-500 mt-2">
              Sektör değişikliği UI etiketlerini ve mevcut pozisyon enum'larını etkiler.
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Hesap Durumu</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            <Button
              variant={tenant.aktif ? "danger" : "primary"}
              fullWidth
              loading={loading === "active"}
              onClick={toggleActive}
            >
              {tenant.aktif ? "Devre Dışı Bırak" : "Aktive Et"}
            </Button>
            <Link href={`/audit-log?tenantId=${tid}`}>
              <Button variant="ghost" fullWidth>Audit kayıtlarını gör</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs text-slate-500">{label}</div>
      <div className="text-slate-900 font-medium">{value}</div>
    </div>
  );
}
