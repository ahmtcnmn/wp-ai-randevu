"use client";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";

export default function FaturalarPage() {
  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Faturalar</h1>
        <Link href="/finans"><Button variant="ghost" size="sm">← Finans</Button></Link>
      </div>
      <Card>
        <CardHeader><CardTitle>Bilgi</CardTitle></CardHeader>
        <CardContent>
          <Alert variant="info">
            Faturalar e-posta ile gönderilmektedir. Detay/talep için iletişim sayfasını kullanabilirsiniz.
          </Alert>
        </CardContent>
      </Card>
    </div>
  );
}
