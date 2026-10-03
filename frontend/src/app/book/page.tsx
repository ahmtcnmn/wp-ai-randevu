"use client";

import { Suspense, useState } from "react";
import { useSearchParams } from "next/navigation";
import StepBranch from "@/components/book/StepBranch";
import StepBarber from "@/components/book/StepBarber";
import StepService from "@/components/book/StepService";
import StepTime from "@/components/book/StepTime";
import StepConfirm from "@/components/book/StepConfirm";

function BookPageInner() {
  const searchParams = useSearchParams();
  const tenantIdNum = Number(searchParams.get("tenantId") || "0");
  const [step, setStep] = useState(1);
  const [bookingData, setBookingData] = useState({
    subeId: 0,
    subeAd: "",
    uzmanId: 0,
    uzmanAd: "",
    hizmetId: 0,
    hizmetAd: "",
    sure: 0,
    fiyat: 0,
    tarih: "",
    saat: "",
  });

  const goNext = () => setStep((s) => s + 1);
  const goBack = () => setStep((s) => s - 1);

  const steps = ["Şube", "Çalışan", "Hizmet", "Tarih/Saat", "Onay"];

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4">
      <div className="max-w-3xl mx-auto">
        <h1 className="text-2xl font-bold text-slate-900 mb-6 text-center">
          Randevu Al
        </h1>
        <div className="mb-6 bg-white rounded-lg p-4 shadow-sm">
          <div className="flex justify-between items-center">
            {steps.map((s, i) => (
              <div
                key={s}
                className={`text-xs font-medium ${
                  step > i ? "text-[var(--color-primary)]" : "text-slate-400"
                }`}
              >
                {s}
              </div>
            ))}
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-lg p-6 min-h-[400px]">
          {step === 1 && (
            <StepBranch
              tenantId={tenantIdNum}
              onNext={(id, ad) => {
                setBookingData({ ...bookingData, subeId: id, subeAd: ad });
                goNext();
              }}
            />
          )}

          {step === 2 && (
            <StepBarber
              tenantId={tenantIdNum}
              subeId={bookingData.subeId}
              onNext={(id, ad) => {
                setBookingData({ ...bookingData, uzmanId: id, uzmanAd: ad });
                goNext();
              }}
              onBack={goBack}
            />
          )}

          {step === 3 && (
            <StepService
              tenantId={tenantIdNum}
              onNext={(id, ad, sure, fiyat) => {
                setBookingData({ ...bookingData, hizmetId: id, hizmetAd: ad, sure, fiyat });
                goNext();
              }}
              onBack={goBack}
            />
          )}

          {step === 4 && (
            <StepTime
              tenantId={tenantIdNum}
              uzmanId={bookingData.uzmanId}
              hizmetId={bookingData.hizmetId}
              onNext={(saat, tarih) => {
                setBookingData({ ...bookingData, saat, tarih });
                goNext();
              }}
              onBack={goBack}
            />
          )}

          {step === 5 && (
            <StepConfirm
              tenantId={tenantIdNum}
              bookingData={bookingData}
              onBack={goBack}
            />
          )}
        </div>
      </div>
    </div>
  );
}

export default function BookPage() {
  return (
    <Suspense fallback={null}>
      <BookPageInner />
    </Suspense>
  );
}
