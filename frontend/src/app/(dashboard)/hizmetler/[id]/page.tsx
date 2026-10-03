"use client";
import { useEffect, useState, use, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import {
  serviceApi, serviceCategoryApi, userApi,
  ServiceCategoryResponse, ServiceResponseBody,
} from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { useSector } from "@/store/SectorContext";

export default function HizmetDuzenlePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const sid = Number(id);
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [original, setOriginal] = useState<ServiceResponseBody | null>(null);
  const [categories, setCategories] = useState<ServiceCategoryResponse[]>([]);
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [form, setForm] = useState({
    ad: "", aciklama: "", sureDakika: 30, fiyat: 200,
    kategoriId: 0, staffIds: [] as number[],
    bufferOnceDk: 0, bufferSonraDk: 0, takvimRengi: "#3B82F6",
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([
      serviceApi.get(sid).catch(() => null),
      serviceCategoryApi.list().catch(() => []),
      userApi.list().catch(() => []),
    ]).then(([s, c, u]) => {
      if (!s) { router.push("/hizmetler"); return; }
      setOriginal(s);
      setCategories(c);
      setStaff(u.filter((x) => x.rol === "STAFF" || x.rol === "BRANCH_MANAGER" || x.rol === "OWNER"));
      setForm({
        ad: s.ad,
        aciklama: s.aciklama || "",
        sureDakika: s.sureDakika,
        fiyat: s.fiyat,
        kategoriId: s.kategoriId || 0,
        staffIds: s.staffIds,
        bufferOnceDk: s.bufferOnceDk,
        bufferSonraDk: s.bufferSonraDk,
        takvimRengi: s.takvimRengi || "#3B82F6",
      });
    });
  }, [sid, router]);

  function toggleStaff(id: number) {
    setForm((f) => ({
      ...f,
      staffIds: f.staffIds.includes(id) ? f.staffIds.filter((x) => x !== id) : [...f.staffIds, id],
    }));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await serviceApi.update(sid, {
        ad: form.ad,
        aciklama: form.aciklama || undefined,
        sureDakika: form.sureDakika,
        fiyat: form.fiyat,
        kategoriId: form.kategoriId || null,
        staffIds: form.staffIds,
        bufferOnceDk: form.bufferOnceDk,
        bufferSonraDk: form.bufferSonraDk,
        takvimRengi: form.takvimRengi,
      });
      toast.success("Güncellendi");
      router.push("/hizmetler");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  if (!original) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>
      <Card className="mt-2">
        <CardHeader><CardTitle>{labels.serviceSingular}i Düzenle</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <Input label={`${labels.serviceSingular} Adı`} value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
            <Input label="Açıklama" value={form.aciklama} onChange={(e) => setForm({ ...form, aciklama: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Süre (dk)" type="number" min={1} value={form.sureDakika} onChange={(e) => setForm({ ...form, sureDakika: Number(e.target.value) })} />
              <Input label="Fiyat (₺)" type="number" min={0} step="0.01" value={form.fiyat} onChange={(e) => setForm({ ...form, fiyat: Number(e.target.value) })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Kategori</label>
              <select value={form.kategoriId} onChange={(e) => setForm({ ...form, kategoriId: Number(e.target.value) })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
                <option value={0}>Kategorisiz</option>
                {categories.map((c) => (<option key={c.id} value={c.id}>{c.ad}</option>))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.staffPlural}</label>
              <div className="border rounded-md max-h-48 overflow-y-auto divide-y divide-slate-100">
                {staff.map((s) => (
                  <label key={s.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer text-sm">
                    <input type="checkbox" checked={form.staffIds.includes(s.id)} onChange={() => toggleStaff(s.id)} className="rounded text-[var(--color-primary)]" />
                    <span>{s.ad} {s.soyad}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <Input label="Buffer Önce (dk)" type="number" min={0} value={form.bufferOnceDk} onChange={(e) => setForm({ ...form, bufferOnceDk: Number(e.target.value) })} />
              <Input label="Buffer Sonra (dk)" type="number" min={0} value={form.bufferSonraDk} onChange={(e) => setForm({ ...form, bufferSonraDk: Number(e.target.value) })} />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Renk</label>
                <input type="color" value={form.takvimRengi} onChange={(e) => setForm({ ...form, takvimRengi: e.target.value })} className="w-full h-10 border border-slate-300 rounded-md cursor-pointer" />
              </div>
            </div>
            <div className="flex gap-2 pt-2">
              <Button variant="secondary" onClick={() => router.back()}>İptal</Button>
              <Button type="submit" loading={loading}>Kaydet</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
