import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Gizlilik Politikası",
  description: "AppointFlow gizlilik politikası — KVKK kapsamında veri işleme, çerezler ve kullanıcı hakları.",
  alternates: { canonical: "/gizlilik" },
  robots: { index: true, follow: true },
};

export default function GizlilikPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 prose prose-slate">
      <h1 className="text-3xl font-bold text-slate-900 mb-2">Gizlilik Politikası</h1>
      <p className="text-sm text-slate-500 mb-8">Son güncelleme: 16.06.2026</p>

      <div className="space-y-6 text-slate-700">
        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">1. Toplanan Bilgiler</h2>
          <p>
            AppointFlow, hizmetlerini sunabilmek için aşağıdaki kişisel verileri toplar:
            ad, soyad, e-posta, telefon, IP adresi, çerez bilgileri, kullanım verileri,
            işletmenize ait randevu ve müşteri kayıtları.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">2. Verilerin Kullanımı</h2>
          <p>
            Kişisel verileriniz; hesabınızın oluşturulması, hizmetlerin sunulması, ödeme işlemlerinin
            gerçekleştirilmesi, müşteri desteğinin sağlanması ve yasal yükümlülüklerin yerine
            getirilmesi amacıyla kullanılır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">3. KVKK Hakları</h2>
          <p>
            6698 sayılı KVKK kapsamında verilerinize erişim, düzeltme, silme ve aktarımın
            durdurulması haklarına sahipsiniz. Bu haklarınızı kullanmak için info@ehasoftware.com
            adresine yazabilirsiniz.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">4. Veri Güvenliği</h2>
          <p>
            Verileriniz SSL/TLS şifreleme ile korunur. Şifreler bcrypt ile hash'lenir. Üçüncü
            taraflarla paylaşım sadece yasal zorunluluk veya sizin onayınızla yapılır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900 mb-2">5. Çerezler</h2>
          <p>
            Oturum yönetimi için zorunlu çerezler kullanırız. Analitik veya pazarlama çerezleri
            için açık onayınız alınır.
          </p>
        </section>
      </div>
    </div>
  );
}
