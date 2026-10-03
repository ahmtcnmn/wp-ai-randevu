"use client";
import Link from "next/link";
import { BRAND_NAME } from "@/lib/constants";
import { Button } from "@/components/ui/Button";

export function PublicHeader() {
  return (
    <header className="border-b border-slate-200 bg-white/80 backdrop-blur-sm sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2 group">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] flex items-center justify-center text-white font-bold text-sm">
              A
            </div>
            <span className="font-semibold text-slate-900 group-hover:text-[var(--color-primary)] transition-colors">
              {BRAND_NAME}
            </span>
          </Link>
          <nav className="hidden md:flex items-center gap-6 text-sm">
            <Link
              href="/fiyatlandirma"
              className="text-slate-600 hover:text-slate-900 transition-colors"
            >
              Fiyatlandırma
            </Link>
            <Link
              href="/iletisim"
              className="text-slate-600 hover:text-slate-900 transition-colors"
            >
              İletişim
            </Link>
          </nav>
        </div>
        <div className="flex items-center gap-2">
          <Link href="/login">
            <Button variant="ghost" size="sm">
              Giriş Yap
            </Button>
          </Link>
          <Link href="/register">
            <Button size="sm">Ücretsiz Dene</Button>
          </Link>
        </div>
      </div>
    </header>
  );
}
