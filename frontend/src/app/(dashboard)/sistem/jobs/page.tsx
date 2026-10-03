"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { jobApi, BackgroundJobResponse, DeadLetterJobResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateTime } from "@/lib/utils/date";
import { Tabs } from "@/components/ui/Tabs";

export default function JobsPage() {
  const toast = useToast();
  const [tab, setTab] = useState<"active" | "dead">("active");
  const [active, setActive] = useState<BackgroundJobResponse[] | null>(null);
  const [dead, setDead] = useState<DeadLetterJobResponse[] | null>(null);

  async function load() {
    if (tab === "active") { try { setActive(await jobApi.list()); } catch { setActive([]); } }
    else { try { setDead(await jobApi.deadLetter()); } catch { setDead([]); } }
  }
  useEffect(() => { load(); }, [tab]);

  async function retry(id: number) {
    try { await jobApi.retryDead(id); toast.success("Yeniden denendi"); await load(); }
    catch (err) { toast.error(extractApiError(err)); }
  }
  async function resolve(id: number) {
    try { await jobApi.resolveDead(id); toast.success("Çözüldü"); await load(); }
    catch (err) { toast.error(extractApiError(err)); }
  }

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Background Jobs</h1>
        <Link href="/sistem"><Button variant="ghost" size="sm">← Sistem</Button></Link>
      </div>

      <Card>
        <CardHeader className="!pb-0">
          <Tabs
            tabs={[{ id: "active", label: "Aktif" }, { id: "dead", label: "Dead Letter" }]}
            active={tab}
            onChange={(id) => setTab(id as typeof tab)}
          />
        </CardHeader>
        <CardContent className="!p-0">
          {tab === "active" ? (
            !active ? <div className="flex justify-center py-12"><Spinner /></div> :
            active.length === 0 ? <EmptyState icon="⚙️" title="Aktif job yok" /> : (
              <table className="w-full">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                  <tr>
                    <th className="px-6 py-3 text-left">Tip</th>
                    <th className="px-6 py-3 text-center">Durum</th>
                    <th className="px-6 py-3 text-right">Deneme</th>
                    <th className="px-6 py-3 text-left">Oluşturuldu</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {active.map((j) => (
                    <tr key={j.id}>
                      <td className="px-6 py-2 text-sm font-medium">{j.jobType}</td>
                      <td className="px-6 py-2 text-center">
                        <Badge variant={j.status === "SUCCESS" ? "success" : j.status === "FAILED" || j.status === "DEAD" ? "danger" : j.status === "RUNNING" ? "info" : "warning"}>{j.status}</Badge>
                      </td>
                      <td className="px-6 py-2 text-sm text-right">{j.attemptCount}</td>
                      <td className="px-6 py-2 text-xs">{formatDateTime(j.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
          ) : (
            !dead ? <div className="flex justify-center py-12"><Spinner /></div> :
            dead.length === 0 ? <EmptyState icon="🎉" title="Dead letter yok!" /> : (
              <ul className="divide-y divide-slate-100">
                {dead.map((d) => (
                  <li key={d.id} className="px-6 py-3 flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{d.jobType}</span>
                        <span className="text-xs text-slate-500">{formatDateTime(d.createdAt)}</span>
                      </div>
                      {d.errorMessage && <p className="text-xs text-red-600 mt-0.5">{d.errorMessage}</p>}
                    </div>
                    {!d.resolvedAt && (
                      <div className="flex gap-2">
                        <Button size="sm" onClick={() => retry(d.id)}>Tekrar Dene</Button>
                        <Button size="sm" variant="secondary" onClick={() => resolve(d.id)}>Çözüldü</Button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            )
          )}
        </CardContent>
      </Card>
    </div>
  );
}
