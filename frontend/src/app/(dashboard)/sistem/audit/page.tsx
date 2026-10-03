"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Badge } from "@/components/ui/Badge";
import { auditApi, AuditLogResponse } from "@/lib/api";
import { formatDateTime } from "@/lib/utils/date";

export default function SistemAuditPage() {
  const [list, setList] = useState<AuditLogResponse[] | null>(null);
  const [search, setSearch] = useState("");

  useEffect(() => {
    auditApi.list(0, 200).then((p) => setList(p.content || [])).catch(() => setList([]));
  }, []);

  const filtered = (list || []).filter((l) =>
    !search || JSON.stringify(l).toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Sistem Audit Log</h1>
        <Link href="/sistem"><Button variant="ghost" size="sm">← Sistem</Button></Link>
      </div>

      <Input placeholder="Ara..." value={search} onChange={(e) => setSearch(e.target.value)} />

      <Card>
        <CardContent className="!p-0">
          {!list ? <div className="flex justify-center py-12"><Spinner /></div> :
           filtered.length === 0 ? <EmptyState icon="📜" title="Kayıt yok" /> : (
            <table className="w-full">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3 text-left">Zaman</th>
                  <th className="px-6 py-3 text-left">Tenant</th>
                  <th className="px-6 py-3 text-left">Kullanıcı</th>
                  <th className="px-6 py-3 text-left">Aksiyon</th>
                  <th className="px-6 py-3 text-left">Entity</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filtered.map((l) => (
                  <tr key={l.id}>
                    <td className="px-6 py-2 text-xs">{formatDateTime(l.createdAt)}</td>
                    <td className="px-6 py-2 text-sm">{l.tenantId ?? "—"}</td>
                    <td className="px-6 py-2 text-sm">{l.userEmail || (l.userId ? `#${l.userId}` : "—")}</td>
                    <td className="px-6 py-2"><Badge>{l.action}</Badge></td>
                    <td className="px-6 py-2 text-sm">{l.entityType ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
