"use client";
import { useEffect } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/Button";

export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-50 to-slate-100">
      <div className="max-w-md text-center">
        <div className="text-8xl">⚠️</div>
        <h1 className="mt-4 text-2xl font-semibold text-slate-900">Bir Hata Oluştu</h1>
        <p className="mt-2 text-slate-600">
          Beklenmedik bir sorunla karşılaştık. Lütfen tekrar deneyin veya ana sayfaya dönün.
        </p>
        {error.digest && (
          <p className="mt-4 text-xs text-slate-400 font-mono">Hata kodu: {error.digest}</p>
        )}
        <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
          <Button onClick={reset}>Tekrar Dene</Button>
          <Link href="/">
            <Button variant="secondary">Ana Sayfaya Dön</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
