import { useLanguage } from "../context/LanguageContext";

export default function LanguageToggle({ className = "" }) {
  const { language, toggleLanguage, t } = useLanguage();
  const isEn = language === "en";

  return (
    <button
      type="button"
      onClick={toggleLanguage}
      aria-label={isEn ? t("language.switchToEs") : t("language.switchToEn")}
      title={isEn ? t("language.switchToEs") : t("language.switchToEn")}
      className={[
        "inline-flex h-8 w-12 items-center justify-center rounded-full border text-xs font-bold transition",
        "border-ink/15 bg-transparent text-ink/70 hover:bg-white/70 hover:text-ink",
        "dark:border-white/15 dark:text-cream/60 dark:hover:bg-slate-800 dark:hover:text-cream",
        className,
      ].join(" ")}
    >
      <span className="tracking-wide uppercase">{isEn ? "ES" : "EN"}</span>
    </button>
  );
}
