"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { adminApi, ContactRequestResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateTime } from "@/lib/utils/date";

export default function ContactRequestsPage() {
  const toast = useToast();
  const [list, setList] = useState<ContactRequestResponse[] | null>(null);

  async function load() {
    try {
      const res = await adminApi.listContactRequests(0, 50);
      setList(res.content || []);
    } catch { setList([]); }
  }
  useEffect(() => { load(); }, []);

  async function setDurum(id: number, durum: string) {
    try {
      await adminApi.updateContactRequest(id, durum);
      toast.success("Güncellendi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">İletişim Talepleri</h1>
        <Link href="/sistem"><Button variant="ghost" size="sm">← Sistem</Button></Link>
      </div>

      <Card>
        <CardContent className="!p-0">
          {!list ? <div className="flex justify-center py-12"><Spinner /></div> :
           list.length === 0 ? <EmptyState icon="✉️" title="Talep yok" /> : (
            <ul className="divide-y divide-slate-100">
              {list.map((c) => (
                <li key={c.id} className="px-6 py-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-medium">{c.ad} {c.soyad}</span>
                        <span className="text-xs text-slate-500">{c.email}</span>
                        {c.telefon && <span className="text-xs text-slate-500">· {c.telefon}</span>}
                        <Badge variant={c.durum === "RESOLVED" ? "success" : c.durum === "IN_PROGRESS" ? "warning" : "info"}>{c.durum}</Badge>
                      </div>
                      <p className="text-sm text-slate-600 mt-1 whitespace-pre-wrap">{c.mesaj}</p>
                      <p className="text-xs text-slate-400 mt-1">{formatDateTime(c.createdAt)}</p>
                    </div>
                    {c.durum !== "RESOLVED" && (
                      <div className="flex flex-col gap-1">
                        {c.durum !== "IN_PROGRESS" && (
                          <Button size="sm" variant="secondary" onClick={() => setDurum(c.id, "IN_PROGRESS")}>Üzerinde</Button>
                        )}
                        <Button size="sm" onClick={() => setDurum(c.id, "RESOLVED")}>Çözüldü</Button>
                      </div>
                    )}
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
