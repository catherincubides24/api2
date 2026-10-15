import { useLanguage } from "../../context/LanguageContext";

export default function Footer() {
  const { t } = useLanguage();

  return (
    <footer className="border-t border-ink/10 bg-white/60 dark:border-white/10 dark:bg-slate-900/60">
      <div className="mx-auto flex w-full max-w-7xl flex-col items-center justify-between gap-3 px-4 py-6 text-sm text-ink/70 dark:text-cream/70 sm:flex-row sm:px-6 lg:px-8">
        <p>{t("footer.tagline")}</p>
        <p>{t("footer.description")}</p>
      </div>
    </footer>
  );
}