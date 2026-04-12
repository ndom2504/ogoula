"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { supabase } from "@/lib/supabase";
import { Lock, Mail, Eye, EyeOff, AlertCircle } from "lucide-react";
import { OgoulaBrandMark } from "@/components/OgoulaBrandMark";

export default function AdminLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPwd, setShowPwd] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const { data, error: authError } = await supabase.auth.signInWithPassword({ email, password });

    if (authError || !data.user) {
      setError("Identifiants incorrects ou compte non autorisé.");
      setLoading(false);
      return;
    }

    // Vérifie que l'utilisateur a un profil admin (alias commence par "@admin" ou email spécifique)
    const { data: profile } = await supabase
      .from("profiles")
      .select("alias")
      .eq("user_id", data.user.id)
      .single();

    const isAdmin =
      profile?.alias?.toLowerCase().includes("admin") ||
      data.user.email === "info@misterdil.ca" ||
      data.user.email?.endsWith("@ogoula.com");

    if (!isAdmin) {
      await supabase.auth.signOut();
      setError("Accès refusé. Ce compte n'a pas les droits administrateur.");
      setLoading(false);
      return;
    }

    router.push("/admin/dashboard");
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-[#004422] via-[#006b32] to-gray-900 p-6">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mb-4 inline-flex items-center gap-3">
            <OgoulaBrandMark size="lg" variant="white" />
            <span className="text-3xl font-black text-white">Ogoula</span>
          </div>
          <p className="text-sm text-white/70">Portail administrateur</p>
        </div>

        {/* Card */}
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl p-8 border border-white/20 shadow-2xl">
          <h1 className="text-white font-bold text-xl mb-6 flex items-center gap-2">
            <Lock size={20} className="text-[#009A44]" />
            Connexion Admin
          </h1>

          {error && (
            <div className="mb-5 flex items-start gap-3 bg-red-500/20 border border-red-500/40 text-red-200 rounded-xl p-4 text-sm">
              <AlertCircle size={18} className="shrink-0 mt-0.5" />
              {error}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="text-gray-300 text-sm font-medium mb-2 block">Email</label>
              <div className="relative">
                <Mail size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="admin@ogoula.com"
                  className="w-full bg-white/10 border border-white/20 text-white placeholder-gray-500 rounded-xl pl-10 pr-4 py-3 focus:outline-none focus:ring-2 focus:ring-[#009A44] transition"
                />
              </div>
            </div>

            <div>
              <label className="text-gray-300 text-sm font-medium mb-2 block">Mot de passe</label>
              <div className="relative">
                <Lock size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  type={showPwd ? "text" : "password"}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-white/10 border border-white/20 text-white placeholder-gray-500 rounded-xl pl-10 pr-12 py-3 focus:outline-none focus:ring-2 focus:ring-[#009A44] transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPwd(!showPwd)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition"
                >
                  {showPwd ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-[#009A44] text-white py-3 rounded-xl font-bold hover:bg-[#007a36] transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <><span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Connexion…</>
              ) : (
                "Se connecter"
              )}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-white/10 text-center">
            <a href="/" className="text-gray-400 text-sm hover:text-white transition">
              ← Retour au site
            </a>
          </div>
        </div>

        <p className="text-center text-gray-600 text-xs mt-6">
          Accès réservé aux administrateurs Ogoula · © {new Date().getFullYear()}
        </p>
      </div>
    </div>
  );
}
