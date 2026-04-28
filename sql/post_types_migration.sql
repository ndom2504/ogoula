-- ================================================================
-- OGOULA – Types de posts (Sondage, Vote, Pétition, Débat, Concours)
-- Exécuter dans Supabase Dashboard → SQL Editor
-- ================================================================

-- Nouvelles colonnes sur la table posts existante
ALTER TABLE public.posts
    ADD COLUMN IF NOT EXISTS post_type          TEXT    DEFAULT 'classique',
    ADD COLUMN IF NOT EXISTS poll_options       JSONB   DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS poll_vote_counts   JSONB   DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS petition_count     INT     DEFAULT 0,
    ADD COLUMN IF NOT EXISTS poll_option_images JSONB   DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS goal_count         INT     DEFAULT 0,
    ADD COLUMN IF NOT EXISTS deadline_at        BIGINT  DEFAULT NULL;

-- Table votes sondage / vote
CREATE TABLE IF NOT EXISTS public.post_poll_votes (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id      TEXT        NOT NULL,
    user_id      TEXT        NOT NULL,
    option_index INT         NOT NULL CHECK (option_index >= 0),
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

ALTER TABLE public.post_poll_votes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "poll_votes_select" ON public.post_poll_votes;
DROP POLICY IF EXISTS "poll_votes_insert" ON public.post_poll_votes;
DROP POLICY IF EXISTS "poll_votes_update" ON public.post_poll_votes;
DROP POLICY IF EXISTS "poll_votes_delete" ON public.post_poll_votes;

CREATE POLICY "poll_votes_select" ON public.post_poll_votes FOR SELECT USING (true);
CREATE POLICY "poll_votes_insert" ON public.post_poll_votes FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "poll_votes_update" ON public.post_poll_votes FOR UPDATE USING (auth.uid()::text = user_id);
CREATE POLICY "poll_votes_delete" ON public.post_poll_votes FOR DELETE USING (auth.uid()::text = user_id);

-- Table signatures pétition
CREATE TABLE IF NOT EXISTS public.post_petition_signatures (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    TEXT        NOT NULL,
    user_id    TEXT        NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (post_id, user_id)
);

ALTER TABLE public.post_petition_signatures ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "petition_sig_select" ON public.post_petition_signatures;
DROP POLICY IF EXISTS "petition_sig_insert" ON public.post_petition_signatures;
DROP POLICY IF EXISTS "petition_sig_delete" ON public.post_petition_signatures;

CREATE POLICY "petition_sig_select" ON public.post_petition_signatures FOR SELECT USING (true);
CREATE POLICY "petition_sig_insert" ON public.post_petition_signatures FOR INSERT WITH CHECK (auth.uid()::text = user_id);
CREATE POLICY "petition_sig_delete" ON public.post_petition_signatures FOR DELETE USING (auth.uid()::text = user_id);
