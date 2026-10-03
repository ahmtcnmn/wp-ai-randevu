"use client";
import { useEffect, useState, use, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { productApi, serviceApi, ProductResponse, ProductSaleResponse, ServiceResponseBody } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateTime } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

export default function UrunDetayPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const pid = Number(id);
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();
  const [p, setP] = useState<ProductResponse | null>(null);
  const [sales, setSales] = useState<ProductSaleResponse[]>([]);
  const [services, setServices] = useState<ServiceResponseBody[]>([]);
  const [form, setForm] = useState({ ad: "", aciklama: "", fiyat: 0, stok: 0, kategori: "", aiOneriAktif: false });
  const [linkedServiceIds, setLinkedServiceIds] = useState<number[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const prod = await productApi.get(pid);
      setP(prod);
      setForm({
        ad: prod.ad,
        aciklama: prod.aciklama || "",
        fiyat: prod.fiyat,
        stok: prod.stok,
        kategori: prod.kategori || "",
        aiOneriAktif: prod.aiOneriAktif,
      });
      setLinkedServiceIds(prod.hizmetIds || []);
      const h = await productApi.productHistory(pid).catch(() => []);
      setSales(h as ProductSaleResponse[]);
    } catch (err) {
      setError(extractApiError(err));
    }
  }

  function toggleService(id: number) {
    setLinkedServiceIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  }

  useEffect(() => {
    load();
    serviceApi.list().then((list) => setServices(list.filter((s) => s.aktif))).catch(() => setServices([]));
  }, [pid]);

  async function save(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await productApi.update(pid, { ...form, hizmetIds: linkedServiceIds });
      toast.success("Güncellendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
    finally { setSaving(false); }
  }

  const servicesByCategory = services.reduce((acc, s) => {
    const cat = s.kategoriAd || "Diğer";
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(s);
    return acc;
  }, {} as Record<string, ServiceResponseBody[]>);

  async function remove() {
    if (!confirm("Ürünü silmek istediğinize emin misiniz?")) return;
    try {
      await productApi.remove(pid);
      toast.success("Silindi");
      router.push("/urunler");
    } catch (err) { toast.error(extractApiError(err)); }
  }

  if (!p) return <div className="flex justify-center py-12"><Spinner /></div>;

  const totalSold = sales.reduce((s, x) => s + x.adet, 0);
  const totalRev = sales.reduce((s, x) => s + x.toplamTutar, 0);

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader><CardTitle>Ürün Bilgileri</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={save} className="space-y-3">
              {error && <Alert variant="error">{error}</Alert>}
              <Input label="Ad" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} />
              <Input label="Açıklama" value={form.aciklama} onChange={(e) => setForm({ ...form, aciklama: e.target.value })} />
              <div className="grid grid-cols-2 gap-3">
                <Input label="Fiyat" type="number" min={0} step="0.01" value={form.fiyat} onChange={(e) => setForm({ ...form, fiyat: Number(e.target.value) })} />
                <Input label="Stok" type="number" min={0} value={form.stok} onChange={(e) => setForm({ ...form, stok: Number(e.target.value) })} />
              </div>
              <Input label="Kategori" value={form.kategori} onChange={(e) => setForm({ ...form, kategori: e.target.value })} />

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">İlgili Hizmetler</label>
                <p className="text-xs text-slate-500 mb-2">
                  Bu ürün hangi hizmetlerle birlikte sunulur? Randevu detayında ve AI önerilerinde kullanılır.
                </p>
                {services.length === 0 ? (
                  <p className="text-xs text-slate-400">Hizmet bulunamadı.</p>
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
                          </label>
                        ))}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <label className="flex items-center gap-3 text-sm">
                <input type="checkbox" checked={form.aiOneriAktif} onChange={(e) => setForm({ ...form, aiOneriAktif: e.target.checked })} className="rounded text-[var(--color-primary)]" />
                AI önerisi aktif
              </label>
              <div className="flex justify-between pt-2">
                <Button variant="danger" type="button" onClick={remove}>Sil</Button>
                <Button type="submit" loading={saving}>Kaydet</Button>
              </div>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader><CardTitle>Stat</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            <Stat label="Toplam satılan" value={totalSold} />
            <Stat label="Toplam ciro" value={formatMoney(totalRev)} />
            <Stat label="İşlem sayısı" value={sales.length} />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>Satış Geçmişi</CardTitle></CardHeader>
        <CardContent className="!p-0">
          {sales.length === 0 ? (
            <div className="text-center py-8 text-sm text-slate-500">Henüz satış yok</div>
          ) : (
            <table className="w-full">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3 text-left font-medium">Tarih</th>
                  <th className="px-6 py-3 text-left font-medium">{labels.staffSingular}</th>
                  <th className="px-6 py-3 text-right font-medium">Adet</th>
                  <th className="px-6 py-3 text-right font-medium">Tutar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {sales.map((s) => (
                  <tr key={s.id}>
                    <td className="px-6 py-3 text-sm">{formatDateTime(s.createdAt)}</td>
                    <td className="px-6 py-3 text-sm">{s.staffAd || "—"}</td>
                    <td className="px-6 py-3 text-sm text-right">{s.adet}</td>
                    <td className="px-6 py-3 text-sm text-right font-medium">{formatMoney(s.toplamTutar)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
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
