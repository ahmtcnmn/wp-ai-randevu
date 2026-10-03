"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { Modal } from "@/components/ui/Modal";
import { EmptyState } from "@/components/ui/EmptyState";
import {
  whatsappConfigApi, whatsappTemplateApi,
  WhatsappConfigResponse, WhatsappTemplateResponse,
} from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

export default function WhatsappAyarlarPage() {
  const toast = useToast();
  const [config, setConfig] = useState<WhatsappConfigResponse | null>(null);
  const [templates, setTemplates] = useState<WhatsappTemplateResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [editConfig, setEditConfig] = useState(false);
  const [showTplModal, setShowTplModal] = useState(false);

  const [cfgForm, setCfgForm] = useState({
    accessToken: "", appSecret: "", phoneNumberId: "", wabaId: "",
    verifyToken: "", webhookUrl: "", displayPhone: "", aktif: false,
  });

  const [tplForm, setTplForm] = useState({
    ad: "", templateKey: "", kategori: "UTILITY", dil: "tr", govde: "", baslik: "", footer: "",
  });

  async function load() {
    try {
      const [c, t] = await Promise.all([
        whatsappConfigApi.get().catch(() => null),
        whatsappTemplateApi.list().catch(() => []),
      ]);
      setConfig(c);
      setTemplates(t);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function saveConfig(e: FormEvent) {
    e.preventDefault();
    try {
      // Boş alanları gönderme (mevcut değerleri silmesin)
      const body: Record<string, unknown> = {};
      Object.entries(cfgForm).forEach(([k, v]) => {
        if (v !== "" && v !== null && v !== undefined) body[k] = v;
      });
      await whatsappConfigApi.update(body);
      toast.success("Yapılandırma güncellendi");
      setEditConfig(false);
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function createTemplate(e: FormEvent) {
    e.preventDefault();
    try {
      await whatsappTemplateApi.create(tplForm);
      toast.success("Şablon oluşturuldu");
      setShowTplModal(false);
      setTplForm({ ad: "", templateKey: "", kategori: "UTILITY", dil: "tr", govde: "", baslik: "", footer: "" });
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function deleteTemplate(id: number) {
    if (!confirm("Şablon silinsin mi?")) return;
    try {
      await whatsappTemplateApi.remove(id);
      toast.success("Silindi");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function submitTemplate(id: number) {
    try {
      await whatsappTemplateApi.submitToMeta(id);
      toast.success("Meta'ya gönderildi");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  if (loading) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">WhatsApp Ayarları</h1>
        <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
      </div>

      {/* Config */}
      <Card>
        <CardHeader className="flex justify-between flex-row">
          <CardTitle>API Yapılandırması</CardTitle>
          <Button size="sm" variant="secondary" onClick={() => {
            if (!editConfig) {
              // Düzenle'ye basınca mevcut config değerlerini form'a yükle
              setCfgForm({
                accessToken: "", appSecret: "",
                phoneNumberId: config?.phoneNumberId ?? "",
                wabaId: config?.wabaId ?? "",
                verifyToken: "", webhookUrl: config?.webhookUrl ?? "",
                displayPhone: config?.displayPhone ?? "",
                aktif: Boolean(config?.aktif),
              });
            }
            setEditConfig(!editConfig);
          }}>
            {editConfig ? "İptal" : "Düzenle"}
          </Button>
        </CardHeader>
        <CardContent>
          {!editConfig ? (
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-600">Access Token</span>
                {config?.tokenConfigured ? <Badge variant="success">Ayarlı</Badge> : <Badge variant="danger">Eksik</Badge>}
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">App Secret</span>
                {config?.appSecretConfigured ? <Badge variant="success">Ayarlı</Badge> : <Badge variant="danger">Eksik</Badge>}
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Phone Number ID</span>
                <span className="font-mono text-xs">{config?.phoneNumberId || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">WABA ID (Business Account ID)</span>
                <span className="font-mono text-xs">{config?.wabaId || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Görünen Telefon</span>
                <span className="font-mono text-xs">{config?.displayPhone || "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Webhook Verify Token</span>
                <span className="font-mono text-xs">{config?.verifyToken ? "•••" : "—"}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-600">Durum</span>
                {config?.aktif ? <Badge variant="success">Aktif</Badge> : <Badge variant="warning">Pasif</Badge>}
              </div>
            </div>
          ) : (
            <form onSubmit={saveConfig} className="space-y-3">
              <Alert variant="info">
                Boş bırakılan alanlar değiştirilmez. Access Token ve App Secret güvenlik gereği yeniden gösterilmez.
              </Alert>
              <label className="flex items-center justify-between p-3 bg-slate-50 rounded-md">
                <div>
                  <div className="font-medium text-slate-900">WhatsApp Entegrasyonu Aktif</div>
                  <div className="text-xs text-slate-500">Kapalıysa WhatsApp mesajları gönderilmez/alınmaz.</div>
                </div>
                <input
                  type="checkbox"
                  checked={cfgForm.aktif}
                  onChange={(e) => setCfgForm({ ...cfgForm, aktif: e.target.checked })}
                  className="w-5 h-5 rounded text-[var(--color-primary)]"
                />
              </label>
              <Input label="Access Token (yeni)" type="password" value={cfgForm.accessToken} onChange={(e) => setCfgForm({ ...cfgForm, accessToken: e.target.value })} placeholder="Boş bırak: korunur" />
              <Input label="App Secret (yeni)" type="password" value={cfgForm.appSecret} onChange={(e) => setCfgForm({ ...cfgForm, appSecret: e.target.value })} placeholder="Boş bırak: korunur" />
              <Input label="Phone Number ID" value={cfgForm.phoneNumberId} onChange={(e) => setCfgForm({ ...cfgForm, phoneNumberId: e.target.value })} />
              <Input label="WABA ID (Business Account ID)" value={cfgForm.wabaId} onChange={(e) => setCfgForm({ ...cfgForm, wabaId: e.target.value })} />
              <Input label="Görünen Telefon (opsiyonel)" value={cfgForm.displayPhone} onChange={(e) => setCfgForm({ ...cfgForm, displayPhone: e.target.value })} placeholder="+90 5XX XXX XX XX" />
              <Input label="Webhook Verify Token" value={cfgForm.verifyToken} onChange={(e) => setCfgForm({ ...cfgForm, verifyToken: e.target.value })} />
              <Input label="Webhook URL (opsiyonel)" value={cfgForm.webhookUrl} onChange={(e) => setCfgForm({ ...cfgForm, webhookUrl: e.target.value })} placeholder="https://apiguzelim.../webhook/whatsapp" />
              <Button type="submit">Kaydet</Button>
            </form>
          )}
        </CardContent>
      </Card>

      {/* Templates */}
      <Card>
        <CardHeader className="flex justify-between flex-row">
          <CardTitle>Mesaj Şablonları</CardTitle>
          <Button size="sm" onClick={() => setShowTplModal(true)}>+ Yeni Şablon</Button>
        </CardHeader>
        <CardContent className="!p-0">
          {templates.length === 0 ? (
            <EmptyState icon="📝" title="Şablon yok" description="Meta onaylı şablonlarınızı ekleyin" />
          ) : (
            <ul className="divide-y divide-slate-100">
              {templates.map((t) => (
                <li key={t.id} className="px-6 py-3 flex items-center justify-between">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-slate-900">{t.ad}</span>
                      <Badge variant={t.status === "APPROVED" ? "success" : t.status === "REJECTED" ? "danger" : "warning"}>
                        {t.status}
                      </Badge>
                      <span className="text-xs text-slate-400">{t.dil}</span>
                    </div>
                    <div className="text-xs text-slate-500 font-mono mt-0.5">{t.templateKey}</div>
                    {t.redSebebi && (
                      <div className="text-xs text-red-600 mt-1">Red sebebi: {t.redSebebi}</div>
                    )}
                  </div>
                  <div className="flex gap-1">
                    {(t.status === "DRAFT" || t.status === "REJECTED") && (
                      <Button size="sm" onClick={() => submitTemplate(t.id)}>Meta'ya Gönder</Button>
                    )}
                    <Button size="sm" variant="danger" onClick={() => deleteTemplate(t.id)}>Sil</Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Modal
        isOpen={showTplModal}
        onClose={() => setShowTplModal(false)}
        title="Yeni Şablon"
        size="lg"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowTplModal(false)}>İptal</Button>
            <Button onClick={(e) => createTemplate(e as unknown as FormEvent)}>Oluştur</Button>
          </>
        }
      >
        <form onSubmit={createTemplate} className="space-y-3">
          <Input label="Şablon Adı" value={tplForm.ad} onChange={(e) => setTplForm({ ...tplForm, ad: e.target.value })} required />
          <Input label="Template Key (Meta'daki ad)" value={tplForm.templateKey} onChange={(e) => setTplForm({ ...tplForm, templateKey: e.target.value })} required placeholder="appointment_reminder" />
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Kategori</label>
              <select
                value={tplForm.kategori}
                onChange={(e) => setTplForm({ ...tplForm, kategori: e.target.value })}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              >
                <option value="UTILITY">UTILITY</option>
                <option value="MARKETING">MARKETING</option>
                <option value="AUTHENTICATION">AUTHENTICATION</option>
              </select>
            </div>
            <Input label="Dil" value={tplForm.dil} onChange={(e) => setTplForm({ ...tplForm, dil: e.target.value })} placeholder="tr" />
          </div>
          <Input label="Başlık (opsiyonel)" value={tplForm.baslik} onChange={(e) => setTplForm({ ...tplForm, baslik: e.target.value })} maxLength={200} />
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Gövde</label>
            <textarea
              value={tplForm.govde}
              onChange={(e) => setTplForm({ ...tplForm, govde: e.target.value })}
              rows={4}
              required
              className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              placeholder="Merhaba {{1}}, yarın {{2}} saatinde randevunuz var."
            />
          </div>
          <Input label="Footer (opsiyonel)" value={tplForm.footer} onChange={(e) => setTplForm({ ...tplForm, footer: e.target.value })} maxLength={200} />
        </form>
      </Modal>
    </div>
  );
}
