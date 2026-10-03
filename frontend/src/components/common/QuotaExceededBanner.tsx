"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function QuotaExceededBanner() {
  const [message, setMessage] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    const handler = (e: Event) => {
      const detail = (e as CustomEvent).detail;
      setMessage(detail?.message ?? "Kota limitinize ulaştınız.");
    };
    window.addEventListener("quotaExceeded", handler);
    return () => window.removeEventListener("quotaExceeded", handler);
  }, []);

  if (!message) return null;

  return (
    <div className="fixed bottom-4 left-1/2 -translate-x-1/2 z-50 bg-yellow-50 border border-yellow-300 rounded-xl shadow-lg px-5 py-4 flex items-center gap-4 max-w-lg w-full mx-4">
      <div className="flex-1">
        <p className="font-semibold text-yellow-800 text-sm">Kota Limitine Ulaşıldı</p>
        <p className="text-yellow-700 text-xs mt-0.5">{message}</p>
      </div>
      <button
        onClick={() => { setMessage(null); router.push("/ayarlar/plan"); }}
        className="bg-yellow-600 hover:bg-yellow-700 text-white text-xs font-medium px-3 py-1.5 rounded-lg whitespace-nowrap">
        Plan Yükselt
      </button>
      <button onClick={() => setMessage(null)} className="text-yellow-600 hover:text-yellow-800 text-lg leading-none">
        ×
      </button>
    </div>
  );
}
