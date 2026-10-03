"use client";
import { useState, useRef, useEffect } from "react";
import Link from "next/link";
import { useAuth } from "@/store/AuthContext";
import { useNotifications } from "@/store/NotificationContext";
import { Avatar } from "@/components/ui/Avatar";
import { ROLE_LABELS } from "@/lib/utils/role";

interface HeaderProps {
  onMenuClick?: () => void;
}

export function Header({ onMenuClick }: HeaderProps) {
  const { user, fullUser, logout } = useAuth();
  const { unreadCount } = useNotifications();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function onClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const fullName = `${user?.ad || ""} ${user?.soyad || ""}`.trim() || user?.email || "Kullanıcı";
  const rolLabel = user?.rol ? ROLE_LABELS[user.rol] : "";

  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-4 lg:px-6">
      <div className="flex items-center gap-2">
        {onMenuClick && (
          <button
            type="button"
            onClick={onMenuClick}
            className="lg:hidden p-2 text-slate-600 hover:bg-slate-100 rounded-md"
            aria-label="Menü"
          >
            ☰
          </button>
        )}
      </div>

      <div className="flex items-center gap-3">
        {/* Bildirim zili */}
        <Link
          href="/bildirimler"
          className="relative p-2 text-slate-600 hover:bg-slate-100 rounded-md transition-colors"
          aria-label="Bildirimler"
        >
          <span className="text-xl">🔔</span>
          {unreadCount > 0 && (
            <span className="absolute top-1 right-1 min-w-[18px] h-[18px] bg-red-500 text-white text-xs font-semibold rounded-full flex items-center justify-center px-1">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          )}
        </Link>

        {/* User dropdown */}
        <div ref={ref} className="relative">
          <button
            type="button"
            onClick={() => setOpen((s) => !s)}
            className="flex items-center gap-2 p-1 pr-3 rounded-md hover:bg-slate-100"
          >
            <Avatar name={fullName} size="sm" />
            <div className="text-left hidden sm:block">
              <div className="text-sm font-medium text-slate-900 leading-tight">{fullName}</div>
              <div className="text-xs text-slate-500 leading-tight">{rolLabel}</div>
            </div>
            <span className="text-slate-400 text-sm">▾</span>
          </button>

          {open && (
            <div className="absolute right-0 top-full mt-1 w-56 bg-white border border-slate-200 rounded-lg shadow-lg py-1 z-50">
              <div className="px-4 py-3 border-b border-slate-100">
                <div className="text-sm font-medium text-slate-900 truncate">{fullName}</div>
                <div className="text-xs text-slate-500 truncate">{user?.email}</div>
                {fullUser && !fullUser.emailDogrulandi && (
                  <div className="mt-2 text-xs text-amber-700 bg-amber-50 px-2 py-1 rounded">
                    ⚠ E-postanızı doğrulayın
                  </div>
                )}
              </div>
              <Link
                href="/hesap"
                onClick={() => setOpen(false)}
                className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
              >
                👤 Profil
              </Link>
              <Link
                href="/hesap/sifre"
                onClick={() => setOpen(false)}
                className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
              >
                🔑 Şifre Değiştir
              </Link>
              <Link
                href="/hesap/2fa"
                onClick={() => setOpen(false)}
                className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
              >
                🛡 İki Faktörlü Doğrulama
              </Link>
              <Link
                href="/ayarlar"
                onClick={() => setOpen(false)}
                className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50"
              >
                ⚙️ Ayarlar
              </Link>
              <div className="border-t border-slate-100 my-1" />
              <button
                type="button"
                onClick={() => {
                  setOpen(false);
                  logout();
                }}
                className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50"
              >
                Çıkış Yap
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
