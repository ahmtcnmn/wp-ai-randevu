import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/ui/Button";
import { Card, CardContent } from "@/components/ui/Card";
import { JsonLd } from "@/components/seo/JsonLd";

const faqJsonLd = {
  "@context": "https://schema.org",
  "@type": "FAQPage",
  mainEntity: [
    {
      "@type": "Question",
      name: "AppointFlow nedir?",
      acceptedAnswer: {
        "@type": "Answer",
        text:
          "AppointFlow, berberler ve uzmanlar için tasarlanmış modern bir randevu yönetim platformudur. WhatsApp entegrasyonu, AI asistan, otomatik hatırlatma ve detaylı raporlama içerir.",
      },
    },
    {
      "@type": "Question",
      name: "Ücretsiz deneme süresi var mı?",
      acceptedAnswer: {
        "@type": "Answer",
        text:
          "Evet, tüm planlar için 14 gün ücretsiz deneme sunulmaktadır. Deneme süresinde kredi kartı bilgisi gerekmez.",
      },
    },
    {
      "@type": "Question",
      name: "WhatsApp entegrasyonu nasıl çalışıyor?",
      acceptedAnswer: {
        "@type": "Answer",
        text:
          "Müşterileriniz WhatsApp üzerinden randevu alabilir, AI asistanınız 7/24 müşteri sorularına otomatik yanıt verir. Kendi WhatsApp Business hesabınızla entegre olur.",
      },
    },
    {
      "@type": "Question",
      name: "Aboneliği istediğim zaman iptal edebilir miyim?",
      acceptedAnswer: {
        "@type": "Answer",
        text:
          "Evet. İptal edildikten sonra mevcut ödeme dönemi sonuna kadar hizmetten yararlanmaya devam edersiniz.",
      },
    },
  ],
};

export const metadata: Metadata = {
  // Template'i bypass et — landing için tek satır title
  title: { absolute: "AppointFlow — Berberler için Modern Randevu Yönetimi" },
  description:
    "Berberinizin tüm randevularını tek yerden yönetin. WhatsApp entegrasyonu, AI asistan, otomatik hatırlatma. 14 gün ücretsiz.",
  alternates: { canonical: "/" },
  openGraph: {
    title: "AppointFlow — Berberler için Modern Randevu Yönetimi",
    description:
      "WhatsApp + AI ile berber/kuaför işletmeniz için randevu sistemi. 14 gün ücretsiz deneyin.",
    url: "/",
    type: "website",
  },
};

