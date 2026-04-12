/**
 * Marque Ogoula : trois pastilles (couleurs d'identité ou blanc sur fond vert).
 */
export function OgoulaBrandMark({
  size = "md",
  variant = "color",
}: {
  size?: "sm" | "md" | "lg";
  /** "white" pour barre de navigation sur fond vert */
  variant?: "color" | "white";
}) {
  const dim =
    size === "sm" ? "w-2 h-2" : size === "lg" ? "w-3.5 h-3.5" : "w-2.5 h-2.5";
  const gap = size === "sm" ? "gap-1" : size === "lg" ? "gap-2" : "gap-1.5";
  if (variant === "white") {
    return (
      <span className={`inline-flex items-center ${gap}`} aria-hidden title="Ogoula">
        <span className={`${dim} rounded-full bg-white`} />
        <span className={`${dim} rounded-full bg-white/85`} />
        <span className={`${dim} rounded-full bg-white/70`} />
      </span>
    );
  }
  return (
    <span className={`inline-flex items-center ${gap}`} aria-hidden title="Ogoula">
      <span className={`${dim} rounded-full bg-[#22c55e]`} />
      <span className={`${dim} rounded-full bg-[#009A44]`} />
      <span className={`${dim} rounded-full bg-[#166534]`} />
    </span>
  );
}
