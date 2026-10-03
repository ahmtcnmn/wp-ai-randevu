"use client";
import { useState, FormEvent, useEffect } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { useAuth } from "@/store/AuthContext";
import { useToast } from "@/store/ToastContext";
import { api } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";
import { isTrPhone, normalizeTrPhone } from "@/lib/utils/validation";

export default function ProfilPage() {
  const { fullUser, refreshMe } = useAuth();
  const toast = useToast();
  const [ad, setAd] = useState("");
  const [soyad, setSoyad] = useState("");
  const [telefon, setTelefon] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (fullUser) {
      setAd(fullUser.ad);
      setSoyad(fullUser.soyad);
      setTelefon(fullUser.telefon);
    }
  }, [fullUser]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (!ad.trim() || !soyad.trim()) return setError("Ad ve soyad zorunlu");
    if (!isTrPhone(telefon)) return setError("Geçerli bir telefon girin");
    setLoading(true);
    try {
      await api.put("/api/v1/auth/me", { ad, soyad, telefon: normalizeTrPhone(telefon) });
      toast.success("Profil güncellendi");
      await refreshMe();
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">Profil</h1>

      <Card>
        <CardHeader>
          <CardTitle>Profil Bilgileri</CardTitle>
        </CardHeader>
        <CardContent>
          {!fullUser ? (
            <div className="text-sm text-slate-500">Yükleniyor...</div>
          ) : (
            <form onSubmit={onSubmit} className="space-y-4">
              {error && <Alert variant="error">{error}</Alert>}
              <div className="grid grid-cols-2 gap-3">
                <Input label="Ad" value={ad} onChange={(e) => setAd(e.target.value)} required />
                <Input label="Soyad" value={soyad} onChange={(e) => setSoyad(e.target.value)} required />
              </div>
              <Input
                label="E-posta"
                value={fullUser.email}
                disabled
                helper="E-posta değiştirilemez"
              />
              <Input
                label="Telefon"
                value={telefon}
                onChange={(e) => setTelefon(e.target.value)}
                required
              />
              <div className="flex items-center gap-2 text-sm">
                <span className="text-slate-600">Rol:</span>
                <span className="font-medium text-slate-900">{fullUser.rol}</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-slate-600">E-posta Durumu:</span>
                {fullUser.emailDogrulandi ? (
                  <span className="text-green-700">✓ Doğrulandı</span>
                ) : (
                  <span className="text-amber-700">⚠ Doğrulanmadı</span>
                )}
              </div>
              <Button type="submit" loading={loading}>
                Kaydet
              </Button>
            </form>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent className="space-y-3">
          <Link href="/hesap/sifre" className="block px-4 py-3 -mx-6 hover:bg-slate-50">
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium text-slate-900">Şifre Değiştir</div>
                <div className="text-sm text-slate-500">Mevcut şifrenizi yeni bir şifreyle değiştirin</div>
              </div>
              <span className="text-slate-400">→</span>
            </div>
          </Link>
          <Link href="/hesap/2fa" className="block px-4 py-3 -mx-6 hover:bg-slate-50">
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium text-slate-900">İki Faktörlü Doğrulama</div>
                <div className="text-sm text-slate-500">Hesabınızı authenticator uygulamasıyla koruyun</div>
              </div>
              <span className="text-slate-400">→</span>
            </div>
          </Link>
          <Link href="/hesap/oturumlar" className="block px-4 py-3 -mx-6 hover:bg-slate-50">
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium text-slate-900">Cihazlar</div>
                <div className="text-sm text-slate-500">Push bildirimi alan cihazlarınız</div>
              </div>
              <span className="text-slate-400">→</span>
            </div>
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}
