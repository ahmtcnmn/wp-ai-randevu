"use client";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";

export default function SlotKampanyasiBilgiPage() {
  return (
    <div className="p-4 lg:p-8 max-w-xl mx-auto space-y-4">
      <Link href="/kampanyalar"><Button variant="ghost" size="sm">← Kampanyalar</Button></Link>
      <Card>
        <CardHeader><CardTitle>Slot Kampanyası</CardTitle></CardHeader>
        <CardContent>
          <Alert variant="info">
            Slot kampanyaları otomatik tetiklenir. Bir randevu iptal edildiğinde uygun müşterilere WhatsApp daveti gider.
            Mesaj şablonunu ve segment hedeflemesini WhatsApp şablon ayarlarından düzenleyebilirsiniz.
          </Alert>
          <div className="mt-4 flex gap-2">
            <Link href="/ayarlar/whatsapp"><Button>WhatsApp Ayarları</Button></Link>
            <Link href="/kampanyalar"><Button variant="secondary">Mevcut Kampanyalar</Button></Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
