"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { featureApi, FeatureResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

export default function OzelliklerPage() {
  const toast = useToast();
  const [list, setList] = useState<FeatureResponse[] | null>(null);

  async function load() {
    try {
      const l = await featureApi.list();
      setList(l);
    } catch {
      setList([]);
    }
  }

  useEffect(() => { load(); }, []);

  async function toggle(featureKey: string, enabled: boolean) {
    try {
      await featureApi.toggle(featureKey, enabled);
      toast.success(enabled ? "Açıldı" : "Kapatıldı");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  if (!list) return <div className="flex justify-center py-12"><Spinner /></div>;

  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Özellikler</h1>
        <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Modül Aç / Kapat</CardTitle>
        </CardHeader>
        <CardContent className="!p-0">
          <ul className="divide-y divide-slate-100">
            {list.map((f) => (
              <li key={f.featureKey} className="px-6 py-4 flex items-center justify-between">
                <div>
                  <div className="font-medium text-slate-900">{f.ad}</div>
                  <div className="text-xs text-slate-500 mt-0.5">{f.aciklama}</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={f.enabled}
                    onChange={(e) => toggle(f.featureKey, e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-slate-300 peer-checked:bg-[var(--color-primary)] rounded-full peer transition-colors after:content-[''] after:absolute after:top-0.5 after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:after:translate-x-5" />
                </label>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
