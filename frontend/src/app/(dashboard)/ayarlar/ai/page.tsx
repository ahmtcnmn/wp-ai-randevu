"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { aiConfigApi, AiConfigResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { useSector } from "@/store/SectorContext";

export default function AiAyarlarPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [cfg, setCfg] = useState<AiConfigResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    aiConfigApi.get().then(setCfg).catch(() => setCfg(null));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!cfg) return;
    setLoading(true);
    setError(null);
    try {
      const updated = await aiConfigApi.update(cfg);
      setCfg(updated);
      toast.success("AI yapılandırması güncellendi");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  if (!cfg) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">AI Asistan</h1>
        <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Yapılandırma</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}

            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-md">
              <div>
                <div className="font-medium text-slate-900">AI Asistan Aktif</div>
                <div className="text-xs text-slate-500">{labels.customerPlural}a otomatik yanıt verir</div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={cfg.aktif}
                  onChange={(e) => setCfg({ ...cfg, aktif: e.target.checked })}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-slate-300 peer-checked:bg-[var(--color-primary)] rounded-full peer transition-colors after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:after:translate-x-5" />
              </label>
            </div>

            <Input
              label="Persona Adı"
              value={cfg.personaAdi}
              onChange={(e) => setCfg({ ...cfg, personaAdi: e.target.value })}
              placeholder="Asistan, Berber Ali, ..."
            />

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Sistem Prompt'u</label>
              <textarea
                value={cfg.sistemPromptu}
                onChange={(e) => setCfg({ ...cfg, sistemPromptu: e.target.value })}
                rows={6}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm font-mono"
                placeholder="Sen bir berber asistanısın..."
              />
              <p className="text-xs text-slate-500 mt-1">AI'a verdiğiniz davranış talimatı</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">İşletme Açıklaması</label>
              <textarea
                value={cfg.isletmeAciklamasi ?? ""}
                onChange={(e) => setCfg({ ...cfg, isletmeAciklamasi: e.target.value })}
                rows={4}
                maxLength={500}
                placeholder="Örn: Kadıköy'de 2 yıldır faaliyet gösteren erkek berber salonu. Saç, sakal, traş hizmetleri sunuyoruz. Pazartesi-Cumartesi 09:00-20:00 açığız."
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              />
              <p className="text-xs text-slate-500 mt-1">
                AI asistanınız bu bilgiyi müşteri sorularına cevap verirken kullanır. Çalışma saatleri, özel hizmetler, konum gibi detayları ekleyebilirsiniz.
              </p>
            </div>

            <div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Dil</label>
                <select
                  value={cfg.dil}
                  onChange={(e) => setCfg({ ...cfg, dil: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                >
                  <option value="tr">Türkçe</option>
                  <option value="en">English</option>
                </select>
              </div>
            </div>

            <Input
              label="Handoff Kelimeleri (virgüllü)"
              value={cfg.handoffKelimeleri}
              onChange={(e) => setCfg({ ...cfg, handoffKelimeleri: e.target.value })}
              placeholder="şikayet, müdür, insan istiyorum"
              helper={`${labels.customerSingular} bu kelimelerden birini yazınca konuşma 'insan bekliyor' moduna geçer`}
            />

            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                id="fiyat"
                checked={!!cfg.fiyatBilgisiGoster}
                onChange={(e) => setCfg({ ...cfg, fiyatBilgisiGoster: e.target.checked })}
                className="rounded text-[var(--color-primary)]"
              />
              <label htmlFor="fiyat" className="text-sm text-slate-700">Fiyat bilgisini WhatsApp'ta göster</label>
            </div>

            <div className="flex items-center gap-3">
              <input
                type="checkbox"
                id="oto"
                checked={!!cfg.otomatikOnay}
                onChange={(e) => setCfg({ ...cfg, otomatikOnay: e.target.checked })}
                className="rounded text-[var(--color-primary)]"
              />
              <label htmlFor="oto" className="text-sm text-slate-700">{labels.appointmentSingular} otomatik onaylansın (yoksa BEKLIYOR kalır)</label>
            </div>

            <Button type="submit" loading={loading}>Kaydet</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
