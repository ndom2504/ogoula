import Link from "next/link";
import { OgoulaBrandMark } from "@/components/OgoulaBrandMark";
import { Smartphone, ArrowRight, Sparkles } from "lucide-react";

const SITE = "https://www.ogoula.com";

export const metadata = {
  title: "Invitation · Ogoula",
  description:
    "Tu as reçu une invitation à rejoindre Ogoula ou une communauté. Télécharge l’app pour participer au fil et aux groupes.",
  openGraph: {
    title: "Invitation · Ogoula",
    description:
      "Rejoins Ogoula — réseau pour les cultures africaines et le monde. Télécharge l’application Android.",
    url: `${SITE}/invite`,
    siteName: "Ogoula",
    locale: "fr_FR",
    type: "website",
  },
};

export default function InvitePage() {
  const playStoreUrl = process.env.NEXT_PUBLIC_PLAY_STORE_URL?.trim() ?? "";

  return (
    <div className="min-h-screen bg-gradient-to-b from-[#007a36] to-[#004422] text-white">
      <header className="mx-auto flex max-w-lg items-center justify-center gap-3 px-6 pt-12 pb-6">
        <OgoulaBrandMark size="lg" variant="white" />
        <span className="text-2xl font-black tracking-tight">Ogoula</span>
      </header>

      <main className="mx-auto max-w-lg px-6 pb-16">
        <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-1.5 text-xs font-semibold">
          <Sparkles size={14} />
          Lien d’invitation
        </div>

        <h1 className="text-3xl font-black leading-tight sm:text-4xl">
          Bienvenue sur Ogoula
        </h1>
        <p className="mt-4 text-base leading-relaxed text-white/90">
          Une personne t’a invité·e à rejoindre le réseau ou une communauté. Installe l’application pour voir le fil,
          publier et retrouver ton groupe — pas besoin de laisser un email ici.
        </p>

        <div className="mt-10 flex flex-col gap-3">
          {playStoreUrl ? (
            <a
              href={playStoreUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center justify-center gap-2 rounded-2xl bg-white px-6 py-4 text-base font-bold text-[#009A44] shadow-lg transition hover:bg-white/95"
            >
              <Smartphone size={22} />
              Télécharger sur le Play Store
              <ArrowRight size={18} />
            </a>
          ) : (
            <div className="rounded-2xl border border-white/30 bg-white/10 px-5 py-4 text-center text-sm text-white/90">
              L’app arrive sur le Play Store. En attendant, découvre le projet sur le site principal.
            </div>
          )}

          <Link
            href="/"
            className="inline-flex items-center justify-center gap-2 rounded-2xl border-2 border-white/80 px-6 py-3.5 text-center text-base font-semibold text-white transition hover:bg-white/10"
          >
            Visiter ogoula.com
            <ArrowRight size={18} />
          </Link>
        </div>

        <p className="mt-10 text-center text-xs text-white/60">
          Partage ce lien pour inviter : <span className="font-mono text-white/80">{SITE}/invite</span>
        </p>
      </main>
    </div>
  );
}
