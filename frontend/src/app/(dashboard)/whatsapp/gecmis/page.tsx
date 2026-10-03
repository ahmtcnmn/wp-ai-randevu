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

export default function WhatsappGecmisPage() {
  const [list, setList] = useState<ConversationResponse[] | null>(null);

  useEffect(() => {
    conversationApi.history().then(setList).catch(() => setList([]));
  }, []);

  const closed = (list || []).filter((c) => c.durum === "CLOSED");

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">WhatsApp Geçmiş</h1>
        <Link href="/whatsapp"><Button variant="secondary">← Aktif Konuşmalar</Button></Link>
      </div>

      <Card>
        <CardContent className="!p-0">
          {list === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : closed.length === 0 ? (
            <EmptyState icon="📜" title="Henüz kapalı konuşma yok" />
          ) : (
            <ul className="divide-y divide-slate-100">
              {closed.map((c) => (
                <li key={c.id}>
                  <Link href={`/whatsapp/${c.id}`} className="flex items-center justify-between px-6 py-3 hover:bg-slate-50">
                    <div>
                      <div className="font-medium text-slate-900">{c.customerName || c.customerPhone}</div>
                      <div className="text-xs text-slate-500">
                        {c.customerPhone} · Kapatılma: {c.kapatmaTarihi ? formatRelative(c.kapatmaTarihi) : "—"}
                      </div>
                    </div>
                    <Badge variant="default">CLOSED</Badge>
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
