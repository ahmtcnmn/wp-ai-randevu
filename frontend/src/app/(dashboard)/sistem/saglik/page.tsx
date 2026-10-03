"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { API_URL } from "@/lib/constants";

export default function SaglikPage() {
  const [health, setHealth] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  async function load() {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/actuator/health`, { credentials: "include" });
      setHealth(await res.json());
    } catch { setHealth(null); }
    finally { setLoading(false); }
  }

  useEffect(() => {
    load();
    const t = setInterval(load, 30000);
    return () => clearInterval(t);
  }, []);

  const status = health?.status || "UNKNOWN";
  const components = health?.components || {};

  return (
    <div className="p-4 lg:p-8 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Sistem Sağlığı</h1>
        <div className="flex gap-2">
          <Button onClick={load} loading={loading}>Yenile</Button>
          <Link href="/sistem"><Button variant="ghost" size="sm">← Sistem</Button></Link>
        </div>
      </div>

      <Card>
        <CardHeader><CardTitle>Genel Durum</CardTitle></CardHeader>
        <CardContent>
          <div className="flex items-center gap-3">
            <span className="text-2xl">{status === "UP" ? "✓" : "⚠"}</span>
            <Badge variant={status === "UP" ? "success" : "danger"}>{status}</Badge>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle>Bileşenler</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {Object.keys(components).length === 0 ? (
            <p className="text-sm text-slate-500">Veri yok (actuator yetkisi gerekebilir)</p>
          ) : Object.entries(components).map(([k, v]: [string, any]) => (
            <div key={k} className="flex items-center justify-between border-b border-slate-100 pb-2">
              <span className="font-medium">{k}</span>
              <Badge variant={v?.status === "UP" ? "success" : "danger"}>{v?.status || "UNKNOWN"}</Badge>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
}
