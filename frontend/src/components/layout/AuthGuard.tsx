"use client";
import { ReactNode, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/store/AuthContext";
import { FullPageSpinner } from "@/components/ui/Spinner";

interface AuthGuardProps {
  children: ReactNode;
}

/**
 * AuthGuard — kullanıcı login değilse /login'e yönlendirir.
 * Yükleme bitince ve isAuthenticated false ise yönlendirme yapar.
 */
export function AuthGuard({ children }: AuthGuardProps) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const redirect = encodeURIComponent(pathname);
      router.replace(`/login?next=${redirect}`);
    }
  }, [isLoading, isAuthenticated, router, pathname]);

  if (isLoading) return <FullPageSpinner />;
  if (!isAuthenticated) return <FullPageSpinner />;

  return <>{children}</>;
}
