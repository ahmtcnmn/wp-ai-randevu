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

function TwoFaVerifyForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const tempToken = searchParams.get("token");
  const next = searchParams.get("next") || AFTER_LOGIN_PATH;
  const { setSession } = useAuth();
  const toast = useToast();

  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!tempToken) return setError("Geçersiz oturum, lütfen tekrar giriş yapın");
    setLoading(true);
    try {
      const resp = await authApi.loginTwoFactor({ tempToken, code });
      setSession(resp);
      toast.success("Giriş yapıldı");
      router.push(next);
    } catch (err) {
      setError(extractApiError(err, "Doğrulama kodu hatalı"));
    } finally {
      setLoading(false);
    }
  }

  if (!tempToken) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Oturum Geçersiz</CardTitle>
        </CardHeader>
        <CardContent>
          <Alert variant="error">2FA doğrulama oturumu geçersiz veya süresi dolmuş.</Alert>
          <div className="mt-4">
            <Link href="/login">
              <Button fullWidth>Giriş Sayfasına Dön</Button>
            </Link>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>İki Faktörlü Doğrulama</CardTitle>
        <p className="text-sm text-slate-500 mt-1">
          Authenticator uygulamanızda gördüğünüz 6 haneli kodu girin.
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="space-y-4">
          {error && <Alert variant="error">{error}</Alert>}
          <Input
            label="Doğrulama Kodu"
            type="text"
            value={code}
            onChange={(e) => setCode(e.target.value.trim())}
            required
            autoFocus
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="123456"
            helper="6 haneli kod veya XXXXX-XXXXX kurtarma kodu"
          />
          <Button type="submit" fullWidth loading={loading} size="lg">
            Doğrula
          </Button>
          <div className="text-center">
            <Link href="/login" className="text-sm text-slate-500 hover:underline">
              Farklı hesapla giriş yap
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}

export default function TwoFaVerifyPage() {
  return (
    <Suspense fallback={null}>
      <TwoFaVerifyForm />
    </Suspense>
  );
}
