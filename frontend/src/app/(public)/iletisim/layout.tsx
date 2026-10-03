import type { Metadata } from "next";
import { ReactNode } from "react";

export const metadata: Metadata = {
  title: "İletişim — Demo İste",
  description:
    "AppointFlow ile iletişime geçin. Demo isteği, özel teklif veya sorularınız için bize yazın.",
  alternates: { canonical: "/iletisim" },
  openGraph: {
    title: "İletişim — AppointFlow",
    description: "Demo isteği, özel teklif veya destek için bize yazın.",
    url: "/iletisim",
  },
};

export default function IletisimLayout({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
