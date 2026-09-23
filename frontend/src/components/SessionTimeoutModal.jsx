import { useState } from "react";

function formatCountdown(ms) {
  const totalSeconds = Math.max(Math.ceil(ms / 1000), 0);
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, "0");
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

export default function SessionTimeoutModal({ remainingMs, onConfirm, onLogout }) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);
    try {
      await onConfirm(password);
    } catch (err) {
      setError(err.response?.data?.message || "Contraseña incorrecta");
      setPassword("");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[60] flex items-center justify-center bg-ink/70 p-4 backdrop-blur-sm"
    >
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-sm space-y-4 rounded-3xl bg-white p-6 shadow-card dark:bg-slate-800"
      >
        <div className="space-y-1 text-center">
          <h2 className="font-display text-2xl text-ink dark:text-cream">
            ¿Sigues ahí?
          </h2>
          <p className="text-sm text-ink/70 dark:text-cream/70">
            Por inactividad, tu sesión se cerrará en
          </p>
          <p className="text-3xl font-extrabold text-coral">
            {formatCountdown(remainingMs)}
          </p>
        </div>

        <input
          type="password"
          required
          autoFocus
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="Escribe tu contraseña para continuar"
          className="w-full rounded-xl border border-ink/15 px-4 py-2.5 text-ink outline-none ring-coral/30 transition focus:ring dark:border-white/15 dark:bg-slate-900 dark:text-cream"
        />

        {error && (
          <div className="rounded-xl border border-coral/30 bg-coral/10 px-3 py-2 text-sm text-coral">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream transition hover:bg-dusk disabled:opacity-60 dark:bg-cream dark:text-ink"
        >
          {loading ? "Verificando..." : "Continuar sesión"}
        </button>
        <button
          type="button"
          onClick={onLogout}
          className="w-full rounded-xl border border-ink/20 px-4 py-2.5 text-sm font-semibold text-ink dark:border-white/20 dark:text-cream"
        >
          Cerrar sesión
        </button>
      </form>
    </div>
  );
}