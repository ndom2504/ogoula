# Ogoula Web

Site web officiel + panneau admin pour la plateforme Ogoula.

## Structure

```
web/
├── app/
│   ├── page.tsx              → Landing page (ogoula.com)
│   ├── admin/
│   │   ├── page.tsx          → Login admin
│   │   └── dashboard/
│   │       └── page.tsx      → Dashboard admin complet
├── lib/
│   └── supabase.ts           → Client Supabase
├── public/
│   └── presentation.mp4      → Vidéo hero (fond sous le header) — servie à l’URL /presentation.mp4
└── .env.local                → Variables d'environnement
```

**Vidéo d’accueil** : place `presentation.mp4` dans `web/public/` (Next.js ne sert pas les médias depuis `app/`). Si tu l’as dans `app/`, copie-la vers `public/presentation.mp4`.

## Installation

```bash
cd web
npm install
npm run dev
```

Ouvre http://localhost:3000

## Admin

- URL : http://localhost:3000/admin (ou https://ogoula.com/admin)
- Connexion avec le compte Supabase ayant email `info@misterdil.ca` ou `@ogoula.com`

## Déploiement

1. **Vercel** (recommandé) : `vercel --prod`
2. **Netlify** : glisse le dossier `web` dans l'interface Netlify
3. Ajoute les variables d'environnement `.env.local` dans le dashboard de déploiement
