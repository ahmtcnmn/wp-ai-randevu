"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { branchApi, BranchResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

export default function SubelerPage() {
  const toast = useToast();
  const [list, setList] = useState<BranchResponse[] | null>(null);
  const [edit, setEdit] = useState<BranchResponse | null>(null);
  const [showNew, setShowNew] = useState(false);
  const [form, setForm] = useState({ ad: "", adres: "", telefon: "" });

  async function load() {
    try {
      setList(await branchApi.list());
    } catch {
      setList([]);
    }
  }

  useEffect(() => { load(); }, []);

  async function save(e: FormEvent) {
    e.preventDefault();
    try {
      if (edit) {
        await branchApi.update(edit.id, form);
        toast.success("Şube güncellendi");
      } else {
        await branchApi.create(form);
        toast.success("Şube oluşturuldu");
      }
      setEdit(null);
      setShowNew(false);
      setForm({ ad: "", adres: "", telefon: "" });
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function deactivate(id: number) {
    if (!confirm("Şube pasifleştirilsin mi? Tüm verileri (çalışan, randevu, ciro) korunur. İstediğiniz zaman tekrar aktif edebilirsiniz.")) return;
    try {
      await branchApi.remove(id);
      toast.success("Şube pasifleştirildi");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function activate(id: number) {
    try {
      await branchApi.activate(id);
      toast.success("Şube aktifleştirildi");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  function openEdit(b: BranchResponse) {
    setEdit(b);
    setForm({ ad: b.ad, adres: b.adres || "", telefon: b.telefon || "" });
  }

  return (
    <div className="p-4 lg:p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Şubeler</h1>
        <div className="flex gap-2">
          <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
          <Button onClick={() => { setShowNew(true); setForm({ ad: "", adres: "", telefon: "" }); }}>+ Yeni Şube</Button>
        </div>
      </div>

      <Card>
        <CardContent className="!p-0">
          {!list ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : list.length === 0 ? (
            <EmptyState icon="🏢" title="Şube yok" description="İlk şubenizi ekleyin" action={<Button onClick={() => setShowNew(true)}>+ Yeni Şube</Button>} />
          ) : (
            <ul className="divide-y divide-slate-100">
              {list.map((b) => (
                <li key={b.id} className="px-6 py-3 flex items-center justify-between">
                  <div>
                    <div className="font-medium text-slate-900 flex items-center gap-2">
                      {b.ad}
                      {!b.aktif && <Badge variant="default">Pasif</Badge>}
                    </div>
                    <div className="text-xs text-slate-500">{b.adres || "—"} · {b.telefon || "—"}</div>
                  </div>
                  <div className="flex gap-1">
                    <Button size="sm" variant="secondary" onClick={() => openEdit(b)}>Düzenle</Button>
                    {b.aktif ? (
                      <Button size="sm" variant="danger" onClick={() => deactivate(b.id)}>Pasifleştir</Button>
                    ) : (
                      <Button size="sm" variant="primary" onClick={() => activate(b.id)}>Aktifleştir</Button>
                    )}
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
        title={edit ? "Şubeyi Düzenle" : "Yeni Şube"}
        footer={
          <>
            <Button variant="secondary" onClick={() => { setShowNew(false); setEdit(null); }}>İptal</Button>
            <Button onClick={(e) => save(e as unknown as FormEvent)}>{edit ? "Güncelle" : "Oluştur"}</Button>
          </>
        }
      >
        <form onSubmit={save} className="space-y-3">
          <Input label="Şube Adı" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
          <Input label="Adres" value={form.adres} onChange={(e) => setForm({ ...form, adres: e.target.value })} />
          <Input label="Telefon" value={form.telefon} onChange={(e) => setForm({ ...form, telefon: e.target.value })} />
        </form>
      </Modal>
    </div>
  );
}
