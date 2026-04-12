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

  return (
    <div className="min-h-screen bg-white text-gray-900 overflow-x-hidden">

      <nav className="fixed top-0 left-0 right-0 z-50 bg-[#009A44] text-white border-b border-white/15 shadow-md">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <span className="text-2xl font-black tracking-tight flex items-center gap-3 text-white">
            <OgoulaBrandMark size="md" variant="white" />
            Ogoula
          </span>
          <div className="hidden md:flex items-center gap-8 text-sm font-medium text-white/95">
            <a href="#pourquoi" className="hover:text-white transition-colors">Notre approche</a>
            <a href="#vision" className="hover:text-white transition-colors">Vision</a>
            <a href="#features" className="hover:text-white transition-colors">Fonctionnalités</a>
            <a href="#valeurs" className="hover:text-white transition-colors">Valeurs</a>
            <a href="#download" className="bg-white text-[#009A44] px-4 py-2 rounded-full font-semibold hover:bg-white/90 transition-colors">
              Télécharger
            </a>
          </div>
          <button type="button" className="md:hidden text-white p-2" onClick={() => setMenuOpen(!menuOpen)} aria-label="Menu">
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
        {menuOpen && (
          <div className="md:hidden bg-[#007a36] border-t border-white/10 px-6 py-4 flex flex-col gap-4 text-sm font-medium text-white">
            <a href="#pourquoi" onClick={() => setMenuOpen(false)}>Notre approche</a>
            <a href="#vision" onClick={() => setMenuOpen(false)}>Vision</a>
            <a href="#features" onClick={() => setMenuOpen(false)}>Fonctionnalités</a>
            <a href="#valeurs" onClick={() => setMenuOpen(false)}>Valeurs</a>
            <a href="#download" className="bg-white text-[#009A44] px-4 py-2 rounded-full text-center font-semibold" onClick={() => setMenuOpen(false)}>
              Télécharger
            </a>
          </div>
        )}
      </nav>

      {/* Hero : vidéo sous le header ; bandeau fixe en bas avec titre, texte, CTA et pastilles */}
      <section className="flex min-h-screen flex-col overflow-hidden pt-16">
        {/* Fenêtre vidéo — occupe l’espace au-dessus du bandeau */}
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
          <div className="pointer-events-none absolute inset-0 z-[1] bg-gradient-to-b from-[#009A44]/10 via-transparent to-black/25" />
        </div>

        {/* Section contenu — ancrée au bas de la fenêtre vidéo (sous la vidéo) */}
        <div className="relative z-20 w-full shrink-0 rounded-t-3xl border-t border-white/20 bg-gradient-to-b from-[#007a36] to-[#004422] px-5 py-8 shadow-[0_-12px_40px_rgba(0,0,0,0.25)] sm:px-8 sm:py-10 md:py-12">
          <div className="mx-auto max-w-4xl text-center">
            <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-1.5 text-xs font-semibold text-white sm:text-sm">
              <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-white" />
              Bientôt disponible sur Android
            </div>

            <h1 className="text-3xl font-black leading-tight text-white sm:text-4xl md:text-5xl lg:text-6xl">
              <span className="block">L&apos;Afrique du monde entier,</span>
              <span className="mt-1 block text-xl font-extrabold tracking-tight text-white/95 sm:text-2xl md:text-3xl lg:text-4xl">
                une culture, une identité
              </span>
            </h1>

            <p className="mx-auto mt-6 max-w-3xl text-center text-sm leading-relaxed text-white/90 sm:text-base md:text-lg">
              Les grandes plateformes mondiales uniformisent ce qu&apos;on voit, ce qu&apos;on dit et dans quelle langue.
              Ogoula propose une autre voie : un espace où les langues, les traditions et les voix locales comptent —
              sans renfermer qui que ce soit. Ici, tu restes toi-même, connecté à ton continent et ouvert au monde.
            </p>

            <div className="mt-8 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
              <a
                href="#download"
                className="inline-flex w-full max-w-xs items-center justify-center gap-2 rounded-2xl bg-white px-6 py-3.5 text-base font-bold text-[#009A44] shadow-lg transition hover:bg-white/95 hover:scale-[1.02] sm:w-auto sm:max-w-none sm:px-8 sm:py-4 sm:text-lg"
              >
                <Smartphone size={20} />
                Rejoindre la communauté
                <ArrowRight size={18} />
              </a>
              <a
                href="#pourquoi"
                className="inline-flex w-full max-w-xs items-center justify-center gap-2 rounded-2xl border-2 border-white/85 px-6 py-3.5 text-base font-semibold text-white transition hover:bg-white/10 sm:w-auto sm:max-w-none sm:px-8 sm:py-4 sm:text-lg"
              >
                Notre approche
                <ChevronDown size={18} />
              </a>
            </div>

            <div className="mt-8 flex flex-wrap justify-center gap-3 sm:gap-4">
              {[
                { icon: <Languages size={16} />, label: "Langues & expressions locales" },
                { icon: <Shield size={16} />, label: "Respect de la vie privée" },
                { icon: <Globe size={16} />, label: "Afrique · diaspora · monde" },
              ].map((item, i) => (
                <div
                  key={i}
                  className="flex items-center gap-2 rounded-full border border-white/25 bg-white/10 px-3 py-2 text-xs text-white backdrop-blur-sm sm:px-4 sm:text-sm"
                >
                  <span className="text-white">{item.icon}</span>
                  <span className="text-white/95">{item.label}</span>
                </div>
              ))}
            </div>

            <a
              href="#pourquoi"
              className="mx-auto mt-6 flex w-fit animate-bounce items-center gap-1 text-xs text-white/50 hover:text-white/80"
              aria-label="Faire défiler vers la suite"
            >
              <ChevronDown size={22} />
            </a>
          </div>
        </div>
      </section>

      <section id="pourquoi" className="border-y border-emerald-100 bg-emerald-50/70 py-24 px-6">
        <div className="max-w-4xl mx-auto text-center">
          <p className="text-[#009A44] font-bold text-sm uppercase tracking-widest mb-4">Notre approche</p>
          <h2 className="text-3xl md:text-5xl font-black leading-tight mb-8 text-[#065f46]">
            Inclusif, pas uniforme
          </h2>
          <p className="text-[#166534]/90 text-lg leading-relaxed mb-6">
            Quand tout le monde consomme le même fil d&apos;actualité mondial, on risque de perdre
            des nuances : les langues qu&apos;on parle à la maison, les histoires qu&apos;on se raconte,
            les rythmes de vie, les solidarités de quartier ou de village. Ogoula existe pour
            <strong className="text-[#064e3b]"> recentrer l&apos;humain et la diversité culturelle</strong>{" "}
            dans l&apos;expérience numérique — sans fermer la porte au reste du monde.
          </p>
          <p className="text-[#166534]/90 text-lg leading-relaxed">
            Nous croyons qu&apos;on peut être <strong className="text-[#047857]">fièrement ancré·e dans ses racines</strong>{" "}
            et <strong className="text-[#009A44]">pleinement connecté·e au monde</strong>. Ce n&apos;est pas
            l&apos;un ou l&apos;autre : c&apos;est le pont que nous voulons construire, ensemble.
          </p>
          <div className="mt-10 flex justify-center">
            <OgoulaBrandMark size="lg" />
          </div>
        </div>
      </section>

      <section id="vision" className="bg-white py-24 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-2 gap-16 items-center">
            <div>
              <p className="text-[#009A44] font-bold text-sm uppercase tracking-widest mb-4">Vision</p>
              <h2 className="text-4xl md:text-5xl font-black leading-tight mb-6 text-gray-900">
                Une communauté numérique au service des{" "}
                <span className="text-[#009A44]">identités</span>,{" "}
                <span className="text-[#15803d]">des langues</span> et des{" "}
                <span className="text-[#047857]">traditions vivantes</span>
              </h2>
              <p className="text-[#166534]/85 text-lg leading-relaxed mb-6">
                Ogoula accueille celles et ceux qui portent l&apos;Afrique dans leur quotidien — sur le continent,
                en diaspora, ou simplement en curiosité respectueuse. Nous voulons un lieu où l&apos;on peut
                s&apos;exprimer dans sa propre musicalité, partager ce qui compte pour soi, et découvrir
                les autres sans tout niveler au même format global.
              </p>
              <p className="text-[#166534]/85 text-lg leading-relaxed">
                La technologie doit servir la <strong className="text-[#064e3b]">transmission</strong>, la <strong className="text-[#064e3b]">fierté</strong> et la{" "}
                <strong className="text-[#064e3b]">convivialité</strong> — pas l&apos;effacement culturel. C&apos;est cette ligne directrice
                qui guide chaque choix de produit chez Ogoula.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                { color: "#009A44", title: "Racines", desc: "Honorer d'où l'on vient, sans exclure qui que ce soit" },
                { color: "#22c55e", title: "Langues", desc: "Espace favorable aux langues et expressions du quotidien" },
                { color: "#15803d", title: "Pont", desc: "Dialoguer entre Afrique, diaspora et amis du monde entier" },
                { color: "#166534", title: "Sobriété", desc: "Pas de pub intrusive, pas de revente de tes données" },
              ].map((card, i) => (
                <div key={i} className="rounded-2xl border border-emerald-100 bg-emerald-50/50 p-5 transition-shadow hover:border-emerald-200 hover:shadow-md">
                  <div className="w-8 h-8 rounded-lg mb-3" style={{ background: card.color }} />
                  <h3 className="mb-1 text-base font-bold text-[#064e3b]">{card.title}</h3>
                  <p className="text-sm text-[#166534]/90">{card.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="border-t border-emerald-100 bg-emerald-50/60 py-24 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-16">
            <p className="text-[#009A44] font-bold text-sm uppercase tracking-widest mb-4">Fonctionnalités</p>
            <h2 className="text-4xl md:text-5xl font-black text-[#065f46]">Tout ce qu&apos;il te faut</h2>
            <p className="mx-auto mt-4 max-w-xl text-lg text-[#166534]/85">
              Une expérience sociale complète, pensée pour des connexions authentiques.
            </p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {FEATURES.map((f, i) => (
              <div key={i} className="rounded-3xl border border-emerald-100 bg-white p-8 transition-all hover:-translate-y-1 hover:border-emerald-200 hover:shadow-lg">
                <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl" style={{ background: f.color + "18" }}>
                  <span style={{ color: f.color }}>{f.icon}</span>
                </div>
                <h3 className="mb-2 text-xl font-bold text-[#064e3b]">{f.title}</h3>
                <p className="leading-relaxed text-[#166534]/85">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="valeurs" className="bg-white py-24 px-6">
        <div className="max-w-6xl mx-auto">
          <div className="mx-auto mb-16 max-w-3xl text-center">
            <p className="text-[#009A44] font-bold text-sm uppercase tracking-widest mb-4">Valeurs</p>
            <h2 className="text-4xl md:text-5xl font-black text-[#065f46]">Ce en quoi nous croyons</h2>
            <p className="mt-4 text-lg leading-relaxed text-[#166534]/85">
              Six principes qui traduisent notre approche : protéger la diversité culturelle,
              ouvrir des ponts sans imposer un modèle unique, et garder la technologie au service des personnes.
            </p>
          </div>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8">
            {VALUES.map((v, i) => (
              <div key={i} className="rounded-3xl border border-emerald-100 bg-emerald-50/40 p-8 text-center transition-colors hover:border-emerald-200">
                <div className="mb-5 inline-flex h-16 w-16 items-center justify-center rounded-2xl" style={{ background: v.color + "22", color: v.color }}>
                  {v.icon}
                </div>
                <h3 className="mb-3 text-xl font-bold text-[#064e3b]">{v.title}</h3>
                <p className="text-sm leading-relaxed text-[#166534]/90 md:text-base">{v.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="py-24 px-6 bg-[#009A44]">
        <div className="max-w-6xl mx-auto text-center">
          <h2 className="text-4xl font-black text-white mb-4">Ils nous soutiennent</h2>
          <p className="mb-12 text-lg text-white/90">Une communauté qui grandit sur plusieurs continents</p>
          <div className="grid md:grid-cols-3 gap-6">
            {TESTIMONIALS.map((t, i) => (
              <div key={i} className="rounded-2xl border border-white/20 bg-white/10 p-6 text-left text-white backdrop-blur">
                <div className="mb-3 flex items-center gap-1">
                  {[...Array(5)].map((_, j) => <Star key={j} size={14} fill="currentColor" className="text-white/90" />)}
                </div>
                <p className="mb-4 italic text-white/95">&ldquo;{t.text}&rdquo;</p>
                <div className="flex items-center gap-3">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/25 text-sm font-bold">
                    {t.name[0]}
                  </div>
                  <div>
                    <p className="text-sm font-semibold">{t.name}</p>
                    <p className="text-xs text-white/75">{t.location}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="download" className="border-t border-emerald-100 bg-emerald-50/80 py-24 px-6">
        <div className="mx-auto max-w-2xl text-center">
          <div className="mb-6 flex justify-center scale-150">
            <OgoulaBrandMark size="lg" />
          </div>
          <h2 className="mb-4 text-4xl font-black text-[#065f46] md:text-5xl">
            Fais partie du mouvement
          </h2>
          <p className="mb-10 text-lg text-[#166534]/85">
            L&apos;application Ogoula sera bientôt disponible sur le Play Store.
            Laisse ton email pour être informé·e au lancement.
          </p>
          <form
            onSubmit={(e) => { e.preventDefault(); alert("Merci ! Tu seras notifié au lancement."); }}
            className="flex flex-col sm:flex-row gap-3 max-w-md mx-auto"
          >
            <input
              type="email"
              required
              placeholder="ton@email.com"
              className="flex-1 rounded-2xl border border-emerald-200 bg-white px-5 py-3 text-sm text-[#166534] placeholder:text-emerald-800/40 focus:outline-none focus:ring-2 focus:ring-[#009A44]"
            />
            <button
              type="submit"
              className="bg-[#009A44] text-white px-6 py-3 rounded-2xl font-bold hover:bg-[#007a36] transition-colors whitespace-nowrap"
            >
              Me notifier
            </button>
          </form>
          <p className="mt-4 text-xs text-[#15803d]/70">Aucun spam — une notification au lancement.</p>
        </div>
      </section>

      <footer className="bg-[#006b32] px-6 py-12 text-white/80">
        <div className="mx-auto max-w-6xl">
          <div className="flex flex-col items-center justify-between gap-6 md:flex-row">
            <div className="flex items-center gap-3">
              <OgoulaBrandMark size="md" variant="white" />
              <div>
                <p className="text-xl font-black text-white">Ogoula</p>
                <p className="mt-1 text-sm text-white/75">Réseau social — cultures africaines & monde</p>
              </div>
            </div>
            <div className="flex gap-6 text-sm">
              <a href="#" className="transition-colors hover:text-white">Confidentialité</a>
              <a href="#" className="transition-colors hover:text-white">CGU</a>
              <a href="mailto:info@misterdil.ca" className="transition-colors hover:text-white">Contact</a>
            </div>
          </div>
          <div className="mt-8 border-t border-white/15 pt-8 text-center text-xs text-white/60">
            © {new Date().getFullYear()} Ogoula. Tous droits réservés.
          </div>
        </div>
      </footer>
    </div>
  );
}

const VALUES = [
  {
    icon: <Globe2 size={28} />,
    title: "Pluralité culturelle",
    color: "#009A44",
    desc: "Aucun « format unique » imposé par un algorithme global. Ici, plusieurs manières d’être, de parler et de raconter peuvent coexister sans être réduites au même moule.",
  },
  {
    icon: <Languages size={28} />,
    title: "Langues & expressions",
    color: "#22c55e",
    desc: "Les langues du quotidien, les mélanges, les registres familiers ou solennels : ils comptent autant que la langue « internationale ». La diversité linguistique est une richesse, pas un obstacle.",
  },
  {
    icon: <Link2 size={28} />,
    title: "Pont sans muraille",
    color: "#15803d",
    desc: "Relier l’Afrique, la diaspora et le reste du monde sans enfermer personne. Être ancré dans une culture n’implique pas de se couper des autres — au contraire.",
  },
  {
    icon: <Lock size={28} />,
    title: "Vie privée & données",
    color: "#047857",
    desc: "Tes données t’appartiennent : pas de revente, pas de publicité ciblée intrusive. Un minimum de collecte, un maximum de transparence sur l’usage.",
  },
  {
    icon: <Scale size={28} />,
    title: "Dignité & équité",
    color: "#166534",
    desc: "Règles claires, modération attentive et lutte contre le harcèlement, les discours de haine et les stéréotypes. Chacun doit pouvoir participer dans le respect.",
  },
  {
    icon: <Sparkles size={28} />,
    title: "Sobriété du fil",
    color: "#065f46",
    desc: "Refuser la course au clic et à l’outrage permanent. Un fil pensé pour des échanges utiles et humains — pas pour te maintenir accroché·e à tout prix.",
  },
];

const FEATURES = [
  {
    icon: <MessageCircle size={24} />, color: "#009A44",
    title: "Fil d'actualité",
    desc: "Partage textes, photos, vidéos et créations — à ton rythme et dans ta voix.",
  },
  {
    icon: <Users size={24} />, color: "#15803d",
    title: "Communautés",
    desc: "Rejoins ou crée des espaces autour de tes centres d'intérêt, de ta région ou de ta langue.",
  },
  {
    icon: <MessageCircle size={24} />, color: "#22c55e",
    title: "Kongossa",
    desc: "Discussions directes avec tes abonnements et personnes de confiance.",
  },
  {
    icon: <Globe size={24} />, color: "#047857",
    title: "Stories",
    desc: "Moments du quotidien, enracinés dans ta réalité — sans format unique imposé.",
  },
  {
    icon: <Shield size={24} />, color: "#166534",
    title: "Modération",
    desc: "Des règles claires et une équipe attentive pour garder un climat sain.",
  },
  {
    icon: <Smartphone size={24} />, color: "#14532d",
    title: "Android natif",
    desc: "Application optimisée pour les appareils courants et les réseaux du monde réel.",
  },
];

const TESTIMONIALS = [
  { name: "Aminata K.", location: "Abidjan, Côte d'Ivoire",
    text: "J'apprécie qu'on parle enfin d'un réseau qui ne nous demande pas de tout traduire en 'global English' pour exister." },
  { name: "Samuel T.", location: "Bruxelles, Belgique",
    text: "En diaspora, je cherche un lieu où l'Afrique n'est pas qu'un décor : ici, on sent une vraie intention." },
  { name: "Léa M.", location: "Montréal, Canada",
    text: "Simple, sobre, sans pub agressive. Une approche qu'on n'a pas assez sur les grandes apps." },
];
