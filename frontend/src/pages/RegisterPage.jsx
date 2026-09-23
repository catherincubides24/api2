import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import PrivacyPolicyModal from "../components/PrivacyPolicyModal";
import { PRIVACY_POLICY_VERSION } from "../data/privacyPolicyContent";

export default function RegisterPage() {
  const { register } = useAuth();
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

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");

    if (form.password !== form.confirmPassword) {
      setError("Las contraseñas no coinciden");
      return;
    }

    if (!acceptedPolicy) {
      setError("Debes aceptar la política de tratamiento de datos para continuar");
      return;
    }

    setLoading(true);

    try {
      await register({
        fullName: form.fullName,
        email: form.email,
        password: form.password,
        role: "CUSTOMER",
        acceptedDataPolicy: acceptedPolicy,
        dataPolicyVersion: PRIVACY_POLICY_VERSION,
      });
      navigate("/");
    } catch (err) {
      setError(err.response?.data?.message || "No se pudo crear la cuenta");
    } finally {
      setLoading(false);
    }
  };

  const inputClass =
    "w-full rounded-xl border border-ink/15 px-4 py-2.5 text-ink outline-none ring-coral/30 transition focus:ring dark:border-white/15 dark:bg-slate-900 dark:text-cream";
  const labelClass = "mb-1 block text-sm font-semibold text-ink dark:text-cream";

  return (
    <div className="mx-auto grid max-w-4xl gap-6 rounded-[2rem] border border-white/70 bg-white/85 p-6 shadow-card dark:border-white/10 dark:bg-slate-800/90 md:grid-cols-2 md:p-10">
      <form onSubmit={handleSubmit} className="order-2 space-y-4 md:order-1">
        <div>
          <label className={labelClass}>Nombre completo</label>
          <input
            type="text"
            required
            value={form.fullName}
            onChange={(event) =>
              setForm((current) => ({ ...current, fullName: event.target.value }))
            }
            className={inputClass}
            placeholder="Nombre Apellido"
          />
        </div>

        <div>
          <label className={labelClass}>Email</label>
          <input
            type="email"
            required
            value={form.email}
            onChange={(event) =>
              setForm((current) => ({ ...current, email: event.target.value }))
            }
            className={inputClass}
            placeholder="tu@email.com"
          />
        </div>

        <div>
          <label className={labelClass}>Contraseña</label>
          <input
            type="password"
            required
            minLength={6}
            value={form.password}
            onChange={(event) =>
              setForm((current) => ({ ...current, password: event.target.value }))
            }
            className={inputClass}
            placeholder="Mínimo 6 caracteres"
          />
        </div>

        <div>
          <label className={labelClass}>Confirmar contraseña</label>
          <input
            type="password"
            required
            minLength={6}
            value={form.confirmPassword}
            onChange={(event) =>
              setForm((current) => ({
                ...current,
                confirmPassword: event.target.value,
              }))
            }
            className={inputClass}
            placeholder="Repite tu contraseña"
          />
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
            He leído y acepto la{" "}
            <button
              type="button"
              onClick={() => setShowPolicy(true)}
              className="font-semibold text-coral underline"
            >
              Política de Tratamiento de Datos Personales
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
          disabled={loading || !acceptedPolicy}
          className="w-full rounded-xl bg-ink px-4 py-3 text-sm font-semibold text-cream transition hover:bg-dusk disabled:opacity-60 dark:bg-cream dark:text-ink dark:hover:bg-white"
        >
          {loading ? "Creando cuenta..." : "Crear cuenta"}
        </button>

        <p className="text-center text-sm text-ink/70 dark:text-cream/70">
          ¿Ya tienes cuenta?{" "}
          <Link to="/login" className="font-semibold text-coral hover:underline">
            Inicia sesión
          </Link>
        </p>
      </form>

      <div className="order-1 rounded-3xl bg-gradient-to-br from-coral via-peach to-mint p-7 text-ink md:order-2">
        <p className="text-xs font-semibold uppercase tracking-[0.25em]">Registro</p>
        <h1 className="mt-4 font-display text-4xl leading-tight">
          Crea tu cuenta en menos de un minuto.
        </h1>
        <p className="mt-4 text-sm text-ink/80">
          Guarda tus pedidos, arma carrito y explora nuevos productos para tu
          mascota.
        </p>
      </div>

      <PrivacyPolicyModal open={showPolicy} onClose={() => setShowPolicy(false)} />
    </div>
  );
}