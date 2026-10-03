"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Tabs } from "@/components/ui/Tabs";
import {
  campaignApi, SlotCampaignResponse, SegmentCampaignResponse,
} from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateTime } from "@/lib/utils/date";
import { useSector } from "@/store/SectorContext";

export default function KampanyalarPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [tab, setTab] = useState<"slot" | "segment">("segment");
  const [slot, setSlot] = useState<SlotCampaignResponse[] | null>(null);
  const [segment, setSegment] = useState<SegmentCampaignResponse[] | null>(null);

  async function load() {
    if (tab === "slot") {
      try { setSlot(await campaignApi.listSlot()); } catch { setSlot([]); }
    } else {
      try { setSegment(await campaignApi.listSegment()); } catch { setSegment([]); }
    }
  }
  useEffect(() => { load(); }, [tab]);

  async function cancelSlot(id: number) {
    if (!confirm("Slot kampanyası iptal edilsin mi?")) return;
    try {
      await campaignApi.cancelSlot(id);
      toast.success("İptal edildi");
      await load();
    } catch (err) { toast.error(extractApiError(err)); }
  }

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Kampanyalar</h1>
        <Link href="/kampanyalar/segment"><Button>+ Segment Kampanyası</Button></Link>
      </div>

      <Card>
        <CardHeader className="!pb-0">
          <Tabs
            tabs={[
              { id: "slot", label: "Slot Kampanyaları (Otomatik)" },
              { id: "segment", label: "Segment Kampanyaları" },
            ]}
            active={tab}
            onChange={(id) => setTab(id as typeof tab)}
          />
        </CardHeader>
        <CardContent className="!p-0">
          {tab === "slot" ? (
            !slot ? <div className="flex justify-center py-12"><Spinner /></div> :
            slot.length === 0 ? <EmptyState icon="📢" title="Slot kampanyası yok" description={`${labels.appointmentSingular} iptal edildiğinde otomatik tetiklenir`} /> : (
              <ul className="divide-y divide-slate-100">
                {slot.map((c) => (
                  <li key={c.id} className="px-6 py-3 flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{c.hizmetAd} — {c.uzmanAd}</span>
                        <Badge variant={c.durum === "ACTIVE" ? "info" : c.durum === "FILLED" ? "success" : "default"}>{c.durum}</Badge>
                      </div>
                      <p className="text-xs text-slate-500">{formatDateTime(c.randevuTarihi)} · Gönderim {c.gonderimSayisi} / Başarılı {c.basariliSayisi}</p>
                    </div>
                    {c.durum === "ACTIVE" && <Button size="sm" variant="danger" onClick={() => cancelSlot(c.id)}>İptal</Button>}
                  </li>
                ))}
              </ul>
            )
          ) : (
            !segment ? <div className="flex justify-center py-12"><Spinner /></div> :
            segment.length === 0 ? <EmptyState icon="📨" title="Segment kampanyası yok" description={`{ad} placeholder'ı müşteri adıyla otomatik değiştirilir.`} action={<Link href="/kampanyalar/segment"><Button>+ Oluştur</Button></Link>} /> : (
              <ul className="divide-y divide-slate-100">
                {segment.map((c) => (
                  <li key={c.id} className="px-6 py-3">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{c.baslik}</span>
                      <Badge variant="info">{c.hedefSegment}</Badge>
                      <Badge>{c.durum}</Badge>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">Hedef {c.hedefSayisi} · Gönderim {c.gonderimSayisi} · Başarılı {c.basariliSayisi}</p>
                    <p className="text-xs text-slate-400 mt-1">{formatDateTime(c.createdAt)}</p>
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
