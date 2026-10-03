"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { Alert } from "@/components/ui/Alert";
import { EmptyState } from "@/components/ui/EmptyState";
import {
  productApi, customerApi, userApi,
  ProductResponse, CustomerResponse,
} from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

export default function HizliUrunSatisPage() {
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [products, setProducts] = useState<ProductResponse[] | null>(null);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [search, setSearch] = useState("");
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [customerId, setCustomerId] = useState<number | "">("");
  const [staffId, setStaffId] = useState<number | "">("");
  const [selling, setSelling] = useState<number | null>(null);

  useEffect(() => {
    Promise.all([
      productApi.list().catch(() => [] as ProductResponse[]),
      customerApi.list().catch(() => [] as CustomerResponse[]),
      userApi.list().catch(() => [] as UserResponse[]),
    ]).then(([p, c, u]) => {
      setProducts(p.filter((x) => x.aktif));
      setCustomers(c);
      setStaff(u.filter((x) => x.aktif && (x.rol === "STAFF" || x.rol === "BRANCH_MANAGER" || x.rol === "OWNER")));
    });
  }, []);

  function getQty(id: number): number {
    return quantities[id] ?? 1;
  }

  const filtered = (products || []).filter((p) =>
    !search || p.ad.toLowerCase().includes(search.toLowerCase()) ||
    (p.kategori && p.kategori.toLowerCase().includes(search.toLowerCase()))
  );

  async function sell(p: ProductResponse) {
    const adet = getQty(p.id);
    if (adet < 1) return toast.error("Adet en az 1 olmalı");
    if (p.stok < adet) return toast.error("Stok yetersiz");
    setSelling(p.id);
    try {
      await productApi.sellStandalone({
        productId: p.id,
        adet,
        customerId: customerId ? Number(customerId) : null,
        staffId: staffId ? Number(staffId) : null,
      });
      toast.success(`${p.ad} satıldı (×${adet}) — ${formatMoney(p.fiyat * adet)}`);
      setProducts((prev) => prev ? prev.map((x) => x.id === p.id ? { ...x, stok: x.stok - adet } : x) : prev);
      setQuantities((prev) => ({ ...prev, [p.id]: 1 }));
    } catch (err) {
      toast.error(extractApiError(err));
    } finally {
      setSelling(null);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Hızlı Ürün Satışı</h1>
        <Button variant="ghost" size="sm" onClick={() => router.push("/dashboard")}>← Dashboard</Button>
      </div>

      <Alert variant="info">
        Randevu olmadan ürün satabilirsiniz. Müşteri ve {labels.staffSingular} seçimi opsiyoneldir
        — {labels.staffSingular} seçerseniz komisyon hesabı yapılır.
      </Alert>

      <Card>
        <CardHeader><CardTitle>Satış Bilgileri</CardTitle></CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.customerSingular} (opsiyonel)</label>
            <select
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : "")}
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
            >
              <option value="">— Müşterisiz —</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>{c.ad} {c.soyad} — {c.telefon}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.staffSingular} (opsiyonel — komisyon için)</label>
            <select
              value={staffId}
              onChange={(e) => setStaffId(e.target.value ? Number(e.target.value) : "")}
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
            >
              <option value="">— Çalışansız —</option>
              {staff.map((s) => (
                <option key={s.id} value={s.id}>{s.ad} {s.soyad}</option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Ürünler</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <Input
            placeholder="Ürün veya kategori ara..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          {!products ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon="📦"
              title="Ürün yok"
              description={products.length === 0 ? "Önce /urunler sayfasından ürün ekleyin." : "Aramayla eşleşen ürün yok."}
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {filtered.map((p) => (
                <li key={p.id} className="py-3 flex items-center gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium">{p.ad}</span>
                      {p.kategori && <Badge variant="info">{p.kategori}</Badge>}
                      {p.stok === 0 && <Badge variant="danger">Stok yok</Badge>}
                      {p.stok > 0 && p.stok < 5 && <Badge variant="warning">Az stok ({p.stok})</Badge>}
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">{formatMoney(p.fiyat)} · Stok: {p.stok}</p>
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
                    loading={selling === p.id}
                    onClick={() => sell(p)}
                  >
                    Sat
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
