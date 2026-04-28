/**
 * Marque Ogoula : logo O blanc avec halo arc-en-ciel.
 * Couleurs : Noir (#000000) background, Blanc (#FFFFFF) logo, dégradé arc-en-ciel halo
 */
export function OgoulaBrandMark({
  size = "md",
  variant = "color",
}: {
  size?: "sm" | "md" | "lg";
  /** "white" pour barre de navigation sur fond noir */
  variant?: "color" | "white";
}) {
  const dim =
    size === "sm" ? "w-4 h-4" : size === "lg" ? "w-7 h-7" : "w-5 h-5";
  const strokeWidth = size === "sm" ? "1.5" : size === "lg" ? "1.2" : "1.3";
  
  if (variant === "white") {
    return (
      <svg
        className={`${dim} text-white`}
        viewBox="0 0 100 100"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <defs>
          <linearGradient id="rainbowGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#FF3B3B" />
            <stop offset="16.66%" stopColor="#FF8C42" />
            <stop offset="33.33%" stopColor="#FFD93D" />
            <stop offset="50%" stopColor="#3BFF8C" />
            <stop offset="66.66%" stopColor="#3BA7FF" />
            <stop offset="83.33%" stopColor="#A259FF" />
            <stop offset="100%" stopColor="#FF3B3B" />
          </linearGradient>
        </defs>
        
        {/* Halo arc-en-ciel */}
        <circle cx="50" cy="50" r="48" stroke="url(#rainbowGradient)" strokeWidth={strokeWidth} opacity="0.8" />
        
        {/* Logo O blanc */}
        <circle cx="50" cy="50" r="35" fill="none" stroke="white" strokeWidth={strokeWidth} />
      </svg>
    );
  }
  
  // Variante "color" (sur fond noir avec dégradé)
  return (
    <svg
      className={`${dim} text-white`}
      viewBox="0 0 100 100"
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <defs>
        <linearGradient id="rainbowGradientColor" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#FF3B3B" />
          <stop offset="16.66%" stopColor="#FF8C42" />
          <stop offset="33.33%" stopColor="#FFD93D" />
          <stop offset="50%" stopColor="#3BFF8C" />
          <stop offset="66.66%" stopColor="#3BA7FF" />
          <stop offset="83.33%" stopColor="#A259FF" />
          <stop offset="100%" stopColor="#FF3B3B" />
        </linearGradient>
      </defs>
      
      {/* Halo arc-en-ciel */}
      <circle cx="50" cy="50" r="48" stroke="url(#rainbowGradientColor)" strokeWidth={strokeWidth} opacity="0.9" />
      
      {/* Logo O blanc */}
      <circle cx="50" cy="50" r="35" fill="none" stroke="white" strokeWidth={strokeWidth} />
    </svg>
  );
}
