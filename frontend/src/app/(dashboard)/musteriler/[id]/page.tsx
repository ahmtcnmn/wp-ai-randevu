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
import { Avatar } from "@/components/ui/Avatar";
import { customerApi, appointmentApi, CustomerResponse, AppointmentResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { formatDateTime, formatRelative } from "@/lib/utils/date";
import { formatMoney } from "@/lib/utils/money";
import { useSector } from "@/store/SectorContext";

export default function MusteriDetayPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const customerId = Number(id);
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();

  const [customer, setCustomer] = useState<CustomerResponse | null>(null);
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newTag, setNewTag] = useState("");

  async function load() {
    setLoading(true);
    try {
      const c = await customerApi.get(customerId);
      setCustomer(c);
      const all = await appointmentApi.list();
      setAppointments(all.filter((a) => a.customerId === customerId));
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, [customerId]);

  async function addTag() {
    if (!newTag.trim()) return;
    try {
      await customerApi.addTag(customerId, newTag.trim());
      setNewTag("");
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function block() {
    if (!confirm(`${labels.customerSingular} kara listeye alınsın mı?`)) return;
    try {
      await customerApi.block(customerId);
      toast.success(`${labels.customerSingular} kara listeye alındı`);
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  async function unblock() {
    try {
      await customerApi.unblock(customerId);
      toast.success(`${labels.customerSingular} kara listeden çıkarıldı`);
      await load();
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  if (loading) {
    return <div className="flex justify-center py-12"><Spinner size="lg" /></div>;
  }
  if (error) {
    return <div className="p-8"><Alert variant="error">{error}</Alert></div>;
  }
  if (!customer) return null;

  const totalSpent = appointments
    .filter((a) => a.durum === "TAMAMLANDI")
    .reduce((s, a) => s + (a.toplamFiyat || 0), 0);

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-4">
      <Button variant="ghost" size="sm" onClick={() => router.push("/musteriler")}>
        ← {labels.customerPlural}
      </Button>

      {customer.karaListedeMi && (
        <Alert variant="error" title="Bu müşteri kara listede">
          Bu müşteri için yeni randevu oluşturulamaz.
        </Alert>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card className="lg:col-span-2">
          <CardHeader className="flex items-center gap-4 flex-row">
            <Avatar name={`${customer.ad} ${customer.soyad}`} size="lg" />
            <div className="flex-1 min-w-0">
              <CardTitle>{customer.ad} {customer.soyad}</CardTitle>
              <p className="text-sm text-slate-500">
                {customer.telefon}{customer.email && ` · ${customer.email}`}
              </p>
            </div>
            {customer.karaListedeMi ? (
              <Button variant="secondary" size="sm" onClick={unblock}>Kara Listeden Çıkar</Button>
            ) : (
              <Button variant="danger" size="sm" onClick={block}>Kara Listeye Al</Button>
            )}
          </CardHeader>
          <CardContent className="space-y-3">
            {customer.notlar && (
              <div>
                <div className="text-xs uppercase text-slate-500 mb-1">Notlar</div>
                <div className="text-sm text-slate-700 bg-slate-50 rounded-md p-3">{customer.notlar}</div>
              </div>
            )}

            <div>
              <div className="text-xs uppercase text-slate-500 mb-2">Etiketler</div>
              <div className="flex gap-2 flex-wrap items-center">
                {customer.etiketler.map((e) => (
                  <Badge key={e} variant="info">{e}</Badge>
                ))}
                <div className="flex gap-1">
                  <input
                    type="text"
                    value={newTag}
                    onChange={(e) => setNewTag(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), addTag())}
                    placeholder="Yeni etiket..."
                    className="text-xs px-2 py-1 border border-slate-300 rounded outline-none focus:ring-1 focus:ring-[var(--color-primary)]"
                  />
                  <Button size="sm" variant="secondary" onClick={addTag}>+</Button>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="space-y-3">
            <Stat label="Sadakat puanı" value={`${customer.sadakatPuani}`} />
            <Stat label="Toplam ziyaret" value={appointments.filter(a => a.durum === "TAMAMLANDI").length} />
            <Stat label="Toplam harcama" value={formatMoney(totalSpent)} />
            <Stat label="Gelmeme sayısı" value={customer.gelmemeSayisi} />
            {customer.sonZiyaret && (
              <Stat label="Son ziyaret" value={formatRelative(customer.sonZiyaret)} />
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex items-center justify-between flex-row">
          <CardTitle>{labels.appointmentSingular} Geçmişi</CardTitle>
          <Link href={`/randevular/yeni?customerId=${customer.id}`}>
            <Button size="sm">+ Yeni {labels.appointmentSingular}</Button>
          </Link>
        </CardHeader>
        <CardContent className="!p-0">
          {appointments.length === 0 ? (
            <div className="text-center py-8 text-sm text-slate-500">Henüz randevu yok</div>
          ) : (
            <ul className="divide-y divide-slate-100">
              {appointments.sort((a, b) => b.tarihSaat.localeCompare(a.tarihSaat)).map((a) => (
                <li key={a.id}>
                  <Link
                    href={`/randevular/${a.id}`}
                    className="flex items-center justify-between px-6 py-3 hover:bg-slate-50"
                  >
                    <div>
                      <div className="text-sm font-medium text-slate-900">
                        {formatDateTime(a.tarihSaat)}
                      </div>
                      <div className="text-xs text-slate-500">
                        {a.hizmetler.map(h => h.hizmetAd).join(", ")} · {a.uzmanAd}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-sm text-slate-700">{formatMoney(a.toplamFiyat)}</span>
                      <Badge variant={a.durum === "TAMAMLANDI" ? "success" : "default"}>
                        {a.durum}
                      </Badge>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex justify-between items-baseline">
      <span className="text-xs text-slate-500">{label}</span>
      <span className="text-sm font-semibold text-slate-900">{value}</span>
    </div>
  );
}
