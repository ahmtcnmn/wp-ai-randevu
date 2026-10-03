"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Badge } from "@/components/ui/Badge";
import { Modal } from "@/components/ui/Modal";
import {
  commissionApi, userApi,
  EarningPeriodResponse,
} from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDate } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

export default function KazancPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [periods, setPeriods] = useState<EarningPeriodResponse[]>([]);
  const [staff, setStaff] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState({
    staffId: 0,
    periodStart: new Date().toISOString().substring(0, 10),
    periodEnd: new Date().toISOString().substring(0, 10),
  });

  async function load() {
    setLoading(true);
    try {
      const [p, u] = await Promise.all([
        commissionApi.listPeriods().catch(() => []),
        userApi.list().catch(() => []),
      ]);
      setPeriods(p);
      setStaff(u.filter((x) => x.rol !== "MUSTERI" && x.rol !== "SUPER_ADMIN"));
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  async function create(e: FormEvent) {
    e.preventDefault();
    if (!form.staffId) return toast.error(`${labels.staffSingular} seçin`);
    try {
      await commissionApi.createPeriod(form);
      toast.success("Dönem oluşturuldu");
      setShowNew(false);
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function collect(id: number) {
    if (!confirm("Dönem kapatılıp komisyon tahsil edilsin mi?")) return;
    try {
      await commissionApi.collectPeriod(id);
      toast.success("Tahsil edildi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  function staffName(id: number) {
    return staff.find((s) => s.id === id);
  }

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Kazanç Dönemleri</h1>
        <div className="flex gap-2">
          <Link href="/calisanlar"><Button variant="ghost" size="sm">← {labels.staffPlural}</Button></Link>
          <Button onClick={() => setShowNew(true)}>+ Yeni Dönem</Button>
        </div>
      </div>

      <Card>
        <CardContent className="!p-0">
          {loading ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : periods.length === 0 ? (
            <EmptyState icon="📊" title="Dönem yok" description="Komisyon tahsil dönemi oluşturun" />
          ) : (
            <table className="w-full">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3 text-left font-medium">{labels.staffSingular}</th>
                  <th className="px-6 py-3 text-left font-medium">Dönem</th>
                  <th className="px-6 py-3 text-right font-medium">Brüt</th>
                  <th className="px-6 py-3 text-right font-medium">Komisyon</th>
                  <th className="px-6 py-3 text-right font-medium">Net</th>
                  <th className="px-6 py-3 text-center font-medium">Durum</th>
                  <th className="px-6 py-3 text-right font-medium">İşlem</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {periods.map((p) => {
                  const s = staffName(p.staffId);
                  return (
                    <tr key={p.id}>
                      <td className="px-6 py-3 text-sm font-medium">
                        {s ? `${s.ad} ${s.soyad}` : `#${p.staffId}`}
                      </td>
                      <td className="px-6 py-3 text-sm">{formatDate(p.periodStart)} – {formatDate(p.periodEnd)}</td>
                      <td className="px-6 py-3 text-sm text-right">{formatMoney(p.totalGross)}</td>
                      <td className="px-6 py-3 text-sm text-right font-medium">{formatMoney(p.totalCommission)}</td>
                      <td className="px-6 py-3 text-sm text-right">{formatMoney(p.totalNet)}</td>
                      <td className="px-6 py-3 text-center">
                        {p.status === "COLLECTED" ? <Badge variant="success">Tahsil edildi</Badge> : <Badge variant="warning">Açık</Badge>}
                      </td>
                      <td className="px-6 py-3 text-right">
                        {p.status !== "COLLECTED" && (
                          <Button size="sm" onClick={() => collect(p.id)}>Tahsil Et</Button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      <Modal
        isOpen={showNew}
        onClose={() => setShowNew(false)}
        title="Yeni Dönem"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowNew(false)}>İptal</Button>
            <Button onClick={(e) => create(e as unknown as FormEvent)}>Oluştur</Button>
          </>
        }
      >
        <form onSubmit={create} className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.staffSingular}</label>
            <select value={form.staffId} onChange={(e) => setForm({ ...form, staffId: Number(e.target.value) })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
              <option value={0}>Seçin...</option>
              {staff.map((s) => (<option key={s.id} value={s.id}>{s.ad} {s.soyad}</option>))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Başlangıç" type="date" value={form.periodStart} onChange={(e) => setForm({ ...form, periodStart: e.target.value })} />
            <Input label="Bitiş" type="date" value={form.periodEnd} onChange={(e) => setForm({ ...form, periodEnd: e.target.value })} />
          </div>
        </form>
      </Modal>
    </div>
  );
}
