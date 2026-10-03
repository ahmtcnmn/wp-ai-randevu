"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";
import { customerApi, CustomerImportResponse } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { useSector } from "@/store/SectorContext";

export default function ImportPage() {
  const router = useRouter();
  const toast = useToast();
  const { labels } = useSector();
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CustomerImportResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onUpload() {
    if (!file) return;
    setError(null);
    setResult(null);
    setLoading(true);
    try {
      const r = await customerApi.importCsv(file);
      setResult(r);
      if (r.basarili > 0) {
        toast.success(`${r.basarili} ${labels.customerSingular.toLowerCase()} eklendi`);
      }
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="p-4 lg:p-8 max-w-2xl mx-auto space-y-4">
      <Button variant="ghost" size="sm" onClick={() => router.push("/musteriler")}>
        ← {labels.customerPlural}
      </Button>

      <Card>
        <CardHeader>
          <CardTitle>CSV ile {labels.customerSingular} İçe Aktar</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Alert variant="info">
            <div className="text-sm">
              <p className="font-semibold mb-1">CSV formatı:</p>
              <ul className="list-disc list-inside text-xs space-y-0.5">
                <li>UTF-8, virgülle ayrılmış</li>
                <li>İlk satır header olmalı: <code className="bg-white px-1 rounded">ad,soyad,telefon,email,notlar</code></li>
                <li>Zorunlu alanlar: <b>ad</b>, <b>soyad</b>, <b>telefon</b></li>
                <li>Dosya boyutu en fazla 5MB</li>
              </ul>
            </div>
          </Alert>

          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1.5">CSV Dosyası</label>
            <input
              type="file"
              accept=".csv"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="block w-full text-sm border border-slate-300 rounded-md cursor-pointer file:mr-4 file:py-2 file:px-4 file:border-0 file:bg-slate-100 file:text-slate-700 file:font-medium file:cursor-pointer hover:file:bg-slate-200"
            />
          </div>

          <Button onClick={onUpload} disabled={!file} loading={loading} fullWidth>
            Yükle ve İçe Aktar
          </Button>

          {error && <Alert variant="error">{error}</Alert>}

          {result && (
            <Alert
              variant={result.hatalar.length > 0 ? "warning" : "success"}
              title={`${result.basarili} / ${result.toplam} müşteri eklendi`}
            >
              {result.hatalar.length > 0 && (
                <div className="mt-2 text-xs">
                  <p className="font-semibold mb-1">Hatalar:</p>
                  <ul className="space-y-0.5 max-h-40 overflow-y-auto">
                    {result.hatalar.map((h, i) => (
                      <li key={i}>
                        Satır {h.satir}: {h.mesaj}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </Alert>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
