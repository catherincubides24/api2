import { GoogleLogin } from "@react-oauth/google";
import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login, loginWithGoogle } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const redirectTo = location.state?.from?.pathname || "/";

  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      await login(form);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo iniciar sesión");
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSuccess = async ({ credential }) => {
    setError("");
    try {
      await loginWithGoogle(credential);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      setError(
        err.response?.data?.message || "No se pudo iniciar sesión con Google"
      );
    }
  };

  return (
    <div className="mx-auto grid max-w-4xl gap-6 rounded-[2rem] border border-white/70 bg-white/85 p-6 shadow-card dark:border-white/10 dark:bg-slate-800/90 md:grid-cols-2 md:p-10">
      <div className="rounded-3xl bg-gradient-to-br from-ink via-dusk to-mint p-7 text-cream">
        <p className="text-xs font-semibold uppercase tracking-[0.25em] text-peach">
          Bienvenido
        </p>
        <h1 className="mt-4 font-display text-4xl">Vuelve a tu cuenta</h1>
        <p className="mt-4 text-sm text-cream/80">
          Inicia sesión para completar pedidos y administrar tus productos favoritos.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-semibold text-ink dark:text-cream">Email</label>
          <input
            type="email"
            required
            value={form.email}
            onChange={(event) =>
              setForm((current) => ({ ...current, email: event.target.value }))
            }
            className="w-full rounded-xl border border-ink/15 px-4 py-2.5 text-ink outline-none ring-coral/30 transition focus:ring dark:border-white/15 dark:bg-slate-900 dark:text-cream"
            placeholder="tu@email.com"
          />
        </div>

        <div>
          <label className="mb-1 block text-sm font-semibold text-ink dark:text-cream">Contraseña</label>
          <input
            type="password"
            required
            value={form.password}
            onChange={(event) =>
              setForm((current) => ({ ...current, password: event.target.value }))
            }
            className="w-full rounded-xl border border-ink/15 px-4 py-2.5 text-ink outline-none ring-coral/30 transition focus:ring dark:border-white/15 dark:bg-slate-900 dark:text-cream"
            placeholder="******"
          />
        </div>

        {error && (
          <div className="rounded-xl border border-coral/30 bg-coral/10 px-3 py-2 text-sm text-coral">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream transition hover:bg-dusk disabled:opacity-60 dark:bg-cream dark:text-ink dark:hover:bg-white"
        >
          {loading ? "Ingresando..." : "Ingresar"}
        </button>

        <div className="flex items-center gap-3 text-xs text-ink/50 dark:text-cream/50">
          <span className="h-px flex-1 bg-ink/10 dark:bg-white/10" />
          o
          <span className="h-px flex-1 bg-ink/10 dark:bg-white/10" />
        </div>

        <div className="flex justify-center">
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={() => setError("Falló el inicio de sesión con Google")}
          />
        </div>

        <p className="pt-2 text-center text-sm text-ink/70 dark:text-cream/70">
          ¿No tienes cuenta?{" "}
          <Link to="/register" className="font-semibold text-coral hover:underline">
            Regístrate aquí
          </Link>
        </p>
      </form>
    </div>
  );
}