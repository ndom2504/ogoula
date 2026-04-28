-- ================================================================
-- OGOULA – Tracker de Promesses
-- Exécuter dans Supabase Dashboard → SQL Editor
-- ================================================================

-- 1. TABLE : leaders
CREATE TABLE IF NOT EXISTS public.leaders (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT        NOT NULL,
  country     TEXT        NOT NULL,
  role        TEXT        NOT NULL,
  photo_url   TEXT,
  bio         TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- 2. TABLE : promises
CREATE TABLE IF NOT EXISTS public.promises (
  id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  leader_id    UUID        NOT NULL REFERENCES public.leaders(id) ON DELETE CASCADE,
  title        TEXT        NOT NULL,
  description  TEXT,
  category     TEXT        DEFAULT 'Général',
  status       TEXT        NOT NULL DEFAULT 'promis'
                           CHECK (status IN ('promis','en_cours','tenu','rompu')),
  source_url   TEXT,
  year         INT,
  votes_kept   INT         DEFAULT 0,
  votes_broken INT         DEFAULT 0,
  created_at   TIMESTAMPTZ DEFAULT NOW(),
  updated_at   TIMESTAMPTZ DEFAULT NOW()
);

-- 3. TABLE : promise_votes (un vote par utilisateur par promesse)
CREATE TABLE IF NOT EXISTS public.promise_votes (
  id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  promise_id  UUID        NOT NULL REFERENCES public.promises(id) ON DELETE CASCADE,
  user_id     UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  vote        TEXT        NOT NULL CHECK (vote IN ('tenu','rompu')),
  created_at  TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (promise_id, user_id)
);

-- 4. RLS
ALTER TABLE public.leaders       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promises      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.promise_votes ENABLE ROW LEVEL SECURITY;

-- Lecture publique
CREATE POLICY "leaders_select"       ON public.leaders       FOR SELECT USING (true);
CREATE POLICY "promises_select"      ON public.promises      FOR SELECT USING (true);
CREATE POLICY "promise_votes_select" ON public.promise_votes FOR SELECT USING (true);

-- Insertion leaders/promesses : utilisateurs connectés (admin à restreindre plus tard)
CREATE POLICY "leaders_insert"  ON public.leaders  FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
CREATE POLICY "promises_insert" ON public.promises FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Votes : chaque utilisateur gère ses propres votes
CREATE POLICY "votes_insert" ON public.promise_votes FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "votes_delete" ON public.promise_votes FOR DELETE USING  (auth.uid() = user_id);

-- 5. Fonction : mise à jour atomique des compteurs de votes
CREATE OR REPLACE FUNCTION public.upsert_promise_vote(
  p_promise_id UUID,
  p_user_id    UUID,
  p_vote       TEXT
) RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
  old_vote TEXT;
BEGIN
  SELECT vote INTO old_vote FROM public.promise_votes
  WHERE promise_id = p_promise_id AND user_id = p_user_id;

  IF old_vote IS NOT NULL THEN
    -- Retirer l'ancien vote
    IF old_vote = 'tenu' THEN
      UPDATE public.promises SET votes_kept   = GREATEST(votes_kept   - 1, 0) WHERE id = p_promise_id;
    ELSE
      UPDATE public.promises SET votes_broken = GREATEST(votes_broken - 1, 0) WHERE id = p_promise_id;
    END IF;
    DELETE FROM public.promise_votes WHERE promise_id = p_promise_id AND user_id = p_user_id;
  END IF;

  IF old_vote IS DISTINCT FROM p_vote THEN
    -- Ajouter le nouveau vote
    INSERT INTO public.promise_votes (promise_id, user_id, vote) VALUES (p_promise_id, p_user_id, p_vote);
    IF p_vote = 'tenu' THEN
      UPDATE public.promises SET votes_kept   = votes_kept   + 1 WHERE id = p_promise_id;
    ELSE
      UPDATE public.promises SET votes_broken = votes_broken + 1 WHERE id = p_promise_id;
    END IF;
  END IF;
END;
$$;

-- 6. Données de démo (optionnel – supprimer en prod)
INSERT INTO public.leaders (name, country, role, bio) VALUES
  ('Paul Biya',         'Cameroun',   'Président',         'Au pouvoir depuis 1982.'),
  ('Félix Tshisekedi',  'RDC',        'Président',         'Élu en 2019, réélu en 2023.'),
  ('Macky Sall',        'Sénégal',    'Président',         'Au pouvoir de 2012 à 2024.'),
  ('Faure Gnassingbé',  'Togo',       'Président',         'Au pouvoir depuis 2005.')
ON CONFLICT DO NOTHING;
