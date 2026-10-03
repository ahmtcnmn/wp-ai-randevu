"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Spinner";
import { tenantApi, branchApi, serviceApi, serviceCategoryApi, userApi, aiConfigApi } from "@/lib/api";
import { useToast } from "@/store/ToastContext";
import { extractApiError } from "@/hooks/useApiError";
import { cn } from "@/lib/utils/cn";

type Step = 1 | 2 | 3 | 4 | 5 | 6;

const STEPS = [
  { n: 1, title: "İşletme Bilgileri" },
  { n: 2, title: "İlk Şube" },
  { n: 3, title: "Çalışanlar" },
  { n: 4, title: "Hizmet Kategorileri" },
  { n: 5, title: "Hizmetler" },
  { n: 6, title: "Bitir" },
] as const;

export default function OnboardingPage() {
  const router = useRouter();
  const toast = useToast();
  const [step, setStep] = useState<Step>(1);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Step 1
  const [tenant, setTenant] = useState({
    ad: "", telefon: "", email: "", adres: "", sehir: "", ulke: "Turkey",
  });
  const [isletmeAciklamasi, setIsletmeAciklamasi] = useState("");

  // Step 2
  const [branch, setBranch] = useState({ ad: "", adres: "", telefon: "" });
  const [skipBranch, setSkipBranch] = useState(false);

  // Step 3
  const [staffList, setStaffList] = useState<{ ad: string; soyad: string; email: string; telefon: string; sifre: string }[]>([]);
  const [newStaff, setNewStaff] = useState({ ad: "", soyad: "", email: "", telefon: "", sifre: "" });
  /** Step 3 sonrası DB'ye yazılan çalışanların id'leri — Step 5'te hizmet çalışan ataması için. */
  const [createdStaffIds, setCreatedStaffIds] = useState<{ ad: string; id: number }[]>([]);

  // Step 4 — Kategoriler
  const [categoryList, setCategoryList] = useState<{ ad: string }[]>([]);
  const [newCategory, setNewCategory] = useState("");
  const [createdCategoryIds, setCreatedCategoryIds] = useState<{ ad: string; id: number }[]>([]);

  // Step 5 — Hizmetler (artık kategoriId + staffIds ile)
  const [serviceList, setServiceList] = useState<{ ad: string; sureDakika: number; fiyat: number; kategoriId: number | null; staffIds: number[] }[]>([]);
  const [newService, setNewService] = useState<{ ad: string; sureDakika: number; fiyat: number; kategoriId: number; staffIds: number[] }>({
    ad: "", sureDakika: 30, fiyat: 200, kategoriId: 0, staffIds: [],
  });

  useEffect(() => {
    tenantApi.get().then((t) => {
      if (t.onboardingCompleted) {
        router.replace("/dashboard");
        return;
      }
      setTenant({
        ad: t.ad || "",
        telefon: t.telefon || "",
        email: t.email || "",
        adres: t.adres || "",
        sehir: t.sehir || "",
        ulke: t.ulke || "Turkey",
      });
      setLoading(false);
    });
  }, [router]);

  async function saveStep1() {
    if (!tenant.ad.trim()) return setError("İşletme adı zorunlu");
    setSubmitting(true);
    setError(null);
    try {
      await tenantApi.update(tenant);
      // İşletme açıklaması girilmişse AI config'e yaz (sessizce — başarısızlık akışı bozmaz)
      if (isletmeAciklamasi.trim()) {
        try {
          await aiConfigApi.update({ isletmeAciklamasi: isletmeAciklamasi.trim() });
        } catch { /* AI config opsiyonel */ }
      }
      setStep(2);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function saveStep2() {
    if (skipBranch) {
      setStep(3);
      return;
    }
    if (!branch.ad.trim()) return setError("Şube adı zorunlu");
    setSubmitting(true);
    setError(null);
    try {
      await branchApi.create(branch);
      setStep(3);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function saveStep3() {
    // Çalışanlar — opsiyonel, sonradan eklenebilir
    if (staffList.length === 0) {
      setStep(4);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const created: { ad: string; id: number }[] = [];
      for (const s of staffList) {
        const u = await userApi.create({
          ad: s.ad,
          soyad: s.soyad,
          email: s.email,
          telefon: s.telefon,
          sifre: s.sifre,
          rol: "STAFF",
        });
        created.push({ ad: `${s.ad} ${s.soyad}`, id: u.id });
      }
      setCreatedStaffIds(created);
      toast.success(`${staffList.length} çalışan eklendi`);
      setStep(4);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function saveStep4() {
    // Kategoriler — opsiyonel
    if (categoryList.length === 0) {
      setStep(5);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const created: { ad: string; id: number }[] = [];
      for (const c of categoryList) {
        const resp = await serviceCategoryApi.create({ ad: c.ad });
        created.push({ ad: c.ad, id: resp.id });
      }
      setCreatedCategoryIds(created);
      toast.success(`${created.length} kategori eklendi`);
      setStep(5);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function saveStep5() {
    if (serviceList.length === 0) {
      setStep(6);
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      for (const s of serviceList) {
        await serviceApi.create({
          ad: s.ad,
          sureDakika: s.sureDakika,
          fiyat: s.fiyat,
          kategoriId: s.kategoriId || null,
          staffIds: s.staffIds.length > 0 ? s.staffIds : undefined,
        });
      }
      toast.success(`${serviceList.length} hizmet eklendi`);
      setStep(6);
    } catch (err) {
      setError(extractApiError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function finish() {
    setSubmitting(true);
    try {
      await tenantApi.completeOnboarding();
      toast.success("Kurulum tamamlandı! Hoş geldiniz 👋");
      router.replace("/dashboard");
    } catch (err) {
      toast.error(extractApiError(err));
      setSubmitting(false);
    }
  }

  if (loading) return <div className="flex justify-center py-12"><Spinner size="lg" /></div>;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between mb-3">
          <CardTitle>İşletmenizi Kuralım</CardTitle>
          <span className="text-sm text-slate-500">{step} / 6</span>
        </div>
        <div className="flex gap-1">
          {STEPS.map((s) => (
            <div
              key={s.n}
              className={cn(
                "flex-1 h-1.5 rounded-full",
                s.n <= step ? "bg-[var(--color-primary)]" : "bg-slate-200"
              )}
            />
          ))}
        </div>
        <p className="text-xs text-slate-500 mt-3">{STEPS[step - 1].title}</p>
      </CardHeader>
      <CardContent className="space-y-4">
        {error && <Alert variant="error">{error}</Alert>}

        {step === 1 && (
          <>
            <Input label="İşletme Adı" value={tenant.ad} onChange={(e) => setTenant({ ...tenant, ad: e.target.value })} required />
            <Input label="Telefon" value={tenant.telefon} onChange={(e) => setTenant({ ...tenant, telefon: e.target.value })} />
            <Input label="E-posta" type="email" value={tenant.email} onChange={(e) => setTenant({ ...tenant, email: e.target.value })} />
            <Input label="Adres" value={tenant.adres} onChange={(e) => setTenant({ ...tenant, adres: e.target.value })} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Şehir" value={tenant.sehir} onChange={(e) => setTenant({ ...tenant, sehir: e.target.value })} />
              <Input label="Ülke" value={tenant.ulke} onChange={(e) => setTenant({ ...tenant, ulke: e.target.value })} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">İşletmeniz hakkında kısaca</label>
              <textarea
                value={isletmeAciklamasi}
                onChange={(e) => setIsletmeAciklamasi(e.target.value)}
                rows={4}
                maxLength={500}
                placeholder="Örn: Kadıköy'de 2 yıldır faaliyet gösteren erkek berber salonu. Saç, sakal, traş hizmetleri sunuyoruz. Pazartesi-Cumartesi 09:00-20:00 açığız."
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
              />
              <p className="text-xs text-slate-500 mt-1">
                AI asistanınız bu bilgiyi müşterilere konuşurken kullanır. Çalışma saatleri, sunduğunuz özel hizmetler vs. ekleyebilirsiniz.
              </p>
            </div>
            <div className="flex justify-end pt-2">
              <Button onClick={saveStep1} loading={submitting}>Devam →</Button>
            </div>
          </>
        )}

        {step === 2 && (
          <>
            <p className="text-sm text-slate-600">
              İşletmenizin ilk şubesi. Daha sonra başka şubeler ekleyebilirsiniz.
            </p>
            {!skipBranch ? (
              <>
                <Input label="Şube Adı" value={branch.ad} onChange={(e) => setBranch({ ...branch, ad: e.target.value })} placeholder="Merkez Şube" required />
                <Input label="Adres" value={branch.adres} onChange={(e) => setBranch({ ...branch, adres: e.target.value })} />
                <Input label="Telefon" value={branch.telefon} onChange={(e) => setBranch({ ...branch, telefon: e.target.value })} />
              </>
            ) : (
              <Alert variant="info">Bu adım atlandı. Şubeleri sonra Ayarlar'dan ekleyebilirsiniz.</Alert>
            )}
            <div className="flex justify-between pt-2">
              <Button variant="ghost" onClick={() => setSkipBranch(!skipBranch)}>
                {skipBranch ? "← Bilgi gir" : "Atla"}
              </Button>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setStep(1)}>← Geri</Button>
                <Button onClick={saveStep2} loading={submitting}>Devam →</Button>
              </div>
            </div>
          </>
        )}

        {step === 3 && (
          <>
            <p className="text-sm text-slate-600">
              Çalışanlarınızı şimdi ekleyebilir veya sonra ekleyebilirsiniz. Her çalışana
              bir geçici şifre belirleyin — çalışan ilk girişinde değiştirebilir.
            </p>
            {staffList.length > 0 && (
              <div className="border rounded-md divide-y divide-slate-100">
                {staffList.map((s, i) => (
                  <div key={i} className="px-3 py-2 flex justify-between items-center text-sm">
                    <span>{s.ad} {s.soyad} ({s.email})</span>
                    <button
                      type="button"
                      onClick={() => setStaffList(staffList.filter((_, j) => j !== i))}
                      className="text-red-500 hover:text-red-700"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="grid grid-cols-2 gap-2">
              <Input placeholder="Ad" value={newStaff.ad} onChange={(e) => setNewStaff({ ...newStaff, ad: e.target.value })} />
              <Input placeholder="Soyad" value={newStaff.soyad} onChange={(e) => setNewStaff({ ...newStaff, soyad: e.target.value })} />
              <Input placeholder="E-posta" type="email" value={newStaff.email} onChange={(e) => setNewStaff({ ...newStaff, email: e.target.value })} />
              <Input placeholder="Telefon" value={newStaff.telefon} onChange={(e) => setNewStaff({ ...newStaff, telefon: e.target.value })} />
            </div>
            <Input
              label="Geçici Şifre"
              type="password"
              value={newStaff.sifre}
              onChange={(e) => setNewStaff({ ...newStaff, sifre: e.target.value })}
              placeholder="En az 8 char + büyük/küçük/rakam"
              helper="Çalışan ilk girişinde değiştirebilir."
            />
            <Button
              variant="secondary"
              fullWidth
              onClick={() => {
                if (!newStaff.ad || !newStaff.soyad || !newStaff.email) return;
                if (!newStaff.sifre || newStaff.sifre.length < 8) {
                  setError("Geçici şifre en az 8 karakter olmalı");
                  return;
                }
                setError(null);
                setStaffList([...staffList, newStaff]);
                setNewStaff({ ad: "", soyad: "", email: "", telefon: "", sifre: "" });
              }}
            >
              + Çalışan Ekle
            </Button>
            <div className="flex justify-between pt-2">
              <Button variant="ghost" onClick={() => setStep(4)}>Atla →</Button>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setStep(2)}>← Geri</Button>
                <Button onClick={saveStep3} loading={submitting}>Devam →</Button>
              </div>
            </div>
          </>
        )}

        {step === 4 && (
          <>
            <p className="text-sm text-slate-600">
              Hizmetlerinizi kategoriye ayırmak için önce kategori ekleyin. (Saç Bakım, Tıraş, Manikür, vb.)
              Hizmetleri bir sonraki adımda bu kategorilere bağlayacaksınız.
            </p>
            {categoryList.length > 0 && (
              <div className="border rounded-md divide-y divide-slate-100">
                {categoryList.map((c, i) => (
                  <div key={i} className="px-3 py-2 flex justify-between items-center text-sm">
                    <span>{c.ad}</span>
                    <button
                      type="button"
                      onClick={() => setCategoryList(categoryList.filter((_, j) => j !== i))}
                      className="text-red-500 hover:text-red-700"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="flex gap-2">
              <Input
                placeholder="Kategori adı (örn. Saç Bakım)"
                value={newCategory}
                onChange={(e) => setNewCategory(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && newCategory.trim()) {
                    e.preventDefault();
                    setCategoryList([...categoryList, { ad: newCategory.trim() }]);
                    setNewCategory("");
                  }
                }}
              />
              <Button
                variant="secondary"
                onClick={() => {
                  if (!newCategory.trim()) return;
                  setCategoryList([...categoryList, { ad: newCategory.trim() }]);
                  setNewCategory("");
                }}
              >
                + Ekle
              </Button>
            </div>
            <div className="flex justify-between pt-2">
              <Button variant="ghost" onClick={() => setStep(5)}>Atla →</Button>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setStep(3)}>← Geri</Button>
                <Button onClick={saveStep4} loading={submitting}>Devam →</Button>
              </div>
            </div>
          </>
        )}

        {step === 5 && (
          <>
            <p className="text-sm text-slate-600">
              Sunduğunuz ilk hizmetleri ekleyin. (Saç kesim, sakal tıraşı, vb.)
            </p>
            {serviceList.length > 0 && (
              <div className="border rounded-md divide-y divide-slate-100">
                {serviceList.map((s, i) => (
                  <div key={i} className="px-3 py-2 flex justify-between items-center text-sm">
                    <span>{s.ad} — {s.sureDakika} dk — {s.fiyat} ₺</span>
                    <button
                      type="button"
                      onClick={() => setServiceList(serviceList.filter((_, j) => j !== i))}
                      className="text-red-500 hover:text-red-700"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}
            <Input label="Hizmet adı" placeholder="Saç kesim" value={newService.ad} onChange={(e) => setNewService({ ...newService, ad: e.target.value })} />
            {createdCategoryIds.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Kategori</label>
                <select
                  value={newService.kategoriId}
                  onChange={(e) => setNewService({ ...newService, kategoriId: Number(e.target.value) })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm"
                >
                  <option value={0}>Kategorisiz</option>
                  {createdCategoryIds.map((c) => (
                    <option key={c.id} value={c.id}>{c.ad}</option>
                  ))}
                </select>
              </div>
            )}
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Süre</label>
                <div className="relative">
                  <input
                    type="number"
                    min={5}
                    value={newService.sureDakika}
                    onChange={(e) => setNewService({ ...newService, sureDakika: Number(e.target.value) })}
                    className="w-full px-3 py-2 pr-10 border border-slate-300 rounded-md text-sm"
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-slate-500">dk</span>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Fiyat</label>
                <div className="relative">
                  <input
                    type="number"
                    min={0}
                    value={newService.fiyat}
                    onChange={(e) => setNewService({ ...newService, fiyat: Number(e.target.value) })}
                    className="w-full px-3 py-2 pr-8 border border-slate-300 rounded-md text-sm"
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-sm text-slate-500">₺</span>
                </div>
              </div>
            </div>
            {createdStaffIds.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Bu hizmeti yapabilecek çalışanlar</label>
                <p className="text-xs text-slate-500 mb-2">Hiçbir seçim yapılmazsa tüm çalışanlar verebilir kabul edilir.</p>
                <div className="border rounded-md max-h-32 overflow-y-auto divide-y divide-slate-100">
                  {createdStaffIds.map((s) => (
                    <label key={s.id} className="flex items-center gap-3 px-3 py-2 hover:bg-slate-50 cursor-pointer text-sm">
                      <input
                        type="checkbox"
                        checked={newService.staffIds.includes(s.id)}
                        onChange={(e) => {
                          setNewService({
                            ...newService,
                            staffIds: e.target.checked
                              ? [...newService.staffIds, s.id]
                              : newService.staffIds.filter((x) => x !== s.id),
                          });
                        }}
                        className="rounded text-[var(--color-primary)]"
                      />
                      <span>{s.ad}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}
            <Button
              variant="secondary"
              fullWidth
              onClick={() => {
                if (!newService.ad.trim()) return;
                setServiceList([...serviceList, { ...newService, kategoriId: newService.kategoriId || null }]);
                setNewService({ ad: "", sureDakika: 30, fiyat: 200, kategoriId: 0, staffIds: [] });
              }}
            >
              + Hizmet Ekle
            </Button>
            <div className="flex justify-between pt-2">
              <Button variant="ghost" onClick={() => setStep(6)}>Atla →</Button>
              <div className="flex gap-2">
                <Button variant="secondary" onClick={() => setStep(4)}>← Geri</Button>
                <Button onClick={saveStep5} loading={submitting}>Devam →</Button>
              </div>
            </div>
          </>
        )}

        {step === 6 && (
          <>
            <div className="text-center py-8 space-y-4">
              <div className="text-6xl">🎉</div>
              <h2 className="text-2xl font-bold text-slate-900">Tebrikler!</h2>
              <p className="text-slate-600">
                İşletmenizin temel kurulumu tamamlandı. Artık dashboard'da müşteri,
                randevu, raporlar gibi tüm özellikleri kullanmaya başlayabilirsiniz.
              </p>
              <ul className="text-sm text-slate-600 max-w-md mx-auto text-left space-y-1 bg-slate-50 rounded-lg p-4">
                <li>💡 İlk WhatsApp şablonunuzu Ayarlar &gt; WhatsApp'tan ayarlayın</li>
                <li>💡 AI asistan ayarlarını Ayarlar &gt; AI'dan yapın</li>
                <li>💡 Komisyon kurallarını Çalışanlar &gt; Komisyon'dan tanımlayın</li>
              </ul>
            </div>
            <Button onClick={finish} loading={submitting} fullWidth size="lg">
              Dashboard'a Git
            </Button>
          </>
        )}
      </CardContent>
    </Card>
  );
}
