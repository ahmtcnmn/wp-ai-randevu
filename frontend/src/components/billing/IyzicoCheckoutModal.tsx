"use client";
import { useEffect, useRef } from "react";
import { CheckoutInitResponse } from "@/types";

interface IyzicoCheckoutModalProps {
  checkout: CheckoutInitResponse;
  onClose: () => void;
}

export default function IyzicoCheckoutModal({ checkout, onClose }: IyzicoCheckoutModalProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (containerRef.current && checkout.checkoutFormContent) {
      containerRef.current.innerHTML = checkout.checkoutFormContent;
      // Execute scripts in the injected HTML
      const scripts = containerRef.current.querySelectorAll("script");
      scripts.forEach((s) => {
        const newScript = document.createElement("script");
        if (s.src) newScript.src = s.src;
        else newScript.textContent = s.textContent;
        document.body.appendChild(newScript);
      });
    }
  }, [checkout.checkoutFormContent]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-auto relative">
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <h3 className="font-semibold text-gray-900">Ödeme</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-xl leading-none"
            aria-label="Kapat">
            ×
          </button>
        </div>
        <div ref={containerRef} className="p-4" />
      </div>
    </div>
  );
}
