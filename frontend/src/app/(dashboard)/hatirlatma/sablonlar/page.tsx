"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Modal } from "@/components/ui/Modal";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { reminderTemplateApi, ReminderTemplateResponse, ReminderTemplateRequest, ReminderKanal, ReminderBirim } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

type FormState = {
  ad: string;
  kanal: ReminderKanal;
  mesaj: string;
  gunSonra: number;
  oncesi: boolean;
  birim: ReminderBirim;
};

const EMPTY: FormState = { ad: "", kanal: "WHATSAPP", mesaj: "", gunSonra: 1, oncesi: false, birim: "GUN" };

export default function HatirlatmaSablonlariPage() {
  const toast = useToast();
  const [list, setList] = useState<ReminderTemplateResponse[] | null>(null);
  const [editing, setEditing] = useState<{ id: number | null; form: FormState } | null>(null);

  async function load() {
    try { setList(await reminderTemplateApi.list()); } catch { setList([]); }
  }
  useEffect(() => { load(); }, []);

  function openNew() {
    setEditing({ id: null, form: EMPTY });
  }
  function openEdit(t: ReminderTemplateResponse) {
    setEditing({
      id: t.id,
      form: { ad: t.ad, kanal: t.kanal, mesaj: t.mesaj, gunSonra: t.gunSonra, oncesi: t.oncesi, birim: t.birim || "GUN" },
    });
  }

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!editing) return;
    const body: ReminderTemplateRequest = { ...editing.form };
    try {
      if (editing.id == null) {
        await reminderTemplateApi.create(body);
        toast.success("Şablon oluşturuldu");
      } else {
        await reminderTemplateApi.update(editing.id, body);
        toast.success("Şablon güncellendi");
      }
      setEditing(null);
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  async function remove(id: number) {
    if (!confirm("Şablon silinsin mi?")) return;
    try {
      await reminderTemplateApi.remove(id);
      toast.success("Silindi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Hatırlatma Şablonları</h1>
        <div className="flex gap-2">
          <Link href="/hatirlatma"><Button variant="ghost" size="sm">← Hatırlatma</Button></Link>
          <Button onClick={openNew}>+ Yeni Şablon</Button>
        </div>
      </div>

      <Card>
        <CardContent className="!p-0">
          {!list ? <div className="flex justify-center py-12"><Spinner /></div> :
           list.length === 0 ? <EmptyState icon="📝" title="Şablon yok" action={<Button onClick={openNew}>+ Yeni Şablon</Button>} /> : (
            <ul className="divide-y divide-slate-100">
              {list.map((t) => (
                <li key={t.id} className="px-6 py-3 flex items-start justify-between gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium">{t.ad}</span>
                      <Badge variant="info">{t.kanal}</Badge>
                      <span className="text-xs text-slate-500">
                        Randevudan {t.gunSonra} {t.birim === "SAAT" ? "saat" : "gün"} {t.oncesi ? "ÖNCE" : "SONRA"}
                      </span>
                      {!t.aktif && <Badge>Pasif</Badge>}
                    </div>
                    <p className="text-xs text-slate-500 mt-1 whitespace-pre-wrap">{t.mesaj}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button size="sm" variant="secondary" onClick={() => openEdit(t)}>Düzenle</Button>
                    <Button size="sm" variant="danger" onClick={() => remove(t.id)}>Sil</Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Modal
        isOpen={editing !== null}
        onClose={() => setEditing(null)}
        title={editing?.id == null ? "Yeni Şablon" : "Şablonu Düzenle"}
        footer={<>
          <Button variant="secondary" onClick={() => setEditing(null)}>İptal</Button>
          <Button onClick={(e) => save(e as unknown as FormEvent)}>
            {editing?.id == null ? "Ekle" : "Kaydet"}
          </Button>
        </>}
      >
        {editing && (
          <form onSubmit={save} className="space-y-3">
            <Input label="Ad" value={editing.form.ad} onChange={(e) => setEditing({ ...editing, form: { ...editing.form, ad: e.target.value } })} required />
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Kanal</label>
              <select
                value={editing.form.kanal}
                onChange={(e) => setEditing({ ...editing, form: { ...editing.form, kanal: e.target.value as ReminderKanal } })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              >
                <option value="WHATSAPP">WhatsApp</option>
                <option value="SMS">SMS</option>
                <option value="EMAIL">E-posta</option>
              </select>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <Input
                label="Miktar"
                type="number"
                min={1}
                value={editing.form.gunSonra}
                onChange={(e) => setEditing({ ...editing, form: { ...editing.form, gunSonra: Number(e.target.value) } })}
              />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Birim</label>
                <select
                  value={editing.form.birim}
                  onChange={(e) => setEditing({ ...editing, form: { ...editing.form, birim: e.target.value as ReminderBirim } })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                >
                  <option value="GUN">Gün</option>
                  <option value="SAAT">Saat</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Zamanı</label>
                <select
                  value={editing.form.oncesi ? "ONCE" : "SONRA"}
                  onChange={(e) => setEditing({ ...editing, form: { ...editing.form, oncesi: e.target.value === "ONCE" } })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                >
                  <option value="SONRA">SONRA</option>
                  <option value="ONCE">ÖNCE</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Mesaj</label>
              <textarea
                value={editing.form.mesaj}
                onChange={(e) => setEditing({ ...editing, form: { ...editing.form, mesaj: e.target.value } })}
                rows={4}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                required
              />
              <p className="text-xs text-slate-500 mt-1">
                {"{ad}, {tarih}, {saat}, {hizmet}"} placeholder'ları desteklenir.
              </p>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
