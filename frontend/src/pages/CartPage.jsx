import { Minus, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router-dom";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import { orderService } from "../services/orderService";
import { paymentService } from "../services/paymentService";
import { formatCurrency } from "../utils/formatCurrency";

export default function CartPage() {
  const { items, total, updateQuantity, removeFromCart, clearCart } = useCart();
  const { user } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState("");

  const handleCheckout = async () => {
    if (items.length === 0) return;
    if (!user) {
      setFeedback("Debes iniciar sesión para continuar.");
      return;
    }

    setIsSubmitting(true);
    setFeedback("");

    try {
      // PASO 1: Crear el pedido en tu base de datos
      const order = await orderService.createOrder({
        userId: user.id, // ahora seguro porque verificamos user arriba
        status: "PENDING",
        items: items.map((item) => ({
          productId: item.id,
          quantity: item.quantity,
        })),
      });

      // PASO 2: Guardar el orderId para recuperarlo después del pago
      sessionStorage.setItem("pendingPayPalOrderId", String(order.id));

      // PASO 3: Crear la orden en PayPal y obtener la URL de aprobación
      const paypal = await paymentService.createPayPalOrder(order.id);

      // PASO 4: Redirigir al usuario a PayPal
      window.location.href = paypal.approvalUrl;
    } catch (err) {
      setFeedback(
        err.response?.data?.message ||
          "No se pudo iniciar el pago. Inténtalo de nuevo."
      );
      setIsSubmitting(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl rounded-3xl bg-white/80 p-8 text-center shadow-soft">
        <h1 className="font-display text-4xl text-ink">Tu carrito está vacío</h1>
        <p className="mt-3 text-ink/70">
          Explora productos y agrega lo que necesites para tu mascota.
        </p>
        <Link
          to="/"
          className="mt-6 inline-block rounded-full bg-ink px-5 py-3 text-sm font-semibold text-cream"
        >
          Volver al catálogo
        </Link>
      </div>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
      <section className="space-y-4 rounded-3xl bg-white/85 p-6 shadow-card">
        <SectionTitle
          eyebrow="Carrito"
          title="Resumen de tu pedido"
          description="Ajusta cantidades antes de confirmar la compra."
        />

        <div className="space-y-4">
          {items.map((item) => (
            <article
              key={item.id}
              className="grid gap-3 rounded-2xl border border-ink/10 bg-white p-4 sm:grid-cols-[90px_1fr_auto] sm:items-center"
            >
              <img
                src={item.imageUrl}
                alt={item.name}
                className="h-20 w-full rounded-xl object-cover sm:w-20"
              />

              <div>
                <h3 className="font-semibold text-ink">{item.name}</h3>
                <p className="text-sm text-ink/60">{formatCurrency(item.price)} c/u</p>
                <p className="text-sm font-semibold text-coral">
                  Subtotal: {formatCurrency(Number(item.price) * item.quantity)}
                </p>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-center">
                <button
                  onClick={() => updateQuantity(item.id, item.quantity - 1)}
                  className="rounded-full border border-ink/20 p-2 text-ink"
                  aria-label="Reducir cantidad"
                >
                  <Minus size={16} />
                </button>
                <span className="min-w-8 text-center text-sm font-semibold">
                  {item.quantity}
                </span>
                <button
                  onClick={() => updateQuantity(item.id, item.quantity + 1)}
                  className="rounded-full border border-ink/20 p-2 text-ink"
                  aria-label="Aumentar cantidad"
                >
                  <Plus size={16} />
                </button>
                <button
                  onClick={() => removeFromCart(item.id)}
                  className="rounded-full border border-coral/30 p-2 text-coral"
                  aria-label="Eliminar producto"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <aside className="h-fit space-y-4 rounded-3xl bg-gradient-to-br from-ink via-dusk to-mint p-6 text-cream shadow-card">
        <h2 className="font-display text-3xl">Total</h2>
        <p className="text-4xl font-extrabold">{formatCurrency(total)}</p>
        <p className="text-sm text-cream/80">
          Cliente: <span className="font-semibold">{user?.fullName}</span>
        </p>

        <button
          onClick={handleCheckout}
          disabled={isSubmitting || !user}
          className="w-full rounded-xl bg-coral px-4 py-3 text-sm font-semibold text-white transition hover:bg-coral/90 disabled:opacity-60"
        >
          {isSubmitting ? (
            "Redirigiendo a PayPal..."
          ) : (
            <span className="flex items-center justify-center gap-2">
              <img
                src="https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg"
                alt="PayPal"
                className="h-5 rounded"
              />
              Pagar con PayPal
            </span>
          )}
        </button>

        {feedback && (
          <p className="rounded-xl bg-white/15 px-3 py-2 text-sm text-cream">
            {feedback}
          </p>
        )}

        <button
          onClick={clearCart}
          className="w-full rounded-xl border border-white/35 px-4 py-3 text-sm font-semibold text-cream/90 transition hover:bg-white/10"
        >
          Vaciar carrito
        </button>
      </aside>
    </div>
  );
}