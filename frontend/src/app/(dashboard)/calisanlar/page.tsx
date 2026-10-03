"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import { Card, CardContent } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Avatar } from "@/components/ui/Avatar";
import { userApi } from "@/lib/api";
import { UserResponse } from "@/types/auth";
import { ROLE_LABELS } from "@/lib/utils/role";
import { useSector } from "@/store/SectorContext";

export default function CalisanlarPage() {
  const [list, setList] = useState<UserResponse[] | null>(null);
  const { labels } = useSector();

  useEffect(() => { userApi.list().then(setList).catch(() => setList([])); }, []);

  return (
    <div className="p-4 lg:p-8 max-w-6xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.staffPlural}</h1>
        <div className="flex gap-2">
          <Link href="/calisanlar/feedback"><Button variant="secondary">Şikayetler</Button></Link>
          <Link href="/calisanlar/komisyon"><Button variant="secondary">Komisyon</Button></Link>
          <Link href="/calisanlar/kazanc"><Button variant="secondary">Kazanç</Button></Link>
          <Link href="/calisanlar/yeni"><Button>+ Yeni {labels.staffSingular}</Button></Link>
        </div>
      </div>

      <Card>
        <CardContent className="!p-0">
          {list === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : list.length === 0 ? (
            <EmptyState icon="👔" title={`${labels.staffSingular} yok`} action={<Link href="/calisanlar/yeni"><Button>+ Yeni {labels.staffSingular}</Button></Link>} />
          ) : (
            <ul className="divide-y divide-slate-100">
              {list.filter((u) => u.rol !== "SUPER_ADMIN" && u.rol !== "MUSTERI").map((u) => (
                <li key={u.id}>
                  <Link href={`/calisanlar/${u.id}`} className="flex items-center gap-3 px-6 py-3 hover:bg-slate-50">
                    <Avatar name={`${u.ad} ${u.soyad}`} size="md" />
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-slate-900 flex items-center gap-2">
                        {u.ad} {u.soyad}
                        {!u.aktif && <Badge variant="default">Pasif</Badge>}
                      </div>
                      <div className="text-xs text-slate-500">{u.email} · {u.telefon}</div>
                    </div>
                    <Badge variant="info">{ROLE_LABELS[u.rol]}</Badge>
                    {u.subeAd && <span className="text-xs text-slate-500">{u.subeAd}</span>}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
