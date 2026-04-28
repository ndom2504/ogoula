"use client";
import { useState } from "react";
import {
  Users, MessageCircle, Shield, Globe, Star, ChevronDown,
  Smartphone, Lock, ArrowRight, Menu, X, Languages, Sparkles,
  Globe2, Link2, Scale
} from "lucide-react";
import { OgoulaBrandMark } from "@/components/OgoulaBrandMark";

export default function LandingPage() {
  const [menuOpen, setMenuOpen] = useState(false);
  const playStoreUrl = process.env.NEXT_PUBLIC_PLAY_STORE_URL?.trim() ?? "";

  return (
    <div className="min-h-screen bg-black text-white overflow-x-hidden">

      <nav className="fixed top-0 left-0 right-0 z-50 bg-black/95 backdrop-blur text-white border-b border-white/10 shadow-md">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <span className="text-2xl font-black tracking-tight flex items-center gap-3 text-white">
            <OgoulaBrandMark size="md" variant="color" />
            Ogoula
          </span>
          <div className="hidden md:flex items-center gap-8 text-sm font-medium text-white/95">
            <a href="#pourquoi" className="hover:text-white transition-colors">Le problème</a>
            <a href="#vision" className="hover:text-white transition-colors">Vision</a>
            <a href="#features" className="hover:text-white transition-colors">Comment ça marche</a>
            <a href="#valeurs" className="hover:text-white transition-colors">Fonctionnalités</a>
            <a href="#download" className="bg-gradient-rainbow bg-cover text-black px-4 py-2 rounded-full font-semibold hover:opacity-90 transition-opacity">
              Télécharger
            </a>
          </div>
          <button type="button" className="md:hidden text-white p-2" onClick={() => setMenuOpen(!menuOpen)} aria-label="Menu">
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
        {menuOpen && (
          <div className="md:hidden bg-black/80 backdrop-blur border-t border-white/10 px-6 py-4 flex flex-col gap-4 text-sm font-medium text-white">
            <a href="#pourquoi" onClick={() => setMenuOpen(false)}>Le problème</a>
            <a href="#vision" onClick={() => setMenuOpen(false)}>Vision</a>
            <a href="#features" onClick={() => setMenuOpen(false)}>Comment ça marche</a>
            <a href="#valeurs" onClick={() => setMenuOpen(false)}>Fonctionnalités</a>
            <a href="#download" className="bg-gradient-rainbow bg-cover text-black px-4 py-2 rounded-full text-center font-semibold" onClick={() => setMenuOpen(false)}>
              Télécharger
            </a>
          </div>
        )}
      </nav>

      <section className="flex min-h-screen flex-col overflow-hidden pt-16">
        <div className="relative min-h-[42vh] flex-1 w-full bg-black">
          <video
            className="absolute inset-0 h-full w-full object-cover"
            autoPlay
            muted
            loop
            playsInline
            aria-hidden
          >
            <source src="/presentation.mp4" type="video/mp4" />
          </video>
          <div className="pointer-events-none absolute inset-0 z-[1] bg-gradient-to-b from-black/20 via-transparent to-black/50" />
        </div>

        <div className="relative z-20 w-full shrink-0 rounded-t-3xl border-t border-white/10 bg-gradient-to-b from-black to-black/80 px-5 py-8 shadow-[0_-12px_40px_rgba(0,0,0,0.75)] sm:px-8 sm:py-10 md:py-12">
          <div className="mx-auto max-w-4xl text-center">
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/5 px-3 py-1.5 text-xs font-semibold text-white sm:text-sm">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-gradient-to-r from-red-500 to-purple-500" />
              Bientôt disponible sur Android
            </div>

            <h1 className="text-3xl font-black leading-tight text-white sm:text-4xl md:text-5xl lg:text-6xl">
              Valorisez les marques. Influencez les tendances.
            </h1>

            <p className="mx-auto mt-6 max-w-3xl text-center text-sm leading-relaxed text-white/80 sm:text-base md:text-lg">
              Ogoula est une plateforme de valorisation et d&apos;influence où les marques, les produits et les personnalités gagnent en visibilité grâce aux interactions, aux votes et aux retours de la communauté.
            </p>

            <div className="mt-8 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
              <a
                href="#download"
                className="inline-flex w-full max-w-xs items-center justify-center gap-2 rounded-2xl bg-gradient-rainbow bg-cover px-6 py-3.5 text-base font-bold text-black shadow-lg transition hover:opacity-90 hover:scale-[1.02] sm:w-auto sm:max-w-none sm:px-8 sm:py-4 sm:text-lg"
              >
                <Smartphone size={20} />
                Rejoindre la communauté
                <ArrowRight size={18} />
              </a>
              <a
                href="#pourquoi"
                className="inline-flex w-full max-w-xs items-center justify-center gap-2 rounded-2xl border-2 border-white/40 px-6 py-3.5 text-base font-semibold text-white transition hover:bg-white/10 sm:w-auto sm:max-w-none sm:px-8 sm:py-4 sm:text-lg"
              >
                En savoir plus
                <ChevronDown size={18} />
              </a>
            </div>

            <div className="mt-8 flex flex-wrap justify-center gap-3 sm:gap-4">
              {[
                { icon: <Star size={16} />, label: "Votez & évaluez" },
                { icon: <Users size={16} />, label: "Communauté authentique" },
                { icon: <Link2 size={16} />, label: "Accès direct aux produits" },
              ].map((item, i) => (
                <div
                  key={i}
                  className="flex items-center gap-2 rounded-full border border-white/20 bg-white/5 px-3 py-2 text-xs text-white backdrop-blur-sm sm:px-4 sm:text-sm"
                >
                  <span className="text-white">{item.icon}</span>
                  <span className="text-white/95">{item.label}</span>
                </div>
              ))}
            </div>

            <a
              href="#pourquoi"
              className="mx-auto mt-6 flex w-fit animate-bounce items-center gap-1 text-xs text-white/40 hover:text-white/70"
              aria-label="Faire défiler vers la suite"
            >
              <ChevronDown size={22} />
            </a>
          </div>
        </div>
      </section>

      <section id="pourquoi" className="border-y border-white/10 bg-gradient-to-b from-black via-black to-black/95 py-24 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <p className="bg-gradient-rainbow bg-clip-text text-transparent font-bold text-sm uppercase tracking-widest mb-4">Le problème</p>
          <h2 className="text-3xl md:text-5xl font-black leading-tight mb-8 text-white">
            Les utilisateurs découvrent sans conviction
          </h2>
          <p className="text-white/75 text-lg leading-relaxed mb-6">
            Aujourd&apos;hui, les utilisateurs découvrent des produits sans savoir s&apos;ils valent réellement le coup.
            Les marques, elles, manquent de retours authentiques et d&apos;engagement réel.
          </p>
          <p className="text-white/75 text-lg leading-relaxed">
            Ogoula réinvente la découverte en donnant du pouvoir à la communauté —
            <span className="bg-gradient-rainbow bg-clip-text text-transparent font-bold"> les opinions réelles remplacent la publicité</span>.
          </p>
          <div className="mt-10 flex justify-center">
            <OgoulaBrandMark size="lg" />
          </div>
        </div>
      </section>

      <section id="vision" className="bg-black/50 py-24 px-6 border-b border-white/10">
        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-2 gap-16 items-center">
            <div>
              <p className="bg-gradient-rainbow bg-clip-text text-transparent font-bold text-sm uppercase tracking-widest mb-4">Vision</p>
              <h2 className="text-4xl md:text-5xl font-black leading-tight mb-6 text-white">
                Une plateforme où l&apos;opinion réelle<span className="bg-gradient-rainbow bg-clip-text text-transparent"> prime sur la publicité</span>
              </h2>
              <p className="text-white/75 text-lg leading-relaxed mb-6">
                Nous croyons en une plateforme où la valeur d&apos;un produit ne dépend pas de la publicité,
                mais de l&apos;opinion réelle des utilisateurs.
              </p>
              <p className="text-white/75 text-lg leading-relaxed">
                Ogoula est l&apos;endroit où les produits et marques gagnent en visibilité grâce aux votes,
                aux retours authentiques et à l&apos;engagement de la communauté — pas via des budgets publicitaires.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                { color: "#FF3B3B", title: "Votez", desc: "Évaluez les produits en temps réel" },
                { color: "#FFD93D", title: "Comparez", desc: "Duels & comparaisons side-by-side" },
                { color: "#3BFF8C", title: "Influencez", desc: "Faites émerger les vraies tendances" },
                { color: "#3BA7FF", title: "Accédez", desc: "Liens directs vers les produits" },
              ].map((card, i) => (
                <div key={i} className="rounded-2xl border border-white/10 bg-white/5 p-5 transition-all hover:border-white/20 hover:bg-white/10">
                  <div className="w-8 h-8 rounded-lg mb-3" style={{ background: card.color }} />
                  <h3 className="mb-1 text-base font-bold text-white">{card.title}</h3>
                  <p className="text-sm text-white/70">{card.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="border-t border-white/10 bg-black py-24 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <p className="bg-gradient-rainbow bg-clip-text text-transparent font-bold text-sm uppercase tracking-widest mb-4">Comment ça marche</p>
            <h2 className="text-4xl md:text-5xl font-black text-white">3 étapes simples</h2>
            <p className="mx-auto mt-4 max-w-xl text-lg text-white/75">
              Découvrez, interagissez et accédez aux meilleurs produits.
            </p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {FEATURES.map((f, i) => (
              <div key={i} className="rounded-3xl border border-white/10 bg-white/5 p-8 transition-all hover:-translate-y-1 hover:border-white/20 hover:bg-white/10">
                <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl" style={{ background: f.color + "22", color: f.color }}>
                  {f.icon}
                </div>
                <h3 className="mb-2 text-xl font-bold text-white">{f.title}</h3>
                <p className="leading-relaxed text-white/70">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="valeurs" className="bg-black/50 py-24 px-6 border-y border-white/10">
        <div className="max-w-6xl mx-auto">
          <div className="mx-auto mb-16 max-w-3xl text-center">
            <p className="bg-gradient-rainbow bg-clip-text text-transparent font-bold text-sm uppercase tracking-widest mb-4">Fonctionnalités principales</p>
            <h2 className="text-4xl md:text-5xl font-black text-white">Les vraies forces d&apos;Ogoula</h2>
            <p className="mt-4 text-lg leading-relaxed text-white/75">
              Des outils conçus pour valoriser authentiquement les produits et faire émerger les tendances réelles.
            </p>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8">
            {VALUES.map((v, i) => (
              <div key={i} className="rounded-3xl border border-white/10 bg-white/5 p-8 text-center transition-all hover:border-white/20 hover:bg-white/10">
                <div className="mb-5 inline-flex h-16 w-16 items-center justify-center rounded-2xl" style={{ background: v.color + "22", color: v.color }}>
                  {v.icon}
                </div>
                <h3 className="mb-3 text-xl font-bold text-white">{v.title}</h3>
                <p className="text-sm leading-relaxed text-white/70 md:text-base">{v.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-24 px-6 bg-gradient-to-r from-black via-purple-900/10 to-black border-b border-white/10">
        <div className="max-w-6xl mx-auto text-center">
          <h2 className="text-4xl font-black text-white mb-4">Pour qui ?</h2>
          <p className="mb-12 text-lg text-white/75">Utilisateurs et marques trouvent ensemble leur place</p>
          <div className="grid md:grid-cols-3 gap-6">
            {TESTIMONIALS.map((t, i) => (
              <div key={i} className="rounded-2xl border border-white/10 bg-white/5 p-6 text-left text-white backdrop-blur transition-all hover:bg-white/10 hover:border-white/20">
                <div className="mb-3 flex items-center gap-1">
                  {[...Array(5)].map((_, j) => <Star key={j} size={14} fill="currentColor" className="text-yellow-400" />)}
                </div>
                <p className="mb-4 italic text-white/90">&ldquo;{t.text}&rdquo;</p>
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-rainbow bg-cover text-sm font-bold">
                    {t.name[0]}
                  </div>
                  <div>
                    <p className="text-sm font-semibold">{t.name}</p>
                    <p className="text-xs text-white/60">{t.location}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="download" className="border-t border-white/10 bg-black py-24 px-6">
        <div className="mx-auto max-w-2xl text-center">
          <div className="mb-6 flex justify-center scale-150">
            <OgoulaBrandMark size="lg" />
          </div>
          <h2 className="mb-4 text-4xl font-black text-white md:text-5xl">
            Rejoignez Ogoula dès maintenant
          </h2>
          <p className="mb-6 text-lg text-white/75">
            L&apos;application Ogoula sera bientôt disponible sur le Play Store.
            {playStoreUrl
              ? " Tu peux l&apos;installer tout de suite ou recevoir une alerte au lancement."
              : " Laisse ton email pour être informé·e au lancement, ou suis le lien d&apos;invitation si on t&apos;a partagé ogoula.com/invite."}
          </p>
          {playStoreUrl && (
            <div className="mb-8">
              <a
                href={playStoreUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-rainbow bg-cover px-8 py-4 text-base font-bold text-black shadow-md transition hover:opacity-90"
              >
                <Smartphone size={22} />
                Ouvrir sur Google Play
              </a>
            </div>
          )}
          <p className="mb-4 text-sm font-semibold text-white/90">
            Tu as reçu un lien d&apos;invitation ?{" "}
            <a href="/invite" className="bg-gradient-rainbow bg-clip-text text-transparent underline underline-offset-2 hover:opacity-80">
              Page dédiée (sans email obligatoire)
            </a>
          </p>
          <form
            onSubmit={(e) => { e.preventDefault(); alert("Merci ! Tu seras notifié au lancement."); }}
            className="flex flex-col sm:flex-row gap-3 max-w-md mx-auto"
          >
            <input
              type="email"
              required
              placeholder="ton@email.com"
              className="flex-1 rounded-2xl border border-white/20 bg-white/5 px-5 py-3 text-sm text-white placeholder:text-white/40 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:bg-white/10"
            />
            <button
              type="submit"
              className="bg-gradient-rainbow bg-cover text-black px-6 py-3 rounded-2xl font-bold hover:opacity-90 transition-opacity whitespace-nowrap"
            >
              Me notifier
            </button>
          </form>
            <p className="mt-4 text-xs text-white/50">Aucun spam — une notification au lancement. L&apos;email reste optionnel si tu installes depuis le Play Store.</p>
        </div>
      </section>

      <footer className="bg-black border-t border-white/10 px-6 py-12 text-white/70">
        <div className="mx-auto max-w-6xl">
          <div className="flex flex-col items-center justify-between gap-6 md:flex-row">
            <div className="flex items-center gap-3">
              <OgoulaBrandMark size="md" variant="color" />
              <div>
                <p className="text-xl font-black text-white">Ogoula</p>
                <p className="mt-1 text-sm text-white/60">Plateforme de valorisation et d&apos;influence</p>
              </div>
            </div>
            <div className="flex flex-wrap justify-center gap-4 text-sm md:gap-6">
              <a href="/privacy" className="transition-colors hover:text-white">
                Politique de confidentialité
              </a>
              <a href="/delete-account" className="transition-colors hover:text-white">
                Suppression des données
              </a>
              <a href="mailto:contact@ogoula.com" className="transition-colors hover:text-white">
                Contact
              </a>
            </div>
          </div>
          <div className="mt-8 border-t border-white/10 pt-8 text-center text-xs text-white/40">
            © {new Date().getFullYear()} Ogoula. Tous droits réservés.
          </div>
        </div>
      </footer>
    </div>
  );
}

const VALUES = [
  {
    icon: <Star size={28} />,
    title: "Produits interactifs",
    color: "#FF3B3B",
    desc: "Chaque produit est noté et évalué par la communauté — des avis authentiques à la place de la publicité.",
  },
  {
    icon: <Users size={28} />,
    title: "Duels & votes",
    color: "#FF8C42",
    desc: "Comparez facilement deux produits et voyez les préférences réelles de la communauté en temps réel.",
  },
  {
    icon: <Globe2 size={28} />,
    title: "Bled (communautés)",
    color: "#FFD93D",
    desc: "Explorez des univers spécialisés : mode, musique, tech, et bien d'autres — avec leurs propres tendances.",
  },
  {
    icon: <MessageCircle size={28} />,
    title: "Kongossa",
    color: "#3BFF8C",
    desc: "Discutez et débattez autour des produits, des tendances et des découvertes qui vous passionnent.",
  },
  {
    icon: <Link2 size={28} />,
    title: "Accès direct",
    color: "#3BA7FF",
    desc: "Liens directs vers les produits — transformez les votes en achats en un seul clic.",
  },
  {
    icon: <Sparkles size={28} />,
    title: "Tendances émergentes",
    color: "#A259FF",
    desc: "Voyez en temps réel ce qui monte, ce qui plaît vraiment à la communauté — pas aux algorithmes.",
  },
];

const FEATURES = [
  {
    icon: <Globe size={24} />, color: "#FF3B3B",
    title: "Découvrez",
    desc: "Parcourez un feed de produits, tendances et contenus.",
  },
  {
    icon: <Users size={24} />, color: "#3BA7FF",
    title: "Interagissez",
    desc: "Votez, commentez, comparez et participez aux duels.",
  },
  {
    icon: <Link2 size={24} />, color: "#A259FF",
    title: "Accédez",
    desc: "Accédez directement aux produits via des liens d'achat.",
  },
];

const TESTIMONIALS = [
  { name: "Utilisateur", location: "Plateforme",
    text: "Une nouvelle façon de découvrir les produits" },
  { name: "Utilisateur", location: "Plateforme",
    text: "Enfin une plateforme où mon avis compte" },
  { name: "Marque", location: "Partenaire",
    text: "Des retours authentiques pour améliorer nos produits" },
];
