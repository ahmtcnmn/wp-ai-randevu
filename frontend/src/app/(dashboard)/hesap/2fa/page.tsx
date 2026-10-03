"use client";
import { useState, useEffect, FormEvent } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { Modal } from "@/components/ui/Modal";
import { useAuth } from "@/store/AuthContext";
import { useToast } from "@/store/ToastContext";
import { authApi } from "@/lib/api";
import { extractApiError } from "@/hooks/useApiError";

type Step = "loading" | "off" | "setup" | "verify" | "done" | "on";

export default function TwoFactorPage() {
  const { fullUser, refreshMe } = useAuth();
  const toast = useToast();

  const [step, setStep] = useState<Step>("loading");
  const [recoveryCount, setRecoveryCount] = useState(0);
  const [setupData, setSetupData] = useState<{ secret: string; qrCodeDataUri: string } | null>(null);
  const [code, setCode] = useState("");
  const [recoveryCodes, setRecoveryCodes] = useState<string[]>([]);
  const [showDisable, setShowDisable] = useState(false);
  const [disablePassword, setDisablePassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!fullUser) return;
    // fullUser'da 2fa bilgisi yok ama auth.me yine de durum gönderebilir
    // basit kontrol: recovery count > 0 ise muhtemelen aktif
    authApi.twoFactor
      .recoveryCodesCount()
      .then((c) => {
        setRecoveryCount(c);
        setStep(c > 0 ? "on" : "off");
      })
      .catch(() => setStep("off"));
  }, [fullUser]);

  async function startSetup() {
    setLoading(true);
    setError(null);
    try {
      const data = await authApi.twoFactor.setup();
      setSetupData(data);
      setStep("setup");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  async function verifyCode(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const resp = await authApi.twoFactor.verify(code);
      setRecoveryCodes(resp.recoveryCodes);
      setStep("done");
      toast.success("2FA aktif edildi");
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  async function disable() {
    if (!disablePassword) return;
    setLoading(true);
    setError(null);
    try {
      await authApi.twoFactor.disable(disablePassword);
      toast.success("2FA devre dışı bırakıldı");
      setShowDisable(false);
      setDisablePassword("");
      setStep("off");
      setRecoveryCount(0);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  async function regenerateRecovery() {
    if (!confirm("Eski kurtarma kodları geçersiz olacak. Devam edilsin mi?")) return;
    try {
      const codes = await authApi.twoFactor.regenerateRecoveryCodes();
      setRecoveryCodes(codes);
      setStep("done");
    } catch (err) {
      toast.error(extractApiError(err));
    }
  }

  if (step === "loading") {
    return (
      <div className="p-8 flex justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">İki Faktörlü Doğrulama</h1>

      {error && <Alert variant="error">{error}</Alert>}

      {step === "off" && (
        <Card>
          <CardHeader>
            <CardTitle>2FA Kapalı</CardTitle>
          </CardHeader>
          <CardContent>
            <Alert variant="warning" title="Hesabınız daha güvenli olabilir">
              İki faktörlü doğrulama, hesabınıza giriş sırasında şifrenize ek olarak
              telefonunuzdaki bir koddan da onay ister.
            </Alert>
            <div className="mt-4">
              <Button onClick={startSetup} loading={loading}>
                2FA'yı Etkinleştir
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === "setup" && setupData && (
        <Card>
          <CardHeader>
            <CardTitle>1. Adım: QR Kodu Tarayın</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <ol className="list-decimal list-inside text-sm text-slate-600 space-y-1">
              <li>Telefonunuzda Google Authenticator, Authy veya 1Password açın</li>
              <li>"Hesap ekle" → "QR tara" seçeneğini seçin</li>
              <li>Aşağıdaki QR kodu tarayın</li>
            </ol>
            <div className="flex justify-center">
              <img
                src={setupData.qrCodeDataUri}
                alt="2FA QR Code"
                className="border-4 border-slate-200 rounded-lg"
                width={200}
                height={200}
              />
            </div>
            <div className="bg-slate-50 rounded-md p-3 text-center">
              <div className="text-xs text-slate-500 mb-1">QR çalışmıyor mu? Manuel kod:</div>
              <code className="text-sm font-mono text-slate-900 tracking-wider">
                {setupData.secret.match(/.{1,4}/g)?.join(" ")}
              </code>
            </div>
            <Button onClick={() => setStep("verify")} fullWidth>
              QR Tarandı, Devam Et
            </Button>
          </CardContent>
        </Card>
      )}

      {step === "verify" && (
        <Card>
          <CardHeader>
            <CardTitle>2. Adım: Doğrulama</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={verifyCode} className="space-y-4">
              <Input
                label="Authenticator'dan 6 haneli kod"
                value={code}
                onChange={(e) => setCode(e.target.value.trim())}
                required
                autoFocus
                inputMode="numeric"
                placeholder="123456"
              />
              <Button type="submit" fullWidth loading={loading}>
                Doğrula ve Aktif Et
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {step === "done" && (
        <Card>
          <CardHeader>
            <CardTitle>3. Adım: Kurtarma Kodları</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Alert variant="warning" title="Bu kodları güvenli bir yere kaydedin!">
              Telefonunuzu kaybederseniz tek giriş yolu bunlar olacak. Bu kodlar bir
              daha gösterilmeyecek.
            </Alert>
            <div className="bg-slate-50 rounded-lg p-4 font-mono text-sm grid grid-cols-2 gap-2">
              {recoveryCodes.map((c) => (
                <div key={c} className="bg-white px-3 py-2 rounded border border-slate-200 text-center">
                  {c}
                </div>
              ))}
            </div>
            <div className="flex gap-2">
              <Button
                variant="secondary"
                onClick={() => {
                  navigator.clipboard.writeText(recoveryCodes.join("\n"));
                  toast.success("Kodlar kopyalandı");
                }}
              >
                Kopyala
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  const blob = new Blob([recoveryCodes.join("\n")], { type: "text/plain" });
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement("a");
                  a.href = url;
                  a.download = "appointflow-2fa-recovery-codes.txt";
                  a.click();
                }}
              >
                İndir (.txt)
              </Button>
              <Button
                onClick={() => {
                  setStep("on");
                  setRecoveryCount(recoveryCodes.length);
                  refreshMe();
                }}
              >
                Tamam, Kaydettim
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {step === "on" && (
        <Card>
          <CardHeader>
            <CardTitle>2FA Aktif ✓</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Alert variant="success">
              Hesabınız iki faktörlü doğrulama ile korunuyor.
            </Alert>
            <div className="flex items-center justify-between p-3 bg-slate-50 rounded-md">
              <div>
                <div className="text-sm font-medium text-slate-900">Kalan Kurtarma Kodu</div>
                <div className="text-xs text-slate-500">{recoveryCount} kod kullanılabilir</div>
              </div>
              <Button variant="secondary" size="sm" onClick={regenerateRecovery}>
                Yeniden Üret
              </Button>
            </div>
            <Button variant="danger" onClick={() => setShowDisable(true)}>
              2FA'yı Devre Dışı Bırak
            </Button>
          </CardContent>
        </Card>
      )}

      <Modal
        isOpen={showDisable}
        onClose={() => setShowDisable(false)}
        title="2FA'yı Devre Dışı Bırak"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowDisable(false)}>
              İptal
            </Button>
            <Button variant="danger" onClick={disable} loading={loading}>
              Kapat
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Alert variant="warning">
            2FA kapatıldığında hesabınız sadece şifre ile korunacak.
          </Alert>
          <Input
            label="Şifreniz"
            type="password"
            value={disablePassword}
            onChange={(e) => setDisablePassword(e.target.value)}
            autoComplete="current-password"
          />
        </div>
      </Modal>
    </div>
  );
}
