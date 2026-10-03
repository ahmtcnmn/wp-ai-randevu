"use client";
import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { appointmentApi, productApi, ProductResponse, AppointmentResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatMoney } from "@/lib/utils/money";

export default function UrunEklePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const randevuId = Number(id);
  const router = useRouter();
  const toast = useToast();

  const [products, setProducts] = useState<ProductResponse[] | null>(null);
  const [recommended, setRecommended] = useState<ProductResponse[]>([]);
  const [search, setSearch] = useState("");
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [adding, setAdding] = useState<number | null>(null);

  useEffect(() => {
    productApi.list()
      .then((list) => setProducts(list.filter((p) => p.aktif)))
      .catch(() => setProducts([]));

    // Randevudaki hizmetlere göre önerilen ürünleri yükle
    appointmentApi.get(randevuId)
      .then((appt: AppointmentResponse) => {
        const hizmetIds = (appt.hizmetler || []).map((h) => h.hizmetId);
        return Promise.all(hizmetIds.map((hid) => productApi.recommendedForHizmet(hid).catch(() => [])));
      })
      .then((lists) => {
        const seen = new Set<number>();
        const flat: ProductResponse[] = [];
        for (const list of lists) {
          for (const p of list) {
            if (!seen.has(p.id)) {
              seen.add(p.id);
              flat.push(p);
            }
          }
        }
        setRecommended(flat);
      })
      .catch(() => setRecommended([]));
  }, [randevuId]);

  const filtered = (products || []).filter((p) =>
    !search || p.ad.toLowerCase().includes(search.toLowerCase()) ||
    (p.kategori && p.kategori.toLowerCase().includes(search.toLowerCase()))
  );

  function getQty(id: number): number {
    return quantities[id] ?? 1;
  }

  async function sell(p: ProductResponse) {
    const adet = getQty(p.id);
    if (adet < 1) return toast.error("Adet en az 1 olmalı");
    if (p.stok < adet) return toast.error("Stok yetersiz");
    setAdding(p.id);
    try {
      await appointmentApi.addProduct(randevuId, { productId: p.id, adet });
      toast.success(`${p.ad} eklendi (×${adet})`);
      // Stok güncelle (optimistic)
      setProducts((prev) => prev ? prev.map((x) => x.id === p.id ? { ...x, stok: x.stok - adet } : x) : prev);
      setQuantities((prev) => ({ ...prev, [p.id]: 1 }));
    } catch (err) {
      toast.error(extractApiError(err));
    } finally {
      setAdding(null);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={() => router.push(`/randevular/${randevuId}`)}>← Randevu detayına dön</Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Ürün Sat</CardTitle>
          <p className="text-sm text-slate-500 mt-1">
            Bu randevuya ürün satışı ekle. Satış randevu ücretine eklenir ve komisyon hesabı otomatik yapılır.
          </p>
        </CardHeader>
        <CardContent className="space-y-3">
          {recommended.length > 0 && (
            <div className="border border-emerald-200 bg-emerald-50 rounded-md p-3">
              <div className="text-xs font-semibold text-emerald-800 mb-2">
                💡 Bu randevunun hizmetleri için önerilen ürünler
              </div>
              <ul className="divide-y divide-emerald-100">
                {recommended.map((p) => (
                  <li key={`rec-${p.id}`} className="py-2 flex items-center gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium">{p.ad}</div>
                      <div className="text-xs text-slate-500">
                        {formatMoney(p.fiyat)} · Stok: {p.stok}
                      </div>
                    </div>
                    <input
                      type="number"
                      min={1}
                      max={p.stok}
                      value={getQty(p.id)}
                      onChange={(e) => setQuantities({ ...quantities, [p.id]: Number(e.target.value) })}
                      className="w-16 px-2 py-1 border border-emerald-300 rounded text-sm text-center"
                    />
                    <Button size="sm" disabled={p.stok === 0} loading={adding === p.id} onClick={() => sell(p)}>
                      Sat
                    </Button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <Input
            placeholder="Ürün veya kategori ara..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          {!products ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : filtered.length === 0 ? (
            <EmptyState icon="📦" title="Ürün bulunamadı" description={products.length === 0 ? "Önce /urunler sayfasından ürün ekleyin." : "Aramayla eşleşen ürün yok."} />
          ) : (
            <ul className="divide-y divide-slate-100">
              {filtered.map((p) => (
                <li key={p.id} className="py-3 flex items-center gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium">{p.ad}</span>
                      {p.kategori && <Badge variant="info">{p.kategori}</Badge>}
                      {p.aiOneriAktif && <Badge variant="success">AI önerir</Badge>}
                      {p.stok === 0 && <Badge variant="danger">Stok yok</Badge>}
                      {p.stok > 0 && p.stok < 5 && <Badge variant="warning">Az stok ({p.stok})</Badge>}
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {formatMoney(p.fiyat)} · Stok: {p.stok}
                    </p>
                  </div>
                  <input
                    type="number"
                    min={1}
                    max={p.stok}
                    value={getQty(p.id)}
                    onChange={(e) => setQuantities({ ...quantities, [p.id]: Number(e.target.value) })}
                    className="w-16 px-2 py-1 border border-slate-300 rounded text-sm text-center"
                  />
                  <Button
                    size="sm"
                    disabled={p.stok === 0}
                    loading={adding === p.id}
                    onClick={() => sell(p)}
                  >
                    Sat
                  </Button>
                </li>
              ))}
            </ul>
          )}

          {products && products.length === 0 && (
            <Alert variant="info">
              Henüz ürün eklenmemiş. <a href="/urunler" className="underline text-[var(--color-primary)]">Ürün ekle →</a>
            </Alert>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
