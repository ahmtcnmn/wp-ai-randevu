"use client";
import { useState, useEffect } from "react";
import api from "@/lib/api";
import { SubscriptionPlanResponse } from "@/types";

export default function AdminPlanlarPage() {
  const [plans, setPlans] = useState<SubscriptionPlanResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [editId, setEditId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<Partial<SubscriptionPlanResponse>>({});
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const fetchPlans = async () => {
    setLoading(true);
    try {
      const res = await api.get("/v1/admin/plans");
      setPlans(res.data.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPlans(); }, []);

  const startEdit = (plan: SubscriptionPlanResponse) => {
    setEditId(plan.id);
    setEditForm({
      ad: plan.ad,
      aciklama: plan.aciklama,
      aylikFiyat: plan.aylikFiyat,
      maxSube: plan.maxSube ?? undefined,
      maxCalisan: plan.maxCalisan ?? undefined,
      maxAylikRandevu: plan.maxAylikRandevu ?? undefined,
      aktif: plan.aktif,
    });
  };

  const savePlan = async (id: number) => {
    setSaving(true);
    setError(null);
    try {
      await api.put(`/v1/admin/plans/${id}`, {
        ad: editForm.ad,
        aciklama: editForm.aciklama,
        aylikFiyat: editForm.aylikFiyat,
        maxSube: editForm.maxSube || null,
        maxCalisan: editForm.maxCalisan || null,
        maxAylikRandevu: editForm.maxAylikRandevu || null,
        aktif: editForm.aktif,
      });
      setSuccess("Plan güncellendi.");
      setEditId(null);
      await fetchPlans();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      setError(err.response?.data?.message ?? "Güncelleme başarısız.");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-64">
        <div className="animate-spin w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Plan Yönetimi</h1>

      {error && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm">{error}</div>
      )}
      {success && (
        <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded-xl text-green-700 text-sm">{success}</div>
      )}

      <div className="space-y-4">
        {plans.map((plan) => (
          <div key={plan.id} className="bg-white rounded-xl border border-gray-200 p-5">
            {editId === plan.id ? (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">Ad</label>
                    <input
                      className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={editForm.ad ?? ""}
                      onChange={(e) => setEditForm((f) => ({ ...f, ad: e.target.value }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">Aylık Fiyat (₺)</label>
                    <input
                      type="number"
                      step="0.01"
                      className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={editForm.aylikFiyat ?? ""}
                      onChange={(e) => setEditForm((f) => ({ ...f, aylikFiyat: parseFloat(e.target.value) }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">Max Şube (boş = sınırsız)</label>
                    <input
                      type="number"
                      className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={editForm.maxSube ?? ""}
                      onChange={(e) => setEditForm((f) => ({ ...f, maxSube: e.target.value ? parseInt(e.target.value) : undefined }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">Max Çalışan (boş = sınırsız)</label>
                    <input
                      type="number"
                      className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={editForm.maxCalisan ?? ""}
                      onChange={(e) => setEditForm((f) => ({ ...f, maxCalisan: e.target.value ? parseInt(e.target.value) : undefined }))}
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">Max Randevu/Ay (boş = sınırsız)</label>
                    <input
                      type="number"
                      className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={editForm.maxAylikRandevu ?? ""}
                      onChange={(e) => setEditForm((f) => ({ ...f, maxAylikRandevu: e.target.value ? parseInt(e.target.value) : undefined }))}
                    />
                  </div>
                  <div className="flex items-center gap-2 pt-5">
                    <input
                      type="checkbox"
                      id={`aktif-${plan.id}`}
                      checked={editForm.aktif ?? true}
                      onChange={(e) => setEditForm((f) => ({ ...f, aktif: e.target.checked }))}
                    />
                    <label htmlFor={`aktif-${plan.id}`} className="text-sm text-gray-700">Aktif</label>
                  </div>
                </div>
                <div>
                  <label className="text-xs font-medium text-gray-600 uppercase tracking-wider">Açıklama</label>
                  <textarea
                    className="mt-1 w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={2}
                    value={editForm.aciklama ?? ""}
                    onChange={(e) => setEditForm((f) => ({ ...f, aciklama: e.target.value }))}
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => savePlan(plan.id)}
                    disabled={saving}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-xl text-sm font-medium disabled:opacity-50">
                    {saving ? "Kaydediliyor..." : "Kaydet"}
                  </button>
                  <button
                    onClick={() => setEditId(null)}
                    className="border border-gray-200 text-gray-700 px-4 py-2 rounded-xl text-sm font-medium hover:bg-gray-50">
                    İptal
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-gray-900">{plan.ad}</h3>
                    <span className="text-xs font-mono bg-gray-100 text-gray-600 px-2 py-0.5 rounded">{plan.planKey}</span>
                    {!plan.aktif && <span className="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded">Pasif</span>}
                  </div>
                  <p className="text-sm text-gray-500 mt-0.5">{plan.aciklama}</p>
                  <div className="flex gap-4 mt-2 text-xs text-gray-500">
                    <span>Fiyat: <strong>₺{plan.aylikFiyat}/ay</strong></span>
                    <span>Şube: <strong>{plan.maxSube ?? "∞"}</strong></span>
                    <span>Çalışan: <strong>{plan.maxCalisan ?? "∞"}</strong></span>
                    <span>Randevu: <strong>{plan.maxAylikRandevu ?? "∞"}/ay</strong></span>
                  </div>
                </div>
                <button
                  onClick={() => startEdit(plan)}
                  className="border border-gray-200 text-gray-700 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-gray-50">
                  Düzenle
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
