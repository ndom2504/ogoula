"use client";
import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { supabase } from "@/lib/supabase";
import type { Profile, Post, AccountStatus, CommunityRow, StoryRow } from "@/lib/supabase";
import {
  Users, FileText, Shield, LogOut, Trash2,
  Search, RefreshCw, TrendingUp, AlertTriangle,
  ChevronRight, Eye, Image as ImageIcon, Video, CheckCircle,
  XCircle, BarChart2, Bell,
  Lock as LockIcon, Ban, Settings, PauseCircle, UserRoundCheck,
  Building2, Send, Plus, Library,
} from "lucide-react";
import { OgoulaBrandMark } from "@/components/OgoulaBrandMark";

type Tab = "overview" | "publish" | "communities" | "users" | "posts" | "stories" | "security" | "reports";

type ReportedPost = Post & { reportCount: number; reportReason: string };

export default function AdminDashboard() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("overview");
  const [adminEmail, setAdminEmail] = useState("");

  // Data
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [communities, setCommunities] = useState<CommunityRow[]>([]);
  const [communitiesLoadError, setCommunitiesLoadError] = useState<string | null>(null);
  const [stories, setStories] = useState<StoryRow[]>([]);
  const [storiesLoadError, setStoriesLoadError] = useState<string | null>(null);
  const [adminProfile, setAdminProfile] = useState<Profile | null>(null);
  const [newAdminFirst, setNewAdminFirst] = useState("");
  const [newAdminLast, setNewAdminLast] = useState("");
  const [newAdminAlias, setNewAdminAlias] = useState("");
  const [profileCreateBusy, setProfileCreateBusy] = useState(false);
  const [newCommName, setNewCommName] = useState("");
  const [newCommDesc, setNewCommDesc] = useState("");
  const [newCommCover, setNewCommCover] = useState("");
  const [newCommMembers, setNewCommMembers] = useState("1");
  const [commCreateBusy, setCommCreateBusy] = useState(false);
  const [publishContent, setPublishContent] = useState("");
  const [publishAsCommunity, setPublishAsCommunity] = useState(false);
  const [publishImageUrls, setPublishImageUrls] = useState("");
  const [publishBusy, setPublishBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [actionMsg, setActionMsg] = useState<string | null>(null);
  const [modUi, setModUi] = useState<
    null | { userId: string; alias: string; kind: "suspend" | "ban" }
  >(null);
  const [modNote, setModNote] = useState("");
  const [suspendDays, setSuspendDays] = useState(7);

  // Stats
  const totalUsers = profiles.length;
  const totalPosts = posts.length;
  const totalVideos = posts.filter((p) => p.video_url).length;
  const totalImages = posts.reduce((acc, p) => acc + (p.image_urls?.length ?? 0), 0);
  const communityPosts = posts.filter((p) => p.is_community_post).length;
  const totalStories = stories.length;

  const showMsg = (msg: string) => {
    setActionMsg(msg);
    setTimeout(() => setActionMsg(null), 3000);
  };

  const checkAuth = useCallback(async () => {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) { router.push("/admin"); return; }
    setAdminEmail(user.email ?? "");
  }, [router]);

  const loadProfilesForAdmin = useCallback(async () => {
    const { data: { session } } = await supabase.auth.getSession();
    const token = session?.access_token;
    if (token) {
      try {
        const res = await fetch("/api/admin/profiles", {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.ok) {
          const j = (await res.json()) as { data?: Profile[] };
          if (Array.isArray(j.data)) return j.data;
        }
      } catch {
        /* fallback client */
      }
    }
    const profilesRes = await supabase.from("profiles").select("*").order("first_name");
    if (profilesRes.error) console.error("profiles", profilesRes.error);
    return profilesRes.data ?? [];
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    const [profilesList, postsRes, commRes, storiesRes] = await Promise.all([
      loadProfilesForAdmin(),
      supabase.from("posts").select("*").order("time", { ascending: false }),
      supabase.from("communities").select("*").order("created_at", { ascending: false }),
      supabase.from("stories").select("*").order("created_at", { ascending: false }),
    ]);
    setProfiles(profilesList);
    setPosts(postsRes.data ?? []);
    if (commRes.error) {
      setCommunitiesLoadError(commRes.error.message);
      setCommunities([]);
    } else {
      setCommunitiesLoadError(null);
      setCommunities(commRes.data ?? []);
    }
    if (storiesRes.error) {
      setStoriesLoadError(storiesRes.error.message);
      setStories([]);
    } else {
      setStoriesLoadError(null);
      setStories((storiesRes.data ?? []) as StoryRow[]);
    }
    setLoading(false);
  }, [loadProfilesForAdmin]);

  useEffect(() => { checkAuth(); loadData(); }, [checkAuth, loadData]);

  useEffect(() => {
    void (async () => {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user?.id) return;
      const { data } = await supabase.from("profiles").select("*").eq("user_id", user.id).maybeSingle();
      setAdminProfile(data ?? null);
      if (!data && user.email) {
        const local = user.email.split("@")[0]?.replace(/[^a-zA-Z0-9._]/g, "") || "admin";
        setNewAdminAlias((a) => (a ? a : `@${local}`));
      }
    })();
  }, []);

  async function createAdminProfile() {
    setProfileCreateBusy(true);
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user?.id) {
        showMsg("Session requise.");
        return;
      }
      const fn = newAdminFirst.trim();
      const ln = newAdminLast.trim();
      let alias = newAdminAlias.trim();
      if (!fn || !ln) {
        showMsg("Indique au moins le prénom et le nom.");
        return;
      }
      if (!alias) {
        showMsg("Indique un alias (ex. @admin_ogoula).");
        return;
      }
      if (!alias.startsWith("@")) alias = `@${alias}`;
      const row = {
        user_id: user.id,
        first_name: fn,
        last_name: ln,
        alias,
        profile_image_url: null as string | null,
        banner_image_url: null as string | null,
        account_status: "active" as const,
      };
      const { data, error } = await supabase
        .from("profiles")
        .upsert(row, { onConflict: "user_id" })
        .select("*")
        .maybeSingle();
      if (error) {
        showMsg(`Création profil : ${error.message}`);
        return;
      }
      if (data) setAdminProfile(data);
      await loadData();
      showMsg("Profil créé — tu peux publier ✓");
    } finally {
      setProfileCreateBusy(false);
    }
  }

  async function createCommunityAdmin() {
    setCommCreateBusy(true);
    try {
      const name = newCommName.trim();
      if (!name) {
        showMsg("Nom de la communauté requis.");
        return;
      }
      const mc = Math.max(1, parseInt(newCommMembers, 10) || 1);
      const row = {
        id: globalThis.crypto.randomUUID(),
        name,
        description: newCommDesc.trim(),
        cover_url: newCommCover.trim() || null,
        member_count: mc,
      };
      const { data, error } = await supabase.from("communities").insert(row).select("*").maybeSingle();
      if (error) {
        showMsg(`Communauté : ${error.message}`);
        return;
      }
      if (data) setCommunities((prev) => [data as CommunityRow, ...prev]);
      setNewCommName("");
      setNewCommDesc("");
      setNewCommCover("");
      setNewCommMembers("1");
      setCommunitiesLoadError(null);
      showMsg("Communauté ajoutée ✓");
    } finally {
      setCommCreateBusy(false);
    }
  }

  async function handleLogout() {
    await supabase.auth.signOut();
    router.push("/admin");
  }

  async function suspendStoryRow(s: StoryRow) {
    const note = window.prompt(
      "Motif de suspension (optionnel) — règles / charte :",
      s.moderation_note ?? "",
    );
    if (note === null) return;
    const { error } = await supabase.from("stories").update({
      status: "suspended",
      moderation_note: note.trim() || null,
    }).eq("id", s.id);
    if (error) {
      showMsg(`Erreur : ${error.message}`);
      return;
    }
    setStories((prev) =>
      prev.map((row) =>
        row.id === s.id
          ? { ...row, status: "suspended" as const, moderation_note: note.trim() || null }
          : row,
      ),
    );
    showMsg("Story suspendue — invisible dans l’app ✓");
  }

  async function reactivateStoryRow(id: string) {
    const { error } = await supabase.from("stories").update({
      status: "active",
      moderation_note: null,
    }).eq("id", id);
    if (error) {
      showMsg(`Erreur : ${error.message}`);
      return;
    }
    setStories((prev) =>
      prev.map((row) =>
        row.id === id ? { ...row, status: "active" as const, moderation_note: null } : row,
      ),
    );
    showMsg("Story réactivée ✓");
  }

  async function deleteStoryRow(id: string) {
    if (!confirm("Supprimer définitivement cette story ? (fichier image éventuel reste dans le storage.)")) return;
    const { error } = await supabase.from("stories").delete().eq("id", id);
    if (error) {
      showMsg(`Erreur : ${error.message}`);
      return;
    }
    setStories((prev) => prev.filter((s) => s.id !== id));
    showMsg("Story supprimée ✓");
  }

  async function deletePost(id: string) {
    if (!confirm("Supprimer ce post définitivement ?")) return;
    await supabase.from("posts").delete().eq("id", id);
    setPosts((prev) => prev.filter((p) => p.id !== id));
    showMsg("Post supprimé ✓");
  }

  async function deleteCommunityRow(id: string) {
    if (!confirm("Supprimer cette communauté de la base ? Elle disparaîtra du fil admin et des applis au prochain rafraîchissement.")) return;
    const { error } = await supabase.from("communities").delete().eq("id", id);
    if (error) {
      showMsg(`Erreur : ${error.message}`);
      return;
    }
    setCommunities((prev) => prev.filter((c) => c.id !== id));
    showMsg("Communauté supprimée ✓");
  }

  async function publishAdminPost() {
    setPublishBusy(true);
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user?.id) {
        showMsg("Session requise.");
        return;
      }
      let profile = adminProfile;
      if (!profile) {
        const { data } = await supabase.from("profiles").select("*").eq("user_id", user.id).maybeSingle();
        profile = data ?? null;
        setAdminProfile(profile);
      }
      if (!profile) {
        showMsg("Crée un profil Ogoula pour ce compte admin (inscription app ou table profiles).");
        return;
      }
      const content = publishContent.trim();
      if (!content) {
        showMsg("Écris un message pour le fil.");
        return;
      }
      const image_urls = publishImageUrls.split(",").map((s) => s.trim()).filter(Boolean);
      const row = {
        id: globalThis.crypto.randomUUID(),
        author: `${profile.first_name} ${profile.last_name}`.trim(),
        handle: profile.alias,
        content,
        time: Date.now(),
        validates: 0,
        loves: 0,
        image_urls,
        video_url: null as string | null,
        author_image_uri: profile.profile_image_url,
        is_community_post: publishAsCommunity,
      };
      const { error } = await supabase.from("posts").insert(row);
      if (error) {
        showMsg(`Erreur publication : ${error.message}`);
        return;
      }
      setPublishContent("");
      setPublishImageUrls("");
      setPublishAsCommunity(false);
      await loadData();
      showMsg("Publication envoyée dans le fil ✓");
    } finally {
      setPublishBusy(false);
    }
  }

  async function deleteUser(userId: string) {
    if (
      !confirm(
        "Supprimer la ligne profil (données Ogoula) ? Le compte de connexion Supabase Auth reste : pour libérer l’e-mail, supprime aussi l’utilisateur dans le tableau Supabase → Authentication → Users.",
      )
    )
      return;
    await supabase.from("profiles").delete().eq("user_id", userId);
    setProfiles((prev) => prev.filter((p) => p.user_id !== userId));
    showMsg("Profil supprimé ✓");
  }

  async function deleteUserPosts(userHandle: string) {
    if (!confirm(`Supprimer tous les posts de ${userHandle} ?`)) return;
    await supabase.from("posts").delete().eq("handle", userHandle);
    setPosts((prev) => prev.filter((p) => p.handle !== userHandle));
    showMsg(`Posts de ${userHandle} supprimés ✓`);
  }

  async function applyProfileModeration(
    userId: string,
    patch: Record<string, unknown>,
  ): Promise<boolean> {
    const { error } = await supabase.from("profiles").update(patch).eq("user_id", userId);
    if (error) {
      showMsg(`Erreur : ${error.message}`);
      return false;
    }
    await loadData();
    showMsg("Compte mis à jour ✓");
    return true;
  }

  async function liftSanction(userId: string) {
    if (!confirm("Lever la suspension ou le bannissement pour ce compte ?")) return;
    await applyProfileModeration(userId, {
      account_status: "active",
      suspended_until: null,
      moderation_note: null,
    });
  }

  async function submitModerationDialog() {
    if (!modUi) return;
    if (modUi.kind === "ban" && !modNote.trim()) {
      showMsg("Indique un motif pour le bannissement.");
      return;
    }
    if (modUi.kind === "suspend") {
      const until = new Date();
      until.setDate(until.getDate() + suspendDays);
      const ok = await applyProfileModeration(modUi.userId, {
        account_status: "suspended",
        suspended_until: until.toISOString(),
        moderation_note: modNote.trim() || null,
      });
      if (ok) {
        setModUi(null);
        setModNote("");
      }
    } else {
      const ok = await applyProfileModeration(modUi.userId, {
        account_status: "banned",
        suspended_until: null,
        moderation_note: modNote.trim(),
      });
      if (ok) {
        setModUi(null);
        setModNote("");
      }
    }
  }

  const filteredUsers = profiles.filter(
    (p) =>
      `${p.first_name} ${p.last_name} ${p.alias}`.toLowerCase().includes(search.toLowerCase())
  );
  const filteredPosts = posts.filter(
    (p) =>
      (p.content + p.author + p.handle).toLowerCase().includes(search.toLowerCase())
  );

  const filteredCommunities = communities.filter((c) =>
    `${c.name} ${c.description}`.toLowerCase().includes(search.toLowerCase())
  );

  const filteredStories = stories.filter((s) =>
    `${s.author_display} ${s.content_text ?? ""} ${s.user_id} ${s.status}`
      .toLowerCase()
      .includes(search.toLowerCase()),
  );

  const NAV: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: "overview", label: "Vue d'ensemble", icon: <BarChart2 size={18} /> },
    { id: "publish", label: "Publier", icon: <Send size={18} /> },
    { id: "communities", label: "Communautés", icon: <Building2 size={18} /> },
    { id: "users", label: "Utilisateurs", icon: <Users size={18} /> },
    { id: "posts", label: "Publications", icon: <FileText size={18} /> },
    { id: "stories", label: "Stories", icon: <Library size={18} /> },
    { id: "security", label: "Sécurité", icon: <Shield size={18} /> },
    { id: "reports", label: "Signalements", icon: <AlertTriangle size={18} /> },
  ];

  return (
    <div className="min-h-screen bg-gray-50 flex">

      {/* ── SIDEBAR ─────────────────────────────────────────────── */}
      <aside className="w-64 bg-gray-900 text-white flex flex-col fixed h-full z-10">
        <div className="p-6 border-b border-gray-800">
          <div className="flex items-center gap-3">
            <OgoulaBrandMark size="lg" />
            <div>
              <p className="font-black text-lg leading-none">Ogoula</p>
              <p className="text-gray-400 text-xs">Admin Panel</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1">
          {NAV.map((item) => (
            <button
              key={item.id}
              onClick={() => setTab(item.id)}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all ${
                tab === item.id
                  ? "bg-[#009A44] text-white"
                  : "text-gray-400 hover:bg-gray-800 hover:text-white"
              }`}
            >
              {item.icon}
              {item.label}
              {tab === item.id && <ChevronRight size={14} className="ml-auto" />}
            </button>
          ))}
        </nav>

        <div className="p-4 border-t border-gray-800">
          <div className="flex items-center gap-3 px-4 py-3 mb-2">
            <div className="w-8 h-8 rounded-full bg-[#009A44] flex items-center justify-center text-xs font-bold">
              {adminEmail[0]?.toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold truncate">{adminEmail}</p>
              <p className="text-gray-500 text-xs">Administrateur</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-2 px-4 py-2 text-gray-400 hover:text-red-400 text-sm transition rounded-xl hover:bg-gray-800"
          >
            <LogOut size={16} />
            Déconnexion
          </button>
        </div>
      </aside>

      {/* ── MAIN ────────────────────────────────────────────────── */}
      <main className="ml-64 flex-1 p-8">

        {/* Toast */}
        {actionMsg && (
          <div className="fixed top-6 right-6 z-50 bg-[#009A44] text-white px-5 py-3 rounded-xl shadow-lg font-semibold text-sm flex items-center gap-2">
            <CheckCircle size={16} /> {actionMsg}
          </div>
        )}

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-black text-gray-900">
              {NAV.find((n) => n.id === tab)?.label}
            </h1>
            <p className="text-gray-500 text-sm mt-1">
              Plateforme Ogoula · {new Date().toLocaleDateString("fr-FR", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={loadData}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-xl text-sm font-medium hover:bg-gray-50 transition"
            >
              <RefreshCw size={15} className={loading ? "animate-spin" : ""} />
              Actualiser
            </button>
            <button className="w-9 h-9 bg-white border border-gray-200 rounded-xl flex items-center justify-center hover:bg-gray-50 transition">
              <Bell size={16} className="text-gray-600" />
            </button>
          </div>
        </div>

        {/* ── OVERVIEW TAB ────────────────────────────────────────── */}
        {tab === "overview" && (
          <div className="space-y-6">
            {/* Stats grid */}
            <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
              {[
                { label: "Utilisateurs", value: totalUsers, icon: <Users size={20} />, color: "#009A44", bg: "#009A44" },
                { label: "Publications", value: totalPosts, icon: <FileText size={20} />, color: "#003DA5", bg: "#003DA5" },
                { label: "Stories", value: totalStories, icon: <Library size={20} />, color: "#a21caf", bg: "#a21caf" },
                { label: "Vidéos", value: totalVideos, icon: <Video size={20} />, color: "#7C3AED", bg: "#7C3AED" },
                { label: "Images", value: totalImages, icon: <ImageIcon size={20} />, color: "#D97706", bg: "#D97706" },
              ].map((stat, i) => (
                <div key={i} className="bg-white rounded-2xl p-5 border border-gray-100 shadow-sm">
                  <div className="flex items-center justify-between mb-3">
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: stat.bg + "20", color: stat.color }}>
                      {stat.icon}
                    </div>
                    <TrendingUp size={14} className="text-green-400" />
                  </div>
                  <p className="text-3xl font-black text-gray-900">{stat.value}</p>
                  <p className="text-gray-500 text-sm mt-1">{stat.label}</p>
                </div>
              ))}
            </div>

            {/* Recent activity */}
            <div className="grid md:grid-cols-2 gap-6">
              <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <h3 className="font-bold text-gray-900 mb-4 flex items-center gap-2"><Users size={16} className="text-[#009A44]" /> Derniers inscrits</h3>
                <div className="space-y-3">
                  {profiles.slice(0, 5).map((p) => (
                    <div key={p.user_id} className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-[#009A44]/10 text-[#009A44] flex items-center justify-center text-sm font-bold">
                        {p.first_name[0] ?? "?"}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold truncate">{p.first_name} {p.last_name}</p>
                        <p className="text-xs text-gray-400">{p.alias}</p>
                      </div>
                    </div>
                  ))}
                  {profiles.length === 0 && <p className="text-gray-400 text-sm">Aucun profil enregistré.</p>}
                </div>
              </div>

              <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <h3 className="font-bold text-gray-900 mb-4 flex items-center gap-2"><FileText size={16} className="text-[#003DA5]" /> Dernières publications</h3>
                <div className="space-y-3">
                  {posts.slice(0, 5).map((p) => (
                    <div key={p.id} className="flex items-start gap-3">
                      <div className="w-9 h-9 rounded-full bg-[#003DA5]/10 text-[#003DA5] flex items-center justify-center text-sm font-bold shrink-0">
                        {p.author[0]}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-semibold">{p.author} <span className="text-gray-400 font-normal">{p.handle}</span></p>
                        <p className="text-xs text-gray-500 truncate">{p.content}</p>
                      </div>
                      {p.video_url && <Video size={14} className="text-purple-400 shrink-0 mt-1" />}
                    </div>
                  ))}
                  {posts.length === 0 && <p className="text-gray-400 text-sm">Aucun post enregistré.</p>}
                </div>
              </div>
            </div>

            {/* Quick stats */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
              <h3 className="font-bold text-gray-900 mb-4">Répartition du contenu</h3>
              <div className="grid grid-cols-3 gap-6">
                {[
                  { label: "Posts texte", value: posts.filter((p) => !p.video_url && (!p.image_urls || p.image_urls.length === 0)).length, color: "#009A44" },
                  { label: "Posts avec image", value: posts.filter((p) => p.image_urls && p.image_urls.length > 0 && !p.video_url).length, color: "#FCD116" },
                  { label: "Posts vidéo", value: totalVideos, color: "#003DA5" },
                  { label: "Posts communauté", value: communityPosts, color: "#7C3AED" },
                  { label: "Communautés (Supabase)", value: communities.length, color: "#15803d" },
                  { label: "Stories actives", value: stories.filter((st) => st.status === "active").length, color: "#a21caf" },
                  { label: "Stories suspendues", value: stories.filter((st) => st.status === "suspended").length, color: "#c2410c" },
                  { label: "Réactions totales", value: posts.reduce((a, p) => a + p.validates + p.loves, 0), color: "#EF4444" },
                  { label: "Commentaires", value: posts.reduce((a, p) => a + (p.comments?.length ?? 0), 0), color: "#F59E0B" },
                ].map((s, i) => (
                  <div key={i} className="text-center">
                    <p className="text-2xl font-black" style={{ color: s.color }}>{s.value}</p>
                    <p className="text-gray-500 text-xs mt-1">{s.label}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* ── PUBLISH TAB ─────────────────────────────────────────── */}
        {tab === "publish" && (
          <div className="max-w-2xl space-y-5">
            <div className="bg-amber-50 border border-amber-100 rounded-xl px-4 py-3 text-sm text-amber-900">
              Le post apparaît dans le fil avec ton <strong>profil admin</strong> (nom + alias). Coche « Post communauté » pour le style mis en avant comme dans l’app.
            </div>
            {adminProfile ? (
              <p className="text-sm text-gray-600">
                Connecté en tant que <span className="font-mono font-semibold">{adminProfile.alias}</span>
                {" · "}{adminProfile.first_name} {adminProfile.last_name}
              </p>
            ) : (
              <div className="rounded-xl border border-amber-200 bg-amber-50/90 p-4 space-y-3 text-sm text-amber-950">
                <p>
                  Aucune ligne dans <code className="rounded bg-white px-1">profiles</code> pour ton compte admin
                  (le <code className="rounded bg-white px-1">user_id</code> Auth doit correspondre à{" "}
                  <code className="rounded bg-white px-1">profiles.user_id</code>). Les communautés créées uniquement sur le téléphone
                  restent en local tant que l’app ne les envoie pas à Supabase.
                </p>
                <p className="text-xs text-amber-900/90">
                  Si le bouton ci-dessous échoue (RLS), exécute{" "}
                  <code className="rounded bg-white px-1">docs/supabase_profiles_self_insert.sql</code> dans Supabase.
                </p>
                <div className="grid gap-2 sm:grid-cols-3">
                  <input
                    value={newAdminFirst}
                    onChange={(e) => setNewAdminFirst(e.target.value)}
                    placeholder="Prénom"
                    className="rounded-lg border border-amber-200/80 bg-white px-3 py-2 text-sm"
                  />
                  <input
                    value={newAdminLast}
                    onChange={(e) => setNewAdminLast(e.target.value)}
                    placeholder="Nom"
                    className="rounded-lg border border-amber-200/80 bg-white px-3 py-2 text-sm"
                  />
                  <input
                    value={newAdminAlias}
                    onChange={(e) => setNewAdminAlias(e.target.value)}
                    placeholder="@ton_alias"
                    className="rounded-lg border border-amber-200/80 bg-white px-3 py-2 text-sm sm:col-span-3"
                  />
                </div>
                <button
                  type="button"
                  disabled={profileCreateBusy}
                  onClick={() => void createAdminProfile()}
                  className="inline-flex items-center gap-2 rounded-xl bg-[#009A44] px-4 py-2.5 text-sm font-bold text-white hover:bg-[#007a36] disabled:opacity-50"
                >
                  <Plus size={18} />
                  {profileCreateBusy ? "Création…" : "Créer mon profil admin"}
                </button>
              </div>
            )}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-4">
              <label className="block text-xs font-semibold text-gray-500 uppercase">Message</label>
              <textarea
                value={publishContent}
                onChange={(e) => setPublishContent(e.target.value)}
                rows={6}
                placeholder="Annonce, actualité, message à la communauté…"
                className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#009A44]"
              />
              <label className="flex items-center gap-2 cursor-pointer text-sm text-gray-700">
                <input
                  type="checkbox"
                  checked={publishAsCommunity}
                  onChange={(e) => setPublishAsCommunity(e.target.checked)}
                  className="rounded border-gray-300 text-[#009A44] focus:ring-[#009A44]"
                />
                Publier comme post communauté (badge vert côté app)
              </label>
              <div>
                <label className="block text-xs font-semibold text-gray-500 uppercase mb-1">Images (optionnel)</label>
                <input
                  value={publishImageUrls}
                  onChange={(e) => setPublishImageUrls(e.target.value)}
                  placeholder="URLs séparées par des virgules (https://…)"
                  className="w-full border border-gray-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009A44]"
                />
              </div>
              <button
                type="button"
                disabled={publishBusy}
                onClick={() => void publishAdminPost()}
                className="inline-flex items-center gap-2 rounded-xl bg-[#009A44] px-6 py-3 text-sm font-bold text-white hover:bg-[#007a36] disabled:opacity-50"
              >
                <Send size={18} />
                {publishBusy ? "Publication…" : "Publier dans le fil"}
              </button>
            </div>
          </div>
        )}

        {/* ── COMMUNITIES TAB ─────────────────────────────────────── */}
        {tab === "communities" && (
          <div className="space-y-5">
            {communitiesLoadError && (
              <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-900">
                <p className="font-semibold">Impossible de charger les communautés</p>
                <p className="mt-1 font-mono text-xs">{communitiesLoadError}</p>
                <p className="mt-2 text-xs">
                  Souvent : table absente → exécute{" "}
                  <code className="rounded bg-white px-1">docs/supabase_communities.sql</code>, ou politique RLS à ajuster.
                </p>
              </div>
            )}
            <p className="text-gray-600 text-sm bg-blue-50 border border-blue-100 rounded-xl px-4 py-3">
              <strong>Pourquoi ta communauté du téléphone n’apparaît pas ?</strong> Tant que la table{" "}
              <code className="rounded bg-white px-1">communities</code> n’existe pas côté Supabase, ou que l’app Android n’a pas la synchro
              (version avec envoi vers Supabase), les groupes restent <strong>uniquement sur l’appareil</strong>. Tu peux les recréer ici
              manuellement (même nom / description) pour les voir dans l’admin et dans le fil après synchro des clients.
            </p>
            <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm space-y-3">
              <h3 className="font-bold text-gray-900 flex items-center gap-2 text-sm">
                <Plus size={18} className="text-[#009A44]" /> Ajouter une communauté (Supabase)
              </h3>
              <input
                value={newCommName}
                onChange={(e) => setNewCommName(e.target.value)}
                placeholder="Nom *"
                className="w-full rounded-xl border border-gray-200 px-3 py-2 text-sm"
              />
              <textarea
                value={newCommDesc}
                onChange={(e) => setNewCommDesc(e.target.value)}
                placeholder="Description"
                rows={2}
                className="w-full rounded-xl border border-gray-200 px-3 py-2 text-sm"
              />
              <input
                value={newCommCover}
                onChange={(e) => setNewCommCover(e.target.value)}
                placeholder="URL image de couverture (optionnel)"
                className="w-full rounded-xl border border-gray-200 px-3 py-2 text-sm"
              />
              <input
                value={newCommMembers}
                onChange={(e) => setNewCommMembers(e.target.value)}
                placeholder="Nombre de membres (ex. 1)"
                className="w-full max-w-xs rounded-xl border border-gray-200 px-3 py-2 text-sm"
                type="number"
                min={1}
              />
              <button
                type="button"
                disabled={commCreateBusy}
                onClick={() => void createCommunityAdmin()}
                className="inline-flex items-center gap-2 rounded-xl bg-[#009A44] px-4 py-2.5 text-sm font-bold text-white hover:bg-[#007a36] disabled:opacity-50"
              >
                {commCreateBusy ? "Enregistrement…" : "Enregistrer en base"}
              </button>
            </div>
            <SearchBar value={search} onChange={setSearch} placeholder="Rechercher une communauté…" />
            <div className="space-y-3">
              {filteredCommunities.map((c) => (
                <div
                  key={c.id}
                  className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex gap-4 flex-wrap md:flex-nowrap"
                >
                  <div className="w-14 h-14 rounded-xl bg-[#009A44]/10 shrink-0 overflow-hidden flex items-center justify-center text-[#009A44] font-bold">
                    {c.cover_url ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={c.cover_url} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <Building2 size={24} />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-gray-900">{c.name}</p>
                    <p className="text-sm text-gray-500 mt-1 line-clamp-2">{c.description}</p>
                    <p className="text-xs text-gray-400 mt-2">
                      {c.member_count} membre(s) · id <span className="font-mono">{c.id}</span>
                      {c.created_at && ` · ${new Date(c.created_at).toLocaleString("fr-FR")}`}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => void deleteCommunityRow(c.id)}
                    className="self-start p-2 text-red-500 hover:bg-red-50 rounded-lg transition"
                    title="Supprimer"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              ))}
              {filteredCommunities.length === 0 && (
                <div className="text-center py-12 text-gray-400 bg-white rounded-2xl border border-gray-100">
                  {communities.length === 0
                    ? "Aucune communauté en base pour l’instant. Utilise le formulaire ci-dessus, ou exécute docs/supabase_communities.sql puis synchronise depuis l’app."
                    : "Aucun résultat pour cette recherche."}
                </div>
              )}
            </div>
            <p className="text-gray-400 text-xs">{filteredCommunities.length} sur {communities.length} communauté(s)</p>
          </div>
        )}

        {/* ── USERS TAB ───────────────────────────────────────────── */}
        {tab === "users" && (
          <div className="space-y-5">
            <SearchBar value={search} onChange={setSearch} placeholder="Rechercher un utilisateur…" />
            <p className="text-gray-600 text-xs bg-sky-50 border border-sky-100 rounded-xl px-4 py-3">
              Après suppression ici, l’e-mail peut encore être « déjà utilisé » à l’inscription : ce panneau enlève surtout la ligne{" "}
              <code className="bg-white px-1 rounded">profiles</code>, pas l’entrée dans{" "}
              <strong>Supabase → Authentication → Users</strong>. Ouvre ce tableau pour supprimer définitivement le compte de connexion.
            </p>
            <p className="text-gray-500 text-xs bg-amber-50 border border-amber-100 rounded-xl px-4 py-3">
              Modération : exécute d&apos;abord le script SQL{" "}
              <code className="bg-white px-1 rounded">docs/supabase_profiles_moderation.sql</code> dans Supabase.
              Les mises à jour nécessitent une politique RLS autorisant les admins à modifier{" "}
              <code className="bg-white px-1 rounded">profiles</code> (ou une clé service côté serveur).
            </p>
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-x-auto">
              <table className="w-full text-sm min-w-[720px]">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    {["Utilisateur", "Alias", "Statut", "Publications", "Actions"].map((h) => (
                      <th key={h} className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {filteredUsers.map((p) => {
                    const userPostCount = posts.filter((po) => po.handle === p.alias).length;
                    const st = (p.account_status ?? "active") as AccountStatus;
                    const canLift = st === "suspended" || st === "banned";
                    return (
                      <tr key={p.user_id} className="hover:bg-gray-50 transition">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            {p.profile_image_url ? (
                              // eslint-disable-next-line @next/next/no-img-element
                              <img src={p.profile_image_url} alt="" className="w-9 h-9 rounded-full object-cover" />
                            ) : (
                              <div className="w-9 h-9 rounded-full bg-[#009A44]/15 text-[#009A44] flex items-center justify-center text-sm font-bold">
                                {p.first_name[0] ?? "?"}
                              </div>
                            )}
                            <div>
                              <p className="font-semibold text-gray-900">{p.first_name} {p.last_name}</p>
                              <p className="text-gray-400 text-xs truncate max-w-[120px]">{p.user_id}</p>
                              {p.moderation_note && (
                                <p className="text-amber-700 text-[11px] mt-0.5 max-w-[220px] truncate" title={p.moderation_note}>
                                  Motif : {p.moderation_note}
                                </p>
                              )}
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-gray-600 font-mono text-xs align-top">{p.alias}</td>
                        <td className="px-6 py-4 align-top">
                          <ProfileStatusBadge profile={p} />
                        </td>
                        <td className="px-6 py-4 align-top">
                          <span className="bg-blue-50 text-blue-700 text-xs font-semibold px-2 py-1 rounded-full">{userPostCount}</span>
                        </td>
                        <td className="px-6 py-4 align-top">
                          <div className="flex flex-wrap items-center gap-1">
                            <button
                              type="button"
                              onClick={() => {
                                setSuspendDays(7);
                                setModNote("");
                                setModUi({ userId: p.user_id, alias: p.alias, kind: "suspend" });
                              }}
                              title="Suspendre temporairement (violation des règles)"
                              className="p-2 text-amber-600 hover:bg-amber-50 rounded-lg transition"
                            >
                              <PauseCircle size={16} />
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                setModNote("");
                                setModUi({ userId: p.user_id, alias: p.alias, kind: "ban" });
                              }}
                              title="Bannir définitivement"
                              className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition"
                            >
                              <Ban size={16} />
                            </button>
                            {canLift && (
                              <button
                                type="button"
                                onClick={() => liftSanction(p.user_id)}
                                title="Lever la sanction"
                                className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                              >
                                <UserRoundCheck size={16} />
                              </button>
                            )}
                            <button
                              type="button"
                              onClick={() => deleteUserPosts(p.alias)}
                              title="Supprimer tous les posts"
                              className="p-2 text-orange-500 hover:bg-orange-50 rounded-lg transition"
                            >
                              <FileText size={15} />
                            </button>
                            <button
                              type="button"
                              onClick={() => deleteUser(p.user_id)}
                              title="Supprimer le profil (données Ogoula)"
                              className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition"
                            >
                              <Trash2 size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                  {filteredUsers.length === 0 && (
                    <tr><td colSpan={5} className="px-6 py-10 text-center text-gray-400">Aucun utilisateur trouvé.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <p className="text-gray-400 text-xs">{filteredUsers.length} utilisateur(s) affiché(s) sur {totalUsers}</p>
          </div>
        )}

        {/* ── POSTS TAB ───────────────────────────────────────────── */}
        {tab === "posts" && (
          <div className="space-y-5">
            <SearchBar value={search} onChange={setSearch} placeholder="Rechercher dans les posts…" />
            <div className="space-y-3">
              {filteredPosts.map((p) => (
                <div key={p.id} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex gap-4 hover:shadow-md transition">
                  <div className="w-10 h-10 rounded-full bg-[#009A44]/15 text-[#009A44] flex items-center justify-center font-bold text-sm shrink-0">
                    {p.author[0]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-semibold text-sm">{p.author}</span>
                      <span className="text-gray-400 text-xs font-mono">{p.handle}</span>
                      {p.is_community_post && (
                        <span className="bg-green-100 text-green-700 text-xs px-2 py-0.5 rounded-full font-semibold">Communauté</span>
                      )}
                      {p.video_url && <Video size={13} className="text-purple-400" />}
                      {p.image_urls?.length > 0 && <ImageIcon size={13} className="text-blue-400" />}
                    </div>
                    <p className="text-gray-700 text-sm line-clamp-2">{p.content}</p>
                    <div className="flex items-center gap-4 mt-2 text-xs text-gray-400">
                      <span>👍 {p.validates}</span>
                      <span>❤️ {p.loves}</span>
                      <span>{new Date(p.time).toLocaleString("fr-FR")}</span>
                    </div>
                  </div>
                  <div className="flex items-start gap-2 shrink-0">
                    {p.video_url && (
                      <a href={p.video_url} target="_blank" rel="noreferrer"
                        className="p-2 text-blue-500 hover:bg-blue-50 rounded-lg transition">
                        <Eye size={15} />
                      </a>
                    )}
                    <button
                      onClick={() => deletePost(p.id)}
                      className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              ))}
              {filteredPosts.length === 0 && (
                <div className="text-center py-12 text-gray-400">Aucune publication trouvée.</div>
              )}
            </div>
            <p className="text-gray-400 text-xs">{filteredPosts.length} post(s) affiché(s) sur {totalPosts}</p>
          </div>
        )}

        {/* ── STORIES TAB (Au Quartier) ───────────────────────────── */}
        {tab === "stories" && (
          <div className="space-y-5">
            {storiesLoadError && (
              <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950">
                <p className="font-semibold">Stories non chargées</p>
                <p className="mt-1 font-mono text-xs">{storiesLoadError}</p>
                <p className="mt-2 text-xs">
                  Exécute{" "}
                  <code className="rounded bg-white px-1">docs/supabase_stories.sql</code> dans Supabase si la table n’existe pas encore.
                </p>
              </div>
            )}
            <p className="text-gray-600 text-sm bg-emerald-50 border border-emerald-100 rounded-xl px-4 py-3">
              <strong>Suspendre</strong> : la story disparaît du carrousel « Au Quartier » dans l’app (filtrage{" "}
              <code className="rounded bg-white px-1">status = active</code>).{" "}
              <strong>Réactiver</strong> la rend visible à nouveau. <strong>Supprimer</strong> enlève la ligne en base (l’image reste dans le storage si tu ne la purges pas à la main).
            </p>
            <SearchBar value={search} onChange={setSearch} placeholder="Rechercher une story (auteur, texte, user id)…" />
            <div className="space-y-3">
              {filteredStories.map((s) => (
                <div
                  key={s.id}
                  className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex gap-4 flex-wrap md:flex-nowrap"
                >
                  <div className="w-16 h-24 rounded-lg bg-gray-100 overflow-hidden shrink-0 border border-gray-200">
                    {s.image_url ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={s.image_url} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-[10px] text-gray-500 p-1 text-center leading-tight">
                        {s.content_text?.slice(0, 80) || "—"}
                      </div>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2 mb-1">
                      <span className="font-semibold text-sm">{s.author_display}</span>
                      {s.status === "suspended" ? (
                        <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-900">
                          Suspendue
                        </span>
                      ) : (
                        <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800">
                          Active
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-gray-400 font-mono truncate">user_id {s.user_id}</p>
                    <p className="text-gray-700 text-sm line-clamp-2 mt-1">{s.content_text || "· image seule ·"}</p>
                    {s.moderation_note && (
                      <p className="text-amber-800 text-xs mt-1">Motif : {s.moderation_note}</p>
                    )}
                    <p className="text-xs text-gray-400 mt-2">
                      id <span className="font-mono">{s.id}</span>
                      {s.created_at && ` · ${new Date(s.created_at).toLocaleString("fr-FR")}`}
                    </p>
                  </div>
                  <div className="flex items-start gap-2 shrink-0">
                    {s.status === "active" ? (
                      <button
                        type="button"
                        title="Suspendre (charte / règles)"
                        onClick={() => void suspendStoryRow(s)}
                        className="p-2 text-amber-600 hover:bg-amber-50 rounded-lg transition"
                      >
                        <PauseCircle size={16} />
                      </button>
                    ) : (
                      <button
                        type="button"
                        title="Réactiver"
                        onClick={() => void reactivateStoryRow(s.id)}
                        className="p-2 text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                      >
                        <UserRoundCheck size={16} />
                      </button>
                    )}
                    <button
                      type="button"
                      title="Supprimer définitivement"
                      onClick={() => void deleteStoryRow(s.id)}
                      className="p-2 text-red-500 hover:bg-red-50 rounded-lg transition"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
              {filteredStories.length === 0 && !storiesLoadError && (
                <div className="text-center py-12 text-gray-400 bg-white rounded-2xl border border-gray-100">
                  {stories.length === 0 ? "Aucune story en base." : "Aucun résultat pour cette recherche."}
                </div>
              )}
            </div>
            <p className="text-gray-400 text-xs">
              {filteredStories.length} affichée(s) sur {totalStories}
            </p>
          </div>
        )}

        {/* ── SECURITY TAB ────────────────────────────────────────── */}
        {tab === "security" && (
          <div className="space-y-5">
            <div className="grid md:grid-cols-2 gap-5">
              <SecurityCard
                icon={<Shield size={20} />} color="#009A44"
                title="Authentification Supabase"
                desc="Tous les utilisateurs sont authentifiés via Supabase Auth (email + mot de passe). Les tokens JWT expirent automatiquement."
                status="Actif"
                ok
              />
              <SecurityCard
                icon={<LockIcon size={20} />} color="#003DA5"
                title="Row Level Security (RLS)"
                desc="Les politiques Supabase garantissent que chaque utilisateur ne peut accéder qu'à ses propres données."
                status="À configurer"
                ok={false}
              />
              <SecurityCard
                icon={<Ban size={20} />} color="#EF4444"
                title="Modération des contenus"
                desc="Suppression manuelle des posts signalés depuis ce panneau admin. Modération automatique à venir."
                status="Manuel"
                ok
              />
              <SecurityCard
                icon={<Settings size={20} />} color="#7C3AED"
                title="Accès Admin"
                desc="L'accès admin est restreint aux comptes email @ogoula.com ou info@misterdil.ca via vérification du profil."
                status="Actif"
                ok
              />
            </div>

            {/* RLS instructions */}
            <div className="bg-amber-50 border border-amber-200 rounded-2xl p-6">
              <h3 className="font-bold text-amber-800 mb-2 flex items-center gap-2">
                <AlertTriangle size={18} /> Action recommandée : Activer le RLS sur Supabase
              </h3>
              <p className="text-amber-700 text-sm mb-4">
                Pour maximiser la sécurité, active le Row Level Security sur tes tables dans Supabase Dashboard.
              </p>
              <div className="bg-gray-900 text-green-400 rounded-xl p-4 text-xs font-mono space-y-1">
                <p>-- Dans Supabase SQL Editor :</p>
                <p>ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;</p>
                <p>ALTER TABLE posts ENABLE ROW LEVEL SECURITY;</p>
                <p className="text-gray-500">-- Politique : chaque user voit son propre profil</p>
                <p>{"CREATE POLICY \"own profile\" ON profiles FOR ALL"}</p>
                <p>{"  USING (auth.uid()::text = user_id);"}</p>
                <p className="text-gray-500">-- Politique : posts visibles de tous (lecture)</p>
                <p>{"CREATE POLICY \"posts public read\" ON posts FOR SELECT"}</p>
                <p>{"  TO public USING (true);"}</p>
              </div>
            </div>
          </div>
        )}

        {/* ── REPORTS TAB ─────────────────────────────────────────── */}
        {tab === "reports" && (
          <div className="space-y-5">
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
              <div className="text-center py-10">
                <AlertTriangle size={48} className="text-gray-300 mx-auto mb-4" />
                <h3 className="text-gray-700 font-bold text-lg mb-2">Système de signalement</h3>
                <p className="text-gray-400 text-sm max-w-md mx-auto">
                  {"Les signalements in-app seront affichés ici. Pour l'activer, ajoute une table "}
                  <code className="bg-gray-100 px-1 rounded">reports</code>
                  {" dans Supabase avec les colonnes : "}
                  <code className="bg-gray-100 px-1 rounded">id, post_id, reporter_id, reason, created_at</code>.
                </p>
                <div className="mt-6 bg-gray-900 text-green-400 rounded-xl p-4 text-xs font-mono text-left max-w-md mx-auto">
                  <p>CREATE TABLE reports (</p>
                  <p>{"  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),"}</p>
                  <p>{"  post_id TEXT REFERENCES posts(id),"}</p>
                  <p>{"  reporter_id TEXT,"}</p>
                  <p>{"  reason TEXT,"}</p>
                  <p>{"  created_at TIMESTAMPTZ DEFAULT NOW()"}</p>
                  <p>{");"}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {modUi && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-4">
            <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 space-y-4">
              <h3 className="font-bold text-gray-900 text-lg">
                {modUi.kind === "suspend" ? "Suspension temporaire" : "Bannissement"}
                <span className="text-gray-500 font-normal text-sm block mt-1">{modUi.alias}</span>
              </h3>
              <p className="text-gray-600 text-sm">
                {modUi.kind === "suspend"
                  ? "Le compte ne pourra plus se connecter jusqu’à la date choisie. Explique brièvement le motif (respect de la communauté)."
                  : "Exclusion définitive pour violation grave de la charte. Le motif sera visible côté utilisateur à la connexion."}
              </p>
              {modUi.kind === "suspend" && (
                <div>
                  <label className="text-xs font-semibold text-gray-500 uppercase">Durée</label>
                  <select
                    value={suspendDays}
                    onChange={(e) => setSuspendDays(Number(e.target.value))}
                    className="mt-1 w-full border border-gray-200 rounded-xl px-3 py-2 text-sm"
                  >
                    <option value={1}>1 jour</option>
                    <option value={3}>3 jours</option>
                    <option value={7}>7 jours</option>
                    <option value={30}>30 jours</option>
                    <option value={90}>90 jours</option>
                  </select>
                </div>
              )}
              <div>
                <label className="text-xs font-semibold text-gray-500 uppercase">
                  {modUi.kind === "ban" ? "Motif (obligatoire)" : "Motif (optionnel)"}
                </label>
                <textarea
                  value={modNote}
                  onChange={(e) => setModNote(e.target.value)}
                  rows={3}
                  className="mt-1 w-full border border-gray-200 rounded-xl px-3 py-2 text-sm"
                  placeholder="Ex. contenu haineux, harcèlement, spam répété…"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => { setModUi(null); setModNote(""); }}
                  className="px-4 py-2 rounded-xl text-sm font-medium text-gray-600 hover:bg-gray-100"
                >
                  Annuler
                </button>
                <button
                  type="button"
                  onClick={() => void submitModerationDialog()}
                  className="px-4 py-2 rounded-xl text-sm font-bold text-white bg-[#009A44] hover:bg-[#007a36]"
                >
                  Confirmer
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

function ProfileStatusBadge({ profile }: { profile: Profile }) {
  const s = (profile.account_status ?? "active") as AccountStatus;
  if (s === "banned") {
    return <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-red-100 text-red-800">Banni</span>;
  }
  if (s === "suspended") {
    const u = profile.suspended_until;
    const extra = u
      ? ` · jusqu’au ${new Date(u).toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" })}`
      : "";
    return <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-900">Suspendu{extra}</span>;
  }
  return <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-800">Actif</span>;
}

function SearchBar({ value, onChange, placeholder }: { value: string; onChange: (v: string) => void; placeholder: string }) {
  return (
    <div className="relative">
      <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full bg-white border border-gray-200 rounded-xl pl-10 pr-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#009A44] shadow-sm"
      />
      {value && (
        <button onClick={() => onChange("")} className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
          <XCircle size={16} />
        </button>
      )}
    </div>
  );
}

function SecurityCard({ icon, color, title, desc, status, ok }: {
  icon: React.ReactNode; color: string; title: string; desc: string; status: string; ok: boolean;
}) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
      <div className="flex items-start justify-between mb-3">
        <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: color + "20", color }}>
          {icon}
        </div>
        <span className={`text-xs font-semibold px-2 py-1 rounded-full ${ok ? "bg-green-100 text-green-700" : "bg-amber-100 text-amber-700"}`}>
          {ok ? <CheckCircle size={12} className="inline mr-1" /> : <AlertTriangle size={12} className="inline mr-1" />}
          {status}
        </span>
      </div>
      <h3 className="font-bold text-gray-900 mb-2">{title}</h3>
      <p className="text-gray-500 text-sm leading-relaxed">{desc}</p>
    </div>
  );
}
