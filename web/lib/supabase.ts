import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!;
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!;

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export type Profile = {
  user_id: string;
  first_name: string;
  last_name: string;
  alias: string;
  profile_image_url: string | null;
  banner_image_url: string | null;
  created_at?: string;
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
};
