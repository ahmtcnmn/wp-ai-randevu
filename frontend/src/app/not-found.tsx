import Link from "next/link";
import { Button } from "@/components/ui/Button";

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gradient-to-br from-slate-50 to-slate-100">
      <div className="max-w-md text-center">
        <div className="text-8xl font-bold text-[var(--color-primary)]">404</div>
        <h1 className="mt-4 text-2xl font-semibold text-slate-900">Sayfa Bulunamadı</h1>
        <p className="mt-2 text-slate-600">
          Aradığınız sayfa taşınmış veya hiç var olmamış olabilir.
        </p>
        <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
          <Link href="/">
            <Button>Ana Sayfaya Dön</Button>
          </Link>
          <Link href="/login">
            <Button variant="secondary">Giriş Yap</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
