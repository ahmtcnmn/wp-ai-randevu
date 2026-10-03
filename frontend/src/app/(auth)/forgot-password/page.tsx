"use client";
import { useState, FormEvent } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Alert } from "@/components/ui/Alert";
import { authApi } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await authApi.forgotPassword({ email });
      setSubmitted(true);
    } catch (err) {
      setError(extractApiError(err, "İstek gönderilemedi"));
    } finally {
      setLoading(false);
    }
  }

  if (submitted) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>E-postanızı kontrol edin</CardTitle>
        </CardHeader>
        <CardContent>
          <Alert variant="success">
            Eğer <b>{email}</b> ile kayıtlı bir hesap varsa, şifre sıfırlama bağlantısı
            içeren bir e-posta gönderdik.
          </Alert>
          <p className="mt-4 text-sm text-slate-600">
            Bağlantı 60 dakika boyunca geçerlidir. E-posta gelmediyse spam klasörünü kontrol edin.
          </p>
          <div className="mt-6">
            <Link href="/login">
              <Button variant="secondary" fullWidth>
                Giriş Sayfasına Dön
              </Button>
            </Link>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Şifremi Unuttum</CardTitle>
        <p className="text-sm text-slate-500 mt-1">
          E-posta adresinizi girin, şifre sıfırlama bağlantısı gönderelim.
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="space-y-4">
          {error && <Alert variant="error">{error}</Alert>}
          <Input
            label="E-posta"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
            placeholder="ornek@email.com"
          />
          <Button type="submit" fullWidth loading={loading} size="lg">
            Sıfırlama Bağlantısı Gönder
          </Button>
          <div className="text-center">
            <Link
              href="/login"
              className="text-sm text-[var(--color-primary)] hover:underline"
            >
              Giriş sayfasına dön
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
