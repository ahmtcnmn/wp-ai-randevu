"use client";
import { useState, FormEvent, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { campaignApi, customerApi, SegmentType } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

const SEGMENTS: { key: SegmentType; label: string }[] = [
  { key: "NEW", label: "Yeni Müşteri" },
  { key: "REGULAR", label: "Düzenli" },
  { key: "LOYAL", label: "Sadık" },
  { key: "VIP", label: "VIP" },
  { key: "OCCASIONAL", label: "Ara sıra" },
  { key: "DRIFTING", label: "Uzaklaşıyor" },
  { key: "AT_RISK", label: "Riskli" },
  { key: "LOST", label: "Kaybedilmiş" },
];

export default function SegmentKampanyasiPage() {
  const router = useRouter();
  const toast = useToast();
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [form, setForm] = useState({
    baslik: "",
    hedefSegment: "LOST" as SegmentType,
    mesaj: "Sevgili {ad}, sizi özledik! Yeniden randevu alın, %15 indirim sizi bekliyor.",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    customerApi.segmentSummary().then((s) => setCounts(s.segmentCounts as any)).catch(() => {});
  }, []);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setLoading(true); setError(null);
    try {
      await campaignApi.createSegment(form);
      toast.success("Kampanya oluşturuldu");
      router.push("/kampanyalar");
    } catch (err) { setError(extractApiError(err)); }
    finally { setLoading(false); }
  }

  return (
    <div className="p-4 lg:p-8 max-w-xl mx-auto">
      <Button variant="ghost" size="sm" onClick={() => router.back()}>← Geri</Button>
      <Card className="mt-2">
        <CardHeader><CardTitle>Segment Kampanyası</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={submit} className="space-y-3">
            <Alert variant="info">Belirli segmente WhatsApp ile toplu mesaj gönderir.</Alert>
            {error && <Alert variant="error">{error}</Alert>}
            <Input label="Başlık" value={form.baslik} onChange={(e) => setForm({ ...form, baslik: e.target.value })} required />
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Hedef Segment</label>
              <select value={form.hedefSegment} onChange={(e) => setForm({ ...form, hedefSegment: e.target.value as SegmentType })} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm">
                {SEGMENTS.map((s) => (
                  <option key={s.key} value={s.key}>{s.label} ({counts[s.key] ?? 0})</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Mesaj</label>
              <textarea value={form.mesaj} onChange={(e) => setForm({ ...form, mesaj: e.target.value })} rows={4} className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm" required />
              <p className="text-xs text-slate-500 mt-1">{"{ad}"} placeholder'ı desteklenir.</p>
            </div>
            <div className="flex gap-2 pt-2">
              <Button variant="secondary" onClick={() => router.back()}>İptal</Button>
              <Button type="submit" loading={loading}>Oluştur</Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
