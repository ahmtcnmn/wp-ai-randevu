"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { feedbackApi, FeedbackResponse } from "@/lib/api";
import { formatDate } from "@/lib/utils/date";
import { useSector } from "@/store/SectorContext";

export default function FeedbackPage() {
  const { labels } = useSector();
  const [list, setList] = useState<FeedbackResponse[] | null>(null);

  useEffect(() => {
    feedbackApi.complaints().then(setList).catch(() => setList([]));
  }, []);

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Şikayetler</h1>
        <Link href="/calisanlar"><Button variant="ghost" size="sm">← {labels.staffPlural}</Button></Link>
      </div>

      <Card>
        <CardContent className="!p-0">
          {!list ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : list.length === 0 ? (
            <EmptyState icon="🎉" title="Şikayet yok!" description={`${labels.customerSingular} memnuniyeti iyi.`} />
          ) : (
            <ul className="divide-y divide-slate-100">
              {list.map((f) => (
                <li key={f.id} className="px-6 py-3">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-slate-900">{f.uzmanAd}</span>
                        <Badge variant="danger">{f.puan}/5 ⭐</Badge>
                        <span className="text-xs text-slate-500">{formatDate(f.tarih)}</span>
                      </div>
                      {f.yorum && <p className="text-sm text-slate-600 mt-1">{f.yorum}</p>}
                    </div>
                    <Link href={`/randevular/${f.randevuId}`} className="text-xs text-[var(--color-primary)] hover:underline">
                      {labels.appointmentSingular}yu Gör →
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
