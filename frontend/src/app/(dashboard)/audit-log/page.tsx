"use client";
import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Badge } from "@/components/ui/Badge";
import { Modal } from "@/components/ui/Modal";
import { auditApi, AuditLogResponse } from "@/lib/api";
import { formatDateTime } from "@/lib/utils/date";
import { auditActionLabel, AUDIT_ACTION_LABELS } from "@/lib/utils/auditLabels";

const ACTION_COLOR: Record<string, "success" | "warning" | "danger" | "info" | "default"> = {
  CREATE: "success",
  UPDATE: "warning",
  DELETE: "danger",
  LOGIN: "info",
  LOGIN_FAILED: "danger",
  PERMISSION_GRANTED: "info",
};

export default function AuditLogPage() {
  const [list, setList] = useState<AuditLogResponse[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [action, setAction] = useState("");
  const [selected, setSelected] = useState<AuditLogResponse | null>(null);

  async function load() {
    setLoading(true);
    try {
      const res = await auditApi.list(0, 100, action || undefined);
      setList(res.content || []);
    } catch { setList([]); }
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <h1 className="text-2xl font-bold text-slate-900">Denetim Kayıtları</h1>

      <Card>
        <CardContent className="flex flex-wrap items-end gap-3">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">Aksiyon</label>
            <select value={action} onChange={(e) => setAction(e.target.value)} className="px-3 py-2 border border-slate-300 rounded-md text-sm">
              <option value="">Tümü</option>
              {Object.entries(AUDIT_ACTION_LABELS).map(([code, label]) => (
                <option key={code} value={code}>{label}</option>
              ))}
            </select>
          </div>
          <Button onClick={load} loading={loading}>Filtrele</Button>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="!p-0">
          {!list ? <div className="flex justify-center py-12"><Spinner /></div> :
           list.length === 0 ? <EmptyState icon="📜" title="Kayıt yok" /> : (
            <table className="w-full">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500 border-b border-slate-200">
                <tr>
                  <th className="px-6 py-3 text-left">Zaman</th>
                  <th className="px-6 py-3 text-left">Kullanıcı</th>
                  <th className="px-6 py-3 text-left">Aksiyon</th>
                  <th className="px-6 py-3 text-left">Entity</th>
                  <th className="px-6 py-3 text-left">IP</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {list.map((l) => (
                  <tr key={l.id} className="hover:bg-slate-50 cursor-pointer" onClick={() => setSelected(l)}>
                    <td className="px-6 py-2 text-xs">{formatDateTime(l.createdAt)}</td>
                    <td className="px-6 py-2 text-sm">{l.userEmail || (l.userId ? `#${l.userId}` : "—")}</td>
                    <td className="px-6 py-2"><Badge variant={ACTION_COLOR[l.action] || "default"}>{auditActionLabel(l.action)}</Badge></td>
                    <td className="px-6 py-2 text-sm">{l.entityType}{l.entityId ? ` #${l.entityId}` : ""}</td>
                    <td className="px-6 py-2 text-xs text-slate-500">{l.ipAddress || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      <Modal isOpen={!!selected} onClose={() => setSelected(null)} title="Detay" size="lg">
        {selected && (
          <div className="space-y-2 text-sm">
            <Row label="Zaman" value={formatDateTime(selected.createdAt)} />
            <Row label="Kullanıcı" value={selected.userEmail || (selected.userId ? `#${selected.userId}` : "—")} />
            <Row label="Aksiyon" value={auditActionLabel(selected.action)} />
            <Row label="Entity" value={`${selected.entityType ?? "—"}${selected.entityId ? ` #${selected.entityId}` : ""}`} />
            <Row label="IP" value={selected.ipAddress || "—"} />
            <Row label="User-Agent" value={selected.userAgent || "—"} />
            {selected.details && (
              <div>
                <div className="text-xs text-slate-500 mb-1">Detay</div>
                <pre className="bg-slate-50 p-3 rounded text-xs overflow-auto max-h-64">{JSON.stringify(selected.details, null, 2)}</pre>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return <div className="flex gap-3"><span className="text-slate-500 w-28 text-xs">{label}</span><span className="text-slate-900">{value}</span></div>;
}
