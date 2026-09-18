import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="mx-auto max-w-3xl rounded-3xl bg-white/85 p-10 text-center shadow-card dark:bg-slate-800/90">
      <p className="text-xs font-semibold uppercase tracking-[0.25em] text-coral">
        Error 404
      </p>
      <h1 className="mt-4 font-display text-5xl text-ink dark:text-cream">Página no encontrada</h1>
      <p className="mt-3 text-ink/70 dark:text-cream/70">
        La ruta que buscas no existe o fue movida.
      </p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-full bg-ink px-5 py-3 text-sm font-semibold text-cream dark:bg-cream dark:text-ink"
      >
        Volver al inicio
      </Link>
    </div>
  );
}