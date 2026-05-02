import { Link } from "react-router-dom";

export default function PaymentCancelPage() {
  return (
    <div className="mx-auto max-w-2xl rounded-3xl bg-white/85 p-10 text-center shadow-card">
      <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 text-amber-600 text-3xl">
        ⚠
      </div>
      <h1 className="font-display text-4xl text-ink">Pago cancelado</h1>
      <p className="mt-3 text-ink/70">
        Cancelaste el proceso de pago. Tu pedido quedó guardado como pendiente.
        Puedes intentarlo de nuevo cuando quieras.
      </p>
      <div className="mt-6 flex justify-center gap-4">
        <Link
          to="/cart"
          className="rounded-full bg-ink px-6 py-3 text-sm font-semibold text-cream"
        >
          Volver al carrito
        </Link>
        <Link
          to="/purchases"
          className="rounded-full border border-ink/20 px-6 py-3 text-sm font-semibold text-ink"
        >
          Ver mis pedidos
        </Link>
      </div>
    </div>
  );
}