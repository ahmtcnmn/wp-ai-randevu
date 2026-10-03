"use client";
import { useState, FormEvent, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Alert } from "@/components/ui/Alert";
import { useAuth } from "@/store/AuthContext";
import { useToast } from "@/store/ToastContext";
import { authApi, sectorApi, SectorTypeInfo, BusinessType } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";
import { normalizeTrPhone, isTrPhone, isEmail, checkPassword } from "@/lib/utils/validation";

const SECTOR_ICONS: Record<string, string> = {
  BARBER: "💈",
  HAIR_SALON: "💇‍♀️",
  DENTAL_CLINIC: "🦷",
  BEAUTY_SALON: "💅",
  SPA: "🧖",
  GYM: "🏋️",
  VETERINARY: "🐾",
  PET_GROOMING: "🐩",
  NAIL_SALON: "💅",
  TATTOO: "🎨",
  OTHER: "🏢",
};

export default function RegisterPage() {
  const router = useRouter();
  const { setSession } = useAuth();
  const toast = useToast();

  const [step, setStep] = useState<1 | 2>(1);
  const [sectors, setSectors] = useState<SectorTypeInfo[]>([]);
  const [businessType, setBusinessType] = useState<BusinessType | null>(null);

  const [form, setForm] = useState({
    ad: "",
    soyad: "",
    email: "",
    telefon: "",
    sifre: "",
    sifreTekrar: "",
    isletmeAdi: "",
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    sectorApi.types().then(setSectors).catch(() => {
      // Backend gelmezse fallback
      setSectors([
        { code: "BARBER", name: "Erkek Berber Salonu" },
        { code: "HAIR_SALON", name: "Kadın Kuaförü" },
        { code: "OTHER", name: "Diğer / Genel" },
      ]);
    });
  }, []);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function selectSector(code: BusinessType) {
    setBusinessType(code);
    setStep(2);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!form.ad || !form.soyad) return setError("Ad ve soyad zorunlu");
    if (!isEmail(form.email)) return setError("Geçerli bir e-posta girin");
    if (!isTrPhone(form.telefon)) return setError("Geçerli bir telefon girin (örn: 05XX XXX XX XX)");
    if (form.sifre !== form.sifreTekrar) return setError("Şifreler eşleşmiyor");
    const pw = checkPassword(form.sifre);
    if (!pw.valid) return setError(pw.message || "Şifre geçersiz");

    setLoading(true);
    try {
      const resp = await authApi.register({
        ad: form.ad,
        soyad: form.soyad,
        email: form.email,
        sifre: form.sifre,
        telefon: normalizeTrPhone(form.telefon),
        businessType: businessType ?? "OTHER",
        isletmeAdi: form.isletmeAdi || undefined,
      });
      setSession(resp);
      toast.success("Hesabınız oluşturuldu! E-postanıza gelen doğrulama linkini kontrol edin.");
      router.push("/onboarding");
    } catch (err) {
      setError(extractApiError(err, "Kayıt başarısız"));
    } finally {
      setLoading(false);
    }
  }

  // Sadece BARBER + HAIR_SALON üstte, diğerleri "Daha fazla" altında
  const FEATURED: BusinessType[] = ["BARBER", "HAIR_SALON"];
  const featured = sectors.filter((s) => FEATURED.includes(s.code));
  const other = sectors.filter((s) => !FEATURED.includes(s.code));

  if (step === 1) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>İşletme Türünüz</CardTitle>
          <p className="text-sm text-slate-500 mt-1">
            Size en uygun deneyimi sunabilmemiz için işletme türünüzü seçin.
          </p>
        </CardHeader>
        <CardContent>
          <div className="space-y-3">
            {featured.map((s) => (
              <button
                key={s.code}
                type="button"
                onClick={() => selectSector(s.code)}
                className="w-full text-left p-4 rounded-xl border-2 border-slate-200 hover:border-[var(--color-primary)] hover:bg-slate-50 transition flex items-center gap-4"
              >
                <span className="text-3xl">{SECTOR_ICONS[s.code] ?? "🏢"}</span>
                <div className="flex-1">
                  <div className="font-semibold text-slate-900">{s.name}</div>
                  <div className="text-xs text-slate-500">
                    {s.code === "BARBER" && "Saç, sakal, traş hizmetleri"}
                    {s.code === "HAIR_SALON" && "Saç, makyaj, bakım hizmetleri"}
                  </div>
                </div>
                <span className="text-slate-400">→</span>
              </button>
            ))}

            {other.length > 0 && (
              <details className="pt-2">
                <summary className="cursor-pointer text-sm text-slate-600 hover:text-slate-900 select-none">
                  Diğer işletme türleri ({other.length})
                </summary>
                <div className="mt-3 space-y-2">
                  {other.map((s) => (
                    <button
                      key={s.code}
                      type="button"
                      onClick={() => selectSector(s.code)}
                      className="w-full text-left p-3 rounded-lg border border-slate-200 hover:border-[var(--color-primary)] hover:bg-slate-50 transition flex items-center gap-3"
                    >
                      <span className="text-xl">{SECTOR_ICONS[s.code] ?? "🏢"}</span>
                      <span className="flex-1 text-sm text-slate-700">{s.name}</span>
                      <span className="text-slate-400">→</span>
                    </button>
                  ))}
                </div>
              </details>
            )}
          </div>

          <p className="text-xs text-slate-500 mt-6 text-center">
            Zaten hesabınız var mı?{" "}
            <Link href="/login" className="text-[var(--color-primary)] font-medium hover:underline">
              Giriş yapın
            </Link>
          </p>
        </CardContent>
      </Card>
    );
  }

  const sectorName = sectors.find((s) => s.code === businessType)?.name ?? "İşletme";

  return (
    <Card>
      <CardHeader>
        <button
          type="button"
          onClick={() => setStep(1)}
          className="text-xs text-slate-500 hover:text-slate-900 mb-2"
        >
          ← İşletme türünü değiştir
        </button>
        <CardTitle>Hesap Oluştur</CardTitle>
        <p className="text-sm text-slate-500 mt-1">
          <span className="text-2xl mr-2">{SECTOR_ICONS[businessType ?? "OTHER"]}</span>
          {sectorName}
        </p>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="space-y-4">
          {error && <Alert variant="error">{error}</Alert>}
          <Input
            label="İşletme Adı"
            value={form.isletmeAdi}
            onChange={(e) => update("isletmeAdi", e.target.value)}
            placeholder="Örn. Ali Berber, Güzelim Kuaför"
            helper="Müşterilerinizin göreceği isim (boş bırakırsanız adınız soyadınız kullanılır)"
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Ad"
              value={form.ad}
              onChange={(e) => update("ad", e.target.value)}
              required
              autoComplete="given-name"
            />
            <Input
              label="Soyad"
              value={form.soyad}
              onChange={(e) => update("soyad", e.target.value)}
              required
              autoComplete="family-name"
            />
          </div>
          <Input
            label="E-posta"
            type="email"
            value={form.email}
            onChange={(e) => update("email", e.target.value)}
            required
            autoComplete="email"
            placeholder="ornek@email.com"
          />
          <Input
            label="Telefon"
            type="tel"
            value={form.telefon}
            onChange={(e) => update("telefon", e.target.value)}
            required
            autoComplete="tel"
            placeholder="05XX XXX XX XX"
            helper="Türkiye telefonu — 5 ile başlayan 10 hane"
          />
          <Input
            label="Şifre"
            type="password"
            value={form.sifre}
            onChange={(e) => update("sifre", e.target.value)}
            required
            autoComplete="new-password"
            helper="En az 8 karakter — büyük/küçük harf, rakam içermeli"
          />
          <Input
            label="Şifre (tekrar)"
            type="password"
            value={form.sifreTekrar}
            onChange={(e) => update("sifreTekrar", e.target.value)}
            required
            autoComplete="new-password"
          />
          <p className="text-xs text-slate-500">
            Hesap oluşturarak{" "}
            <Link href="/sartlar" className="underline">Kullanım Şartları</Link> ve{" "}
            <Link href="/gizlilik" className="underline">Gizlilik Politikası</Link>'nı kabul etmiş olursunuz.
          </p>
          <Button type="submit" fullWidth loading={loading} size="lg">
            14 Gün Ücretsiz Hesap Oluştur
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
