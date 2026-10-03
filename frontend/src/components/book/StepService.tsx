"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";
import { Hizmet } from "@/types";

interface Props {
  tenantId: number;
  onNext: (hizmetId: number, hizmetAd: string, sure: number, fiyat: number) => void;
  onBack: () => void;
}

export default function StepService({ tenantId, onNext, onBack }: Props) {
  const [hizmetler, setHizmetler] = useState<Hizmet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchHizmetler = async () => {
      try {
        const res = await api.get<{ data: Hizmet[] }>(`/v1/public/services?tenantId=${tenantId}`);
        setHizmetler(res.data.data ?? []);
      } catch (err) {
        console.error("Hizmetler çekilemedi", err);
        setError("Hizmetler yüklenirken bir hata oluştu.");
      } finally {
        setLoading(false);
      }
    };
    fetchHizmetler();
  }, [tenantId]);

  if (loading) return <div className="text-center p-4">Yükleniyor...</div>;
  if (error) return <div className="text-center p-4 text-red-500">{error}</div>;

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold">Hizmet Seçin</h2>
        <button onClick={onBack} className="text-sm text-blue-600 hover:underline">Geri Dön</button>
      </div>

      {hizmetler.length === 0 && <p className="text-gray-500">Kayıtlı hizmet bulunmuyor.</p>}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {hizmetler.map((hizmet) => (
          <div
            key={hizmet.id}
            onClick={() => onNext(hizmet.id, hizmet.ad, hizmet.sureDakika, hizmet.fiyat)}
            className="p-4 border rounded-lg cursor-pointer hover:bg-blue-50 transition-colors flex justify-between items-center"
          >
            <div>
              <h3 className="font-semibold text-lg">{hizmet.ad}</h3>
              <p className="text-sm text-gray-500">{hizmet.sureDakika} Dakika</p>
            </div>
            <div className="font-bold text-green-600">
              {hizmet.fiyat} ₺
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
