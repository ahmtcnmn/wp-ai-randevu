"use client";
import { useEffect, useState, FormEvent } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { tenantApi, TenantResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

export default function AyarlarPage() {
  const toast = useToast();
  const [tenant, setTenant] = useState<TenantResponse | null>(null);
  const [form, setForm] = useState({
    ad: "", telefon: "", email: "", adres: "", sehir: "", ulke: "", tckn: "", logoUrl: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    tenantApi.get().then((t) => {
      setTenant(t);
      setForm({
        ad: t.ad,
        telefon: t.telefon || "",
        email: t.email || "",
        adres: t.adres || "",
        sehir: t.sehir || "",
        ulke: t.ulke || "",
        tckn: t.tckn || "",
        logoUrl: t.logoUrl || "",
      });
    });
  }, []);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!form.ad.trim()) return setError("İşletme adı zorunlu");
    setLoading(true);
    try {
      const t = await tenantApi.update(form);
      setTenant(t);
      toast.success("Bilgiler güncellendi");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  if (!tenant) return <div className="flex justify-center py-12"><Spinner /></div>;

  const items = [
    { href: "/ayarlar/subeler", icon: "🏢", title: "Şubeler", desc: "Şube CRUD yönetimi" },
    { href: "/ayarlar/whatsapp", icon: "💬", title: "WhatsApp", desc: "Config + şablonlar" },
    { href: "/ayarlar/ai", icon: "🤖", title: "AI Asistan", desc: "Persona, model, handoff" },
    { href: "/ayarlar/ozellikler", icon: "🔧", title: "Özellikler", desc: "Modül aç/kapat" },
    { href: "/ayarlar/iptal-politikasi", icon: "📋", title: "İptal Politikası", desc: "Saat, ücret, mesaj" },
    { href: "/hatirlatma/sablonlar", icon: "🔔", title: "Hatırlatma Şablonları", desc: "Mesaj şablonları" },
    { href: "/ayarlar/plan", icon: "💼", title: "Plan & Abonelik", desc: "Mevcut plan, faturalar" },
    { href: "/ayarlar/hesap", icon: "👤", title: "Hesap", desc: "E-posta, şifre, hesabı sil" },
    { href: "/audit-log", icon: "📜", title: "Denetim Kayıtları", desc: "Sistemdeki tüm aktiviteler" },
  ];

  return (
    <div className="p-4 lg:p-8 max-w-5xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">Ayarlar</h1>

      <Card>
        <CardHeader><CardTitle>İşletme Bilgileri</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <Input label="İşletme Adı" value={form.ad} onChange={(e) => setForm({ ...form, ad: e.target.value })} required />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Telefon" value={form.telefon} onChange={(e) => setForm({ ...form, telefon: e.target.value })} />
              <Input label="E-posta" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
            </div>
            <Input label="Adres" value={form.adres} onChange={(e) => setForm({ ...form, adres: e.target.value })} />
            <div className="grid grid-cols-3 gap-3">
              <Input label="Şehir" value={form.sehir} onChange={(e) => setForm({ ...form, sehir: e.target.value })} />
              <Input label="Ülke" value={form.ulke} onChange={(e) => setForm({ ...form, ulke: e.target.value })} />
              <Input label="TCKN (fatura için)" value={form.tckn} onChange={(e) => setForm({ ...form, tckn: e.target.value })} maxLength={11} />
            </div>
            <Input label="Logo URL (opsiyonel)" value={form.logoUrl} onChange={(e) => setForm({ ...form, logoUrl: e.target.value })} placeholder="https://..." />
            <Button type="submit" loading={loading}>Kaydet</Button>
          </form>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {items.map((it) => (
          <Link key={it.href} href={it.href}>
            <Card className="hover:shadow-md transition-shadow cursor-pointer">
              <CardContent className="flex items-center gap-4">
                <div className="text-3xl">{it.icon}</div>
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-slate-900">{it.title}</div>
                  <div className="text-xs text-slate-500">{it.desc}</div>
                </div>
                <span className="text-slate-400">→</span>
              </CardContent>
            </Card>
          </Link>
        ))}
      </div>
    </div>
  );
}
