"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { conversationApi, ConversationResponse } from "@/lib/api";
import { formatRelative } from "@/lib/utils/date";
import { useSector } from "@/store/SectorContext";

export default function WhatsappPage() {
  const { labels } = useSector();
  const [list, setList] = useState<ConversationResponse[] | null>(null);

  useEffect(() => {
    conversationApi.active().then(setList).catch(() => setList([]));
  }, []);

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">WhatsApp Konuşmalar</h1>
        <div className="flex gap-2">
          <Link href="/whatsapp/gecmis"><Button variant="secondary">Geçmiş</Button></Link>
          <Link href="/ayarlar/whatsapp"><Button variant="secondary">WhatsApp Ayarları</Button></Link>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>Aktif Konuşmalar</CardTitle></CardHeader>
        <CardContent className="!p-0">
          {list === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : list.length === 0 ? (
            <EmptyState
              icon="💬"
              title="Aktif konuşma yok"
              description={`${labels.customerPlural} WhatsApp üzerinden mesaj attığında burada görünür`}
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {list.map((c) => (
                <li key={c.id}>
                  <Link href={`/whatsapp/${c.id}`} className="flex items-center justify-between px-6 py-3 hover:bg-slate-50">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-slate-900 truncate">
                          {c.customerName || c.customerPhone}
                        </span>
                        <StatusBadge durum={c.durum} />
                      </div>
                      <div className="text-xs text-slate-500 mt-0.5">
                        {c.customerPhone} ·{" "}
                        {c.sonMesajZamani ? `Son mesaj: ${formatRelative(c.sonMesajZamani)}` : `Açılış: ${formatRelative(c.olusturmaTarihi)}`}
                      </div>
                    </div>
                    {c.aktifHandler && (
                      <Badge variant="info">{c.aktifHandler}</Badge>
                    )}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function StatusBadge({ durum }: { durum: string }) {
  const map: Record<string, { v: "success" | "warning" | "danger" | "info" | "default"; label: string }> = {
    ACTIVE: { v: "info", label: "AI Yanıtlıyor" },
    WAITING: { v: "warning", label: "Bekliyor" },
    HUMAN_ACTIVE: { v: "success", label: "İnsan" },
    CLOSED: { v: "default", label: "Kapalı" },
  };
  const conf = map[durum] || { v: "default" as const, label: durum };
  return <Badge variant={conf.v}>{conf.label}</Badge>;
}
