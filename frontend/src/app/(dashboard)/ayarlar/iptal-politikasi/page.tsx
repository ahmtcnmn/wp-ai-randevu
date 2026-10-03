"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { tenantApi, CancellationPolicy } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { useSector } from "@/store/SectorContext";

export default function IptalPolitikasiPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [policy, setPolicy] = useState<CancellationPolicy | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    tenantApi.getCancellationPolicy().then(setPolicy).catch(() => setPolicy({ saatOnce: null, mesaj: null }));
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!policy) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await tenantApi.updateCancellationPolicy(policy);
      setPolicy(updated);
      toast.success("Politika güncellendi");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setSaving(false);
    }
  }

  if (!policy) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">İptal Politikası</h1>
        <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
      </div>

      <Card>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <Alert variant="info">
              Belirleyeceğiniz politika randevu oluşturulurken ve WhatsApp/SMS hatırlatma mesajlarında müşterilere gösterilir.
            </Alert>
            <Input
              label="Kaç saat öncesine kadar iptal edilebilir?"
              type="number"
              min={0}
              max={168}
              value={policy.saatOnce ?? ""}
              onChange={(e) => setPolicy({ ...policy, saatOnce: e.target.value ? Number(e.target.value) : null })}
              placeholder="24"
              helper="0-168 saat. Boş bırakırsanız iptal süresi sınırlanmaz."
            />
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{labels.customerSingular}ye Gösterilecek Mesaj</label>
              <textarea
                value={policy.mesaj ?? ""}
                onChange={(e) => setPolicy({ ...policy, mesaj: e.target.value || null })}
                rows={4}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                placeholder="Randevunuzu en geç 24 saat öncesinden iptal edebilirsiniz."
              />
              <p className="text-xs text-slate-500 mt-1">
                Bu mesaj randevu oluşturulduğunda WhatsApp/SMS ile müşteriye gönderilir.
              </p>
            </div>
            <Button type="submit" loading={saving}>Kaydet</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