export default function LandingPage() {
  return (
    <>
      <JsonLd data={faqJsonLd} />
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-slate-50 via-white to-[var(--color-primary-soft)]/30" />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 lg:py-32">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[var(--color-accent-soft)] text-[var(--color-accent-hover)] text-xs font-medium mb-6">
                <span className="w-2 h-2 rounded-full bg-[var(--color-accent)]" />
                Yeni: WhatsApp + AI Asistan
              </div>
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-slate-900 tracking-tight leading-tight">
                Berberinizin tüm randevularını <span className="text-[var(--color-primary)]">tek yerden</span> yönetin
              </h1>
              <p className="mt-6 text-lg text-slate-600 max-w-xl">
                AppointFlow, berberler ve uzmanlar için tasarlanmış modern randevu yönetim platformu. WhatsApp entegrasyonu, AI asistan, otomatik hatırlatma ve detaylı raporlama.
              </p>
              <div className="mt-8 flex flex-col sm:flex-row gap-3">
                <Link href="/register">
                  <Button size="lg" className="w-full sm:w-auto">
                    14 Gün Ücretsiz Dene
                  </Button>
                </Link>
                <Link href="/iletisim">
                  <Button size="lg" variant="secondary" className="w-full sm:w-auto">
                    Demo İste
                  </Button>
                </Link>
              </div>
              <p className="mt-4 text-sm text-slate-500">
                Kredi kartı gerekmez · 14 gün ücretsiz · İstediğin zaman iptal et
              </p>
            </div>
            <div className="relative">
              <div className="aspect-square max-w-md mx-auto bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-accent)] rounded-3xl p-1 shadow-2xl">
                <div className="bg-white rounded-3xl h-full p-8 flex flex-col gap-4">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                    <div>
                      <div className="font-semibold text-slate-900">Bugün</div>
                      <div className="text-xs text-slate-500">8 randevu, 4.250 ₺ ciro</div>
                    </div>
                    <div className="w-10 h-10 rounded-full bg-[var(--color-primary-soft)] flex items-center justify-center text-[var(--color-primary)] font-bold text-sm">
                      AT
                    </div>
                  </div>
                  {[
                    { t: "09:00", n: "Ali V.", h: "Saç + Sakal", c: "bg-green-100 text-green-700" },
                    { t: "10:30", n: "Mehmet K.", h: "Saç kesim", c: "bg-blue-100 text-blue-700" },
                    { t: "12:00", n: "Hamdi B.", h: "Sakal tıraşı", c: "bg-amber-100 text-amber-700" },
                    { t: "14:00", n: "Burak O.", h: "Premium paket", c: "bg-purple-100 text-purple-700" },
                  ].map((r) => (
                    <div key={r.t} className="flex items-center justify-between p-3 rounded-lg bg-slate-50">
                      <div className="flex items-center gap-3">
                        <div className="text-sm font-medium text-slate-900">{r.t}</div>
                        <div>
                          <div className="text-sm font-medium text-slate-900">{r.n}</div>
                          <div className="text-xs text-slate-500">{r.h}</div>
                        </div>
                      </div>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${r.c}`}>
                        Onaylı
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="py-16 lg:py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <h2 className="text-3xl lg:text-4xl font-bold text-slate-900">
              İşletmenizi büyütmek için her şey
            </h2>
            <p className="mt-4 text-slate-600">
              Operasyonu kolaylaştıran, müşteriyi memnun eden, geliri arttıran araçlar
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[
              {
                icon: "💬",
                title: "WhatsApp Entegrasyonu",
                desc: "Müşterileriniz WhatsApp üzerinden randevu alır, AI asistanınız 7/24 cevap verir",
              },
              {
                icon: "🤖",
                title: "AI Asistan",
                desc: "Yapay zeka destekli müşteri iletişimi — randevu, iptal, soru-cevap otomatik",
              },
              {
                icon: "🔔",
                title: "Otomatik Hatırlatma",
                desc: "Müşterilerinize otomatik WhatsApp/SMS hatırlatma — gelmeme oranını düşürün",
              },
              {
                icon: "💰",
                title: "Komisyon Takibi",
                desc: "Çalışan başına, hizmet/ürün bazlı komisyon ayarları — otomatik kazanç hesabı",
              },
              {
                icon: "📊",
                title: "Detaylı Raporlar",
                desc: "Ciro, müşteri segmentleri, kampanya performansı — kararları veriyle alın",
              },
              {
                icon: "🛒",
                title: "Ürün Satışı",
                desc: "Hizmet yanında ürün de satın — stok takibi, satış raporu, AI önerileri",
              },
            ].map((f) => (
              <Card key={f.title}>
                <CardContent>
                  <div className="text-4xl mb-3">{f.icon}</div>
                  <h3 className="font-semibold text-slate-900 mb-2">{f.title}</h3>
                  <p className="text-sm text-slate-600">{f.desc}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-16 lg:py-24 bg-slate-50">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl lg:text-4xl font-bold text-slate-900">
            14 gün ücretsiz deneyin
          </h2>
          <p className="mt-4 text-slate-600 max-w-xl mx-auto">
            Kayıt olun, kredi kartı gerekmez. Tüm özellikleri 14 gün boyunca ücretsiz kullanın.
          </p>
          <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
            <Link href="/register">
              <Button size="lg" className="w-full sm:w-auto">
                Hemen Başla
              </Button>
            </Link>
            <Link href="/fiyatlandirma">
              <Button size="lg" variant="secondary" className="w-full sm:w-auto">
                Planları İncele
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
