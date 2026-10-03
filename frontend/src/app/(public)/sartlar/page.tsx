import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Kullanım Şartları",
  description: "AppointFlow kullanım şartları — hizmet kapsamı, hesap kuralları, ödeme ve sorumluluk reddi.",
  alternates: { canonical: "/sartlar" },
  robots: { index: true, follow: true },
};

export default function SartlarPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 prose prose-slate">
      <h1 className="text-3xl font-bold text-slate-900 mb-2">Kullanım Şartları</h1>
      <p className="text-sm text-slate-500 mb-8">Son güncelleme: 16.06.2026</p>

      <div className="space-y-6 text-slate-700">
        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">1. Hizmet Kapsamı</h2>
          <p>
            AppointFlow, berberler ve uzmanlar için randevu yönetimi, müşteri takibi, WhatsApp
            iletişimi ve AI asistan hizmetleri sunan bir SaaS platformudur.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">2. Hesap ve Kullanım</h2>
          <p>
            Kullanıcılar, hesap bilgilerinin gizliliğinden ve hesap üzerinden gerçekleştirilen
            tüm faaliyetlerden sorumludur. Hesabın yetkisiz kullanımı durumunda derhal
            bilgilendirme yapılması gerekir.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">3. Ödeme ve Abonelik</h2>
          <p>
            14 günlük ücretsiz deneme süresinin ardından, seçilen plana göre aylık abonelik
            bedeli tahsil edilir. Aboneliğinizi istediğiniz zaman iptal edebilirsiniz; iptal
            sonrası mevcut ödeme dönemi sonuna kadar hizmetten yararlanmaya devam edersiniz.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">4. Sorumluluk Reddi</h2>
          <p>
            AppointFlow, hizmetin kesintisiz veya hatasız çalışacağını garanti etmez. Veri kaybı,
            mali kayıp veya dolaylı zararlardan sorumlu tutulamaz.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">5. Değişiklikler</h2>
          <p>
            AppointFlow, bu şartları gerektiğinde güncelleyebilir. Önemli değişiklikler e-posta
            ile bildirilir.
          </p>
        </section>
      </div>
    </div>
  );
}
