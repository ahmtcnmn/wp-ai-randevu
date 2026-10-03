"use client";
import { Suspense } from "react";
import { useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";

type Status = "loading" | "success" | "error";

function CallbackContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [status, setStatus] = useState<Status>("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const paymentStatus = searchParams.get("status");
    const error = searchParams.get("error");
    const plan = searchParams.get("plan");

    if (paymentStatus === "success") {
      setStatus("success");
      setMessage(`Ödeme başarılı! ${plan ? plan + " planı" : "Aboneliğiniz"} aktifleştirildi.`);
      setTimeout(() => router.push("/ayarlar/plan"), 2000);
    } else if (paymentStatus === "failed") {
      setStatus("error");
      setMessage(error ? decodeURIComponent(error) : "Ödeme işlemi başarısız oldu.");
    } else {
      // No status param yet — still loading or direct navigation
      setStatus("error");
      setMessage("Geçersiz ödeme callback parametreleri.");
    }
  }, [searchParams, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white rounded-2xl shadow-lg p-8 max-w-sm w-full text-center">
        {status === "loading" && (
          <>
            <div className="animate-spin w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-4" />
            <p className="text-gray-700 font-medium">Ödeme doğrulanıyor...</p>
          </>
        )}
        {status === "success" && (
          <>
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-green-600 text-2xl">✓</span>
            </div>
            <h2 className="font-semibold text-gray-900 text-lg mb-2">Ödeme Başarılı!</h2>
            <p className="text-gray-600 text-sm">{message}</p>
            <p className="text-gray-400 text-xs mt-2">Plan sayfasına yönlendiriliyorsunuz...</p>
          </>
        )}
        {status === "error" && (
          <>
            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-red-600 text-2xl">✕</span>
            </div>
            <h2 className="font-semibold text-gray-900 text-lg mb-2">Ödeme Başarısız</h2>
            <p className="text-gray-600 text-sm mb-4">{message}</p>
            <button
              onClick={() => router.push("/ayarlar/plan")}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-xl text-sm font-medium">
              Plan Sayfasına Dön
            </button>
          </>
        )}
      </div>
    </div>
  );
}

export default function BillingCallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full" />
      </div>
    }>
      <CallbackContent />
    </Suspense>
  );
}
