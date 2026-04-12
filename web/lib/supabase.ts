import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!;
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!;

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export type AccountStatus = "active" | "suspended" | "banned";

export type Profile = {
  user_id: string;
  first_name: string;
  last_name: string;
  alias: string;
  profile_image_url: string | null;
  banner_image_url: string | null;
  created_at?: string;
  /** active | suspended | banned — colonnes ajoutées par docs/supabase_profiles_moderation.sql */
  account_status?: AccountStatus | null;
  suspended_until?: string | null;
  moderation_note?: string | null;
};

export type Post = {
  id: string;
  author: string;
  handle: string;
  content: string;
  time: number;
  validates: number;
  loves: number;
  image_urls: string[];
  video_url: string | null;
  author_image_uri: string | null;
  is_community_post: boolean;
  /** Présent si la table expose une colonne JSON/array de commentaires */
  comments?: unknown[] | null;
};
