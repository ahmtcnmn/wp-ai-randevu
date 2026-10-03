import type { Metadata } from "next";
import { ReactNode } from "react";
import { OnboardingClientShell } from "@/components/layout/OnboardingClientShell";

export const metadata: Metadata = {
  title: "Kurulum",
  robots: { index: false, follow: false, nocache: true },
};

export default function OnboardingLayout({ children }: { children: ReactNode }) {
  return <OnboardingClientShell>{children}</OnboardingClientShell>;
}
