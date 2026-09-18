import { Moon, Sun } from "lucide-react";
import { useTheme } from "../context/ThemeContext";

export default function ThemeToggle({ className = "" }) {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === "dark";

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={isDark ? "Cambiar a modo claro" : "Cambiar a modo oscuro"}
      title={isDark ? "Modo claro" : "Modo oscuro"}
      className={[
        "inline-flex h-10 w-10 items-center justify-center rounded-full border transition",
        "border-ink/15 bg-white/80 text-ink hover:bg-white",
        "dark:border-white/15 dark:bg-slate-800 dark:text-cream dark:hover:bg-slate-700",
        className,
      ].join(" ")}
    >
      {isDark ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}