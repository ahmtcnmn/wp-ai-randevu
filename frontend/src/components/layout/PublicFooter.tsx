"use client";
import Link from "next/link";
import { BRAND_NAME } from "@/lib/constants";

export function PublicFooter() {
  return (
    <footer className="border-t border-slate-200 bg-slate-50 mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 grid grid-cols-1 md:grid-cols-4 gap-8">
        <div className="col-span-1 md:col-span-2">
          <div className="flex items-center gap-2 mb-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] flex items-center justify-center text-white font-bold text-sm">
              A
            </div>
            <span className="font-semibold text-slate-900">{BRAND_NAME}</span>
          </div>
          <p className="text-sm text-slate-600 max-w-md">
            Berberler ve uzmanlar için modern randevu yönetim platformu. WhatsApp
            entegrasyonu, AI asistan ve gelişmiş raporlama.
          </p>
        </div>

        <div>
          <h3 className="font-semibold text-slate-900 text-sm mb-3">Ürün</h3>
          <ul className="space-y-2 text-sm">
            <li>
              <Link href="/fiyatlandirma" className="text-slate-600 hover:text-slate-900">
                Fiyatlandırma
              </Link>
            </li>
            <li>
              <Link href="/iletisim" className="text-slate-600 hover:text-slate-900">
                Demo İste
              </Link>
            </li>
          </ul>
        </div>

        <div>
          <h3 className="font-semibold text-slate-900 text-sm mb-3">Yasal</h3>
          <ul className="space-y-2 text-sm">
            <li>
              <Link href="/gizlilik" className="text-slate-600 hover:text-slate-900">
                Gizlilik
              </Link>
            </li>
            <li>
              <Link href="/sartlar" className="text-slate-600 hover:text-slate-900">
                Kullanım Şartları
              </Link>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-slate-200 py-6">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-xs text-slate-500 text-center">
          © {new Date().getFullYear()} {BRAND_NAME}. Tüm hakları saklıdır.
        </div>
      </div>
    </footer>
  );
}
