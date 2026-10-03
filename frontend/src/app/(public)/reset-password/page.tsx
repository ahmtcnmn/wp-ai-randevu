"use client";
import { useState, FormEvent, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Alert } from "@/components/ui/Alert";
import { authApi } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";
import { checkPassword } from "@/lib/utils/validation";

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [sifre, setSifre] = useState("");
  const [sifreTekrar, setSifreTekrar] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!token) return setError("Bağlantı geçersiz veya eksik token");
    if (sifre !== sifreTekrar) return setError("Şifreler eşleşmiyor");
    const pw = checkPassword(sifre);
    if (!pw.valid) return setError(pw.message || "Şifre geçersiz");

    setLoading(true);
    try {
      await authApi.resetPassword({ token, yeniSifre: sifre });
      setSuccess(true);
      setTimeout(() => router.push("/login"), 3000);
    } catch (err) {
      setError(extractApiError(err, "Şifre sıfırlanamadı"));
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-50 via-white to-slate-100">
        <div className="max-w-md w-full">
          <Card>
            <CardHeader>
              <CardTitle>Geçersiz Bağlantı</CardTitle>
            </CardHeader>
            <CardContent>
              <Alert variant="error">
                Şifre sıfırlama bağlantısı geçersiz veya eksik. Lütfen yeni bir bağlantı talep edin.
              </Alert>
              <div className="mt-4">
                <Link href="/forgot-password">
                  <Button fullWidth>Yeni Bağlantı İste</Button>
                </Link>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-50 via-white to-slate-100">
      <div className="max-w-md w-full">
        <Card>
          <CardHeader>
            <CardTitle>Yeni Şifre Belirle</CardTitle>
            <p className="text-sm text-slate-500 mt-1">
              Hesabınız için yeni bir şifre oluşturun.
            </p>
          </CardHeader>
          <CardContent>
            {success ? (
              <>
                <Alert variant="success" title="Şifreniz değiştirildi">
                  Yeni şifrenizle giriş yapabilirsiniz. Birkaç saniye içinde
                  yönlendirileceksiniz...
                </Alert>
                <div className="mt-4">
                  <Link href="/login">
                    <Button fullWidth>Giriş Sayfasına Git</Button>
                  </Link>
                </div>
              </>
            ) : (
              <form onSubmit={onSubmit} className="space-y-4">
                {error && <Alert variant="error">{error}</Alert>}
                <Input
                  label="Yeni Şifre"
                  type="password"
                  value={sifre}
                  onChange={(e) => setSifre(e.target.value)}
                  required
                  autoComplete="new-password"
                  helper="En az 8 karakter"
                />
                <Input
                  label="Yeni Şifre (tekrar)"
                  type="password"
                  value={sifreTekrar}
                  onChange={(e) => setSifreTekrar(e.target.value)}
                  required
                  autoComplete="new-password"
                />
                <Button type="submit" fullWidth loading={loading} size="lg">
                  Şifreyi Sıfırla
                </Button>
              </form>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={null}>
      <ResetPasswordForm />
    </Suspense>
  );
}
