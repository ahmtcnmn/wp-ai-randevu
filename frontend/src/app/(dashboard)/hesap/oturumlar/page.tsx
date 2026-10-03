"use client";
import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { useToast } from "@/store/ToastContext";
import { userDeviceApi, UserDeviceResponse } from "@/lib/api";
import { formatRelative } from "@/lib/utils/date";

export default function OturumlarPage() {
  const toast = useToast();
  const [devices, setDevices] = useState<UserDeviceResponse[] | null>(null);

  async function load() {
    try {
      const list = await userDeviceApi.list();
      setDevices(list);
    } catch {
      setDevices([]);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function remove(id: number) {
    if (!confirm("Bu cihazı kaldırmak istediğinize emin misiniz?")) return;
    try {
      await userDeviceApi.remove(id);
      toast.success("Cihaz kaldırıldı");
      await load();
    } catch {
      toast.error("Cihaz kaldırılamadı");
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Cihazlar</CardTitle>
          <p className="text-sm text-slate-500 mt-1">
            Push bildirimi alan tüm cihazlarınız burada listelenir. Tanımadığınız bir
            cihaz görüyorsanız kaldırın.
          </p>
        </CardHeader>
        <CardContent className="!p-0">
          {devices === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : devices.length === 0 ? (
            <EmptyState
              icon="📱"
              title="Kayıtlı cihaz yok"
              description="Mobil uygulamayı kullandığınızda cihazınız otomatik kaydedilir."
            />
          ) : (
            <ul className="divide-y divide-slate-100">
              {devices.map((d) => (
                <li key={d.id} className="flex items-center justify-between px-6 py-4">
                  <div className="flex items-center gap-3">
                    <div className="text-3xl">
                      {d.platform === "ios" ? "📱" : d.platform === "android" ? "🤖" : "💻"}
                    </div>
                    <div>
                      <div className="text-sm font-medium text-slate-900 capitalize">
                        {d.platform || "Bilinmeyen"}
                      </div>
                      <div className="text-xs text-slate-500">
                        Son kullanım: {d.sonKullanim ? formatRelative(d.sonKullanim) : "—"}
                      </div>
                      <div className="text-[10px] text-slate-400 font-mono truncate max-w-xs">
                        {d.expoPushToken.substring(0, 30)}...
                      </div>
                    </div>
                  </div>
                  <Button variant="danger" size="sm" onClick={() => remove(d.id)}>
                    Kaldır
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
