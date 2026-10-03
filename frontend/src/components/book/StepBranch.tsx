"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";
import { Sube } from "@/types";

interface Props {
  tenantId: number;
  onNext: (subeId: number, subeAd: string) => void;
}

export default function StepBranch({ tenantId, onNext }: Props) {
  const [subeler, setSubeler] = useState<Sube[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchSubeler = async () => {
      try {
        const res = await api.get<{ data: Sube[] }>(`/v1/public/branches?tenantId=${tenantId}`);
        setSubeler(res.data.data ?? []);
      } catch (err) {
        console.error("Şubeler çekilemedi", err);
        setError("Şubeler yüklenirken bir hata oluştu.");
      } finally {
        setLoading(false);
      }
    };
    fetchSubeler();
  }, [tenantId]);

  if (loading) return <div className="text-center p-4">Yükleniyor...</div>;
  if (error) return <div className="text-center p-4 text-red-500">{error}</div>;

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-bold mb-4">Şube Seçin</h2>
      {subeler.length === 0 && <p>Hiç şube bulunamadı.</p>}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {subeler.map((sube) => (
          <div
            key={sube.id}
            onClick={() => onNext(sube.id, sube.ad)}
            className="p-4 border rounded-lg cursor-pointer hover:bg-blue-50 transition-colors"
          >
            <h3 className="font-semibold text-lg">{sube.ad}</h3>
            <p className="text-gray-600 text-sm">{sube.adres}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
