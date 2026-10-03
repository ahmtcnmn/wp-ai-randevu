"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { serviceCategoryApi, ServiceCategoryResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { useSector } from "@/store/SectorContext";

export default function HizmetKategorileriPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [list, setList] = useState<ServiceCategoryResponse[] | null>(null);
  const [edit, setEdit] = useState<ServiceCategoryResponse | null>(null);
  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState({ ad: "", aciklama: "", takvimRengi: "#3B82F6" });

  async function load() {
    try { setList(await serviceCategoryApi.list()); } catch { setList([]); }
  }

  useEffect(() => { load(); }, []);

  async function save(e: FormEvent) {
    e.preventDefault();
    try {
      if (edit) {
        await serviceCategoryApi.update(edit.id, form);
        toast.success("Güncellendi");
      } else {
        await serviceCategoryApi.create(form);
        toast.success("Oluşturuldu");
      }
      setEdit(null);
      setShowNew(false);
      setForm({ ad: "", aciklama: "", takvimRengi: "#3B82F6" });
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function remove(id: number) {
    if (!confirm("Kategoriyi pasifleştirelim mi?")) return;
    try {
      await serviceCategoryApi.remove(id);
      toast.success("Pasifleştirildi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  function openEdit(c: ServiceCategoryResponse) {
    setEdit(c);
    setForm({ ad: c.ad, aciklama: c.aciklama || "", takvimRengi: c.takvimRengi || "#3B82F6" });
  }

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.serviceSingular} Kategorileri</h1>
        <div className="flex gap-2">
          <Link href="/hizmetler"><Button variant="ghost" size="sm">← {labels.servicePlural}</Button></Link>
          <Button onClick={() => { setShowNew(true); setForm({ ad: "", aciklama: "", takvimRengi: "#3B82F6" }); }}>+ Yeni Kategori</Button>
        </div>
      </div>

      <Card>
        <CardContent className="!p-0">
          {!list ? <div className="py-8 flex justify-center"><Spinner /></div> :
            list.length === 0 ? (
              <EmptyState icon="📁" title="Kategori yok" />
            ) : (
              <ul className="divide-y divide-slate-100">
                {list.sort((a, b) => a.sira - b.sira).map((c) => (
                  <li key={c.id} className="px-6 py-3 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-4 h-4 rounded" style={{ background: c.takvimRengi || "#3B82F6" }} />
                      <div>
                        <div className="font-medium text-slate-900">{c.ad}</div>
                        {c.aciklama && <div className="text-xs text-slate-500">{c.aciklama}</div>}
                      </div>
                    </div>
                    <div className="flex gap-1">
                      <Button size="sm" variant="secondary" onClick={() => openEdit(c)}>Düzenle</Button>
                      <Button size="sm" variant="danger" onClick={() => remove(c.id)}>Sil</Button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
        </CardContent>
      </Card>

      <Modal
        isOpen={showNew || !!edit}
        onClose={() => { setShowNew(false); setEdit(null); }}
        title={edit ? "Kategoriyi Düzenle" : "Yeni Kategori"}
        footer={
          <>
            <Button variant="secondary" onClick={() => { setShowNew(false); setEdit(null); }}>İptal</Button>
            <Button onClick={(e) => save(e as unknown as FormEvent)}>{edit ? "Güncelle" : "Oluştur"}</Button>
          </>
        }
      >
        <form onSubmit={save} className="space-y-3">
          <Input label="Kategori Adı" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
          <Input label="Açıklama (opsiyonel)" value={form.aciklama} onChange={(e) => setForm({ ...form, aciklama: e.target.value })} />
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Takvim Rengi</label>
            <input type="color" value={form.takvimRengi} onChange={(e) => setForm({ ...form, takvimRengi: e.target.value })} className="w-full h-10 border border-slate-300 rounded-md cursor-pointer" />
          </div>
        </form>
      </Modal>
    </div>
  );
}
