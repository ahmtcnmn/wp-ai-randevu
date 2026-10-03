"use client";
import { InvoiceResponse } from "@/types";

interface InvoiceTableProps {
  invoices: InvoiceResponse[];
}

const DURUM_BADGE: Record<string, string> = {
  PAID:     "bg-green-100 text-green-800",
  FAILED:   "bg-red-100 text-red-800",
  PENDING:  "bg-yellow-100 text-yellow-800",
  REFUNDED: "bg-gray-100 text-gray-600",
};

const DURUM_LABEL: Record<string, string> = {
  PAID:     "Ödendi",
  FAILED:   "Başarısız",
  PENDING:  "Bekliyor",
  REFUNDED: "İade Edildi",
};

export default function InvoiceTable({ invoices }: InvoiceTableProps) {
  if (!invoices.length) {
    return <p className="text-sm text-gray-500 py-4">Fatura bulunamadı.</p>;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-200 text-left text-gray-500 uppercase text-xs tracking-wider">
            <th className="pb-3 pr-4">Dönem</th>
            <th className="pb-3 pr-4">Tutar</th>
            <th className="pb-3 pr-4">Durum</th>
            <th className="pb-3">Ödeme Tarihi</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {invoices.map((inv) => (
            <tr key={inv.id} className="py-3">
              <td className="py-3 pr-4 text-gray-700">
                {inv.donemBaslangic} – {inv.donemBitis}
              </td>
              <td className="py-3 pr-4 font-mono font-semibold text-gray-900">
                ₺{inv.tutar} {inv.paraBirimi}
              </td>
              <td className="py-3 pr-4">
                <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${DURUM_BADGE[inv.durum] ?? "bg-gray-100 text-gray-600"}`}>
                  {DURUM_LABEL[inv.durum] ?? inv.durum}
                </span>
              </td>
              <td className="py-3 text-gray-500">
                {inv.odemeTarihi
                  ? new Date(inv.odemeTarihi).toLocaleDateString("tr-TR")
                  : "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
