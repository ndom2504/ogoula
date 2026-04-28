import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Palette Ogoula - Noir, Blanc, Arc-en-ciel
        ogoula: {
          black: "#000000",
          white: "#FFFFFF",
          red: "#FF3B3B",
          orange: "#FF8C42",
          yellow: "#FFD93D",
          green: "#3BFF8C",
          blue: "#3BA7FF",
          purple: "#A259FF",
          // Variantes utiles
          "dark-bg": "#000000",
          "text-white": "#FFFFFF",
        },
        green: { gabo: "#009A44" }, // Conservé pour compatibilité
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
      },
      backgroundImage: {
        "gradient-rainbow": `linear-gradient(90deg, 
          #FF3B3B 0%, 
          #FF8C42 16.66%, 
          #FFD93D 33.33%, 
          #3BFF8C 50%, 
          #3BA7FF 66.66%, 
          #A259FF 83.33%, 
          #FF3B3B 100%)`,
      },
    },
  },
  plugins: [],
};
export default config;
