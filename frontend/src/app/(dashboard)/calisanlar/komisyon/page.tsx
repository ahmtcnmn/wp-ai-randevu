"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Tabs } from "@/components/ui/Tabs";
import { Alert } from "@/components/ui/Alert";
import { Modal } from "@/components/ui/Modal";
import { EmptyState } from "@/components/ui/EmptyState";
import { Badge } from "@/components/ui/Badge";
import {
  commissionApi, userApi, productApi,
  CommissionRuleResponse, CommissionScope,
  ProductResponse,
} from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatPercent } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

export default function KomisyonPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [scope, setScope] = useState<CommissionScope>("SERVICE");
  const [rules, setRules] = useState<CommissionRuleResponse[]>([]);
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [products, setProducts] = useState<ProductResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState({
    staffId: 0,
    commissionType: "PERCENTAGE" as "PERCENTAGE" | "SALARY_PLUS_BONUS",
    rate: 0.4,
    bonusThreshold: 0,
    productId: 0,
    productKategori: "",
  });

  async function load() {
    setLoading(true);
    try {
      const [r, u, p] = await Promise.all([
        commissionApi.listRules(scope).catch(() => []),
        userApi.list().catch(() => []),
        productApi.list().catch(() => []),
      ]);
      setRules(r);
      setStaff(u.filter((x) => x.rol !== "MUSTERI" && x.rol !== "SUPER_ADMIN"));
      setProducts(p);
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, [scope]);

  async function create(e: FormEvent) {
    e.preventDefault();
    try {
      await commissionApi.createRule({
        staffId: form.staffId || null,
        commissionType: form.commissionType,
        rate: form.rate,
        bonusThreshold: form.bonusThreshold || undefined,
        scope,
        productId: scope === "PRODUCT" && form.productId ? form.productId : null,
        productKategori: scope === "PRODUCT" && form.productKategori ? form.productKategori : null,
      });
      toast.success("Kural eklendi");
      setShowNew(false);
      setForm({ staffId: 0, commissionType: "PERCENTAGE", rate: 0.4, bonusThreshold: 0, productId: 0, productKategori: "" });
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function remove(id: number) {
    if (!confirm("Kural silinsin mi?")) return;
    try {
      await commissionApi.deleteRule(id);
      toast.success("Silindi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  function staffName(staffId: number | null) {
    if (!staffId) return "Tüm çalışanlar (default)";
    const s = staff.find((x) => x.id === staffId);
    return s ? `${s.ad} ${s.soyad}` : `#${staffId}`;
  }

  // PRODUCT grupları
  const grouped = rules.reduce((acc, r) => {
    let key = "Genel";
    if (r.productId) key = "Ürün bazlı";
    else if (r.productKategori) key = "Kategori bazlı";
    if (!acc[key]) acc[key] = [];
    acc[key].push(r);
    return acc;
  }, {} as Record<string, CommissionRuleResponse[]>);

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Komisyon Kuralları</h1>
        <div className="flex gap-2">
          <Link href="/calisanlar"><Button variant="ghost" size="sm">← {labels.staffPlural}</Button></Link>
          <Button onClick={() => setShowNew(true)}>+ Yeni Kural</Button>
        </div>
      </div>

      <Card>
        <CardHeader className="!pb-0">
          <Tabs
            tabs={[
              { id: "SERVICE", label: `${labels.serviceSingular} Komisyonu` },
              { id: "PRODUCT", label: "Ürün Komisyonu" },
            ]}
            active={scope}
            onChange={(id) => setScope(id as CommissionScope)}
          />
        </CardHeader>
        <CardContent className="!p-0">
          {loading ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : rules.length === 0 ? (
            <EmptyState
              icon={scope === "SERVICE" ? "✂️" : "🛒"}
              title={`${scope === "SERVICE" ? labels.serviceSingular : "Ürün"} komisyon kuralı yok`}
              description={
                scope === "PRODUCT"
                  ? `${labels.staffPlural}a ürün satışından komisyon vermek için kural ekleyin`
                  : `${labels.staffPlural}a ${labels.serviceSingular.toLowerCase()} komisyonu tanımlayın`
              }
              action={<Button onClick={() => setShowNew(true)}>+ Yeni Kural</Button>}
            />
          ) : (
            <div className="divide-y divide-slate-100">
              {Object.entries(grouped).map(([groupName, items]) => (
                <div key={groupName}>
                  <div className="px-6 py-2 bg-slate-50 text-xs uppercase text-slate-500 font-semibold">
                    {groupName}
                  </div>
                  <ul className="divide-y divide-slate-100">
                    {items.map((r) => (
                      <li key={r.id} className="px-6 py-3 flex items-center justify-between">
                        <div>
                          <div className="font-medium text-slate-900 flex items-center gap-2 flex-wrap">
                            <span>{staffName(r.staffId)}</span>
                            {r.productId && (
                              <Badge variant="primary">
                                {products.find((p) => p.id === r.productId)?.ad || `Ürün #${r.productId}`}
                              </Badge>
                            )}
                            {r.productKategori && <Badge variant="info">Kategori: {r.productKategori}</Badge>}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            {r.commissionType === "PERCENTAGE"
                              ? `Yüzde — ${formatPercent(r.rate)}`
                              : `Bonus — ${formatPercent(r.rate)} eşik ${r.bonusThreshold ?? "—"}`}
                          </div>
                        </div>
                        <Button size="sm" variant="danger" onClick={() => remove(r.id)}>Sil</Button>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Modal
        isOpen={showNew}
        onClose={() => setShowNew(false)}
        title={`Yeni ${scope === "SERVICE" ? labels.serviceSingular : "Ürün"} Komisyonu`}
        size="lg"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowNew(false)}>İptal</Button>
            <Button onClick={(e) => create(e as unknown as FormEvent)}>Ekle</Button>
          </>
        }
      >
        <form onSubmit={create} className="space-y-3">
          <Alert variant="info">
            Boş bırakılan alanlar "tümü" anlamına gelir. Backend en spesifik kuralı seçer.
          </Alert>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.staffSingular}</label>
            <select value={form.staffId} onChange={(e) => setForm({ ...form, staffId: Number(e.target.value) })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
              <option value={0}>Tüm çalışanlar (default)</option>
              {staff.map((s) => (<option key={s.id} value={s.id}>{s.ad} {s.soyad}</option>))}
            </select>
          </div>

          {scope === "PRODUCT" && (
            <>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Belirli Ürün (opsiyonel)</label>
                <select value={form.productId} onChange={(e) => setForm({ ...form, productId: Number(e.target.value), productKategori: "" })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
                  <option value={0}>—</option>
                  {products.map((p) => (<option key={p.id} value={p.id}>{p.ad}</option>))}
                </select>
              </div>
              <Input
                label="Veya: Belirli Kategori"
                value={form.productKategori}
                onChange={(e) => setForm({ ...form, productKategori: e.target.value, productId: 0 })}
                placeholder="Bakım, Tıraş, ..."
                disabled={form.productId > 0}
              />
            </>
          )}

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Tip</label>
            <select value={form.commissionType} onChange={(e) => setForm({ ...form, commissionType: e.target.value as typeof form.commissionType })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
              <option value="PERCENTAGE">Yüzde</option>
              <option value="SALARY_PLUS_BONUS">Maaş + Bonus (eşik üstünde)</option>
            </select>
          </div>

          <Input
            label="Oran (0-1)"
            type="number"
            step="0.01"
            min={0}
            max={1}
            value={form.rate}
            onChange={(e) => setForm({ ...form, rate: Number(e.target.value) })}
            helper="0.40 = %40"
          />
          {form.commissionType === "SALARY_PLUS_BONUS" && (
            <Input label="Bonus Eşiği (₺)" type="number" min={0} value={form.bonusThreshold} onChange={(e) => setForm({ ...form, bonusThreshold: Number(e.target.value) })} />
          )}
        </form>
      </Modal>
    </div>
  );
}
