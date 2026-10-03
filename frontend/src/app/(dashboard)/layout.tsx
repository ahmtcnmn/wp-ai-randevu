import type { Metadata } from "next";
import { ReactNode } from "react";
import { DashboardClientShell } from "@/components/layout/DashboardClientShell";

// Dashboard sayfaları indexlenmesin — yalnız oturum açmış kullanıcılar görür
export const metadata: Metadata = {
  robots: { index: false, follow: false, nocache: true },
};

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return <DashboardClientShell>{children}</DashboardClientShell>;
}
