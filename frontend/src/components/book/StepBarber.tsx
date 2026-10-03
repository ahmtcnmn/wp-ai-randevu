"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";
import { Kullanici } from "@/types";

interface Props {
  tenantId: number;
  subeId: number;
  onNext: (uzmanId: number, uzmanAd: string) => void;
  onBack: () => void;
}

export default function StepBarber({ tenantId, subeId, onNext, onBack }: Props) {
  const [uzmanlar, setUzmanler] = useState<Kullanici[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchUzmanler = async () => {
      try {
        const res = await api.get<{ data: Kullanici[] }>(`/v1/public/staff?tenantId=${tenantId}`);
        const staff = res.data.data ?? [];
        // Seçilen şubeye göre filtrele (subeId varsa)
        const filtered = subeId ? staff.filter((u) => !u.sube || u.sube.id === subeId) : staff;
        setUzmanler(filtered);
      } catch (err) {
        console.error("Uzmanler çekilemedi", err);
        setError("Uzman listesi yüklenirken bir hata oluştu.");
      } finally {
        setLoading(false);
      }
    };
    fetchUzmanler();
  }, [tenantId, subeId]);

  if (loading) return <div className="text-center p-4">Yükleniyor...</div>;
  if (error) return <div className="text-center p-4 text-red-500">{error}</div>;

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold">Uzman Seçin</h2>
        <button onClick={onBack} className="text-sm text-blue-600 hover:underline">Geri Dön</button>
      </div>

      {uzmanlar.length === 0 && <p className="text-gray-500">Bu şubede şu an kayıtlı uzman bulunmuyor.</p>}

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
        {uzmanlar.map((uzman) => (
          <div
            key={uzman.id}
            onClick={() => onNext(uzman.id, `${uzman.ad} ${uzman.soyad}`)}
            className="p-4 border rounded-lg cursor-pointer hover:bg-blue-50 transition-colors flex items-center space-x-3"
          >
            <div className="w-12 h-12 bg-gray-200 rounded-full flex items-center justify-center text-xl font-bold text-gray-500">
              {uzman.ad.charAt(0)}
            </div>
            <div>
              <h3 className="font-semibold">{uzman.ad} {uzman.soyad}</h3>
              <p className="text-sm text-gray-500">Uzman</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
