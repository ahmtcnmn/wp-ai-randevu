"use client";
import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useSubscription } from "@/store/SubscriptionContext";

/**
 * Abonelik süresi dolmuşsa kullanıcıyı /finans'a yönlendirir.
 * İstisna path'ler: /finans, /fiyatlandirma, /hesap, /billing/callback, /sistem/* (SUPER_ADMIN için)
 */
const ALLOWED_PATHS = [
  "/finans",
  "/fiyatlandirma",
  "/hesap",
  "/billing/callback",
  "/login",
  "/logout",
  "/ayarlar/plan",
];

function isAllowed(pathname: string): boolean {
  if (ALLOWED_PATHS.some((p) => pathname === p || pathname.startsWith(p + "/"))) return true;
  if (pathname.startsWith("/sistem")) return true; // SUPER_ADMIN bypass
  return false;
}

export function ExpiredSubscriptionGuard() {
  const { subscription } = useSubscription();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!subscription) return;
    if (subscription.isUsable === false || ["EXPIRED", "SUSPENDED", "CANCELLED"].includes(subscription.status)) {
      if (!isAllowed(pathname)) {
        router.push("/finans");
      }
    }
  }, [subscription, pathname, router]);

  return null;
}
