"use client";

import { useState } from "react";
import api from "@/lib/api";

interface Props {
  tenantId: number;
  bookingData: {
    subeId: number;
    subeAd: string;
    uzmanId: number;
    uzmanAd: string;
    hizmetId: number;
    hizmetAd: string;
    sure: number;
    fiyat: number;
    tarih: string;
    saat: string;
  };
  onBack: () => void;
}

export default function StepConfirm({ tenantId, bookingData, onBack }: Props) {
  const [musteriAd, setMusteriAd] = useState("");
  const [musteriSoyad, setMusteriSoyad] = useState("");
  const [musteriTelefon, setMusteriTelefon] = useState("");
  const [not, setNot] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const handleConfirm = async () => {
    if (!musteriAd.trim() || !musteriTelefon.trim()) {
      setError("Lütfen adınızı ve telefon numaranızı girin.");
      return;
    }

    setLoading(true);
    setError("");
    try {
      const tarihSaatStr = `${bookingData.tarih}T${bookingData.saat}`;

      await api.post(`/v1/public/book?tenantId=${tenantId}`, {
        musteriAd: musteriAd.trim(),
        musteriSoyad: musteriSoyad.trim() || "-",
        musteriTelefon: musteriTelefon.trim(),
        uzmanId: bookingData.uzmanId,
        hizmetIds: [bookingData.hizmetId],
        tarihSaat: tarihSaatStr,
        not: not,
      });

      setSuccess(true);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setError(axiosErr.response?.data?.message || "Randevu alınırken bir hata oluştu.");
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="text-center space-y-4 py-8">
        <div className="text-5xl text-green-600">✓</div>
        <h2 className="text-xl font-bold text-green-600">Randevunuz Alındı!</h2>
        <p className="text-gray-600">
          {bookingData.uzmanAd} ile {new Date(bookingData.tarih).toLocaleDateString("tr-TR")} tarihinde{" "}
          {bookingData.saat.substring(0, 5)} saatinde görüşeceksiniz.
        </p>
        <a href="/" className="inline-block mt-4 text-blue-600 hover:underline text-sm">
          Ana Sayfaya Dön
        </a>
      </div>
    );
  }

  const formattedTarih = new Date(bookingData.tarih).toLocaleDateString("tr-TR");
  const formattedSaat = bookingData.saat.substring(0, 5);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold">Randevu Özeti</h2>
        <button onClick={onBack} className="text-sm text-blue-600 hover:underline">Geri Dön</button>
      </div>

      <div className="bg-gray-50 border rounded-lg p-6 space-y-4">
        <div className="flex justify-between border-b pb-2">
          <span className="text-gray-600">Şube:</span>
          <span className="font-medium">{bookingData.subeAd}</span>
        </div>
        <div className="flex justify-between border-b pb-2">
          <span className="text-gray-600">Uzman:</span>
          <span className="font-medium">{bookingData.uzmanAd}</span>
        </div>
        <div className="flex justify-between border-b pb-2">
          <span className="text-gray-600">Hizmet:</span>
          <span className="font-medium">{bookingData.hizmetAd} ({bookingData.sure} dk)</span>
        </div>
        <div className="flex justify-between border-b pb-2">
          <span className="text-gray-600">Tarih / Saat:</span>
          <span className="font-medium text-blue-600">{formattedTarih} - {formattedSaat}</span>
        </div>
        <div className="flex justify-between pt-2">
          <span className="text-gray-600 font-bold">Ödenecek Tutar:</span>
          <span className="font-bold text-green-600">{bookingData.fiyat} ₺</span>
        </div>
      </div>

      {/* Müşteri Bilgileri */}
      <div className="space-y-3">
        <h3 className="font-semibold text-gray-800">İletişim Bilgileri</h3>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Ad <span className="text-red-500">*</span></label>
          <input
            type="text"
            value={musteriAd}
            onChange={(e) => setMusteriAd(e.target.value)}
            placeholder="Adınız"
            className="w-full p-2 border rounded-md"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Soyad</label>
          <input
            type="text"
            value={musteriSoyad}
            onChange={(e) => setMusteriSoyad(e.target.value)}
            placeholder="Soyadınız"
            className="w-full p-2 border rounded-md"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Telefon <span className="text-red-500">*</span></label>
          <input
            type="tel"
            value={musteriTelefon}
            onChange={(e) => setMusteriTelefon(e.target.value)}
            placeholder="05XX XXX XX XX"
            className="w-full p-2 border rounded-md"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">Uzmana Not (Opsiyonel)</label>
        <textarea
          value={not}
          onChange={(e) => setNot(e.target.value)}
          placeholder="İsteğe bağlı bir not bırakabilirsiniz..."
          className="w-full p-2 border rounded-md"
          rows={3}
        ></textarea>
      </div>

      {error && <p className="text-red-500 text-sm font-medium">{error}</p>}

      <button
        onClick={handleConfirm}
        disabled={loading}
        className="w-full bg-blue-600 text-white font-bold py-3 rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
      >
        {loading ? "Onaylanıyor..." : "Randevuyu Onayla"}
      </button>
    </div>
  );
}
