"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card, CardContent } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { billingApi, PlanResponse } from "@/lib/api/billing";
import { formatMoney } from "@/lib/utils/money";

export default function FiyatlandirmaPage() {
  const [plans, setPlans] = useState<PlanResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    billingApi
      .plans()
      .then((p) => setPlans(p.filter((x) => x.aktif).sort((a, b) => a.aylikFiyat - b.aylikFiyat)))
      .catch(() => setError("Planlar yüklenemedi"));
  }, []);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 lg:py-24">
      <div className="text-center max-w-2xl mx-auto mb-12">
        <h1 className="text-4xl lg:text-5xl font-bold text-slate-900">Fiyatlandırma</h1>
        <p className="mt-4 text-lg text-slate-600">
          İşletmenizin büyüklüğüne uygun planı seçin. Her plan 14 gün ücretsiz denenir.
        </p>
      </div>

      {error && <p className="text-center text-red-600">{error}</p>}
      {!plans && !error && (
        <div className="flex justify-center py-8">
          <Spinner size="lg" />
        </div>
      )}

      {plans && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
          {plans.map((plan, idx) => {
            const featured = idx === 1; // ortadaki vurgulu
            return (
              <Card
                key={plan.id}
                className={
                  featured
                    ? "ring-2 ring-[var(--color-primary)] shadow-lg relative"
                    : ""
                }
              >
                {featured && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-[var(--color-primary)] text-white text-xs px-3 py-1 rounded-full font-medium">
                    En Popüler
                  </div>
                )}
                <CardContent>
                  <h3 className="text-lg font-semibold text-slate-900">{plan.ad}</h3>
                  <p className="text-sm text-slate-500 mt-1">{plan.aciklama}</p>
                  <div className="mt-6">
                    <span className="text-4xl font-bold text-slate-900">
                      {formatMoney(plan.aylikFiyat)}
                    </span>
                    <span className="text-sm text-slate-500"> / ay</span>
                  </div>
                  <ul className="mt-6 space-y-2 text-sm text-slate-700">
                    <li className="flex items-center gap-2">
                      <span className="text-green-600">✓</span>
                      {plan.maxSube === null ? "Sınırsız şube" : `${plan.maxSube} şube`}
                    </li>
                    <li className="flex items-center gap-2">
                      <span className="text-green-600">✓</span>
                      {plan.maxCalisan === null
                        ? "Sınırsız çalışan"
                        : `${plan.maxCalisan} çalışan`}
                    </li>
                    <li className="flex items-center gap-2">
                      <span className="text-green-600">✓</span>
                      {plan.maxAylikRandevu === null
                        ? "Sınırsız randevu"
                        : `${plan.maxAylikRandevu} randevu / ay`}
                    </li>
                    <li className="flex items-center gap-2">
                      <span className="text-green-600">✓</span>
                      WhatsApp + AI asistan
                    </li>
                    <li className="flex items-center gap-2">
                      <span className="text-green-600">✓</span>
                      Tüm raporlar
                    </li>
                  </ul>
                  <div className="mt-8">
                    <Link href="/register">
                      <Button fullWidth variant={featured ? "primary" : "secondary"}>
                        14 Gün Ücretsiz Dene
                      </Button>
                    </Link>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      <div className="mt-16 text-center text-sm text-slate-600">
        <p>Daha büyük bir ekibiniz mi var?</p>
        <Link
          href="/iletisim"
          className="text-[var(--color-primary)] hover:underline font-medium"
        >
          Özel teklif için bizimle iletişime geçin →
        </Link>
      </div>
    </div>
  );
}
