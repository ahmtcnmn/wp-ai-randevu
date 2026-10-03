"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { serviceApi, serviceCategoryApi, ServiceResponseBody, ServiceCategoryResponse } from "@/lib/api";
import { formatMoney } from "@/lib/utils/money";
import { useToast } from "@/store/ToastContext";
import { useSector } from "@/store/SectorContext";

export default function HizmetlerPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [list, setList] = useState<ServiceResponseBody[] | null>(null);
  const [categories, setCategories] = useState<ServiceCategoryResponse[]>([]);

  async function load() {
    try {
      const [s, c] = await Promise.all([serviceApi.list(), serviceCategoryApi.list()]);
      setList(s);
      setCategories(c);
    } catch {
      setList([]);
    }
  }

  useEffect(() => { load(); }, []);

  async function remove(id: number) {
    if (!confirm(`${labels.serviceSingular} pasifleştirilsin mi?`)) return;
    try {
      await serviceApi.remove(id);
      toast.success("Pasifleştirildi");
      await load();
    } catch { toast.error("İşlem başarısız"); }
  }

  async function activate(id: number) {
    try {
      await serviceApi.activate(id);
      toast.success("Aktifleştirildi");
      await load();
    } catch { toast.error("İşlem başarısız"); }
  }

  // Kategori bazında grupla
  const grouped = (list || []).reduce((acc, s) => {
    const key = s.kategoriAd || "Kategorisiz";
    if (!acc[key]) acc[key] = [];
    acc[key].push(s);
    return acc;
  }, {} as Record<string, ServiceResponseBody[]>);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.servicePlural}</h1>
        <div className="flex gap-2">
          <Link href="/hizmetler/kategoriler"><Button variant="secondary">Kategoriler ({categories.length})</Button></Link>
          <Link href="/hizmetler/yeni"><Button>+ Yeni {labels.serviceSingular}</Button></Link>
        </div>
      </div>

      {list === null ? (
        <div className="flex justify-center py-12"><Spinner /></div>
      ) : list.length === 0 ? (
        <Card>
          <CardContent>
            <EmptyState
              icon="✂️"
              title="Henüz hizmet yok"
              description="İlk hizmetinizi ekleyin"
              action={<Link href="/hizmetler/yeni"><Button>+ Yeni {labels.serviceSingular}</Button></Link>}
            />
          </CardContent>
        </Card>
      ) : (
        Object.entries(grouped).map(([cat, items]) => (
          <Card key={cat}>
            <CardHeader><CardTitle>{cat}</CardTitle></CardHeader>
            <CardContent className="!p-0">
              <ul className="divide-y divide-slate-100">
                {items.map((s) => (
                  <li key={s.id} className="px-6 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3 min-w-0 flex-1">
                      <div
                        className="w-3 h-3 rounded-full flex-shrink-0"
                        style={{ background: s.takvimRengi || "#3B82F6" }}
                      />
                      <div className="min-w-0">
                        <div className="font-medium text-slate-900 flex items-center gap-2">
                          {s.ad}
                          {!s.aktif && <Badge variant="default">Pasif</Badge>}
                          {s.staffIds.length > 0 && (
                            <Badge variant="info">{s.staffIds.length} çalışan</Badge>
                          )}
                        </div>
                        {s.aciklama && <div className="text-xs text-slate-500 truncate">{s.aciklama}</div>}
                      </div>
                    </div>
                    <div className="flex items-center gap-3 text-sm">
                      <span className="text-slate-600">{s.sureDakika} dk</span>
                      <span className="font-semibold text-slate-900">{formatMoney(s.fiyat)}</span>
                      <Link href={`/hizmetler/${s.id}`}><Button size="sm" variant="secondary">Düzenle</Button></Link>
                      {s.aktif ? (
                        <Button size="sm" variant="danger" onClick={() => remove(s.id)}>Pasifleştir</Button>
                      ) : (
                        <Button size="sm" variant="primary" onClick={() => activate(s.id)}>Aktifleştir</Button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        ))
      )}
    </div>
  );
}
