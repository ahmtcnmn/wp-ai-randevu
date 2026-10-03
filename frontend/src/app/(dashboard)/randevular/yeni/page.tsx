"use client";
import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import {
  appointmentApi, customerApi, serviceApi, userApi, tenantApi, branchApi,
  CustomerResponse, ServiceResponseBody, CancellationPolicy, BranchResponse,
} from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

function YeniRandevuForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const prefillCustomer = searchParams.get("customerId");
  const toast = useToast();
  const { labels } = useSector();

  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [services, setServices] = useState<ServiceResponseBody[]>([]);
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [loadingData, setLoadingData] = useState(true);

  const [customerId, setCustomerId] = useState<number>(prefillCustomer ? Number(prefillCustomer) : 0);
  const [subeId, setSubeId] = useState<number>(0);
  const [uzmanId, setUzmanId] = useState<number>(0);
  const [hizmetIds, setHizmetIds] = useState<number[]>([]);
  const [tarih, setTarih] = useState("");
  const [saat, setSaat] = useState("");
  const [availability, setAvailability] = useState<string[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [not, setNot] = useState("");
  const [policy, setPolicy] = useState<CancellationPolicy | null>(null);

  useEffect(() => {
    Promise.all([
      customerApi.list().catch(() => [] as CustomerResponse[]),
      userApi.list().catch(() => [] as UserResponse[]),
      serviceApi.list().catch(() => [] as ServiceResponseBody[]),
      tenantApi.getCancellationPolicy().catch(() => null),
      branchApi.list().catch(() => [] as BranchResponse[]),
    ]).then(([c, u, s, p, b]) => {
      setCustomers(c);
      setStaff(u.filter((x) => x.rol === "STAFF" || x.rol === "BRANCH_MANAGER" || x.rol === "OWNER"));
      setServices(s.filter((x) => x.aktif));
      setPolicy(p);
      setBranches(b);
      setLoadingData(false);
    });
  }, []);

  // Şube + hizmet'e göre uzman listesini filtrele:
  // - şube seçildiyse o şubeye atanmış çalışanlar
  // - hizmet seçildiyse o hizmeti yapabilen çalışanlar (StaffService)
  const filteredStaff = staff.filter((s) => {
    if (subeId && s.subeId !== subeId) return false;
    if (hizmetIds.length > 0) {
      const allowedIds = new Set<number>();
      hizmetIds.forEach((hid) => {
        const svc = services.find((x) => x.id === hid);
        svc?.staffIds?.forEach((id) => allowedIds.add(id));
      });
      // Hizmette hiç çalışan tanımlı değilse (eski kayıt) — filtreleme atla
      if (allowedIds.size > 0 && !allowedIds.has(s.id)) return false;
    }
    return true;
  });

  useEffect(() => {
    if (!uzmanId || hizmetIds.length === 0 || !tarih) {
      setAvailability(null);
      return;
    }
    appointmentApi.availability(uzmanId, hizmetIds, tarih)
      .then(setAvailability)
      .catch(() => setAvailability([]));
  }, [uzmanId, hizmetIds, tarih]);

  function toggleHizmet(id: number) {
    setHizmetIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  }

  const totalSure = hizmetIds.reduce((sum, id) => sum + (services.find((s) => s.id === id)?.sureDakika || 0), 0);
  const totalFiyat = hizmetIds.reduce((sum, id) => sum + (services.find((s) => s.id === id)?.fiyat || 0), 0);

  async function onSubmit() {
    setError(null);
    if (!customerId) return setError(`${labels.customerSingular} seçin`);
    if (!uzmanId) return setError(`${labels.staffSingular} seçin`);
    if (hizmetIds.length === 0) return setError("En az 1 hizmet seçin");
    if (!tarih || !saat) return setError("Tarih ve saat seçin");

    setLoading(true);
    try {
      // saat zaten "HH:MM:SS" (8 char) olarak gelebilir — dupe ":00" eklemeyi engelle.
      const normalizedSaat = /^\d{2}:\d{2}$/.test(saat) ? `${saat}:00` : saat;
      const tarihSaat = `${tarih}T${normalizedSaat}`;
      const r = await appointmentApi.create({
        customerId, uzmanId, hizmetIds, tarihSaat, kaynak: "MANUAL", not: not || undefined,
      });
      toast.success("Randevu oluşturuldu");
      router.push(`/randevular/${r.id}`);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  if (loadingData) {
    return <div className="flex justify-center py-12"><Spinner size="lg" /></div>;
  }

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-4">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>

      <Card>
        <CardHeader><CardTitle>{`Yeni ${labels.appointmentSingular}`}</CardTitle></CardHeader>
        <CardContent className="space-y-5">
          {error && <Alert variant="error">{error}</Alert>}

          {/* Customer */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.customerSingular}</label>
            <select
              value={customerId}
              onChange={(e) => setCustomerId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
            >
              <option value={0}>Seçin...</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id} disabled={c.karaListedeMi}>
                  {c.ad} {c.soyad} — {c.telefon}{c.karaListedeMi ? " (Kara liste)" : ""}
                </option>
              ))}
            </select>
            <a href="/musteriler/yeni" className="text-xs text-[var(--color-primary)] hover:underline mt-1 inline-block">
              + Yeni müşteri ekle
            </a>
          </div>

          {/* Şube (sadece >=1 şube varsa) */}
          {branches.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Şube</label>
              <select
                value={subeId}
                onChange={(e) => { setSubeId(Number(e.target.value)); setUzmanId(0); }}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              >
                <option value={0}>Tüm şubeler</option>
                {branches.filter((b) => b.aktif).map((b) => (
                  <option key={b.id} value={b.id}>{b.ad}</option>
                ))}
              </select>
            </div>
          )}

          {/* Uzman */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.staffSingular}</label>
            <select
              value={uzmanId}
              onChange={(e) => setUzmanId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
            >
              <option value={0}>Seçin...</option>
              {filteredStaff.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.ad} {s.soyad}{s.pozisyonAd ? ` — ${s.pozisyonAd}` : ""}
                </option>
              ))}
            </select>
            {hizmetIds.length > 0 && filteredStaff.length === 0 && (
              <p className="text-xs text-amber-600 mt-1">
                Seçili hizmeti yapabilen çalışan bulunamadı. Hizmet ayarlarından çalışan atayın.
              </p>
            )}
          </div>

          {/* Hizmetler */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.servicePlural}</label>
            <div className="space-y-1 max-h-64 overflow-y-auto border border-slate-200 rounded-md">
              {services.map((s) => (
                <label key={s.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={hizmetIds.includes(s.id)}
                    onChange={() => toggleHizmet(s.id)}
                    className="rounded text-[var(--color-primary)]"
                  />
                  <span className="flex-1 text-sm">{s.ad}</span>
                  <span className="text-xs text-slate-500">{s.sureDakika} dk</span>
                  <span className="text-sm font-medium">{formatMoney(s.fiyat)}</span>
                </label>
              ))}
            </div>
            {hizmetIds.length > 0 && (
              <div className="mt-2 text-sm text-slate-700 flex justify-between bg-slate-50 px-3 py-2 rounded-md">
                <span>Toplam: <b>{totalSure} dk</b></span>
                <span className="font-semibold">{formatMoney(totalFiyat)}</span>
              </div>
            )}
          </div>

          {/* Tarih + Saat */}
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Tarih"
              type="date"
              value={tarih}
              onChange={(e) => setTarih(e.target.value)}
              min={new Date().toISOString().substring(0, 10)}
            />
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Saat</label>
              <select
                value={saat}
                onChange={(e) => setSaat(e.target.value)}
                disabled={!availability || availability.length === 0}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent disabled:bg-slate-50"
              >
                <option value="">{!availability ? "Önce uzman/hizmet/tarih seçin" : availability.length === 0 ? "Bu uzman için bu tarihte müsait saat yok" : "Seçin..."}</option>
                {availability?.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Not */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Not (opsiyonel)</label>
            <textarea
              value={not}
              onChange={(e) => setNot(e.target.value)}
              rows={2}
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
              placeholder="Özel istekler..."
            />
          </div>

          {(policy?.saatOnce != null || policy?.mesaj) && (
            <Alert variant="info">
              <div className="text-xs">
                <b>İptal politikası:</b>{" "}
                {policy.mesaj
                  ? policy.mesaj
                  : `Bu randevuyu en geç ${policy.saatOnce} saat öncesinden iptal edebilirsiniz.`}
              </div>
            </Alert>
          )}

          <div className="flex gap-2 pt-4 border-t">
            <Button variant="secondary" onClick={() => router.back()}>İptal</Button>
            <Button onClick={onSubmit} loading={loading}>Randevu Oluştur</Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function YeniRandevuPage() {
  return <Suspense fallback={null}><YeniRandevuForm /></Suspense>;
}
