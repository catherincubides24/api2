import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";

export default function SessionNotice() {
  const { sessionNotice, dismissSessionNotice } = useAuth();
  const { t } = useLanguage();
  if (!sessionNotice) return null;

  return (
    <div
      role="alert"
      className="fixed inset-x-4 top-20 z-50 mx-auto flex max-w-md items-start gap-3 rounded-2xl border border-amber-300 bg-amber-100 px-4 py-3 text-sm text-amber-900 shadow-card"
    >
      <p className="flex-1">{sessionNotice}</p>
      <button
        onClick={dismissSessionNotice}
        className="font-semibold"
        aria-label={t("ticket.close")}
      >
        ✕
      </button>
    </div>
  );
}