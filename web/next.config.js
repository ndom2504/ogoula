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
};

module.exports = nextConfig;
