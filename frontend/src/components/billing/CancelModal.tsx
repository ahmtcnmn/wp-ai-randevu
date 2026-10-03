"use client";

interface CancelModalProps {
  onConfirm: () => void;
  onClose: () => void;
  loading?: boolean;
}

export default function CancelModal({ onConfirm, onClose, loading }: CancelModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6">
        <h3 className="font-semibold text-gray-900 text-lg mb-2">Aboneliği İptal Et</h3>
        <p className="text-sm text-gray-600 mb-6">
          Aboneliğinizi iptal etmek istediğinize emin misiniz? Mevcut dönemin sonuna kadar erişiminiz devam eder.
        </p>
        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-2.5 rounded-xl border border-gray-200 text-sm font-medium text-gray-700 hover:bg-gray-50">
            Vazgeç
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-medium disabled:opacity-50">
            {loading ? "İptal ediliyor..." : "İptal Et"}
          </button>
        </div>
      </div>
    </div>
  );
}
