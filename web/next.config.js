/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "vqthzwhuvcglwjwmqeil.supabase.co",
      },
  ],
  },
  /**
   * URLs propres /privacy et /delete-account → fichiers dans public/.
   * Les rewrites beforeFiles passent avant le routage App : utile si l'hébergeur
   * ou un cache sert mal les routes App (404 Play Console alors que le build est bon).
   */
  async rewrites() {
    return {
      beforeFiles: [
        { source: "/privacy", destination: "/privacy.html" },
        { source: "/delete-account", destination: "/delete-account.html" },
      ],
    };
  },
};

module.exports = nextConfig;
