"use client";
import { ReactNode, useState } from "react";
import { Sidebar } from "./Sidebar";
import { Header } from "./Header";
import { useAuth } from "@/store/AuthContext";

interface DashboardShellProps {
  children: ReactNode;
}

/**
 * Dashboard yerleşimi — Sidebar (sol) + Header (üst) + main content.
 * Mobile'da Sidebar drawer olarak açılır.
 */
export function DashboardShell({ children }: DashboardShellProps) {
  const { user } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 flex">
      {/* Desktop sidebar */}
      <div className="hidden lg:block flex-shrink-0">
        <Sidebar role={user?.rol} />
      </div>

      {/* Mobile sidebar overlay */}
      {drawerOpen && (
        <div
          className="lg:hidden fixed inset-0 z-50 bg-slate-900/50"
          onClick={() => setDrawerOpen(false)}
        >
          <div className="absolute left-0 top-0 h-full" onClick={(e) => e.stopPropagation()}>
            <Sidebar role={user?.rol} onItemClick={() => setDrawerOpen(false)} />
          </div>
        </div>
      )}

      <div className="flex-1 flex flex-col min-w-0">
        <Header onMenuClick={() => setDrawerOpen(true)} />
        <main className="flex-1 overflow-x-auto">{children}</main>
      </div>
    </div>
  );
}
