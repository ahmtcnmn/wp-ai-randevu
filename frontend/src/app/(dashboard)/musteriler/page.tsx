"use client";
import { useEffect, useState, useMemo } from "react";
import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { customerApi, CustomerResponse } from "@/lib/api";
import { useDebounce } from "@/hooks/useDebounce";
import { useToast } from "@/store/ToastContext";
import { useSector } from "@/store/SectorContext";

export default function MusterilerPage() {
  const toast = useToast();
  const { labels } = useSector();
  const [customers, setCustomers] = useState<CustomerResponse[] | null>(null);
  const [query, setQuery] = useState("");
  const debounced = useDebounce(query, 250);

  useEffect(() => {
    customerApi.list().then(setCustomers).catch(() => {
      toast.error(`${labels.customerSingular} listesi yüklenemedi`);
      setCustomers([]);
    });
  }, [toast, labels]);

  const filtered = useMemo(() => {
    if (!customers) return [];
    const q = debounced.trim().toLowerCase();
    if (!q) return customers;
    return customers.filter((c) =>
      `${c.ad} ${c.soyad} ${c.telefon} ${c.email || ""}`.toLowerCase().includes(q)
    );
  }, [customers, debounced]);

  return (
    <div className="p-4 lg:p-8 max-w-7xl mx-auto space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">{labels.customerPlural}</h1>
        <div className="flex gap-2">
          <Link href="/musteriler/segmentler">
            <Button variant="secondary">Segmentler</Button>
          </Link>
          <Link href="/musteriler/import">
            <Button variant="secondary">CSV İçe Aktar</Button>
          </Link>
          <Link href="/musteriler/yeni">
            <Button>+ Yeni {labels.customerSingular}</Button>
          </Link>
        </div>
      </div>

      <Card>
        <CardHeader>
          <Input
            placeholder="Müşteri ara (ad, telefon, e-posta)..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </CardHeader>
        <CardContent className="!p-0">
          {customers === null ? (
            <div className="flex justify-center py-12"><Spinner /></div>
          ) : filtered.length === 0 ? (
            <EmptyState
              icon="👥"
              title={query ? "Eşleşen müşteri yok" : "Henüz müşteri yok"}
              description={query ? "Arama kriterlerinizi değiştirin" : "İlk müşterinizi ekleyin"}
              action={
                !query ? (
                  <Link href="/musteriler/yeni">
                    <Button>+ Yeni Müşteri</Button>
                  </Link>
                ) : undefined
              }
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-slate-50 border-b border-slate-200 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-6 py-3 text-left font-medium">Ad Soyad</th>
                    <th className="px-6 py-3 text-left font-medium">Telefon</th>
                    <th className="px-6 py-3 text-left font-medium hidden md:table-cell">E-posta</th>
                    <th className="px-6 py-3 text-left font-medium">Etiketler</th>
                    <th className="px-6 py-3 text-right font-medium">Sadakat</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filtered.map((c) => (
                    <tr key={c.id} className="hover:bg-slate-50">
                      <td className="px-6 py-3">
                        <Link
                          href={`/musteriler/${c.id}`}
                          className="font-medium text-slate-900 hover:text-[var(--color-primary)]"
                        >
                          {c.ad} {c.soyad}
                        </Link>
                        {c.karaListedeMi && (
                          <Badge variant="danger" className="ml-2">Kara Liste</Badge>
                        )}
                      </td>
                      <td className="px-6 py-3 text-sm text-slate-600">{c.telefon}</td>
                      <td className="px-6 py-3 text-sm text-slate-600 hidden md:table-cell">{c.email || "—"}</td>
                      <td className="px-6 py-3">
                        <div className="flex gap-1 flex-wrap">
                          {c.etiketler.length === 0 && (
                            <Badge variant="info">Yeni Müşteri</Badge>
                          )}
                          {c.etiketler.slice(0, 2).map((e) => (
                            <Badge key={e} variant="info">{e}</Badge>
                          ))}
                          {c.etiketler.length > 2 && (
                            <Badge variant="default">+{c.etiketler.length - 2}</Badge>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-3 text-sm text-slate-700 text-right">
                        {c.sadakatPuani} puan
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
