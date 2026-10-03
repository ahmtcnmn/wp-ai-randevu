"use client";
import { useState, FormEvent, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Alert } from "@/components/ui/Alert";
import { useAuth } from "@/store/AuthContext";
import { useToast } from "@/store/ToastContext";
import { authApi } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";
import { AFTER_LOGIN_PATH } from "@/lib/constants";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = searchParams.get("next") || AFTER_LOGIN_PATH;
  const { setSession } = useAuth();
  const toast = useToast();

  const [email, setEmail] = useState("");
  const [sifre, setSifre] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const resp = await authApi.login({ email, sifre });
      if (resp.requires2fa && resp.tempToken) {
        // 2FA gerekli — temp token ile 2fa-verify sayfasına git
        router.push(
          `/2fa-verify?token=${encodeURIComponent(resp.tempToken)}&next=${encodeURIComponent(next)}`
        );
        return;
      }
      setSession(resp);
      toast.success("Giriş yapıldı");
      router.push(next);
    } catch (err) {
      setError(extractApiError(err, "Giriş yapılamadı"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Giriş Yap</CardTitle>
        <p className="text-sm text-slate-500 mt-1">
          Hesabınız yok mu?{" "}
          <Link
            href="/register"
            className="text-[var(--color-primary)] font-medium hover:underline"
          >
            Hemen kaydolun
          </Link>
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="space-y-4">
          {error && <Alert variant="error">{error}</Alert>}
          <Input
            label="E-posta"
            type="email"
            name="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
            placeholder="ornek@email.com"
          />
          <Input
            label="Şifre"
            type="password"
            name="sifre"
            value={sifre}
            onChange={(e) => setSifre(e.target.value)}
            required
            autoComplete="current-password"
            placeholder="••••••••"
          />
          <div className="flex justify-end">
            <Link
              href="/forgot-password"
              className="text-sm text-[var(--color-primary)] hover:underline"
            >
              Şifremi unuttum
            </Link>
          </div>
          <Button type="submit" fullWidth loading={loading} size="lg">
            Giriş Yap
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}
