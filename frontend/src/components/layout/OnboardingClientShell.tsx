"use client";
import { ReactNode } from "react";
import Link from "next/link";
import { AuthGuard } from "./AuthGuard";
import { BRAND_NAME } from "@/lib/constants";

export function OnboardingClientShell({ children }: { children: ReactNode }) {
  return (
    <AuthGuard>
      <div className="min-h-screen flex flex-col bg-gradient-to-br from-slate-50 via-white to-slate-100">
        <header className="px-4 sm:px-6 lg:px-8 py-6">
          <Link href="/dashboard" className="flex items-center gap-2 w-fit">
            <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] flex items-center justify-center text-white font-bold">
              A
            </div>
            <span className="font-semibold text-slate-900 text-lg">{BRAND_NAME}</span>
          </Link>
        </header>
        <main className="flex-1 flex items-start justify-center px-4 py-8">
          <div className="w-full max-w-2xl">{children}</div>
        </main>
      </div>
    </AuthGuard>
  );
}
