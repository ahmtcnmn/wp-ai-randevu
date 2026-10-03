"use client";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Alert } from "@/components/ui/Alert";

export default function SistemKullanicilarPage() {
  return (
    <div className="p-4 lg:p-8 max-w-3xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Sistem Kullanıcıları</h1>
        <Link href="/sistem"><Button variant="ghost" size="sm">← Sistem</Button></Link>
      </div>
      <Card>
        <CardContent>
          <Alert variant="info">
            Tüm sistem kullanıcılarını tek listede görmek için tenant detayına gidip o tenant'ın kullanıcılarını listeleyebilirsiniz.
          </Alert>
          <Link href="/sistem/tenants" className="inline-block mt-4">
            <Button>Tenantlara Git</Button>
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}
