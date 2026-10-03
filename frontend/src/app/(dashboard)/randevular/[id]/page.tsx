"use client";
import { useEffect, useState, use } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { Alert } from "@/components/ui/Alert";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import {
  appointmentApi, AppointmentResponse, ProductSaleResponse, RandevuDurumu,
} from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateLong, formatTime } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

const STATUS_OPTIONS: { id: RandevuDurumu; label: string; v: "warning" | "info" | "success" | "danger" | "default" }[] = [
  { id: "BEKLIYOR", label: "Bekliyor", v: "warning" },
  { id: "ONAYLANDI", label: "Onayla", v: "info" },
  { id: "TAMAMLANDI", label: "Tamamlandı", v: "success" },
  { id: "GELMEDI", label: "Gelmedi", v: "danger" },
];

export default function RandevuDetayPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const randevuId = Number(id);
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [data, setData] = useState<AppointmentResponse | null>(null);
  const [sales, setSales] = useState<ProductSaleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCancel, setShowCancel] = useState(false);
  const [cancelReason, setCancelReason] = useState("");
  const [showNote, setShowNote] = useState(false);
  const [noteContent, setNoteContent] = useState("");
  const [noteTur, setNoteTur] = useState<"INTERNAL" | "MUSTERI">("INTERNAL");

  async function load() {
    setLoading(true);
    try {
      const a = await appointmentApi.get(randevuId);
      setData(a);
      try {
        const s = await appointmentApi.listProducts(randevuId);
        setSales(s);
      } catch {
        setSales([]);
      }
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [randevuId]);

  async function changeStatus(durum: RandevuDurumu, toplamFiyat?: number) {
    try {
      await appointmentApi.updateStatus(randevuId, { durum, toplamFiyat });
      toast.success("Durum güncellendi");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function cancelAppt() {
    try {
      await appointmentApi.cancel(randevuId, cancelReason);
      toast.success(`${labels.appointmentSingular} iptal edildi`);
      setShowCancel(false);
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function addNote() {
    if (!noteContent.trim()) return;
    try {
      await appointmentApi.addNote(randevuId, { icerik: noteContent, tur: noteTur });
      toast.success("Not eklendi");
      setShowNote(false);
      setNoteContent("");
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  if (loading) return <div className="flex justify-center py-12"><Spinner size="lg" /></div>;
  if (error) return <div className="p-8"><Alert variant="error">{error}</Alert></div>;
  if (!data) return null;

  const isTerminal =
    data.durum === "TAMAMLANDI" ||
    data.durum === "IPTAL_EDILDI" ||
    data.durum === "GELMEDI";

  const salesTotal = sales.reduce((s, x) => s + x.toplamTutar, 0);

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <Button variant="ghost" size="sm" onClick={() => router.push("/randevular")}>
          ← {labels.appointmentPlural}
        </Button>
        <div className="flex gap-2">
          {!isTerminal && (
            <>
              <Link href={`/randevular/${randevuId}/duzenle`}>
                <Button variant="secondary" size="sm">Düzenle</Button>
              </Link>
              <Button variant="danger" size="sm" onClick={() => setShowCancel(true)}>
                İptal Et
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>{labels.appointmentSingular} #{data.id}</CardTitle>
            <p className="text-sm text-slate-500 mt-1">{formatDateLong(data.tarihSaat)} · {formatTime(data.tarihSaat)}</p>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Info label={labels.customerSingular} value={data.musteriAd} />
              <Info label={labels.staffSingular} value={data.uzmanAd} />
              <Info label="Süre" value={`${data.toplamSureDk} dk`} />
              <Info label="Hizmet Tutarı" value={formatMoney(data.toplamFiyat)} />
              <Info label="Kaynak" value={data.kaynak} />
              <Info label="Durum" value={<StatusBadge durum={data.durum} />} />
            </div>

            {(data.urunToplami ?? 0) > 0 && (
              <div className="bg-slate-50 border border-slate-200 rounded-md p-3 space-y-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-slate-600">Hizmet:</span>
                  <span className="font-medium">{formatMoney(data.toplamFiyat)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Ürünler:</span>
                  <span className="font-medium">{formatMoney(data.urunToplami ?? 0)}</span>
                </div>
                <div className="flex justify-between pt-1 border-t border-slate-300 text-base">
                  <span className="font-semibold">Genel Toplam:</span>
                  <span className="font-bold">{formatMoney(data.genelToplam ?? data.toplamFiyat)}</span>
                </div>
              </div>
            )}

            <div>
              <div className="text-xs uppercase text-slate-500 mb-2">{labels.servicePlural}</div>
              <ul className="divide-y divide-slate-100 border rounded-md">
                {data.hizmetler.map((h, i) => (
                  <li key={i} className="px-4 py-2 flex justify-between text-sm">
                    <span>{h.hizmetAd} <span className="text-slate-400">({h.sureDk}dk)</span></span>
                    <span className="font-medium">{formatMoney(h.fiyat)}</span>
                  </li>
                ))}
              </ul>
            </div>

            {data.not && (
              <div>
                <div className="text-xs uppercase text-slate-500 mb-1">Not</div>
                <div className="text-sm bg-slate-50 rounded-md p-3">{data.not}</div>
              </div>
            )}

            {data.iptalNedeni && (
              <Alert variant="warning" title="İptal Nedeni">{data.iptalNedeni}</Alert>
            )}
          </CardContent>
        </Card>

        <div className="space-y-4">
          {!isTerminal && (
            <Card>
              <CardHeader><CardTitle>Durum Değiştir</CardTitle></CardHeader>
              <CardContent className="space-y-2">
                {STATUS_OPTIONS.filter((o) => o.id !== data.durum).map((o) => (
                  <Button
                    key={o.id}
                    variant="secondary"
                    fullWidth
                    onClick={() => changeStatus(o.id)}
                  >
                    {o.label}
                  </Button>
                ))}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader><CardTitle>İşlemler</CardTitle></CardHeader>
            <CardContent className="space-y-2">
              <Button variant="secondary" fullWidth onClick={() => setShowNote(true)}>
                + Not Ekle
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Ürün satışları (Sprint 6) */}
      <Card>
        <CardHeader className="flex items-center justify-between flex-row">
          <CardTitle>Ürün Satışları</CardTitle>
          <Link href={`/randevular/${randevuId}/urun-ekle`}>
            <Button size="sm">+ Ürün Sat</Button>
          </Link>
        </CardHeader>
        <CardContent className="!p-0">
          {sales.length === 0 ? (
            <div className="text-center py-8 text-sm text-slate-500">
              Henüz ürün satışı yok
            </div>
          ) : (
            <table className="w-full">
              <thead className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                <tr>
                  <th className="px-6 py-3 text-left font-medium">Ürün</th>
                  <th className="px-6 py-3 text-right font-medium">Adet</th>
                  <th className="px-6 py-3 text-right font-medium">Birim</th>
                  <th className="px-6 py-3 text-right font-medium">Toplam</th>
                  <th className="px-6 py-3 text-right font-medium hidden md:table-cell">Komisyon</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {sales.map((s) => (
                  <tr key={s.id}>
                    <td className="px-6 py-3 text-sm font-medium text-slate-900">{s.productAd}</td>
                    <td className="px-6 py-3 text-sm text-right">{s.adet}</td>
                    <td className="px-6 py-3 text-sm text-right">{formatMoney(s.birimFiyatSnapshot)}</td>
                    <td className="px-6 py-3 text-sm font-medium text-right">{formatMoney(s.toplamTutar)}</td>
                    <td className="px-6 py-3 text-sm text-right text-slate-600 hidden md:table-cell">
                      {s.commissionAmount ? formatMoney(s.commissionAmount) : "—"}
                    </td>
                  </tr>
                ))}
                <tr className="bg-slate-50 font-semibold">
                  <td colSpan={3} className="px-6 py-3 text-sm text-right">Toplam</td>
                  <td className="px-6 py-3 text-sm text-right">{formatMoney(salesTotal)}</td>
                  <td className="hidden md:table-cell" />
                </tr>
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      {/* Modal: Cancel */}
      <Modal
        isOpen={showCancel}
        onClose={() => setShowCancel(false)}
        title={`${labels.appointmentSingular}u İptal Et`}
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowCancel(false)}>Vazgeç</Button>
            <Button variant="danger" onClick={cancelAppt}>İptal Et</Button>
          </>
        }
      >
        <div className="space-y-3">
          <Alert variant="warning">
            Bu işlem geri alınamaz. Slot kampanyası tetiklenebilir.
          </Alert>
          <Input
            label="İptal Sebebi (opsiyonel)"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder={`${labels.customerSingular} rica etti, ...`}
          />
        </div>
      </Modal>

      {/* Modal: Note */}
      <Modal
        isOpen={showNote}
        onClose={() => setShowNote(false)}
        title="Not Ekle"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowNote(false)}>İptal</Button>
            <Button onClick={addNote}>Ekle</Button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setNoteTur("INTERNAL")}
              className={`flex-1 px-3 py-2 rounded-md border text-sm font-medium ${noteTur === "INTERNAL" ? "bg-[var(--color-primary)] text-white border-[var(--color-primary)]" : "border-slate-300"}`}
            >
              🔒 Dahili
            </button>
            <button
              type="button"
              onClick={() => setNoteTur("MUSTERI")}
              className={`flex-1 px-3 py-2 rounded-md border text-sm font-medium ${noteTur === "MUSTERI" ? "bg-[var(--color-primary)] text-white border-[var(--color-primary)]" : "border-slate-300"}`}
            >
              👤 {labels.customerSingular} ile Paylaşılır
            </button>
          </div>
          <textarea
            value={noteContent}
            onChange={(e) => setNoteContent(e.target.value)}
            rows={4}
            placeholder="Not içeriği..."
            className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm outline-none focus:ring-2 focus:ring-[var(--color-primary)] focus:border-transparent"
          />
        </div>
      </Modal>
    </div>
  );
}

function Info({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <div className="text-xs uppercase text-slate-500 mb-0.5">{label}</div>
      <div className="text-sm text-slate-900 font-medium">{value}</div>
    </div>
  );
}

function StatusBadge({ durum }: { durum: RandevuDurumu }) {
  const map: Record<RandevuDurumu, { v: "success" | "warning" | "danger" | "info" | "default"; label: string }> = {
    BEKLIYOR: { v: "warning", label: "Bekliyor" },
    ONAYLANDI: { v: "info", label: "Onaylı" },
    TAMAMLANDI: { v: "success", label: "Tamamlandı" },
    GELMEDI: { v: "danger", label: "Gelmedi" },
    IPTAL_EDILDI: { v: "default", label: "İptal" },
  };
  return <Badge variant={map[durum].v}>{map[durum].label}</Badge>;
}
