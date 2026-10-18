import { Check, Circle } from "lucide-react";
import { useLanguage } from "../context/LanguageContext";
import { evaluatePassword } from "../utils/passwordPolicy";

export default function PasswordRequirements({ password }) {
  const { t } = useLanguage();
  const results = evaluatePassword(password);

  return (
    <ul className="mt-2 space-y-1" aria-live="polite">
      {results.map(({ id, labelKey, met }) => (
        <li
          key={id}
          className={[
            "flex items-center gap-2 text-xs transition-colors",
            met
              ? "font-semibold text-emerald-600 dark:text-emerald-400"
              : "text-ink/40 dark:text-cream/40",
          ].join(" ")}
        >
          {met ? <Check size={14} /> : <Circle size={14} />}
          <span>{t(labelKey)}</span>
          <span className="sr-only">
            {met ? t("password.met") : t("password.pending")}
          </span>
        </li>
      ))}
    </ul>
  );
}