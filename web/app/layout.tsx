import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ogoula — L'Afrique et sa richesse culturelle dans le monde entier",
  description:
    "L'Afrique et sa richesse culturelle dans le monde entier. Réseau social au service des langues, des traditions et du lien entre continents.",
  openGraph: {
    title: "Ogoula — L'Afrique et sa richesse culturelle dans le monde entier",
    description:
      "L'Afrique et sa richesse culturelle dans le monde entier — langues, traditions et lien entre continents.",
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
