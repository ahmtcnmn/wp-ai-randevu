"use client";
import { useEffect, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { userApi, branchApi, sectorApi, serviceApi, BranchResponse, PositionInfo, ServiceResponseBody } from "@/lib/api";
import { Role } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { isEmail, isTrPhone, normalizeTrPhone, checkPassword } from "@/lib/utils/validation";
import { useSector } from "@/store/SectorContext";

export default function YeniCalisanPage() {
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();
  const [form, setForm] = useState({
    ad: "", soyad: "", email: "", sifre: "", telefon: "", rol: "STAFF" as Role,
    subeId: "" as string,
    pozisyon: "" as string,
  });
  const [branches, setBranches] = useState<BranchResponse[]>([]);
  const [positions, setPositions] = useState<PositionInfo[]>([]);
  const [services, setServices] = useState<ServiceResponseBody[]>([]);
  const [hizmetIds, setHizmetIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function toggleHizmet(id: number) {
    setHizmetIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  }

  useEffect(() => {
    branchApi.list().then(setBranches).catch(() => setBranches([]));
    serviceApi.list().then((list) => setServices(list.filter((s) => s.aktif))).catch(() => setServices([]));
    sectorApi.positions().then(setPositions).catch(() => setPositions([]));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.ad || !form.soyad) return setError("Ad/soyad zorunlu");
    if (!isEmail(form.email)) return setError("Geçerli e-posta girin");
    if (!isTrPhone(form.telefon)) return setError("Geçerli telefon girin");
    const pw = checkPassword(form.sifre);
    if (!pw.valid) return setError(pw.message || "Geçersiz şifre");
    setLoading(true);
    try {
      await userApi.create({
        ad: form.ad,
        soyad: form.soyad,
        email: form.email,
        sifre: form.sifre,
        telefon: normalizeTrPhone(form.telefon),
        rol: form.rol,
        subeId: form.subeId ? Number(form.subeId) : null,
        pozisyon: form.pozisyon || null,
        hizmetIds,
      });
      toast.success(`${labels.staffSingular} eklendi. Doğrulama maili gönderildi.`);
      router.push("/calisanlar");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-xl mx-auto">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>
      <Card className="mt-2">
        <CardHeader><CardTitle>{`Yeni ${labels.staffSingular}`}</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <div className="grid grid-cols-2 gap-3">
              <Input label="Ad" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
              <Input label="Soyad" value={form.soyad} onChange={(e) => setForm({ ...form, soyad: e.target.value })} required />
            </div>
            <Input label="E-posta" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            <Input label="Telefon" value={form.telefon} onChange={(e) => setForm({ ...form, telefon: e.target.value })} required placeholder="05XX XXX XX XX" />
            <Input
              label="Geçici Şifre"
              type="password"
              value={form.sifre}
              onChange={(e) => setForm({ ...form, sifre: e.target.value })}
              required
              helper="En az 8 karakter — büyük/küçük harf, rakam içermeli"
            />
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Rol</label>
              <select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value as Role })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
                <option value="STAFF">{labels.staffSingular} (Staff)</option>
                <option value="BRANCH_MANAGER">Şube Müdürü</option>
                <option value="ADMIN">Yönetici (Admin)</option>
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
                <p className="text-xs text-slate-500 mt-1">İşletme içi unvan. Opsiyonel.</p>
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

            {services.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Yapabileceği {labels.servicePlural}</label>
                <p className="text-xs text-slate-500 mb-2">
                  Hiç seçim yapmazsanız tüm hizmetleri yapabilir kabul edilir.
                </p>
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
              </div>
            )}

            <div className="flex gap-2">
              <Button variant="secondary" onClick={() => router.back()}>İptal</Button>
              <Button type="submit" loading={loading}>Oluştur</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
