"use client";
import { ReactNode } from "react";
import { AuthGuard } from "./AuthGuard";
import { DashboardShell } from "./DashboardShell";
import { AiChatWidget } from "@/components/common/AiChatWidget";
import { TrialBanner } from "@/components/common/TrialBanner";
import { ExpiredSubscriptionGuard } from "@/components/common/ExpiredSubscriptionGuard";
import { NotificationProvider } from "@/store/NotificationContext";

/**
 * Dashboard'un client tarafı — AuthGuard + Trial banner + Shell + AI Widget + NotificationProvider.
 * Trial banner her sayfa üstünde, ExpiredSubscriptionGuard süresi dolan tenantları /finans'a yönlendirir.
 */
export function DashboardClientShell({ children }: { children: ReactNode }) {
  return (
    <AuthGuard>
      <NotificationProvider>
        <ExpiredSubscriptionGuard />
        <div className="flex flex-col min-h-screen">
          <TrialBanner />
          <DashboardShell>{children}</DashboardShell>
        </div>
        <AiChatWidget />
      </NotificationProvider>
    </AuthGuard>
  );
}
