"use client";
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Alert } from "@/components/ui/Alert";
import { authApi } from "@/lib/api";
import { useAuth } from "@/store/AuthContext";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";

export default function HesapAyarlariPage() {
  const router = useRouter();
  const toast = useToast();
  const { user, logout } = useAuth();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmText, setConfirmText] = useState("");
  const [loading, setLoading] = useState(false);

  const canDelete = user?.rol === "OWNER";

  async function deleteAccount() {
    if (confirmText !== "SİL") {
      toast.error('Doğrulamak için "SİL" yazın');
      return;
    }
    setLoading(true);
    try {
      await authApi.deleteAccount();
      toast.success("Hesabınız silinmek üzere işaretlendi");
      await logout();
      router.push("/login");
    } catch (err) {
      toast.error(extractApiError(err));
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Hesap</h1>
        <Link href="/ayarlar"><Button variant="ghost" size="sm">← Ayarlar</Button></Link>
      </div>

      <Card>
        <CardHeader><CardTitle>Hesap Bilgileri</CardTitle></CardHeader>
        <CardContent className="space-y-2 text-sm">
          <Row label="E-posta" value={user?.email} />
          <Row label="Ad Soyad" value={`${user?.ad ?? ""} ${user?.soyad ?? ""}`} />
          <Row label="Rol" value={user?.rol} />
        </CardContent>
      </Card>

      {canDelete && (
        <Card className="border-red-200">
          <CardHeader>
            <CardTitle className="text-red-700">Tehlikeli Bölge</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Alert variant="warning">
              Hesabınızı sildiğinizde işletmenizin tüm verileri (müşteri, randevu, çalışan, fatura)
              30 gün boyunca arşivde tutulur. Bu süre içinde destekle iletişime geçerek geri yükleyebilirsiniz.
              30 gün sonra tüm veriler kalıcı olarak silinir.
            </Alert>
            <Button variant="danger" onClick={() => setConfirmOpen(true)}>
              Hesabımı Sil
            </Button>
          </CardContent>
        </Card>
      )}

      <Modal
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        title="Hesabı Sil"
        footer={<>
          <Button variant="secondary" onClick={() => setConfirmOpen(false)}>İptal</Button>
          <Button variant="danger" onClick={deleteAccount} loading={loading} disabled={confirmText !== "SİL"}>
            Kalıcı Olarak Sil
          </Button>
        </>}
      >
        <div className="space-y-3">
          <p className="text-sm text-slate-700">
            Bu işlem aboneliğinizi iptal edecek ve 30 gün sonra tüm verileriniz silinecek.
          </p>
          <p className="text-sm">
            Onaylamak için aşağıya <code className="bg-slate-100 px-1 rounded">SİL</code> yazın:
          </p>
          <input
            type="text"
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder="SİL"
            className="w-full px-3 py-2 border border-red-300 rounded-md text-sm"
            autoFocus
          />
        </div>
      </Modal>
    </div>
  );
}

function Row({ label, value }: { label: string; value?: string | null }) {
  return (
    <div className="flex">
      <span className="text-slate-500 w-32">{label}</span>
      <span className="text-slate-900">{value || "—"}</span>
    </div>
  );
}
