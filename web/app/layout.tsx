import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
   title: "Ogoula — Valorisez les marques. Influencez les tendances.",
  description:
    "Plateforme de valorisation et d'influence où les marques, produits et personnalités gagnent en visibilité grâce aux votes, interactions et retours authentiques de la communauté.",
  openGraph: {
    title: "Ogoula — Valorisez les marques. Influencez les tendances.",
    description:
      "Plateforme de valorisation et d'influence — votes, interactions et retours authentiques pour les produits et marques.",
    url: "https://www.ogoula.com",
    siteName: "Ogoula",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
