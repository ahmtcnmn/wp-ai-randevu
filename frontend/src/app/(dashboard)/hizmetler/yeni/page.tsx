"use client";
import { useEffect, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import {
  serviceApi, serviceCategoryApi, userApi,
  ServiceCategoryResponse,
} from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { useSector } from "@/store/SectorContext";

export default function YeniHizmetPage() {
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [categories, setCategories] = useState<ServiceCategoryResponse[]>([]);
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [form, setForm] = useState({
    ad: "", aciklama: "", sureDakika: 30, fiyat: 200,
    kategoriId: 0, staffIds: [] as number[],
    bufferOnceDk: 0, bufferSonraDk: 0,
    takvimRengi: "#3B82F6",
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([
      serviceCategoryApi.list().catch(() => []),
      userApi.list().catch(() => []),
    ]).then(([c, u]) => {
      setCategories(c);
      setStaff(u.filter((x) => x.rol === "STAFF" || x.rol === "BRANCH_MANAGER" || x.rol === "OWNER"));
    });
  }, []);

  function toggleStaff(id: number) {
    setForm((f) => ({
      ...f,
      staffIds: f.staffIds.includes(id) ? f.staffIds.filter((x) => x !== id) : [...f.staffIds, id],
    }));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.ad.trim()) return setError(`${labels.serviceSingular} adı zorunlu`);
    if (form.sureDakika < 1) return setError("Süre 1 dakikadan az olamaz");
    if (form.fiyat < 0) return setError("Fiyat negatif olamaz");

    setLoading(true);
    try {
      await serviceApi.create({
        ad: form.ad,
        aciklama: form.aciklama || undefined,
        sureDakika: form.sureDakika,
        fiyat: form.fiyat,
        kategoriId: form.kategoriId || null,
        staffIds: form.staffIds.length ? form.staffIds : undefined,
        bufferOnceDk: form.bufferOnceDk,
        bufferSonraDk: form.bufferSonraDk,
        takvimRengi: form.takvimRengi,
      });
      toast.success(`${labels.serviceSingular} oluşturuldu`);
      router.push("/hizmetler");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>
      <Card className="mt-2">
        <CardHeader><CardTitle>{`Yeni ${labels.serviceSingular}`}</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <Input label={`${labels.serviceSingular} Adı`} value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
            <Input label="Açıklama" value={form.aciklama} onChange={(e) => setForm({ ...form, aciklama: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Süre (dakika)" type="number" min={1} value={form.sureDakika} onChange={(e) => setForm({ ...form, sureDakika: Number(e.target.value) })} required />
              <Input label="Fiyat (₺)" type="number" min={0} step="0.01" value={form.fiyat} onChange={(e) => setForm({ ...form, fiyat: Number(e.target.value) })} required />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Kategori (opsiyonel)</label>
              <select
                value={form.kategoriId}
                onChange={(e) => setForm({ ...form, kategoriId: Number(e.target.value) })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              >
                <option value={0}>Kategorisiz</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.ad}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Bu hizmeti verebilen çalışanlar</label>
              <div className="border rounded-md max-h-48 overflow-y-auto divide-y divide-slate-100">
                {staff.map((s) => (
                  <label key={s.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer text-sm">
                    <input type="checkbox" checked={form.staffIds.includes(s.id)} onChange={() => toggleStaff(s.id)} className="rounded text-[var(--color-primary)]" />
                    <span>{s.ad} {s.soyad}</span>
                    <span className="text-xs text-slate-400 ml-auto">{s.rol}</span>
                  </label>
                ))}
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Hiçbiri seçilmezse tüm aktif çalışanlar verebilir kabul edilir.
              </p>
            </div>

            <div className="grid grid-cols-3 gap-3">
              <Input label="Buffer Önce (dk)" type="number" min={0} value={form.bufferOnceDk} onChange={(e) => setForm({ ...form, bufferOnceDk: Number(e.target.value) })} helper="Hazırlık" />
              <Input label="Buffer Sonra (dk)" type="number" min={0} value={form.bufferSonraDk} onChange={(e) => setForm({ ...form, bufferSonraDk: Number(e.target.value) })} helper="Temizlik" />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Takvim Rengi</label>
                <input
                  type="color"
                  value={form.takvimRengi}
                  onChange={(e) => setForm({ ...form, takvimRengi: e.target.value })}
                  className="w-full h-10 border border-slate-300 rounded-md cursor-pointer"
                />
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              <Button variant="secondary" onClick={() => router.back()}>İptal</Button>
              <Button type="submit" loading={loading}>Oluştur</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
