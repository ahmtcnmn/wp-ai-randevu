"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Role } from "@/types/auth";
import { cn } from "@/lib/utils/cn";
import { BRAND_NAME } from "@/lib/constants";
import { useSector } from "@/store/SectorContext";

interface NavItem {
  label: string;
  href: string;
  icon: string;
  roles?: Role[]; // boş = tüm roller
}

function buildNav(labels: { staffPlural: string; customerPlural: string; servicePlural: string; appointmentPlural: string }): NavItem[] {
  return [
    { label: "Dashboard", href: "/dashboard", icon: "📊" },
    { label: "Takvim", href: "/takvim", icon: "📅" },
    { label: labels.appointmentPlural, href: "/randevular", icon: "📋" },
    { label: labels.customerPlural, href: "/musteriler", icon: "👥" },
    { label: "WhatsApp", href: "/whatsapp", icon: "💬", roles: ["OWNER", "ADMIN", "BRANCH_MANAGER", "STAFF"] },
    { label: "Kampanyalar", href: "/kampanyalar", icon: "🎯", roles: ["OWNER", "ADMIN"] },
    { label: "Hatırlatma", href: "/hatirlatma", icon: "🔔", roles: ["OWNER", "ADMIN", "BRANCH_MANAGER"] },
    { label: labels.staffPlural, href: "/calisanlar", icon: "👔", roles: ["OWNER", "ADMIN"] },
    { label: labels.servicePlural, href: "/hizmetler", icon: "✂️", roles: ["OWNER", "ADMIN"] },
    { label: "Ürünler", href: "/urunler", icon: "🛒", roles: ["OWNER", "ADMIN", "BRANCH_MANAGER"] },
    { label: "Finans", href: "/finans", icon: "💰", roles: ["OWNER", "ADMIN"] },
    { label: "Raporlar", href: "/raporlar", icon: "📈", roles: ["OWNER", "ADMIN"] },
    { label: "Denetim Kayıtları", href: "/audit-log", icon: "📜", roles: ["OWNER", "ADMIN"] },
    { label: "Ayarlar", href: "/ayarlar", icon: "⚙️", roles: ["OWNER", "ADMIN"] },
  ];
}

const SUPER_ADMIN_NAV: NavItem[] = [
  { label: "Tenant'lar", href: "/sistem/tenants", icon: "🏢" },
  { label: "Planlar", href: "/sistem/planlar", icon: "💼" },
  { label: "Audit Log", href: "/sistem/audit", icon: "📜" },
  { label: "Sağlık", href: "/sistem/saglik", icon: "🩺" },
  { label: "Jobs", href: "/sistem/jobs", icon: "⚙️" },
  { label: "İletişim Talepleri", href: "/sistem/contact-requests", icon: "📨" },
];

function filterByRole(items: NavItem[], rol?: Role) {
  return items.filter((i) => !i.roles || (rol && i.roles.includes(rol)));
}

interface SidebarProps {
  role?: Role;
  onItemClick?: () => void;
}

export function Sidebar({ role, onItemClick }: SidebarProps) {
  const pathname = usePathname();
  const { labels } = useSector();
  const isSuperAdmin = role === "SUPER_ADMIN";
  const items = isSuperAdmin ? SUPER_ADMIN_NAV : filterByRole(buildNav(labels), role);

  return (
    <aside className="h-full bg-white border-r border-slate-200 w-64 flex flex-col">
      <Link
        href="/dashboard"
        className="flex items-center gap-2 px-5 h-16 border-b border-slate-200"
        onClick={onItemClick}
      >
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] flex items-center justify-center text-white font-bold text-sm">
          A
        </div>
        <span className="font-semibold text-slate-900">{BRAND_NAME}</span>
      </Link>

      {isSuperAdmin && (
        <div className="px-4 py-2 bg-amber-50 border-b border-amber-200 text-xs text-amber-800 text-center font-medium">
          🔒 SÜPER ADMİN
        </div>
      )}

      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {items.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={onItemClick}
              className={cn(
                "flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                isActive
                  ? "bg-[var(--color-primary-soft)] text-[var(--color-primary)]"
                  : "text-slate-700 hover:bg-slate-100"
              )}
            >
              <span className="text-lg" aria-hidden>{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-slate-200 p-4 text-xs text-slate-400 text-center">
        © {new Date().getFullYear()} {BRAND_NAME}
      </div>
    </aside>
  );
}
