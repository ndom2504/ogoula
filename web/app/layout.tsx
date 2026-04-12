import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Ogoula — L'Afrique du monde entier, une culture, une identité",
  description:
    "L'Afrique du monde entier : une culture, une identité. Réseau social au service des langues, des traditions et du lien entre continents.",
  openGraph: {
    title: "Ogoula",
    description: "L'Afrique du monde entier — une culture, une identité.",
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
