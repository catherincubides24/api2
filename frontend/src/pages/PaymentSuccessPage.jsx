import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { paymentService } from "../services/paymentService";

export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const { clearCart } = useCart();

  const [status, setStatus] = useState("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const paypalOrderId = searchParams.get("token"); // PayPal devuelve el token aquí
    const orderId = sessionStorage.getItem("pendingPayPalOrderId"); // lo guardamos antes de redirigir

    if (!paypalOrderId || !orderId) {
      setStatus("error");
      setMessage("Faltan datos del pago. Por favor contacta soporte.");
      return;
    }

    const capture = async () => {
      try {
        const result = await paymentService.capturePayPalOrder(
          paypalOrderId,
          Number(orderId)
        );

        if (result.status === "COMPLETED") {
          clearCart();
          sessionStorage.removeItem("pendingPayPalOrderId");
          setStatus("success");
          setMessage(result.message);
        } else {
          setStatus("error");
          setMessage("El pago no fue completado. Estado: " + result.status);
        }
      } catch (err) {
        setStatus("error");
        setMessage(
          err.response?.data?.message || "Error al confirmar el pago."
        );
      }
    };

    capture();
  }, []);

  return (
    <div className="mx-auto max-w-2xl rounded-3xl bg-white/85 p-10 text-center shadow-card">
      {status === "loading" && (
        <>
          <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-mint border-t-transparent" />
          <h1 className="font-display text-3xl text-ink">Confirmando tu pago...</h1>
          <p className="mt-2 text-ink/70">Por favor espera un momento.</p>
        </>
      )}

      {status === "success" && (
        <>
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-mint/20 text-mint text-3xl">
            ✓
          </div>
          <h1 className="font-display text-4xl text-ink">¡Pago exitoso!</h1>
          <p className="mt-3 text-ink/70">{message}</p>
          <Link
            to="/purchases"
            className="mt-6 inline-block rounded-full bg-ink px-6 py-3 text-sm font-semibold text-cream"
          >
            Ver mis pedidos
          </Link>
        </>
      )}

      {status === "error" && (
        <>
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-coral/20 text-coral text-3xl">
            ✕
          </div>
          <h1 className="font-display text-4xl text-ink">Pago no completado</h1>
          <p className="mt-3 text-ink/70">{message}</p>
          <Link
            to="/cart"
            className="mt-6 inline-block rounded-full bg-ink px-6 py-3 text-sm font-semibold text-cream"
          >
            Volver al carrito
          </Link>
        </>
      )}
    </div>
  );
}