import type { MetadataRoute } from "next";
import { BRAND_NAME } from "@/lib/constants";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: BRAND_NAME,
    short_name: BRAND_NAME,
    description: "Berberler ve uzmanlar için modern randevu yönetim platformu",
    start_url: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#0f4c75",
    lang: "tr",
    orientation: "portrait",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml" },
      { src: "/favicon.ico", sizes: "any", type: "image/x-icon" },
    ],
  };
}
