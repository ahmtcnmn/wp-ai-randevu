"use client";
import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Badge } from "@/components/ui/Badge";
import { useToast } from "@/store/ToastContext";
import { useNotifications } from "@/store/NotificationContext";
import { notificationApi, NotificationResponse } from "@/lib/api";
import { Page } from "@/types/api";
import { formatRelative } from "@/lib/utils/date";
import { cn } from "@/lib/utils/cn";

const TIP_ICONS: Record<string, string> = {
  APPOINTMENT_CREATED: "📅",
  APPOINTMENT_CANCELLED: "❌",
  APPOINTMENT_COMPLETED: "✅",
  QUOTA_WARNING: "⚠️",
  QUOTA_EXCEEDED: "🚫",
  PAYMENT_SUCCESS: "💰",
  PAYMENT_FAILED: "💸",
  WHATSAPP_HANDOFF: "💬",
  SUBSCRIPTION_RENEWED: "🔄",
};

export default function NotificationsPage() {
  const toast = useToast();
  const { refresh } = useNotifications();
  const [page, setPage] = useState<Page<NotificationResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [pageNum, setPageNum] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const p = await notificationApi.list(pageNum, 20);
      setPage(p);
    } catch {
      toast.error("Bildirimler yüklenemedi");
    } finally {
      setLoading(false);
    }
  }, [pageNum, toast]);

  useEffect(() => {
    load();
  }, [load]);

  async function markRead(id: number) {
    try {
      await notificationApi.read(id);
      await load();
      await refresh();
    } catch {
      toast.error("İşlem başarısız");
    }
  }

  async function markAllRead() {
    try {
      const count = await notificationApi.readAll();
      toast.success(`${count} bildirim okundu işaretlendi`);
      await load();
      await refresh();
    } catch {
      toast.error("İşlem başarısız");
    }
  }

  async function remove(id: number) {
    if (!confirm("Bildirimi silmek istediğinize emin misiniz?")) return;
    try {
      await notificationApi.remove(id);
      await load();
      await refresh();
    } catch {
      toast.error("Silinemedi");
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-4xl mx-auto">
      <Card>
        <CardHeader className="flex items-center justify-between flex-row">
          <CardTitle>Bildirimler</CardTitle>
          {page && page.totalElements > 0 && (
            <Button variant="ghost" size="sm" onClick={markAllRead}>
              Hepsini Okundu Yap
            </Button>
          )}
        </CardHeader>
        <CardContent className="!p-0">
          {loading ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : !page || page.empty ? (
            <EmptyState
              icon="🔔"
              title="Hiç bildiriminiz yok"
              description="Yeni bir bildirim olduğunda burada görüntülenecek."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {page.content.map((n) => (
                <li
                  key={n.id}
                  className={cn(
                    "flex items-start gap-3 px-6 py-4 hover:bg-slate-50",
                    !n.okundu && "bg-blue-50/40"
                  )}
                >
                  <div className="text-2xl flex-shrink-0">
                    {TIP_ICONS[n.tip] || "🔔"}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-slate-900 text-sm">{n.baslik}</span>
                      {!n.okundu && (
                        <span className="w-2 h-2 bg-[var(--color-primary)] rounded-full" aria-label="Okunmadı" />
                      )}
                    </div>
                    {n.icerik && (
                      <p className="text-sm text-slate-600 mt-0.5 line-clamp-2">{n.icerik}</p>
                    )}
                    <div className="flex items-center gap-3 mt-2 text-xs">
                      <Badge variant="default">{n.tip.replace(/_/g, " ")}</Badge>
                      <span className="text-slate-400">{formatRelative(n.createdAt)}</span>
                      {n.link && (
                        <Link
                          href={n.link}
                          onClick={() => !n.okundu && markRead(n.id)}
                          className="text-[var(--color-primary)] hover:underline"
                        >
                          Görüntüle →
                        </Link>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 flex-shrink-0">
                    {!n.okundu && (
                      <button
                        type="button"
                        onClick={() => markRead(n.id)}
                        className="text-xs text-slate-500 hover:text-slate-900 px-2 py-1"
                      >
                        Okundu
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={() => remove(n.id)}
                      className="text-slate-400 hover:text-red-600 px-2"
                      aria-label="Sil"
                    >
                      🗑
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          {page && page.totalPages > 1 && (
            <div className="px-6 py-3 border-t border-slate-200 flex items-center justify-between text-sm">
              <span className="text-slate-500">
                {page.number * page.size + 1}-{page.number * page.size + page.numberOfElements} /{" "}
                {page.totalElements}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page.first}
                  onClick={() => setPageNum((p) => p - 1)}
                >
                  ←
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page.last}
                  onClick={() => setPageNum((p) => p + 1)}
                >
                  →
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
