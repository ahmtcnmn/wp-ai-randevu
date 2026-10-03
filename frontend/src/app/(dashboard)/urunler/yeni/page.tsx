"use client";
import { useEffect, useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { productApi, serviceApi, ServiceResponseBody } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

export default function YeniUrunPage() {
  const router = useRouter();
  const toast = useToast();
  const [form, setForm] = useState({
    ad: "", aciklama: "", fiyat: 0, stok: 0, kategori: "", aiOneriAktif: true,
  });
  const [services, setServices] = useState<ServiceResponseBody[]>([]);
  const [linkedServiceIds, setLinkedServiceIds] = useState<number[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    serviceApi.list().then((list) => setServices(list.filter((s) => s.aktif))).catch(() => setServices([]));
  }, []);

  function toggleService(id: number) {
    setLinkedServiceIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.ad.trim()) return setError("Ürün adı zorunlu");
    if (form.fiyat < 0) return setError("Fiyat negatif olamaz");
    setLoading(true);
    try {
      await productApi.create({
        ad: form.ad,
        aciklama: form.aciklama || undefined,
        fiyat: form.fiyat,
        stok: form.stok,
        kategori: form.kategori || undefined,
        aiOneriAktif: form.aiOneriAktif,
        hizmetIds: linkedServiceIds,
      });
      toast.success("Ürün oluşturuldu");
      router.push("/urunler");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  // Kategoriye göre hizmetleri grupla — UX iyileşmesi
  const servicesByCategory = services.reduce((acc, s) => {
    const cat = s.kategoriAd || "Diğer";
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(s);
    return acc;
  }, {} as Record<string, ServiceResponseBody[]>);

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>
      <Card className="mt-2">
        <CardHeader><CardTitle>Yeni Ürün</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <Input label="Ürün Adı" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
            <Input label="Açıklama" value={form.aciklama} onChange={(e) => setForm({ ...form, aciklama: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Fiyat (₺)" type="number" min={0} step="0.01" value={form.fiyat} onChange={(e) => setForm({ ...form, fiyat: Number(e.target.value) })} required />
              <Input label="Stok" type="number" min={0} value={form.stok} onChange={(e) => setForm({ ...form, stok: Number(e.target.value) })} />
            </div>
            <Input label="Kategori (opsiyonel)" value={form.kategori} onChange={(e) => setForm({ ...form, kategori: e.target.value })} placeholder="Saç Bakım, Tıraş, ..." />

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">İlgili Hizmetler</label>
              <p className="text-xs text-slate-500 mb-2">
                Bu ürün hangi hizmetlerle birlikte sunulur? Müşteri bu hizmetlerden birini aldığında AI ürünü önerebilir
                ve randevu detayında "Önerilen ürünler" listesinde görünür.
              </p>
              {services.length === 0 ? (
                <p className="text-xs text-slate-400">Önce hizmet eklemeniz gerek.</p>
              ) : (
                <div className="border border-slate-200 rounded-md max-h-64 overflow-y-auto">
                  {Object.entries(servicesByCategory).map(([cat, list]) => (
                    <div key={cat}>
                      <div className="px-3 py-1.5 bg-slate-50 text-xs font-semibold text-slate-600">{cat}</div>
                      {list.map((s) => (
                        <label key={s.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={linkedServiceIds.includes(s.id)}
                            onChange={() => toggleService(s.id)}
                            className="rounded text-[var(--color-primary)]"
                          />
                          <span className="text-sm">{s.ad}</span>
                          <span className="ml-auto text-xs text-slate-500">{s.sureDakika} dk</span>
                        </label>
                      ))}
                    </div>
                  ))}
                </div>
              )}
            </div>

            <label className="flex items-center gap-3 text-sm bg-emerald-50 border border-emerald-200 rounded-md p-3">
              <input type="checkbox" checked={form.aiOneriAktif} onChange={(e) => setForm({ ...form, aiOneriAktif: e.target.checked })} className="rounded text-[var(--color-primary)]" />
              <span>
                <span className="font-medium">AI asistanı bu ürünü önerebilsin</span>
                <span className="block text-xs text-slate-600">
                  Açıksa, WhatsApp ve panel AI'ı bu ürünü ilgili hizmet alan müşterilere önerebilir.
                </span>
              </span>
            </label>

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
