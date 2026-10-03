"use client";
import { useEffect, useState, use, FormEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Avatar } from "@/components/ui/Avatar";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { userApi, commissionApi, branchApi, sectorApi, serviceApi, EarningsSummaryResponse, BranchResponse, PositionInfo, ServiceResponseBody } from "@/lib/api";
import { UserResponse, Role } from "@/types/auth";
import { ROLE_LABELS } from "@/lib/utils/role";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

export default function CalisanDetayPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const sid = Number(id);
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [user, setUser] = useState<UserResponse | null>(null);
  const [summary, setSummary] = useState<EarningsSummaryResponse | null>(null);
  const [form, setForm] = useState({
    ad: "", soyad: "", telefon: "", rol: "STAFF" as Role,
    subeId: "" as string, pozisyon: "" as string,
  });
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [positions, setPositions] = useState<PositionInfo[]>([]);
  const [services, setServices] = useState<ServiceResponseBody[]>([]);
  const [hizmetIds, setHizmetIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function load() {
    try {
      const u = await userApi.get(sid);
      setUser(u);
      setForm({
        ad: u.ad, soyad: u.soyad, telefon: u.telefon, rol: u.rol,
        subeId: u.subeId ? String(u.subeId) : "",
        pozisyon: u.pozisyon || "",
      });
      setHizmetIds(u.hizmetIds || []);
      commissionApi.staffSummary(sid).then(setSummary).catch(() => setSummary(null));
    } catch (err) {
      setError(extractApiError(err));
    }
  }

  useEffect(() => {
    load();
    branchApi.list().then(setBranches).catch(() => setBranches([]));
    sectorApi.positions().then(setPositions).catch(() => setPositions([]));
    serviceApi.list().then((list) => setServices(list.filter((s) => s.aktif))).catch(() => setServices([]));
  }, [sid]);

  function toggleHizmet(id: number) {
    setHizmetIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await userApi.update(sid, {
        ad: form.ad,
        soyad: form.soyad,
        telefon: form.telefon,
        rol: form.rol,
        subeId: form.subeId ? Number(form.subeId) : null,
        pozisyon: form.pozisyon || null,
        hizmetIds,
      });
      toast.success("Güncellendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
    finally { setSaving(false); }
  }

  async function remove() {
    if (!confirm(`${labels.staffSingular} pasifleştirilsin mi?`)) return;
    try {
      await userApi.remove(sid);
      toast.success("Pasifleştirildi");
      router.push("/calisanlar");
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function activate() {
    try {
      await userApi.activate(sid);
      toast.success("Aktifleştirildi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function resendVerification() {
    try {
      await userApi.resendVerification(sid);
      toast.success("Doğrulama maili tekrar gönderildi");
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function markEmailVerified() {
    if (!confirm("Email'i manuel olarak doğrulanmış işaretlemek istediğinize emin misiniz? (Mail erişimi olmayan çalışanlar için.)")) return;
    try {
      await userApi.markEmailVerified(sid);
      toast.success("Email doğrulanmış olarak işaretlendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  if (!user) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader className="flex items-center gap-3 flex-row">
            <Avatar name={`${user.ad} ${user.soyad}`} size="lg" />
            <div className="flex-1 min-w-0">
              <CardTitle>{user.ad} {user.soyad}</CardTitle>
              <p className="text-sm text-slate-500">{user.email}</p>
            </div>
            <Badge variant="info">{ROLE_LABELS[user.rol]}</Badge>
            {!user.aktif && <Badge variant="danger">Pasif</Badge>}
            {!user.emailDogrulandi && <Badge variant="warning">Email Doğrulanmamış</Badge>}
          </CardHeader>
          <CardContent>
            <form onSubmit={save} className="space-y-3">
              {error && <Alert variant="error">{error}</Alert>}
              <div className="grid grid-cols-2 gap-3">
                <Input label="Ad" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} />
                <Input label="Soyad" value={form.soyad} onChange={(e) => setForm({ ...form, soyad: e.target.value })} />
              </div>
              <Input label="Telefon" value={form.telefon} onChange={(e) => setForm({ ...form, telefon: e.target.value })} />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Rol</label>
                <select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value as Role })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
                  <option value="STAFF">{labels.staffSingular}</option>
                  <option value="BRANCH_MANAGER">Şube Müdürü</option>
                  <option value="ADMIN">Yönetici</option>
                  <option value="OWNER">Sahip</option>
                </select>
              </div>
              {positions.length > 0 && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">Pozisyon</label>
                  <select
                    value={form.pozisyon}
                    onChange={(e) => setForm({ ...form, pozisyon: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  >
                    <option value="">— Seçim yok —</option>
                    {positions.map((p) => (
                      <option key={p.code} value={p.code}>{p.name}</option>
                    ))}
                  </select>
                </div>
              )}
              {branches.length > 0 && (
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1.5">Şube</label>
                  <select
                    value={form.subeId}
                    onChange={(e) => setForm({ ...form, subeId: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                  >
                    <option value="">— Şube seçilmedi —</option>
                    {branches.filter((b) => b.aktif).map((b) => (
                      <option key={b.id} value={b.id}>{b.ad}</option>
                    ))}
                  </select>
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Yapabileceği {labels.servicePlural}</label>
                <p className="text-xs text-slate-500 mb-2">
                  Bu çalışanın verebileceği hizmetleri seçin. Randevu oluştururken sadece bu hizmetleri yapabilen çalışanlar listelenir.
                </p>
                {services.length === 0 ? (
                  <p className="text-xs text-slate-400">Önce hizmet eklemelisiniz.</p>
                ) : (
                  <div className="border border-slate-200 rounded-md max-h-48 overflow-y-auto divide-y divide-slate-100">
                    {services.map((s) => (
                      <label key={s.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer text-sm">
                        <input
                          type="checkbox"
                          checked={hizmetIds.includes(s.id)}
                          onChange={() => toggleHizmet(s.id)}
                          className="rounded text-[var(--color-primary)]"
                        />
                        <span className="flex-1">{s.ad}</span>
                        {s.kategoriAd && <span className="text-xs text-slate-400">{s.kategoriAd}</span>}
                      </label>
                    ))}
                  </div>
                )}
              </div>

              {!user.emailDogrulandi && (
                <div className="bg-amber-50 border border-amber-200 rounded-md p-3 space-y-2">
                  <p className="text-xs text-amber-800">
                    ⚠ Bu çalışanın e-postası doğrulanmamış. Doğrulama mailindeki link tıklanana kadar giriş yapamaz.
                  </p>
                  <div className="flex gap-2">
                    <Button size="sm" variant="secondary" type="button" onClick={resendVerification}>
                      Doğrulama mailini tekrar gönder
                    </Button>
                    <Button size="sm" variant="ghost" type="button" onClick={markEmailVerified}>
                      Manuel doğrulanmış işaretle
                    </Button>
                  </div>
                </div>
              )}
              <div className="flex justify-between pt-2">
                {user.aktif ? (
                  <Button variant="danger" type="button" onClick={remove}>Pasifleştir</Button>
                ) : (
                  <Button variant="primary" type="button" onClick={activate}>Aktifleştir</Button>
                )}
                <Button type="submit" loading={saving}>Kaydet</Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Kazanç Özeti</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {summary ? (
              <>
                <Stat label="Brüt" value={formatMoney(summary.totalGross)} />
                <Stat label="Komisyon" value={formatMoney(summary.totalCommission)} />
                <Stat label="Bekleyen" value={formatMoney(summary.pendingCommission)} />
                <Stat label="Tahsil edilen" value={formatMoney(summary.collectedCommission)} />
                <Stat label="Toplam earning" value={summary.totalEarnings} />
              </>
            ) : (
              <div className="text-sm text-slate-500">Veri yok</div>
            )}
            <Link href={`/calisanlar/kazanc?staffId=${user.id}`}>
              <Button fullWidth variant="secondary" className="mt-3">Detayları gör</Button>
            </Link>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex justify-between">
      <span className="text-xs text-slate-500">{label}</span>
      <span className="text-sm font-semibold text-slate-900">{value}</span>
    </div>
  );
}
