"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { Modal } from "@/components/ui/Modal";
import { EmptyState } from "@/components/ui/EmptyState";
import { adminApi, AdminTenantResponse, sectorApi, SectorTypeInfo } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDate } from "@/lib/utils/date";
import { planLabel, statusLabel } from "@/lib/utils/plan";

const SECTOR_NAMES: Record<string, string> = {
  BARBER: "Berber",
  HAIR_SALON: "Kuaför",
  DENTAL_CLINIC: "Diş Kliniği",
  BEAUTY_SALON: "Güzellik",
  SPA: "Spa",
  GYM: "Spor",
  VETERINARY: "Veteriner",
  PET_GROOMING: "Pet Kuaförü",
  NAIL_SALON: "Tırnak",
  TATTOO: "Dövme",
  OTHER: "Diğer",
};

export default function TenantsPage() {
  const toast = useToast();
  const [list, setList] = useState<AdminTenantResponse[] | null>(null);
  const [allSectors, setAllSectors] = useState<SectorTypeInfo[]>([]);
  const [search, setSearch] = useState("");
  const [sectorFilter, setSectorFilter] = useState<string>("");

  async function load() {
    try { setList(await adminApi.listTenants()); } catch { setList([]); }
  }
  useEffect(() => {
    load();
    sectorApi.types().then(setAllSectors).catch(() => {});
  }, []);

  const [planModal, setPlanModal] = useState<{ tenant: AdminTenantResponse; selectedPlan: string } | null>(null);
  const [sectorModal, setSectorModal] = useState<{ tenant: AdminTenantResponse; selectedSector: string } | null>(null);

  async function confirmPlanChange() {
    if (!planModal) return;
    try {
      await adminApi.overridePlan(planModal.tenant.id, planModal.selectedPlan);
      toast.success("Plan güncellendi");
      setPlanModal(null);
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function confirmSectorChange() {
    if (!sectorModal) return;
    try {
      await adminApi.setBusinessType(sectorModal.tenant.id, sectorModal.selectedSector);
      toast.success("Sektör güncellendi");
      setSectorModal(null);
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function toggleActive(t: AdminTenantResponse) {
    try {
      await adminApi.setActive(t.id, !t.aktif);
      toast.success(t.aktif ? "Pasifleştirildi" : "Aktifleştirildi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  const filtered = (list || []).filter((t) => {
    if (search) {
      const q = search.toLowerCase();
      if (!t.ad?.toLowerCase().includes(q) && !t.email?.toLowerCase().includes(q)) return false;
    }
    if (sectorFilter && t.businessType !== sectorFilter) return false;
    return true;
  });

  const sectorCounts = (list || []).reduce((acc, t) => {
    const key = t.businessType || "OTHER";
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Tenantlar</h1>
        <Link href="/sistem"><Button variant="ghost" size="sm">← Sistem</Button></Link>
      </div>

      {list && list.length > 0 && (
        <Card>
          <CardContent>
            <div className="flex items-center gap-2 flex-wrap text-xs">
              <span className="text-slate-500 font-medium">Sektör dağılımı:</span>
              {Object.entries(sectorCounts)
                .sort((a, b) => b[1] - a[1])
                .map(([type, count]) => (
                  <button
                    key={type}
                    onClick={() => setSectorFilter(sectorFilter === type ? "" : type)}
                    className={`px-2 py-1 rounded border ${
                      sectorFilter === type
                        ? "bg-[var(--color-primary)] text-white border-[var(--color-primary)]"
                        : "bg-white text-slate-700 border-slate-200 hover:bg-slate-50"
                    }`}
                  >
                    {SECTOR_NAMES[type] || type} ({count})
                  </button>
                ))}
              {sectorFilter && (
                <button
                  onClick={() => setSectorFilter("")}
                  className="px-2 py-1 text-red-600 hover:underline"
                >
                  ✕ filtreyi kaldır
                </button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      <Input placeholder="Ara (işletme adı veya e-posta)..." value={search} onChange={(e) => setSearch(e.target.value)} />

      <Card>
        <CardContent className="!p-0">
          {!list ? <div className="flex justify-center py-12"><Spinner /></div> :
           filtered.length === 0 ? <EmptyState icon="🏢" title="Tenant yok" /> : (
            <table className="w-full">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3 text-left">İşletme</th>
                  <th className="px-6 py-3 text-left">Sektör</th>
                  <th className="px-6 py-3 text-left">Email</th>
                  <th className="px-6 py-3 text-left">Plan</th>
                  <th className="px-6 py-3 text-left">Bitiş</th>
                  <th className="px-6 py-3 text-center">Aktif</th>
                  <th className="px-6 py-3 text-right">İşlem</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50">
                    <td className="px-6 py-2 text-sm font-medium">
                      <Link href={`/sistem/tenants/${t.id}`} className="hover:underline text-[var(--color-primary)]">{t.ad}</Link>
                    </td>
                    <td className="px-6 py-2">
                      <Badge variant="info">{SECTOR_NAMES[t.businessType || "OTHER"]}</Badge>
                    </td>
                    <td className="px-6 py-2 text-sm">{t.email || "—"}</td>
                    <td className="px-6 py-2">
                      <Badge variant="primary">{planLabel(t.planKey)}</Badge>
                      {t.subscriptionStatus && <Badge variant="info" className="ml-1">{statusLabel(t.subscriptionStatus)}</Badge>}
                    </td>
                    <td className="px-6 py-2 text-sm">{t.subscriptionEnd ? formatDate(t.subscriptionEnd) : "—"}</td>
                    <td className="px-6 py-2 text-center">{t.aktif ? "✓" : "✕"}</td>
                    <td className="px-6 py-2 text-right space-x-1">
                      <Button size="sm" variant="secondary" onClick={() => setSectorModal({ tenant: t, selectedSector: t.businessType || "OTHER" })}>Sektör</Button>
                      <Button size="sm" variant="secondary" onClick={() => setPlanModal({ tenant: t, selectedPlan: t.planKey || "STARTER" })}>Plan</Button>
                      <Button size="sm" variant={t.aktif ? "danger" : "primary"} onClick={() => toggleActive(t)}>
                        {t.aktif ? "Pasifleştir" : "Aktif"}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      <Modal
        isOpen={!!planModal}
        onClose={() => setPlanModal(null)}
        title={`Plan Değiştir — ${planModal?.tenant.ad ?? ""}`}
        footer={<>
          <Button variant="secondary" onClick={() => setPlanModal(null)}>İptal</Button>
          <Button onClick={confirmPlanChange}>Onayla</Button>
        </>}
      >
        {planModal && (
          <div className="space-y-3">
            <p className="text-sm text-slate-600">
              Mevcut plan: <b>{planLabel(planModal.tenant.planKey)}</b>
            </p>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Yeni Plan</label>
              <select
                value={planModal.selectedPlan}
                onChange={(e) => setPlanModal({ ...planModal, selectedPlan: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              >
                <option value="STARTER">Başlangıç (STARTER)</option>
                <option value="GROWTH">Büyüme (GROWTH)</option>
                <option value="ENTERPRISE">Kurumsal (ENTERPRISE)</option>
              </select>
            </div>
            <p className="text-xs text-amber-700 bg-amber-50 p-2 rounded">
              ⚠ Bu işlem mevcut aboneliği iptal edip yeni plan ile aktif abonelik oluşturur.
            </p>
          </div>
        )}
      </Modal>

      <Modal
        isOpen={!!sectorModal}
        onClose={() => setSectorModal(null)}
        title={`Sektör Değiştir — ${sectorModal?.tenant.ad ?? ""}`}
        footer={<>
          <Button variant="secondary" onClick={() => setSectorModal(null)}>İptal</Button>
          <Button onClick={confirmSectorChange}>Onayla</Button>
        </>}
      >
        {sectorModal && (
          <div className="space-y-3">
            <p className="text-sm text-slate-600">
              Mevcut sektör: <b>{SECTOR_NAMES[sectorModal.tenant.businessType || "OTHER"]}</b>
            </p>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Yeni Sektör</label>
              <select
                value={sectorModal.selectedSector}
                onChange={(e) => setSectorModal({ ...sectorModal, selectedSector: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              >
                {allSectors.map((s) => (
                  <option key={s.code} value={s.code}>{s.name}</option>
                ))}
              </select>
            </div>
            <p className="text-xs text-amber-700 bg-amber-50 p-2 rounded">
              ⚠ Sektör değişikliği UI etiketlerini ve mevcut pozisyon enum listesini değiştirir.
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
}
