"use client";
import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { customerApi } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { isEmail, isTrPhone, normalizeTrPhone } from "@/lib/utils/validation";
import { useSector } from "@/store/SectorContext";

export default function YeniMusteriPage() {
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();
  const [form, setForm] = useState({
    ad: "",
    soyad: "",
    telefon: "",
    email: "",
    notlar: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.ad.trim() || !form.soyad.trim()) return setError("Ad ve soyad zorunlu");
    if (!isTrPhone(form.telefon)) return setError("Geçerli bir telefon girin");
    if (form.email && !isEmail(form.email)) return setError("Geçerli bir e-posta girin");

    setLoading(true);
    try {
      const c = await customerApi.create({
        ad: form.ad.trim(),
        soyad: form.soyad.trim(),
        telefon: normalizeTrPhone(form.telefon),
        email: form.email.trim() || undefined,
        notlar: form.notlar.trim() || undefined,
      });
      toast.success(`${labels.customerSingular} oluşturuldu`);
      router.push(`/musteriler/${c.id}`);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-xl mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Yeni {labels.customerSingular}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <div className="grid grid-cols-2 gap-3">
              <Input label="Ad" value={form.ad} onChange={(e) => update("ad", e.target.value)} required />
              <Input label="Soyad" value={form.soyad} onChange={(e) => update("soyad", e.target.value)} required />
            </div>
            <Input
              label="Telefon"
              type="tel"
              value={form.telefon}
              onChange={(e) => update("telefon", e.target.value)}
              required
              placeholder="05XX XXX XX XX"
            />
            <Input
              label="E-posta (opsiyonel)"
              type="email"
              value={form.email}
              onChange={(e) => update("email", e.target.value)}
            />
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Notlar</label>
              <textarea
                value={form.notlar}
                onChange={(e) => update("notlar", e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
                placeholder="Müşteri hakkında özel notlar..."
              />
            </div>
            <div className="flex gap-2">
              <Button type="button" variant="secondary" onClick={() => router.back()}>
                İptal
              </Button>
              <Button type="submit" loading={loading}>
                {labels.customerSingular} Oluştur
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
