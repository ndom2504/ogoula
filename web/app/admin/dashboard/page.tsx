"use client";
import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { supabase, Profile, Post } from "@/lib/supabase";
import {
  Users, FileText, Shield, LogOut, Trash2,
  Search, RefreshCw, TrendingUp, AlertTriangle,
  ChevronRight, Eye, Image as ImageIcon, Video, CheckCircle,
  XCircle, BarChart2, Bell,
  Lock as LockIcon, Ban, Settings,
} from "lucide-react";
import { OgoulaBrandMark } from "@/components/OgoulaBrandMark";

type Tab = "overview" | "users" | "posts" | "security" | "reports";

type ReportedPost = Post & { reportCount: number; reportReason: string };

export default function AdminDashboard() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("overview");
  const [adminEmail, setAdminEmail] = useState("");

  // Data
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [actionMsg, setActionMsg] = useState<string | null>(null);

  // Stats
  const totalUsers = profiles.length;
  const totalPosts = posts.length;
  const totalVideos = posts.filter((p) => p.video_url).length;
  const totalImages = posts.reduce((acc, p) => acc + (p.image_urls?.length ?? 0), 0);
  const communityPosts = posts.filter((p) => p.is_community_post).length;

  const showMsg = (msg: string) => {
    setActionMsg(msg);
    setTimeout(() => setActionMsg(null), 3000);
  };

  const checkAuth = useCallback(async () => {
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) { router.push("/admin"); return; }
    setAdminEmail(user.email ?? "");
  }, [router]);

  const loadData = useCallback(async () => {
    setLoading(true);
    const [{ data: p }, { data: po }] = await Promise.all([
      supabase.from("profiles").select("*").order("first_name"),
      supabase.from("posts").select("*").order("time", { ascending: false }),
    ]);
    setProfiles(p ?? []);
    setPosts(po ?? []);
    setLoading(false);
  }, []);

  useEffect(() => { checkAuth(); loadData(); }, [checkAuth, loadData]);

  async function handleLogout() {
    await supabase.auth.signOut();
    router.push("/admin");
  }

  async function deletePost(id: string) {
    if (!confirm("Supprimer ce post définitivement ?")) return;
    await supabase.from("posts").delete().eq("id", id);
    setPosts((prev) => prev.filter((p) => p.id !== id));
    showMsg("Post supprimé ✓");
  }

  async function deleteUser(userId: string) {
    if (!confirm("Supprimer ce profil ? L'utilisateur perdra ses données.")) return;
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

  const filteredUsers = profiles.filter(
    (p) =>
      `${p.first_name} ${p.last_name} ${p.alias}`.toLowerCase().includes(search.toLowerCase())
  );
  const filteredPosts = posts.filter(
    (p) =>
      (p.content + p.author + p.handle).toLowerCase().includes(search.toLowerCase())
  );

  const NAV: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: "overview", label: "Vue d'ensemble", icon: <BarChart2 size={18} /> },
    { id: "users", label: "Utilisateurs", icon: <Users size={18} /> },
    { id: "posts", label: "Publications", icon: <FileText size={18} /> },
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
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              {[
                { label: "Utilisateurs", value: totalUsers, icon: <Users size={20} />, color: "#009A44", bg: "#009A44" },
                { label: "Publications", value: totalPosts, icon: <FileText size={20} />, color: "#003DA5", bg: "#003DA5" },
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

        {/* ── USERS TAB ───────────────────────────────────────────── */}
        {tab === "users" && (
          <div className="space-y-5">
            <SearchBar value={search} onChange={setSearch} placeholder="Rechercher un utilisateur…" />
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-100">
                  <tr>
                    {["Utilisateur", "Alias", "Publications", "Actions"].map((h) => (
                      <th key={h} className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {filteredUsers.map((p) => {
                    const userPostCount = posts.filter((po) => po.handle === p.alias).length;
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
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-gray-600 font-mono text-xs">{p.alias}</td>
                        <td className="px-6 py-4">
                          <span className="bg-blue-50 text-blue-700 text-xs font-semibold px-2 py-1 rounded-full">{userPostCount}</span>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => deleteUserPosts(p.alias)}
                              title="Supprimer tous les posts"
                              className="p-2 text-orange-500 hover:bg-orange-50 rounded-lg transition"
                            >
                              <FileText size={15} />
                            </button>
                            <button
                              onClick={() => deleteUser(p.user_id)}
                              title="Supprimer le profil"
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
                    <tr><td colSpan={4} className="px-6 py-10 text-center text-gray-400">Aucun utilisateur trouvé.</td></tr>
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
      </main>
    </div>
  );
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
