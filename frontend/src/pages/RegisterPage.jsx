import { GoogleLogin } from "@react-oauth/google";
import { useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import PasswordRequirements from "../components/PasswordRequirements";
import PrivacyPolicyModal from "../components/PrivacyPolicyModal";
import { useAuth } from "../context/AuthContext";
import { useLanguage } from "../context/LanguageContext";
import { PRIVACY_POLICY_VERSION } from "../data/privacyPolicyContent";
import { isPasswordValid } from "../utils/passwordPolicy";

export default function RegisterPage() {
  const { register, loginWithGoogle } = useAuth();
  const { t } = useLanguage();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [acceptedPolicy, setAcceptedPolicy] = useState(false);
  const [showPolicy, setShowPolicy] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const passwordOk = useMemo(() => isPasswordValid(form.password), [form.password]);
  const passwordsMatch = form.password === form.confirmPassword;
  const canSubmit = passwordOk && passwordsMatch && acceptedPolicy && !loading;

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    // Defensa en profundidad: aunque el botón ya esté deshabilitado
    if (!passwordOk) {
      setError(t("register.passwordPolicyError"));
      return;
    }

    if (!passwordsMatch) {
      setError(t("register.passwordMismatch"));
      return;
    }

    if (!acceptedPolicy) {
      setError(t("register.policyRequired"));
      return;
    }

    setLoading(true);

    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        acceptedDataPolicy: acceptedPolicy,
        dataPolicyVersion: PRIVACY_POLICY_VERSION,
      });
      navigate("/");
    } catch (err) {
      const details = err.response?.data?.details;
      setError(
        details?.password || err.response?.data?.message || t("register.createError")
      );
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async ({ credential }) => {
    if (!acceptedPolicy) {
      setError(t("register.policyRequired"));
      return;
    }

    setError("");
    try {
      await loginWithGoogle(credential);
      navigate("/");
    } catch (err) {
      setError(
        err.response?.data?.message || t("register.googleCreateError")
      );
    }
  };

  const inputClass =
    "w-full rounded-xl border border-ink/15 px-4 py-2.5 text-ink outline-none ring-coral/30 transition focus:ring dark:border-white/15 dark:bg-slate-900 dark:text-cream";
  const labelClass = "mb-1 block text-sm font-semibold text-ink dark:text-cream";

  return (
    <div className="mx-auto grid max-w-4xl gap-6 rounded-[2rem] border border-white/70 bg-white/85 p-6 shadow-card dark:border-white/10 dark:bg-slate-800/90 md:grid-cols-2 md:p-10">
      <form onSubmit={handleSubmit} className="order-2 space-y-4 md:order-1">
        <div>
          <label className={labelClass}>{t("register.fullName")}</label>
          <input
            type="text"
            required
            maxLength={100}
            value={form.fullName}
            onChange={(event) =>
              setForm((current) => ({ ...current, fullName: event.target.value }))
            }
            className={inputClass}
            placeholder={t("register.fullNamePlaceholder")}
          />
        </div>

        <div>
          <label className={labelClass}>{t("register.email")}</label>
          <input
            type="email"
            required
            maxLength={255}
            value={form.email}
            onChange={(event) =>
              setForm((current) => ({ ...current, email: event.target.value }))
            }
            className={inputClass}
            placeholder="tu@email.com"
          />
        </div>

        <div>
          <label className={labelClass}>{t("register.password")}</label>
          <input
            type="password"
            required
            autoComplete="new-password"
            value={form.password}
            onChange={(event) =>
              setForm((current) => ({ ...current, password: event.target.value }))
            }
            className={inputClass}
            placeholder={t("register.passwordPlaceholder")}
          />
          <PasswordRequirements password={form.password} />
        </div>

        <div>
          <label className={labelClass}>{t("register.confirmPassword")}</label>
          <input
            type="password"
            required
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                confirmPassword: event.target.value,
              }))
            }
            className={inputClass}
            placeholder={t("register.confirmPasswordPlaceholder")}
          />
          {form.confirmPassword && !passwordsMatch && (
            <p className="mt-1 text-xs text-coral">{t("register.passwordMismatch")}</p>
          )}
        </div>

        <label className="flex items-start gap-2 rounded-xl bg-sand/60 p-3 text-sm text-ink/80 dark:bg-slate-900/60 dark:text-cream/80">
          <input
            type="checkbox"
            checked={acceptedPolicy}
            onChange={(event) => setAcceptedPolicy(event.target.checked)}
            className="mt-1"
            required
          />
          <span>
            {t("register.acceptPolicy")}{" "}
            <button
              type="button"
              onClick={() => setShowPolicy(true)}
              className="font-semibold text-coral underline"
            >
              {t("register.policyLink")}
            </button>
          </span>
        </label>

        {error && (
          <div className="rounded-xl border border-coral/30 bg-coral/10 px-3 py-2 text-sm text-coral">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={!canSubmit}
          className="w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream transition hover:bg-dusk disabled:cursor-not-allowed disabled:opacity-50 dark:bg-cream dark:text-ink dark:hover:bg-white"
        >
          {loading ? t("register.submitting") : t("register.submit")}
        </button>

        <div className="flex items-center gap-3 text-xs text-ink/50 dark:text-cream/50">
          <span className="h-px flex-1 bg-ink/10 dark:bg-white/10" />
          {t("register.or")}
          <span className="h-px flex-1 bg-ink/10 dark:bg-white/10" />
        </div>

        <div
          className={
            acceptedPolicy
              ? "flex justify-center"
              : "flex justify-center opacity-50 pointer-events-none"
          }
        >
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={() => setError(t("register.googleError"))}
          />
        </div>

        <p className="pt-2 text-center text-sm text-ink/70 dark:text-cream/70">
          {t("register.hasAccount")}{" "}
          <Link to="/login" className="font-semibold text-coral hover:underline">
            {t("register.loginHere")}
          </Link>
        </p>
      </form>

      <div className="order-1 rounded-3xl bg-gradient-to-br from-coral via-peach to-mint p-7 text-ink md:order-2">
        <p className="text-xs font-semibold uppercase tracking-[0.25em]">{t("register.eyebrow")}</p>
        <h1 className="mt-4 font-display text-4xl leading-tight">
          {t("register.title")}
        </h1>
        <p className="mt-4 text-sm text-ink/80">
          {t("register.subtitle")}
        </p>
      </div>

      <PrivacyPolicyModal open={showPolicy} onClose={() => setShowPolicy(false)} />
    </div>
  );
}