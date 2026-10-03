"use client";
import { useEffect, useState, Suspense } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { authApi } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";

function VerifyEmailContent() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [state, setState] = useState<"loading" | "success" | "error">("loading");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      setState("error");
      setError("E-posta doğrulama bağlantısı geçersiz veya eksik");
      return;
    }
    authApi
      .verifyEmail(token)
      .then(() => setState("success"))
      .catch((err) => {
        setState("error");
        setError(extractApiError(err, "Doğrulama yapılamadı"));
      });
  }, [token]);

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-50 via-white to-slate-100">
      <div className="max-w-md w-full">
        <Card>
          <CardHeader>
            <CardTitle>E-posta Doğrulama</CardTitle>
          </CardHeader>
          <CardContent>
            {state === "loading" && (
              <div className="py-8 flex flex-col items-center gap-3">
                <Spinner size="lg" />
                <p className="text-sm text-slate-600">E-postanız doğrulanıyor...</p>
              </div>
            )}
            {state === "success" && (
              <>
                <Alert variant="success" title="E-postanız doğrulandı">
                  Hesabınız artık tüm özellikleri kullanabilir. Teşekkürler!
                </Alert>
                <div className="mt-4">
                  <Link href="/login">
                    <Button fullWidth>Giriş Yap</Button>
                  </Link>
                </div>
              </>
            )}
            {state === "error" && (
              <>
                <Alert variant="error" title="Doğrulama başarısız">
                  {error}
                </Alert>
                <p className="mt-4 text-sm text-slate-600">
                  Bağlantı süresi dolmuş olabilir. Hesabınıza giriş yapıp yeni bir
                  doğrulama maili talep edebilirsiniz.
                </p>
                <div className="mt-4">
                  <Link href="/login">
                    <Button fullWidth>Giriş Sayfasına Git</Button>
                  </Link>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={null}>
      <VerifyEmailContent />
    </Suspense>
  );
}
