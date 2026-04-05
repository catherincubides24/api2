import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
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

  return (
    <div className="mx-auto grid max-w-4xl gap-6 rounded-[2rem] border border-white/70 bg-white/85 p-6 shadow-card md:grid-cols-2 md:p-10">
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
          <label className="mb-1 block text-sm font-semibold">Email</label>
          <input
            type="email"
            required
            value={form.email}
            onChange={(event) =>
              setForm((current) => ({ ...current, email: event.target.value }))
            }
            className="w-full rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
            placeholder="tu@email.com"
          />
        </div>

        <div>
          <label className="mb-1 block text-sm font-semibold">Contraseña</label>
          <input
            type="password"
            required
            value={form.password}
            onChange={(event) =>
              setForm((current) => ({ ...current, password: event.target.value }))
            }
            className="w-full rounded-xl border border-ink/15 px-4 py-2.5 outline-none ring-coral/30 transition focus:ring"
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
          className="w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream transition hover:bg-dusk disabled:opacity-60"
        >
          {loading ? "Ingresando..." : "Ingresar"}
        </button>

        <p className="text-center text-sm text-ink/70">
          ¿No tienes cuenta?{" "}
          <Link to="/register" className="font-semibold text-coral hover:underline">
            Regístrate aquí
          </Link>
        </p>
      </form>
    </div>
  );
}
