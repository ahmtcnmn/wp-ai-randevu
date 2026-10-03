import type { Metadata } from "next";
import { ReactNode } from "react";
import Link from "next/link";
import { BRAND_NAME } from "@/lib/constants";

export const metadata: Metadata = {
  // Auth sayfaları indexlenmesin
  robots: { index: false, follow: false, nocache: true },
};

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-br from-slate-50 via-white to-slate-100">
      <header className="px-4 sm:px-6 lg:px-8 py-6">
        <Link href="/" className="flex items-center gap-2 w-fit group">
          <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] flex items-center justify-center text-white font-bold">
            A
          </div>
          <span className="font-semibold text-slate-900 text-lg group-hover:text-[var(--color-primary)] transition-colors">
            {BRAND_NAME}
          </span>
        </Link>
      </header>
      <main className="flex-1 flex items-center justify-center px-4 py-8">
        <div className="w-full max-w-md">{children}</div>
      </main>
      <footer className="px-4 py-6 text-center text-xs text-slate-500">
        © {new Date().getFullYear()} {BRAND_NAME}
      </footer>
    </div>
  );
}
