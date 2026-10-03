"use client";
import { Card, CardContent } from "@/components/ui/Card";
import Link from "next/link";
import { useSector } from "@/store/SectorContext";

export default function RaporlarPage() {
  const { labels } = useSector();
  const tabs = [
    { id: "randevular", label: labels.appointmentPlural, icon: "📅", desc: "Durum, ortalama süre, doluluk" },
    { id: "ciro", label: "Ciro", icon: "💰", desc: `${labels.serviceSingular}/${labels.staffSingular.toLowerCase()}/gün bazlı` },
    { id: "musteriler", label: labels.customerPlural, icon: "👥", desc: `Segment, sıklık, top ${labels.customerSingular.toLowerCase()}` },
    { id: "kampanyalar", label: "Kampanyalar", icon: "📢", desc: "Tıklanma, dönüşüm" },
  ];

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <h1 className="text-2xl font-bold text-slate-900">Raporlar</h1>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {tabs.map((t) => (
          <Link key={t.id} href={`/raporlar/${t.id}`}>
            <Card className="hover:shadow-md transition cursor-pointer">
              <CardContent className="flex items-center gap-4">
                <div className="text-4xl">{t.icon}</div>
                <div>
                  <div className="font-semibold text-slate-900">{t.label}</div>
                  <div className="text-xs text-slate-500">{t.desc}</div>
                </div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
