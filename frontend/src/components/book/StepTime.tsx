"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";

interface Props {
  tenantId: number;
  uzmanId: number;
  hizmetId: number;
  onNext: (saat: string, tarih: string) => void;
  onBack: () => void;
}

export default function StepTime({ tenantId, uzmanId, hizmetId, onNext, onBack }: Props) {
  const [seciliTarih, setSeciliTarih] = useState<string>(() => {
    const d = new Date();
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  });
  const [saatler, setSaatler] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchSaatler = async () => {
      setLoading(true);
      setError("");
      try {
        const res = await api.get<{ data: string[] }>(
          `/v1/public/availability?tenantId=${tenantId}&uzmanId=${uzmanId}&hizmetIds=${hizmetId}&tarih=${seciliTarih}`
        );
        setSaatler(res.data.data ?? []);
      } catch (err) {
        console.error("Boş saatler alınamadı", err);
        setError("Uygun saatler yüklenirken bir hata oluştu.");
      } finally {
        setLoading(false);
      }
    };
    if (seciliTarih) {
      fetchSaatler();
    }
  }, [tenantId, uzmanId, hizmetId, seciliTarih]);

  // Önümüzdeki 7 günü üret (timezone-safe)
  const gunler = Array.from({ length: 7 }).map((_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    const y = d.getFullYear();
    const mo = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${mo}-${day}`;
  });

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-bold">Tarih ve Saat Seçin</h2>
        <button onClick={onBack} className="text-sm text-blue-600 hover:underline">Geri Dön</button>
      </div>

      {/* Tarih Seçici Yatay Scroll */}
      <div className="flex space-x-2 overflow-x-auto pb-2">
        {gunler.map((gun) => {
          const [y, mo, d] = gun.split("-").map(Number);
          const dateObj = new Date(y, mo - 1, d);
          const gunIsmi = dateObj.toLocaleDateString("tr-TR", { weekday: "short" });
          const gunNo = dateObj.getDate();
          const isActive = seciliTarih === gun;

          return (
            <button
              key={gun}
              onClick={() => setSeciliTarih(gun)}
              className={`flex-shrink-0 flex flex-col items-center p-3 w-16 rounded-lg border ${
                isActive ? "bg-blue-600 text-white border-blue-600" : "bg-white text-gray-700 hover:bg-gray-50"
              }`}
            >
              <span className="text-xs">{gunIsmi}</span>
              <span className="text-lg font-bold">{gunNo}</span>
            </button>
          );
        })}
      </div>

      {/* Saat Slotları */}
      <div className="min-h-[200px]">
        {loading ? (
          <p className="text-center text-gray-500 py-8">Uygun saatler yükleniyor...</p>
        ) : error ? (
          <p className="text-center text-red-500 py-8">{error}</p>
        ) : saatler.length === 0 ? (
          <p className="text-center text-red-500 py-8">Seçili günde uygun saat bulunamadı.</p>
        ) : (
          <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
            {saatler.map((saatStr) => {
              const formattedSaat = String(saatStr).substring(0, 5);
              return (
                <button
                  key={saatStr}
                  onClick={() => onNext(saatStr, seciliTarih)}
                  className="py-2 px-1 border rounded-md text-center hover:bg-blue-600 hover:text-white transition-colors font-medium border-blue-200 text-blue-800"
                >
                  {formattedSaat}
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
