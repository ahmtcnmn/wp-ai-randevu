"use client";
import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { useToast } from "@/store/ToastContext";
import { api } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";
import { checkPassword } from "@/lib/utils/validation";

export default function SifrePage() {
  const router = useRouter();
  const toast = useToast();
  const [mevcutSifre, setMevcutSifre] = useState("");
  const [yeniSifre, setYeniSifre] = useState("");
  const [yeniSifre2, setYeniSifre2] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (yeniSifre !== yeniSifre2) return setError("Yeni şifreler eşleşmiyor");
    const pw = checkPassword(yeniSifre);
    if (!pw.valid) return setError(pw.message || "Yeni şifre geçersiz");

    setLoading(true);
    try {
      await api.put("/api/v1/auth/change-password", { mevcutSifre, yeniSifre });
      toast.success("Şifreniz güncellendi");
      router.push("/hesap");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-md mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Şifre Değiştir</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-4">
            {error && <Alert variant="error">{error}</Alert>}
            <Input
              label="Mevcut Şifre"
              type="password"
              value={mevcutSifre}
              onChange={(e) => setMevcutSifre(e.target.value)}
              required
              autoComplete="current-password"
            />
            <Input
              label="Yeni Şifre"
              type="password"
              value={yeniSifre}
              onChange={(e) => setYeniSifre(e.target.value)}
              required
              autoComplete="new-password"
              helper="En az 8 karakter — büyük/küçük harf, rakam içermeli"
            />
            <Input
              label="Yeni Şifre (tekrar)"
              type="password"
              value={yeniSifre2}
              onChange={(e) => setYeniSifre2(e.target.value)}
              required
              autoComplete="new-password"
            />
            <Button type="submit" fullWidth loading={loading}>
              Şifreyi Değiştir
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
