"use client";
import { useState, FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Alert } from "@/components/ui/Alert";
import { publicContactApi } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";
import { isEmail, isTrPhone, normalizeTrPhone } from "@/lib/utils/validation";

export default function IletisimPage() {
  const [form, setForm] = useState({
    ad: "",
    soyad: "",
    email: "",
    telefon: "",
    mesaj: "",
  });
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.ad || !form.soyad) return setError("Ad ve soyad zorunlu");
    if (!isEmail(form.email)) return setError("Geçerli bir e-posta girin");
    if (!isTrPhone(form.telefon)) return setError("Geçerli bir telefon girin");
    if (form.mesaj.trim().length < 10) return setError("Mesaj en az 10 karakter olmalı");

    setLoading(true);
    try {
      await publicContactApi.submit({
        ...form,
        telefon: normalizeTrPhone(form.telefon),
      });
      setSubmitted(true);
    } catch (err) {
      setError(extractApiError(err, "Mesajınız gönderilemedi"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 lg:py-24">
      <div className="text-center mb-12">
        <h1 className="text-4xl lg:text-5xl font-bold text-slate-900">İletişim</h1>
        <p className="mt-4 text-slate-600">
          Demo isteği, teklif veya sorularınız için bize yazın. En kısa sürede dönüş yapacağız.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Bize Yazın</CardTitle>
        </CardHeader>
        <CardContent>
          {submitted ? (
            <Alert variant="success" title="Mesajınız alındı">
              En kısa sürede sizinle iletişime geçeceğiz. Teşekkürler!
            </Alert>
          ) : (
            <form onSubmit={onSubmit} className="space-y-4">
              {error && <Alert variant="error">{error}</Alert>}
              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="Ad"
                  value={form.ad}
                  onChange={(e) => update("ad", e.target.value)}
                  required
                />
                <Input
                  label="Soyad"
                  value={form.soyad}
                  onChange={(e) => update("soyad", e.target.value)}
                  required
                />
              </div>
              <Input
                label="E-posta"
                type="email"
                value={form.email}
                onChange={(e) => update("email", e.target.value)}
                required
              />
              <Input
                label="Telefon"
                type="tel"
                value={form.telefon}
                onChange={(e) => update("telefon", e.target.value)}
                required
                placeholder="05XX XXX XX XX"
              />
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Mesajınız <span className="text-red-500">*</span>
                </label>
                <textarea
                  value={form.mesaj}
                  onChange={(e) => update("mesaj", e.target.value)}
                  required
                  rows={5}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent outline-none"
                  placeholder="Demo görmek istiyorum / Özel teklif... / Sorum var..."
                />
              </div>
              <Button type="submit" fullWidth loading={loading} size="lg">
                Mesaj Gönder
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
