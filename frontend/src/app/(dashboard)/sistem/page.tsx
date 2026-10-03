"use client";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";

const NODES = [
  { href: "/sistem/tenants", icon: "🏢", title: "Tenantlar", desc: "Müşteri işletmeleri yönet" },
  { href: "/sistem/kullanicilar", icon: "👤", title: "Kullanıcılar", desc: "Tüm sistem kullanıcıları" },
  { href: "/sistem/audit", icon: "📜", title: "Audit", desc: "Sistemde tüm aksiyonlar" },
  { href: "/sistem/saglik", icon: "❤️", title: "Sağlık", desc: "Servis durumu" },
  { href: "/sistem/jobs", icon: "⚙️", title: "Jobs", desc: "Background görevler" },
  { href: "/sistem/contact-requests", icon: "✉️", title: "İletişim Talepleri", desc: "Landing'den gelen formlar" },
];

export default function SistemPage() {
  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <h1 className="text-2xl font-bold text-slate-900">Süper Admin Paneli</h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {NODES.map((n) => (
          <Link key={n.href} href={n.href}>
            <Card className="hover:shadow-md transition cursor-pointer">
              <CardContent className="flex items-center gap-4">
                <div className="text-4xl">{n.icon}</div>
                <div>
                  <div className="font-semibold text-slate-900">{n.title}</div>
                  <div className="text-xs text-slate-500">{n.desc}</div>
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
